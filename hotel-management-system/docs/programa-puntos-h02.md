# H-02 — Programa HotelUQ: contrato y configuración

Fuente principal: documento oficial, §4, página física/impresa 7. El ejemplo 100 puntos/100.000 COP no es una tarifa obligatoria. Tampoco están aprobadas las propuestas numéricas de `analisis.md`. **No hay tasa ni beneficios activos por defecto.** No se cambiaron las tasas oficiales de cancelación.

## Componentes y operaciones

`LoyaltyService` es la fachada autenticada. Usa la misma instancia de `Hotel`, sesión y monitor que las reservas/tarjetas. `PointsLedger` mantiene las cuentas lógicas por ID de cliente: una consulta autorizada devuelve una cuenta vacía incluso antes del primer pago; no requiere duplicar el cliente ni crear otra identidad. `LoyaltyPolicy` especifica acumulación y reversión. `Benefit` especifica una equivalencia versionada. `LoyaltyCatalog` mantiene referencias válidas y paquetes; `ServiceComponent.Leaf/Package` implementan Composite con servicios o paquetes hijos.

- `configure(admin, policy)`: configurar explícitamente la tasa y las tres decisiones de cancelación. La misma combinación ID/versión no puede sobrescribirse con otros valores. Los lotes conservan la política completa utilizada.
- `benefit(admin, benefit)`: registrar equivalencia por beneficio y versión. Los cambios de una versión existente o retrocesos de versión se rechazan. Los canjes conservan el snapshot anterior.
- `account(owner/admin, clientId)`: saldo utilizable, deuda, lotes con origen/fechas, canjes y eventos. Otro cliente no puede leerlos.
- `createPackage(admin, name, description, serviceIds, childPackageIds)`: composición inmutable por referencias registradas; rechaza vacío, duplicados en todo el árbol, servicios ausentes/no disponibles y paquetes inexistentes. Solo puede referir paquetes previamente creados e inmutables: los ciclos no son construibles. No hay un setter que permita introducir ciclos después. La eliminación de un servicio referenciado queda bloqueada.
- `packages/benefits`: consultas autenticadas. El precio del paquete suma precios intermedios decimales de sus hojas y redondea una vez en COP, HALF_UP; la disponibilidad depende de todas las hojas. No incluye descuentos comerciales implícitos. Los servicios conservan sus controles anteriores de modificación e historial.
- `redeem(token, reservationId, expectedVersion, key, benefitId)`: **confirma y paga una reserva PENDING aplicando un beneficio**, no emite un vale independiente. Un beneficio por reserva. Descuentos, servicios y paquetes son operativos cuando se configure su equivalencia. Un servicio ya seleccionado se bonifica una vez, sin duplicarlo. Los servicios de paquetes se integran en el contrato y se validan de nuevo al pagar.
- `deferred/settle`: sin tasa, el pago genera una referencia pendiente, no puntos ficticios. Habilitar una tasa no acredita retroactivamente. Solo una liquidación administrativa explícita acredita el pago pendiente bajo la política activa, preservando instante y vencimiento originales. Pagos ya cancelados quedan excluidos de esa liquidación; no se inventa la tasa histórica que habrían tenido. Una liquidación repetida no acredita dos veces.

No existe una API pública del agregado para acreditar puntos arbitrariamente o pasar un reloj/importe de acumulación desde una solicitud. Instanciar un `PointsLedger` independiente no da acceso al libro privado del hotel. Las recargas y los pagos fallidos no otorgan puntos.

## Acumulación y vencimiento

La base es `Payment.amount` efectivamente debitado y aprobado, después de descuentos por puntos y redondeo COP; incluye servicios pagados dentro de la reserva. No se acreditan puntos sobre servicios gratuitos ni sobre la parte pagada con puntos. La fórmula implementada es `floor(importeCOP * pointsPerUnit / paidUnit)` por pago, con aritmética BigDecimal y puntos enteros `long` con operaciones exactas. No hay arrastre de fracciones entre pagos. Esta semántica debe ser aceptada junto a la tasa antes de habilitar producción. Los desbordamientos rechazan la operación y activan rollback; no crean saldos truncados.

Cada lote identifica cliente, reserva, pago original, importe en su comprobante, puntos originales/derecho vigente/usados, política y dos instantes inequívocos. Vence en el aniversario calendario local de Bogotá: `obtainedAt.atZone(America/Bogota).plusYears(1).toInstant()`. El 29/02 vence el 28/02 siguiente a la misma hora local. Está vigente estrictamente antes del instante de vencimiento y no a partir de ese instante. No depende de la zona del servidor ni equivale siempre a 365 días.

El saldo y la redención excluyen los lotes vencidos sin necesidad de un proceso programado. La primera consulta/redención que observa un vencimiento registra un evento EXPIRED con el instante efectivo original y el de observación. Repetirlo no duplica eventos. Se conserva el lote histórico, incluso cuando sus puntos vencieron o se revocaron.

La redención utiliza FEFO: vencimiento, fecha de obtención y UUID como desempate estable. Valida saldo antes de consumir. Cada asignación del canje referencia un lote concreto. No se financia un canje con los puntos que produciría su propio pago.

## Cancelaciones y puntos: opciones explícitas, pendientes de aprobación

La configuración exige las tres decisiones siguientes; ninguna se activa automáticamente:

