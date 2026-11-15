# H-03 — Promociones condicionadas: configuración y contrato

Fuente principal: PDF oficial HotelUQ §2.1, §2.2 y §3.6 (página impresa 6). Exige verificar condiciones antes del descuento y deja al grupo definirlas; fechas/tipo/noches/cliente frecuente/servicios son ejemplos de criterios, no una tabla obligatoria de porcentajes. Las cifras de `analisis.md` siguen siendo propuestas. **No se crean promociones ni combinaciones comerciales automáticamente al iniciar.**

## API y responsabilidades

- `PromotionService`: fachada autenticada para crear, consultar, actualizar, activar/desactivar y consultar auditoría administrativa. Constructor inyectable con el mismo `Hotel` de las otras fachadas.
- `PromotionDefinition.Settings`: nombre y descripción normalizados, porcentaje BigDecimal entre 0 y 100, actividad, periodo opcional completo, condiciones, combinación y acumulación explícitas. Rechaza periodo incompleto/invertido, umbrales no positivos, conjuntos vacíos y familias de condición repetidas/contradictorias.
- `PromotionProgram`: definiciones inmutables por identidad/versiones, control optimista y eventos con UUID, actor, instante y estados anterior/posterior. La instancia autoritativa es privada de Hotel.
- `PromotionCondition`: Specifications selladas, evaluadas por polimorfismo y conjunción `All`. No se aceptan predicados arbitrarios suministrados por el cliente de la aplicación.
- `PriceComponent`: contrato de importe crudo y Decorator Base → Percent/Fixed. Compone descuentos en un orden explícito y redondea solo el importe final exigible.
- `Hotel`: conserva el único monitor, comprueba sesión/rol/propietario, obtiene hechos reales de reservas/catálogos, coordina registro y transacción de reserva/tarjeta/puntos. No contiene la fórmula ni las implementaciones individuales de elegibilidad.

`PromotionService.create(token, settings)` requiere administrador y genera identidad UUID/version=1. `update(token,id,expectedVersion,settings)` exige la versión actual; `expectedVersion=0` configura por primera vez un registro del catálogo antiguo. `setActive` también produce una nueva versión. `list` requiere sesión; `history` requiere administrador.

Los catálogos antiguos de `HotelService` se conservan para compatibilidad. Una promoción antigua sin condiciones **sigue sin poder llegar al pago**. Configurar explícitamente sus reglas habilita el flujo. Una vez versionada, setters/actualizaciones antiguas se rechazan y se exige PromotionService, incluso al administrador, para no saltar versiones/auditoría. El catálogo legado refleja nombre/porcentaje actuales; los contratos de reservas conservan copias. La eliminación de promociones versionadas se bloquea: se desactivan conservando historia. Las promociones antiguas no referenciadas conservan su eliminación previa.

## Condiciones y límites

Todas las condiciones seleccionadas deben cumplirse. Se permite como máximo una instancia por familia de condición en una configuración, evitando restricciones duplicadas contradictorias. No hay OR, selección automática de «mejor promoción» ni varias promociones en una reserva.

