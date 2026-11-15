# Diagnóstico y propuesta de persistencia durable (sin implementación)

## Estado observado

`Hotel` almacena usuarios, habitaciones/tipos, servicios/promociones y reservas en listas; sesiones, claves de creación y versiones de políticas en mapas. `VirtualCard` guarda saldo y movimientos; `Reservation` guarda condiciones/snapshots, versión, State, pago, reembolso, horario contractual, check-out y auditoría. No hay repositorio durable, base de datos, archivo de recuperación, WAL de aplicación ni restauración al iniciar.

El monitor del agregado y los checkpoints restauran errores recuperables dentro del proceso. No protegen frente a cierre de JVM, corte eléctrico, pérdida de memoria o reinicio. Tras reiniciar se pierden también las claves que impedían repetir operaciones. Guardar solo reservas sin sus pagos/movimientos/políticas permitiría inventar o duplicar saldos y devoluciones; no es una recuperación válida.

## Datos que deben persistir juntos

- Identidades, roles, hashes/sales y versión de credenciales; catálogo y sus identidades/referencias.
- Reservas con UUID/versión, estado y condiciones contractuales históricas, fechas de estadía, zona/instantes, moneda y detalle bruto/final del cálculo.
- Cuentas de tarjeta, saldo, libro de movimientos y vínculos reserva/pago/reembolso.
- Pagos y reembolsos únicos, moneda/importe, instantánea de política y regla de redondeo usada.
- Check-outs únicos con responsable, clave, instante y ventana autorizada.
- Auditoría con identificadores de operación y snapshots antes/después.
- Claves de idempotencia y huellas de solicitud/respuesta; no solo un hash sin resultado recuperable.
- Políticas comerciales inmutables por ID/versión y sus activaciones, actor e instante.

Las sesiones pueden seguir siendo efímeras y exigir nuevo login al reiniciar. La pérdida de sesiones no debe borrar clientes, saldos ni idempotencia. No se deben guardar contraseñas en texto plano ni serializar los tokens/ThreadLocal/guardas internas como si fueran datos del negocio.

## Arquitectura propuesta

Introducir repositorios y una unidad de trabajo transaccional detrás de la fachada Java existente, mediante JDBC y un motor relacional transaccional. Para la ejecución local de un único proceso, evaluar un motor embebido; si se autoriza un servicio multiusuario, evaluar un motor servidor. La selección de motor, driver, ubicación, copias y rango/precisión monetarios debe aprobarse antes de agregar dependencias o migraciones. No se instaló ni seleccionó automáticamente una base de datos en esta fase.

La unidad de trabajo debe incluir, en un solo commit: disponibilidad, control de versión, débito/reembolso, movimiento, estado, comprobante, auditoría e idempotencia. State sigue validando transiciones y Strategy calcula con la política versionada seleccionada. La transacción de base de datos reemplaza la compensación exclusivamente en memoria como garantía durable; no basta con envolver el código actual y escribir después una copia de cada lista.

Esquema conceptual:

| Entidad | Restricción esencial |
|---|---|
| Usuario / tarjeta | IDs únicos, correo normalizado único, tarjeta original estable, saldo no negativo |
| Reserva / snapshot | UUID único, versión para CAS, FK de cliente/habitación y condiciones históricas inmutables |
| Pago | Una aprobación por reserva; referencia y moneda COP; importe entero positivo |
| Reembolso | Una decisión por reserva/pago, importe entero no negativo y nunca superior a lo cobrado elegible |
| Movimiento | UUID/operación únicos, referencia a pago/reserva; relación trazable con el libro |
| Check-out | Una operación por reserva y clave idempotente única con responsable/instante |
| Solicitud idempotente | Clave compuesta por ámbito/cliente/operación, huella y resultado de commit |
| Política | ID/versión únicos e inmutables; activación auditable |
| Auditoría | Eventos append-only ligados al commit y UUID de operación |