1. `REVOKE_ALL_ON_CANCELLATION`: revocar todos los puntos ganados por el pago cancelado; o `RECALCULATE_RETAINED_PAYMENT`: recalcular los puntos sobre el pago original menos el reembolso efectivamente registrado. No se multiplica un saldo supuesto por el porcentaje; se usa el importe real devuelto, incluido su redondeo.
2. `CARRY_FORWARD_DEBT`: si los puntos de ese pago se gastaron, conservar la insuficiencia como deuda de puntos, sin saldo utilizable negativo y sin bloquear la devolución monetaria oficial. El libro calcula el déficit de los lotes revocados y reserva contra él los excedentes de lotes vivos, en FEFO, antes de permitir nuevos canjes. **Es una compensación dinámica, no una amortización definitiva:** si un crédito compensador vence o se revoca antes de cerrar el déficit, la deuda vuelve a quedar descubierta. Esta semántica también requiere aprobación; no genera un pago monetario ni prolonga vigencias.
3. `KEEP_SPENT`: mantener el canje consumido al cancelar su reserva; o `RESTORE_ORIGINAL_EXPIRY`: liberar exactamente sus asignaciones originales. No se cambia la fecha de vencimiento, no se resucitan puntos ya revocados ni se regalan puntos nuevos. Si el lote venció, la restauración se registra, pero no vuelve a ser gastable. Esta decisión se guarda en el canje, no se toma de la política actual al cancelar.

Las pruebas eligen opciones **solo como fixtures**. Las preguntas comerciales continúan abiertas. La cancelación monetaria oficial no depende de que exista configuración de puntos. Al cancelar pagos diferidos se marca su origen como cancelado sin inventar puntos históricos.

## Atomicidad, idempotencia y trazabilidad

La confirmación y cancelación ejecutan dentro del mismo monitor de `Hotel`. `bookingAtomic` toma checkpoints de reserva, tarjeta y libro de puntos completo antes de mutar. Ante RuntimeException/Error restaura términos, estado, saldo, movimientos, recibos, lotes, canjes, diferidos y eventos. Los tres hooks de fallo existentes se conservan. El consumo y la concesión del beneficio ocurren antes del cargo, dentro del mismo bloque reversible; la acumulación ocurre después de registrar el pago real y antes del último hook. Los ajustes de puntos ocurren después de registrar el reembolso y antes del último hook.

La clave natural de acumulación es el UUID del pago; la de devolución monetaria sigue siendo la reserva y su primer reembolso; el ajuste de puntos utiliza el UUID del reembolso. La redención exige una clave no vacía y conserva reserva, cliente, versión esperada, beneficio/versionado, asignaciones e instante. La misma solicitud retorna el resultado registrado sin repetir efectos, incluso después de actualizar una equivalencia. Cambiar clave/beneficio/versión en un reintento es conflicto. Una clave de cliente no puede usarse para otra reserva. La autorización se comprueba antes de devolver cualquier reintento.

El contrato guarda servicios con sus precios originales, el descuento total y el total exigible reconciliado. El evento POINTS_BENEFIT enlaza el UUID del canje y los snapshots antes/después; el canje identifica su equivalencia. `Quote.rawDiscount` incluye aquí la bonificación por puntos; `percent=0` y `promotionId=null` porque no es una promoción. Ni el beneficio ni los puntos permiten evadir la prohibición de total cero tras redondeo. No se aplican beneficios a reservas ya pagadas, canceladas o finalizadas; el replay de un canje ya registrado solo devuelve su estado actual, sin mutarlo.

## Límites y decisiones restantes

- Aprobar tasa, tratamiento de fracciones, equivalencias individuales y las tres decisiones de reversión. No se adoptan automáticamente los ejemplos del docente ni de análisis.
- Los tres beneficios están ligados a una reserva y su pago residual positivo. Combinación de varios canjes, promociones y beneficios independientes requieren reglas adicionales. No se inició promociones.
- Las compras de servicios dentro de reservas acumulan sobre su pago real. **No hay todavía una compra autónoma fuera de una reserva**; este alcance de RF-008/RF-025/RF-027 propuesto sigue parcial y requiere su propio contrato/pago/devolución antes de poder acreditar su origen sin suplantar una reserva.
- Los paquetes se crean y consultan y se canjean; no se implementa venta autónoma ni edición destructiva de paquetes históricos. Su precio se deriva de servicios actuales hasta registrar el contrato; el snapshot pagado no cambia.
- Los puntos se representan con enteros long y controles exactos de desbordamiento: cantidades fuera de rango rechazan y restauran la operación completa; no se truncan ni saturan. Los checkpoints copian el libro entero, con coste lineal en su historial; queda optimización para una futura unidad de trabajo persistente.
- Colecciones, configuraciones, claves idempotentes y todo el historial son exclusivamente de memoria. Un reinicio los pierde; la atomicidad no equivale a durabilidad. La propuesta de persistencia existente debe incluir tablas de lotes, políticas/equivalencias versionadas, asignaciones, deuda/proyección, diferidos, eventos y claves únicas por pago/redención/reembolso; commits únicos con pagos/reservas y pruebas de reinicio/rollback/reintentos. No se añadió base de datos ni migración.
- No hay pantallas JavaFX ni nuevos reportes/exportadores. RF completos de aplicación no se declaran por tener solo lógica Java. No se hicieron commits/push/despliegues.

## Integración posterior H-03

Las restricciones de combinación descritas arriba corresponden al cierre H-02. H-03 añade promociones con reglas explícitas por versión: EXCLUSIVE mantiene el rechazo; los otros dos órdenes solo se permiten si un administrador configura esa capacidad. La acumulación de un pago promocionado también exige una decisión explícita, sin cambiar tasas/equivalencias H-02. Se mantiene un beneficio por reserva y pago residual positivo. El contrato actual preserva la promoción en Quote/BookingView y sus snapshots; `Quote.rawDiscount` puede incluir porcentaje y beneficio, con desglose trazable en la definición y en el canje. Ver [promociones-h03.md](promociones-h03.md). No se reescriben contratos ni políticas históricos.