| Criterio | Configuración y significado exacto |
|---|---|
| Vigencia de operación | `validFrom` y `validUntil`, ambos ausentes o ambos presentes; instantes `[desde,hasta)`. Se comprueba al crear/modificar y nuevamente al confirmar con Clock del hotel. Bogotá sigue siendo la zona oficial; un adaptador de UI debe convertir horas locales explícitamente con HotelTime, sin zona del servidor. |
| Fechas específicas de estadía | `StayWindow(from,until)`: todas las noches deben estar dentro de `[from,until)`. Entrada ≥ from, salida ≤ until; salida en el extremo es válida por ser exclusiva. No confundirlo con la fecha de creación o de pago. |
| Habitación | `RoomTypes(Set<RoomCategory>)`: categoría efectiva de la habitación registrada. No acepta tipo declarado por el solicitante. |
| Noches | `MinimumNights(n)` positivo: diferencia de fechas de estadía, no duración UTC de la operación. |
| Cliente frecuente | `FrequentClient(n)` positivo: cuenta reservas del mismo cliente COMPLETED, pagadas y sin reembolso, excluyendo la reserva actual. Pendientes, confirmadas sin check-out, canceladas y reservas de otros clientes no cuentan. Este es el criterio soportado para configuración; confirmar comercialmente su adopción/umbral antes de activarlo. No se fija n=3 ni otro valor por defecto. |
| Servicios | `Services(ids)`: todos los UUID requeridos deben estar seleccionados y registrados/disponibles al pagar. Una condición no agrega servicios sin consentimiento. |
| Paquetes | `Packages(ids)`: debe constar la selección de esos paquetes; contratar las mismas hojas por separado no equivale a seleccionar el paquete. Solo se permiten paquetes registrados de H-02. |

Las referencias a servicios/paquetes se validan antes de publicar la configuración. Las condiciones históricas protegen la eliminación de servicios referenciados. Una configuración puede describir una promoción futura; su periodo/actividad se comprueba antes de aplicar efectos. El nombre es único, sin distinción de mayúsculas ni espacios exteriores, en el mismo catálogo anterior.

## Reservas y paquetes

`BookingRequest` añade `packageIds` y conserva el constructor previo de seis argumentos (paquetes vacíos). Seleccionar paquetes incorpora sus servicios una sola vez con sus precios reales. Selecciones repetidas y cruces que duplican un servicio entre paquetes/servicios se rechazan; no se cobra dos veces ni se infiere un descuento de paquete.

`Reservation.Builder` conserva compatibilidad con promociones antiguas para ensamblaje/pruebas del modelo; el agregado no acepta ese ensamblaje como prueba de elegibilidad. La ruta de aplicación carga la definición autoritativa, obtiene los hechos del cliente/catálogo y valida antes de registrar/confirmar. Los paquetes declarados deben incluir todas sus hojas disponibles: no es válido introducir solo un UUID para simular cumplimiento.

La cotización y BookingView conservan la definición inmutable, con identidad, versión, porcentaje, condiciones, vigencia, actividad y decisiones de combinación/acumulación. Se conserva además la selección de paquetes y el detalle de servicios/precios. Los AuditEvent existentes guardan cotizaciones antes/después, ahora con la definición de promoción correspondiente.

Si cambia o se desactiva la promoción mientras una reserva está PENDING, la confirmación falla **antes del cargo**, sin sustituir silenciosamente la oferta. Una modificación versionada PENDING debe aceptar una nueva cotización o retirar la promoción. Las condiciones se recalculan tras cambios de fechas, habitación, servicios o paquetes. Una modificación incompatible no cambia contrato ni disponibilidad registrada.

Después del pago no se recalculan contratos por actualizar/desactivar una promoción. El replay de una confirmación ya registrada retorna el mismo comprobante aunque la promoción haya vencido o cambiado. Las modificaciones confirmadas siguen bajo el bloqueo económico previo; H-03 no lo elimina.

## Dinero y combinaciones explícitas

Base soportada de la promoción: subtotal crudo completo de habitación/noches más servicios seleccionados, incluidos los paquetes. Se conserva esta base del Builder previo. No se inventa una tarifa de paquete ni una base alternativa de solo habitación. Las condiciones por servicio/paquete limitan elegibilidad, no una base distinta de descuento.

El neto se calcula con BigDecimal, sin double/float en el motor nuevo. Se conserva precisión intermedia y solo el importe exigible final se redondea a COP entero con HALF_UP. El descuento final se reconcilia como subtotal final menos total pagadero. Un resultado cero/negativo, incluido el que se redondea a cero, se rechaza antes de cobrar y sin comprobantes/movimientos.

Cada configuración debe elegir expresamente una opción de combinación:

