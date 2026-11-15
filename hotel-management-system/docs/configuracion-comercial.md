# Configuración comercial — perfil oficial de cancelación y reglas conservadas de fase 3B

La corrección posterior a la auditoría H-01 da prioridad al documento oficial del docente (§3.3) sobre las propuestas anteriores de devolución tardía parcial.

## Reglas aprobadas y activas en código

- Zona oficial única: `HotelTime.ZONE = America/Bogota`. Ninguna operación usa la zona del sistema operativo. El reloj productivo es `Clock.system(HotelTime.ZONE)`; los agregados de prueba reciben un `Clock` y consultan siempre su `instant()` actual, sin convertirlo en un reloj fijo.
- Las fechas de estadía son `LocalDate`; los horarios contractuales se guardan como `Instant` + zona. Pagos, movimientos, reembolsos, sesiones, check-out y auditoría usan instantes inequívocos. Los movimientos presentan su fecha local en Bogotá.
- COP, cálculos `BigDecimal`, obligación final a escala 0 y `RoundingMode.HALF_UP`. No se permiten otras monedas ni importes negativos/no finitos. Los adaptadores públicos `double` preexistentes convierten entradas, pero no participan en la aritmética de precios/saldos/reembolsos.
- No se crea ni confirma una reserva cuyo total final redondeado sea cero. Una promoción del 100 % o un total positivo menor que 0,5 COP se rechaza antes de generar reserva, pago o movimiento. La confirmación revalida importe positivo e integral.
- PENDING permite cambios válidos y exige versión vigente. CONFIRMED no admite cambios directos; una propuesta administrativa puede cotizarse y auditarse, pero su aplicación/cobro/devolución está bloqueada mientras no exista una regla de liquidación aprobada. CANCELLED/COMPLETED no se modifican.
- Cancelación oficial del cliente activa por defecto: más de 72 h 100 %, de 24 a 72 h inclusive 80 %, menos de 24 h 0 %. Solo administrador cancela por hotel con 100 % del pago original elegible. Ninguna configuración alternativa puede modificar esos porcentajes/límites.
- Solo un check-out explícito y autorizado puede pasar CONFIRMED a COMPLETED. Llegar a la fecha/hora prevista no cambia el estado. Solo administrador, pago válido y ventana de check-out explícita. Repetir la misma clave retorna el mismo check-out; una clave diferente tras finalizar se rechaza.

## Parámetros que el responsable todavía debe completar

Los siguientes parámetros siguen pendientes; los porcentajes y límites de cancelación ya están resueltos por el documento oficial:

1. Horas oficiales de entrada y salida. No se eligen automáticamente 15:00/12:00.
2. Ventana de check-out anticipado/tardío autorizada: `earliestOffset` y `latestOffset`, duraciones relativas al instante de salida contractual. Ambos extremos son inclusivos. Una ventana ausente bloquea el check-out; no supone tolerancia ilimitada ni finalización automática.
3. Si se usan metadatos de política propios, elegir ID/versión técnicos opcionales: las ventanas deben ser semánticamente equivalentes al perfil oficial. No se requiere configuración manual para cancelar ni se permite elegir nuevas penalizaciones.
4. Tratamiento económico de modificaciones CONFIRMED: diferencias a favor/en contra, quién puede aprobarlas y eventual reembolso. La aplicación permanece bloqueada.
5. Alcance de persistencia, motor y rango monetario/esquema de migración: propuesta en `propuesta-persistencia.md`. No se instaló una base de datos.

El cobro de cada servicio una vez por reserva conserva la fórmula existente. No se añadieron multiplicadores por noche/persona, TTL de PENDING, plazos comerciales nuevos ni condiciones de promoción (fase 4).

## Inicialización mediante la fachada Java

El proyecto no tiene todavía interfaz, archivo de configuración productiva ni controlador remoto. La configuración se suministra explícitamente por un administrador autenticado, usando valores aprobados por el responsable:

```java
HotelService identity = new HotelService();
ReservationService reservations = new ReservationService();
// Inicializar administrador y obtener adminToken como en fase 2.

BookingRules rules = new BookingRules(checkInTimeApproved, checkOutTimeApproved,
    new BookingRules.CheckoutWindow(earliestOffsetApproved, latestOffsetApproved));
reservations.configure(adminToken, rules);

// El agregado ya tiene este perfil oficial. Esta llamada en el arranque es opcional/idempotente.
reservations.configureCancellation(adminToken, CancellationSchedule.official());
```

Las variables de horas y tolerancia de check-out deben proceder de configuración aprobada. La cancelación tiene un perfil normativo predeterminado, no una elección comercial. No se cargan fixtures desde `src/main`.

Los horarios se configuran una sola vez antes de reservas para no desplazar fechas históricas. Si solo faltaba la ventana de check-out y la hora de salida sí estaba definida, `configureCheckoutWindow(token, window)` permite completarla una vez, sin modificar los instantes contractuales. Si falta la propia hora de salida histórica, el check-out queda bloqueado: no se inventa ni reconstruye silenciosamente esa hora.

