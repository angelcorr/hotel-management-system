# H-05 — Reportes, estadísticas y exportaciones

## Fuente y alcance

Fuente principal: documento oficial HotelUQ, sección 7 y subsecciones 7.1/7.2. Exige las ocho familias, acceso administrativo, metadatos, totales y gráficos. El documento acepta TXT o PDF; esta etapa implementa **TXT UTF-8** y los tres gráficos pedidos por el responsable: barras, pastel y líneas. PDF queda como extensión del contrato de exportación, sin incorporar bibliotecas ni una implementación incompleta.

Los datos se obtienen exclusivamente de reservas, comprobantes, movimientos y lotes de la aplicación. No se generan datos de demostración en Main ni en el módulo. Las capturas y TXT de evidencia utilizan operaciones reales de las fachadas dentro de fixtures de prueba, no cantidades constantes en los reportes de producción.

## Arquitectura y autorización

| Componente | Responsabilidad |
|---|---|
| `Hotel.reportSnapshot` | Comprobar administrador y capturar todo bajo el mismo monitor y un instante de Clock. Copia un modelo mínimo sin entidades mutables, correos, tarjetas, contraseñas ni tokens. |
| `ReportSnapshot` | Corte inmutable de inventario actual, contratos/estados, pagos, reembolsos, servicios históricos, recargas y cuentas de puntos. |
| `ReportQuery` | Familia, periodo inclusivo, filtros tipados y validación de compatibilidad. |
| `ReportService` | Fachada administrativa de generación y exportación. Revalida sesión/rol incluso si el reporte ya había sido generado. |
| `ReportEngine` | Consultas/agregaciones puras, una instancia por generación; no usa JavaFX, disco ni entidades del agregado. |
| `MoneyAllocation` | Reparto analítico conciliado de COP enteros por mayor resto. |
| `PointsReportData` | Proyección de movimientos y vencimientos, sin alterar el libro operativo. |
| `ReportResult` | Secciones/tablas, totales con unidad, criterios, corte, filtros y series transportables inmutables. |
| `ReportExporter` / `TxtReportExporter` | Formato de salida sobre el resultado ya calculado; permite añadir PDF sin repetir consultas ni fórmulas. |
| `ReportFiles` | Escritura temporal completa y movimiento al destino; no reemplaza archivos sin permiso explícito. |
| `ReportChartView` / `ReportChartAdapter` | Puerto de presentación y Adapter de series del dominio a los contratos de datos de JavaFX. |
| `ReportScreen` | Selección de filtros, consulta, tablas, gráficos y FileChooser; no suma ingresos ni calcula ocupación/puntos. |

Solo el administrador ve **Reportes y estadísticas**. La navegación vuelve a verificar sesión/rol; generación y exportación vuelven a autorizar en el agregado. No se confía en ocultar un botón. Cerrar sesión invalida un intento posterior de exportar un resultado anterior.

La consulta/generación conserva el alcance de sesión y monitor del agregado; en rangos grandes puede bloquear otras operaciones mientras agrega. La captura comparte el bloqueo de confirmaciones, reembolsos y recargas. Un reporte no puede ver el débito de una tarjeta sin su pago/estado correspondiente. Después se calcula sobre copias: una operación nueva no cambia el resultado que ya se muestra. Exportar conserva **ese resultado**, no ejecuta una consulta silenciosa con datos posteriores.

## Periodos, filtros y reproducibilidad

- Inicio y fin son fechas de Bogotá **inclusivas**. Los instantes se filtran como `[inicio 00:00, día posterior al fin 00:00)` en America/Bogota; nunca con la zona del servidor.
- Los registros mantienen sus instantes originales. Tablas y TXT presentan ISO-8601 con el desplazamiento de America/Bogota (`-05:00` para los datos actuales); el corte incluye hora y zona. Fechas de estadía son LocalDate, separadas de creación/pago/cancelación.
- Se rechazan nulos, rangos invertidos y extremos/comparaciones no representables. Hay un límite **técnico**, no comercial, de 36.600 días para limitar salida/recorrido por noches. Periodos largos aún pueden ser costosos para gráficos; conviene reducir el rango.
- Filtros opcionales: cliente, categoría, habitación y estado actual en familias de reservas; servicio específico solo en Servicios. Recargas/Puntos solo admiten cliente. Filtros incompatibles se rechazan, no se ignoran. Un ID sin registros produce un resultado vacío, no datos inventados.
- La comparación anterior se permite en Reservas y usa igual número de días inmediatamente anterior. Se muestra diferencia absoluta; no se inventa porcentaje de crecimiento con base cero.
- Estados y última cotización/contrato son los existentes **al corte**. No se afirma que un filtro por estado actual equivalga al estado al cierre histórico del periodo. Cambios PENDING posteriores pueden cambiar habitación/servicios de esa cohorte; pagos confirmados usan sus snapshots congelados.
- Orden y desempates son deterministas: fechas, IDs/identidades, ranking por cantidad y número/ID. Para el mismo snapshot y consulta, resultados y bytes TXT son iguales. Nuevas operaciones o un nuevo instante de generación constituyen otro corte y naturalmente pueden cambiar metadatos/saldos.
- Fechas sin evento CREATED no se sustituyen por fechas de estadía: se excluyen de la cohorte y se informa cuántas carecen de fecha.