| Opción | Fórmula de neto crudo con un beneficio por puntos |
|---|---|
| `EXCLUSIVE` | Rechaza toda redención combinada, incluso si el beneficio tiene valor monetario cero. |
| `PROMOTION_THEN_POINTS` | `subtotal * (1 - porcentaje/100) - beneficio`. |
| `POINTS_THEN_PROMOTION` | `(subtotal - beneficio) * (1 - porcentaje/100)`. |

`beneficio` es el descuento COP explícito o la suma cruda de los servicios bonificados. El orden puede cambiar el importe: en particular, bonificar servicios después del porcentaje resta su precio completo, mientras hacerlo antes evita incluirlos en la base del porcentaje. **Ambas opciones son capacidades configurables, no decisiones aprobadas ni activas por defecto.** No hay una combinación implícita por el mero hecho de existir una equivalencia de puntos. Se conserva un beneficio por reserva y una promoción por reserva.

La configuración también exige `PointsAccrual`:

- `EARN_ON_NET_PAYMENT`: invoca el programa H-02 únicamente con Payment.amount realmente cobrado, después de todos los descuentos y redondeo; su tasa sigue debiendo estar configurada. Si no la hay, H-02 registra el origen diferido.
- `EXCLUDED`: ese pago no otorga puntos ni crea una acreditación diferida. La exclusión queda trazable en la definición del contrato. No impide restaurar una redención previa según la política histórica propia del canje.

No se altera ninguna tasa, equivalencia o política de reembolso de puntos de H-02 automáticamente. Los ejemplos de pruebas son fixtures explícitos, no configuración de producción.

## Atomicidad, idempotencia y cancelación

Se usa el mismo bloqueo y `bookingAtomic` de reserva/tarjeta/puntos. El beneficio, los servicios/paquetes añadidos, el precio combinado, el consumo de puntos, el débito, el State, el comprobante y su auditoría forman un solo bloque reversible. Se conserva la captura única de instante para la confirmación. Revalidar la promoción después de añadir el beneficio también ocurre dentro de ese bloque, antes de efectuar el débito.

Las claves de confirmación y redención permanecen: UUID de reserva, pago original y clave de cliente/versión/beneficio para el canje. No se crea un cargo nuevo por reintentar. Versiones de promoción impiden actualizaciones perdidas. El monitor serializa actualizaciones/activación y confirmación: solo puede confirmarse con una definición todavía vigente en el orden serial observado.

Cancelaciones siguen intactas: cliente >72 h=100 %, [24,72] h=80 %, <24 h=0 %; hotel=100 %. El importe elegible es el pago **descontado** original, no la tarifa sin promoción. Se conserva el vínculo pago/reembolso y el ajuste H-02 sobre el dinero efectivamente retenido. Una devolución fallida restaura contrato, saldo, movimientos, puntos, redención, estado y auditoría. La versión actual de promoción no reescribe el contrato cancelado.

## Decisiones abiertas y límites

Aprobar promociones concretas: porcentajes, periodos, categorías, noches, criterio/umbral de frecuencia, servicios/paquetes, combinación/orden y acumulación con pagos promocionados. No se activan las propuestas numéricas de análisis ni nuevas equivalencias de puntos. Sigue pendiente la compra autónoma fuera de una reserva de H-02.

La atomicidad continúa siendo en memoria. Configuraciones, versiones, condiciones, claves e historial se pierden al cerrar la JVM; deben incorporarse a la propuesta de persistencia durable, junto con cotizaciones y sus snapshots. No se añadió base de datos ni dependencia.

Para JavaFX: consumir PromotionService/ReservationService/LoyaltyService; presentar condiciones, vigencia, versión y total de la cotización; seleccionar una promoción/beneficio y paquetes por UUID; al recibir conflicto pedir revisión de la nueva cotización; usar expectedVersion para edición administrativa. Mostrar errores del dominio y revalidar siempre en el servicio, sin confiar en permisos ni cálculos del controlador. No se implementó JavaFX en H-03.