Persistir COP como decimal integral exacto, nunca FLOAT/DOUBLE; la precisión máxima se acuerda con el motor y los datos históricos. Guardar instantes UTC inequívocos y fechas de estadía por separado, más `America/Bogota` como zona contractual. Versionar el formato de snapshots/políticas.

## Concurrencia e idempotencia después de reinicio

- La exclusión del monitor Java no basta si hay más de una instancia. Bloquear/serializar la habitación y tarjeta afectadas, o utilizar el mecanismo de aislamiento apropiado del motor, y revalidar superposición dentro de esa transacción.
- `UPDATE ... WHERE id = ? AND version = ?` con comprobación de filas afectadas para evitar actualizaciones perdidas.
- Registrar clave/huella/resultado idempotente dentro del mismo commit que el efecto económico. Si se pierde la respuesta después del commit, el reintento devuelve los UUID originales, aunque haya reiniciado la aplicación.
- Validar la devolución contra el pago original y el acumulado confirmado bajo bloqueo; usar restricciones y comprobaciones transaccionales, no un simple CHECK que consulte otras filas de manera no soportada por el motor.
- Recargar al iniciar únicamente estados comprometidos y volver a instalar guardas de autorización del agregado. No reconstruir un cobro desde un estado CONFIRMED sin su pago original.
- Restaurar coherencia entre saldo materializado y suma del libro, comprobar moneda/referencias y fallar de forma explícita ante corrupción; no corregir saldos silenciosamente.
- Si se agregan notificaciones/eventos externos en fase 4, proponer outbox durable con consumidor idempotente después de commit. Si se adopta una pasarela, añadir identificadores de proveedor y reconciliación/compensación durables antes de usarla. Ninguno se implementó aquí.

## Migración e historia: decisiones antes de autorizar

No hay datos durables que este código pueda migrar automáticamente. Si el responsable conserva un estado externo o un volcado manual, se necesita inventario, copia y plan explícito. Los importes fraccionarios, total cero, timestamps sin zona y reservas finalizadas sin check-out de versiones antiguas requieren clasificación y decisión; no deben redondearse, recobrarse, reinterpretarse ni convertirse en check-outs ficticios de forma masiva.

Fijar versión de esquema, tratamiento de contratos históricos y política de recuperación/copia antes de importar. Las ventanas nuevas no pueden recalcular devoluciones históricas. Debe acordarse también conservación de auditoría e idempotencia; eliminarlas indiscriminadamente permite duplicar efectos de reintentos antiguos.

## Pruebas de recuperación propuestas

1. Guardar y reiniciar tras alta, pago, cancelación parcial/cero/total y check-out: recuperar los mismos UUID, snapshots, saldo, movimientos y políticas.
2. Caída antes de commit en cada punto de escritura: recuperar el estado anterior sin cargo/estado parcial.
3. Caída después de commit y antes de responder: reintentar la misma clave tras reiniciar y obtener exactamente el mismo comprobante, sin movimiento adicional.
4. Dos procesos con altas superpuestas, dos pagos sobre saldo limitado, doble reembolso/check-out y dos modificaciones de la misma versión: un único efecto válido.
5. Reiniciar después de cambiar una política: los reembolsos anteriores conservan versión/configuración; nuevas operaciones usan la activación comprometida.
6. Reconstruir cuentas desde libro y detectar corrupción, referencias huérfanas, duplicados, moneda inválida o snapshot incompatible sin autocorrección silenciosa.
7. Ejecutar recuperación con otra zona del servidor y en cambios de día/mes/año: conservar los instantes y fechas contractuales originales.
8. Probar backup/restauración íntegros, versión de esquema y migración reversible sobre copia; pérdida/corrupción del archivo o fallo de escritura no deben presentarse como operación exitosa.
9. Revocar sesiones al reiniciar y verificar que nuevo login mantiene roles/propiedad sin reutilizar tokens antiguos.

Esta propuesta requiere autorización y selección técnica antes de implementarse. Las pruebas actuales de rollback en memoria no sustituyen estas pruebas con cierre/reapertura real del almacenamiento.