## Ocho familias

### A. Reservas por periodo

Cohorte por el instante del evento `CREATED`. Incluye todos los estados salvo filtro explícito. Tabla de reservas/cliente/habitación/tipo/fechas/estado actual, distribución diaria, totales y distribución por estado. Una solicitud cancelada sigue siendo una reserva creada; no se borra de esta cohorte. Comparación opcional con periodo anterior de igual longitud.

### B. Habitaciones más reservadas

Misma cohorte por creación. Cuenta una reserva por identidad y habitación de su contrato al corte, con categoría/tipo del snapshot. Orden descendente de cantidad y, en empates, número ascendente. Incluye canceladas/PENDING si no se filtran; el usuario puede seleccionar estado. El título/criterio no pretende medir únicamente ventas pagadas.

### C. Ocupación hotelera contractual

Se cuenta cada noche de estadía en `[entrada,salida)`, intersectada con las fechas consultadas. Son compromisos de reservas **pagadas CONFIRMED/COMPLETED**; las canceladas no ocupan. Se deduplican habitaciones por noche. PENDING bloquea disponibilidad pero se muestra aparte y no se considera ocupación confirmada.

Por noche:

- Capacidad: habitaciones actualmente activas, con tipo activo, más habitaciones con compromiso confirmado esa noche aunque actualmente estén inactivas. Esa unión evita porcentajes mayores al 100 % por apagar un catálogo que conserva contratos.
- Ocupadas contractuales: habitaciones distintas con esos compromisos.
- Bloqueadas PENDING: habitaciones activas con borrador que no están ya ocupadas.
- Disponibles: capacidad menos ocupadas menos bloqueadas PENDING.
- Ocupación: `100 × ocupadas / capacidad`, dos decimales HALF_UP. Sin capacidad se muestra **N/A**, no un porcentaje inventado.

Periodo: se suman habitaciones-noches y se calcula `100 × noches ocupadas / capacidad-noches`, no el promedio simple de porcentajes diarios. Las cifras de habitaciones de cada fecha se muestran en tabla; los totales están rotulados habitación-noche. Cliente/estado filtran compromisos, pero no reducen el inventario del hotel a las habitaciones de ese cliente.

**Límite explícito:** no hay historial de altas/activaciones ni registro de presencia física/check-in. Se usa inventario activo actual más compromisos y fechas contractuales; no se presenta como reconstrucción de capacidad histórica observada ni ocupación física. Check-out anticipado/tardío no reescribe por inferencia las noches contratadas. Cambiar el catálogo o cancelar contratos puede cambiar esta proyección en un nuevo corte. No se inventaron eventos históricos.

### D. Ingresos por periodo

Caja operativa, no reconocimiento contable por noche:

- Pagos: suma de comprobantes `Payment.amount` cuya fecha de pago cae en el rango.
- Reembolsos: suma de `Refund.amount` por fecha de devolución, incluso cuando su pago original estaba en otro periodo.
- Neto: pagos menos reembolsos. Puede ser negativo.
- Recargas: **excluidas**, se informan en su familia propia.

Los UUID de comprobante evitan duplicar pagos/devoluciones por reintentos. La tabla separa cada flujo y mantiene reserva, cliente, identidad/fecha e importe. Valores originales COP, no totales supuestos de PENDING. Una cancelación posterior no borra el ingreso bruto histórico: aparece como devolución en su fecha.

El comprobante existente no desglosa fiscalmente habitación/servicios. Para mostrar importes asociados se hace una **atribución analítica**, sin cambiar precios ni políticas:

1. Pesos: valor crudo contractual de habitación/noches y de cada servicio; servicios bonificados por puntos tienen peso cero.
2. Para cada componente: piso de `pago efectivo × peso / suma de pesos` en COP enteros.
3. Pesos residuales exactos: se distribuye cada peso COP restante por fracción mayor, con clave estable para desempates.
4. Habitación más servicios concilia exactamente con el pago real. Se muestran valores brutos atribuidos y devolución total separada; el neto global sigue siendo el comprobante menos su devolución.

La técnica distribuye descuentos globales/redondeo y no es una nueva equivalencia de puntos, penalización, precio de factura ni regla comercial. Si no hay base histórica para un pago positivo se rechaza la atribución en lugar de inventarla.