El constructor antiguo de `BookingRules` se mantiene como adaptador de compatibilidad, pero rechaza una zona diferente de Bogotá, habilitar total cero o deshabilitar la modificación PENDING. Su antiguo flag de cancelación tardía no activa ninguna política. `allowLateCancellation()` queda deprecado y exige consultar la política versionada, en lugar de sugerir una autorización implícita.

## Semántica oficial de las ventanas de cancelación

Referencia única: `timeLeft = Duration.between(cancelledAt, contractualCheckIn)`. `cancelledAt` se captura una vez del Clock del agregado y se guarda en reembolso/auditoría. La llegada contractual es un Instant previamente guardado con zona `America/Bogota`; no se acepta una hora aportada por el solicitante ni se recalculan fechas históricas.

| Anticipación del cliente | Porcentaje |
|---|---:|
| Más de 72 horas | 100 % |
| Exactamente 72 horas | 80 % |
| Entre 24 y 72 horas | 80 % |
| Exactamente 24 horas | 80 % |
| Menos de 24 horas, incluida llegada exacta | 0 % |
| Tiempo negativo, para estados aún cancelables | 0 % |
| Cancelación del hotel, con cualquier anticipación | 100 % |

**Interpretación documentada del límite de 24 h:** se incluye en la ventana de 24 a 72 h, por coherencia con la franja y con «menos de 24». Es la interpretación del proyecto autorizada para esta corrección; no se atribuye al docente una aclaración adicional. Las 72 h exactas también pertenecen al 80 %.

No se agregó una prohibición o plazo nuevo tras la llegada. Se conserva el conjunto de estados cancelables PENDING/CONFIRMED; al tiempo negativo se aplica la extensión técnica de «menos de 24 h» con 0 %, sin sostener que el PDF haya regulado expresamente el derecho a cancelar después de llegar. CANCELLED retorna la primera decisión de forma idempotente y COMPLETED se rechaza. El hotel mantiene 100 % en cualquier instante para estados cancelables.

`CancellationSchedule.official()` devuelve el perfil `HOTELUQ-ACADEMIC-CANCELLATION/v1`, activo desde la construcción del agregado. Usa una partición completa, ordenada y sin solapamientos:

- `(-infinito, 24h)` → 0 %.
- `[24h, 72h + 1ns)` → 80 %.
- `[72h + 1ns, +infinito)` → 100 %.

`Duration` tiene precisión de nanosegundos: el desplazamiento de un nanosegundo expresa el siguiente valor representable por encima de 72 h, no una tolerancia ni un plazo comercial nuevo. `RefundPolicySelector` comparte los límites oficiales; FullRefundPolicy, PartialRefundPolicy y NoRefundPolicy se conservan. La ruta transaccional utiliza la Strategy seleccionada por el snapshot oficial validado.

`configureCancellation` conserva su firma y exige administrador, versión positiva y una partición completa con porcentajes [0,100]. **Antes de modificar el registro, historial o política activa**, `requireOfficial()` cruza cada ventana candidata con cada ventana oficial y rechaza toda intersección con porcentaje distinto. No se limita a comprobar unas pocas fechas: detecta también segmentos remotos de un nanosegundo. Se admiten particiones equivalentes y escalas como `80.00`, porque conservan la misma función; ID/versión son metadatos trazables, no permiso para cambiar la regla. La ruta de cancelación vuelve a comprobar la política activa antes de escribir.

Las activaciones administrativas conservan la regla de versiones crecientes, contenido inmutable y reintento inocuo de la versión activa idéntica; no se permite reactivar versiones registradas antiguas. El perfil inicial del sistema ya está registrado, sin fabricar una activación atribuida a un administrador. El historial administrativo contiene solo las activaciones efectivamente solicitadas. Cada Refund guarda siempre ID/versión y configuración completa, también cuando se usa el perfil inicial.

Se eliminó la restricción 3B que obligaba a un porcentaje parcial para cualquier ventana negativa. `CancellationSchedule` sigue pudiendo representar datos históricos no oficiales para leerlos y explicar su cálculo; **no pueden activarse ni usarse para nuevos reembolsos del agregado**. No se reescriben comprobantes pasados. El reintento de una cancelación ya registrada retorna su resultado original incluso si después cambia la versión activa; no recalcula ni abona diferencias.

La cancelación del hotel usa `FullRefundPolicy` y `HOTEL_FULL_REFUND/v1`. El reembolso vuelve a la tarjeta del pago original, se calcula sobre lo cobrado elegible no devuelto, redondea una vez a COP entero y no lo supera. Sigue existiendo una sola decisión de cancelación por reserva. Si el cliente ya canceló, llamar después a cancelación por hotel es un reintento sobre CANCELLED, no una nueva cancelación con otro abono.

## Redondeo y trazabilidad monetaria

`CopMoney` centraliza moneda, validación y redondeo. Las tarifas/servicios y el descuento intermedio retienen precisión decimal. El total exigible se calcula **una sola vez** como `round(rawSubtotal - rawDiscount)`. Para que el comprobante entero reconcilie, `subtotal = round(rawSubtotal)` y `discount = subtotal - total`. El contrato conserva también `rawSubtotal/rawDiscount`, de modo que el ajuste de redondeo sea explicable sin alterar la fórmula.