### E. Servicios adicionales

Una unidad por línea de servicio en reserva **pagada**, por fecha de pago, sin multiplicar por noches. Se incluyen bonificados (cantidad contratada, ingreso atribuido cero). Agrupación por UUID histórico; nombre/precio desde Quote, no catálogo actual. El nombre mostrado es el del último contrato de la cohorte considerada, ordenado por fecha/UUID. Ranking por cantidad y ID estable; paquetes aportan sus hojas sin contarlas dos veces.

Se muestran cantidad, bruto crudo sin descuentos, ingreso efectivo atribuido, reembolso atribuido y neto. Las devoluciones se toman por su fecha aunque el pago estuviera fuera del rango; no crean otra contratación. Para atribuir la devolución se aplica el mismo reparto por mayor resto usando los componentes del pago original como pesos. Es información analítica, no una instrucción de devolución por servicio.

### F. Recargas

Solo movimientos RECHARGE realmente registrados. UUID, cliente, fecha e importe; número, total y evolución por fecha. Se omiten número de tarjeta y descripción libre por minimización de datos. Una recarga explícita posterior sigue siendo otro movimiento; repetir una consulta no la duplica. No se suma a caja operativa.

### G. Cancelaciones y reembolsos

Cohorte por fecha del comprobante de cancelación/devolución, un registro por UUID. Cuenta cliente/hotel, importe devuelto y porcentaje histórico. Incluye cancelaciones PENDING sin pago y devoluciones de cero; contar una cancelación no implica movimiento monetario positivo. Se mantiene vínculo al pago original cuando existe.

No se cambiaron las reglas: cliente >72 h=100 %, [24,72] h=80 %, <24 h=0 %; hotel=100 %. El reporte lee los valores congelados y no recalcula contratos con configuración actual.

### H. Programa de puntos

Movimientos por fecha efectiva: acumulaciones y redenciones **brutas**, ajustes por reembolso, restauraciones y referencias de origen. Créditos diferidos liquidados posteriormente conservan fecha efectiva y vencimiento originales de H-02: el informe retrospectivo al corte puede incluir ese conocimiento posterior, no afirma qué se conocía al cierre de aquel periodo.

Se informa por separado el saldo disponible/deuda **actuales al corte**, por cliente. Un rango histórico filtra movimientos, no convierte los saldos actuales en saldos de cierre de ese rango.

Los eventos EXPIRED operativos se registran al observar vencimientos y podrían ser posteriores a un reembolso. El reporte no los suma como si fueran una medición exacta al aniversario. Reconstruye, sin mutar el ledger:

- Para cada lote que ya venció al corte: derecho original menos ajustes y consumo neto anteriores al vencimiento; cuenta solo remanente positivo al aniversario.
- Conserva las asignaciones por lote de las redenciones y sus restauraciones. Respeta fecha de vencimiento individual calculada por H-02 (incluido 29/02 → 28/02 siguiente).
- Una restauración posterior al vencimiento no revive puntos. Se informa como `RESTORED_EXPIRED` únicamente el derecho que vuelve a quedar sin consumir y ya caducado; si el lote fue revocado, no se inventan puntos vencidos nuevos.
- La proyección de saldos reutiliza `PointsLedger.snapshotAccount`, que no agrega eventos de observación. Generar reportes no cambia puntos ni su historial.

No se espera que acumulados − redimidos − vencidos dé por sí solo el saldo: son magnitudes brutas y hay ajustes, restauraciones, deuda y fechas de cortes diferentes.

## Interfaz y gráficos

En la sesión administrativa: **Reportes y estadísticas** → familia → fechas/filtros → **Generar reporte**. Los filtros incompatibles se deshabilitan/limpian, y el servicio también los valida. Se muestran criterios, corte, filtros realmente aplicados, totales, tablas y conjuntos de series. Cambiar campos requiere generar de nuevo; exportar conserva el último resultado mostrado.

Se implementan `BarChart`, `PieChart` y `LineChart` mediante ReportChartAdapter. Dataset y visualización se eligen por separado: por ejemplo, ocupación tiene varias series diarias y una distribución de habitación-noche apta para pastel. Pastel exige una serie no negativa y omite sectores de valor cero; para netos negativos/múltiples series se pide barras/líneas, sin dejar un gráfico anterior engañoso. Ausencia de datos se informa con tablas vacías/ceros reales y N/A cuando no existe denominador.

Las tablas/TXT conservan BigDecimal exacto. El toolkit puede representar posiciones gráficas mediante Number/double, únicamente para dibujar; esa aproximación visual no recalcula ni sustituye importes. La tabla permite comparar numéricamente valores, no solo ordenarlos como texto.

## Exportación TXT