Ejemplo de cálculo, no de política comercial: subtotal bruto 1,50 y descuento bruto 0,75 producen total 1 COP, subtotal final 2 y descuento final conciliado 1. No se redondean repetidamente cada noche/servicio antes de sumarlos.

El reembolso aplica el porcentaje configurado al pago elegible entero y redondea una vez a COP entero; jamás excede el pago elegible. Para importes pequeños, un porcentaje parcial puede redondearse al peso completo o a cero: se conserva el porcentaje y el cálculo histórico, sin inventar otra regla para alterar `HALF_UP`.

Recargas y movimientos también son importes finales COP enteros; una recarga que redondee a cero se rechaza. Los comprobantes, movimientos y cotizaciones exponen moneda e importes exactos. Los getters `double` legados son adaptadores de consulta; deben preferirse `BigDecimal`/`exactBalance` en nuevas integraciones.

## Check-out, auditoría y modificaciones

`checkout(token, reservationId, operationKey)` conserva UUID de operación, clave, instante real, instante previsto, zona, usuario responsable y ventana autorizada. El evento `CHECKED_OUT` usa el mismo UUID/instante. La actualización de State, comprobante y auditoría es atómica y se restaura íntegramente ante fallo. La API anterior `complete` es un alias de una llamada explícita con clave estable, no una tarea programada.

Cada reserva tiene un historial inmutable de creación, cambios PENDING con snapshots antes/después, confirmación, cancelación, propuesta de cambio confirmado y check-out. Confirmación y cancelación enlazan sus UUID de operación con Payment/Refund. No se implementaron observadores, bandejas ni notificaciones de fase 4.

`prepareConfirmedModification(token, id, expectedVersion, reason, request)` exige administrador, motivo, versión, estado CONFIRMED, referencias/disponibilidad/capacidad/fechas válidas; calcula la diferencia COP contra el pago original y devuelve una propuesta auditable con razón explícita de bloqueo. No modifica el contrato ni el saldo. `applyConfirmedModification` rechaza hasta que exista una regla económica aprobada; no cobra ni devuelve dinero por iniciativa propia.

## Historia y límites operativos

Los snapshots existentes no se recalculan por activar otra política ni por cambiar la zona del servidor. No se ejecutó migración de datos ni se reescribieron pruebas/evidencias históricas de fases anteriores. El constructor legado de movimientos con `LocalDateTime` conserva esos campos locales y declara Bogotá para nuevas instancias; cualquier futura importación de timestamps antiguos sin zona debe identificar su procedencia, sin atribuirles retrospectivamente un instante supuesto.

Estado, saldos, auditoría, políticas e idempotencia siguen en memoria. Las sesiones vencen por `Instant`; no hay un TTL aprobado para reservas PENDING ni puntos implementados. No se garantiza recuperación después de cerrar la JVM, y no se presenta esta fase como persistencia durable.

## H-02 — Configuración de puntos y beneficios

Consultar [programa-puntos-h02.md](programa-puntos-h02.md). No se activa ninguna equivalencia numérica automáticamente. Solo un administrador autenticado configura `LoyaltyPolicy` (ID/versión, COP por unidad, puntos por unidad, tratamiento de reembolso, puntos gastados y restauración del canje) y `Benefit` (ID/versión/nombre/coste positivo/tipo/importe COP o referencia válida). El ejemplo docente y las propuestas de análisis son fixtures/propuestas, no autorización comercial.

Sin tasa los pagos elegibles dejan un origen diferido, no puntos nuevos; liquidarlo requiere acción administrativa separada, con fecha/vencimiento originales. Sin equivalencia el canje queda rechazado. Cada lote/canje conserva su configuración histórica. Aprobar también piso por pago sin arrastre de fracciones, compensación dinámica de deuda y un beneficio por reserva con pago residual positivo. La cancelación monetaria oficial sigue operativa e idéntica aunque falte configuración de puntos.

## H-03 — Promociones sin reglas implícitas

`PromotionService` exige Settings explícitas: nombre/descripcion/porcentaje, actividad, periodo opcional completo, condiciones, Combination y PointsAccrual. No se instala promoción alguna al iniciar. Las cifras de análisis son propuestas; las pruebas son fixtures. Configurar explícitamente criterio/umbral de frecuencia, descuentos, fechas, categorías, servicios/paquetes, combinación/orden y derecho a acumulación antes de activar.

Vigencia de operación [Instant desde, Instant hasta), estadía [fecha entrada, fecha salida), y revalidación de la versión aceptada antes del cargo. Si cambia la oferta PENDING, pedir nueva aceptación; no alterar recibos pagados. Una promoción más un beneficio por reserva, con combinaciones solo expresamente permitidas y total COP positivo. No se incorporan equivalencias de puntos nuevas. Contrato/fórmulas: [promociones-h03.md](promociones-h03.md).