**Exportar TXT** abre FileChooser. El archivo contiene título, instante de generación/offset de Bogotá, periodo inclusivo, filtros, criterios, secciones con registros y totales con unidades/COP. Codificación UTF-8, saltos `\n`, columnas tabuladas y escapes de barras/saltos/tabulaciones de campos libres para no falsificar nuevas filas.

Por defecto no sobrescribe archivos. Si el destino existe, la UI exige confirmación; el servicio escribe un temporal completo antes de moverlo y lo limpia ante error. Un fallo no informa éxito ni altera reservas/saldos. La escritura autorizada mantiene el alcance de sesión/monitor, por lo que un disco lento puede bloquear temporalmente otras operaciones; esta aplicación local en memoria conserva esa frontera simple.

El contrato `ReportExporter` permite un `PdfReportExporter` futuro. En H-05 no se agregó PDF, dependencias, base de datos ni un motor contable nuevo. Los TXT contienen identificadores de clientes autorizados: compartirlos fuera de la aplicación es responsabilidad del administrador, aunque se omiten correos, contraseñas, tokens y tarjetas.

## Validación y ejecución

```bash
cd hotel-management-system
./gradlew --no-daemon clean test build
./gradlew --no-daemon fxTest
./gradlew --no-daemon run --args=--smoke-ui
# Ejecución normal interactiva:
./gradlew run
```

La suite gráfica necesita pantalla/GTK reales. Cada clase de pruebas gráficas utiliza su propia JVM porque administra el ciclo del toolkit; las 460 pruebas anteriores no se editaron ni desactivaron. Casos nuevos: ocho familias, filtros/rangos/vacíos, fechas Bogotá, reparto monetario, reembolsos cruzados, recargas, puntos/vencimientos/restauración, permisos, UTF-8/sobrescritura, corte concurrente y gráficos reales. La exportación se prueba en servicio y con archivos generados por los recorridos gráficos; el diálogo nativo de elección de archivo requiere selección humana y no se automatiza como si fuese un control JavaFX.

El resultado real, capturas y TXT de esta ejecución se registran en [evidencia-h05.txt](evidencia-h05.txt). Ninguna semilla de estos fixtures se incorpora al arranque normal.

## Pendientes y siguiente etapa

Persistencia/recuperación durable sigue pendiente y no se confunde con consistencia del corte en memoria. Capacidad histórica/ocupación física precisa requiere registrar historial de inventario y eventos de presencia, previa definición y autorización. Atribución fiscal de líneas requeriría que el pago guarde esos componentes como contrato, no convertir este reparto analítico en una tarifa comercial aprobada.

PDF es una extensión futura opcional. Sigue la compra autónoma fuera de reservas, reglas comerciales abiertas de H-02/H-03, liquidación de cambios CONFIRMED y plazo PENDING. Se recomienda la siguiente etapa de cierre académico: revisión integral de requisitos, pruebas de aceptación con el equipo, datos de demostración explícitos, documento/diagramas finales y ensayo de sustentación. Una implementación durable debe aprobarse como trabajo separado. No se hicieron commits, push ni despliegues.

## Resultado y artefactos conservados

**Verificación H-05 (15/11/2026):** Java 21 / Gradle 8.14.3; `clean test build fxTest run --args=--smoke-ui`: BUILD SUCCESSFUL (3 min 11 s), salida 0. Regresión sin pantalla: 506 aprobadas; JavaFX real: 12 aprobadas. **Total: 518 ejecutadas/aprobadas, 0 fallidas, 0 errores y 0 omitidas**, incluidas las 460 anteriores sin editar y 58 nuevas. Inicio real con `HOTELUQ_UI_STARTED`; `git diff --check`: salida 0. Detalle: [evidencia-h05.txt](evidencia-h05.txt).

Capturas de las escenas del hotel y TXT generados con fixtures reales del servicio (no datos incorporados a producción):

- [cancelaciones pastel](h05/cancelaciones-pastel.png)
- [cancellations barras](h05/cancellations-barras.png)
- [cancellations](h05/cancellations.txt)
- [ingresos lineas](h05/ingresos-lineas.png)
- [occupancy barras](h05/occupancy-barras.png)
- [occupancy](h05/occupancy.txt)
- [points barras](h05/points-barras.png)
- [points](h05/points.txt)
- [recharges barras](h05/recharges-barras.png)
- [recharges](h05/recharges.txt)
- [reservations barras](h05/reservations-barras.png)
- [reservations](h05/reservations.txt)
- [revenue barras](h05/revenue-barras.png)
- [revenue](h05/revenue.txt)
- [room ranking barras](h05/room_ranking-barras.png)
- [room ranking](h05/room_ranking.txt)
- [services barras](h05/services-barras.png)
- [services](h05/services.txt)
