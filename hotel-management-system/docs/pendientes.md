# HotelUQ: qué falta para completar el proyecto

Revisión actualizada el 14 de noviembre de 2026 tras recuperar la integración de fases 1 y 2. Los apartados 9 y 10 conservan la evidencia histórica del 13 de noviembre; el apartado 11 registra la recuperación. Fuente funcional: `docs/analisis.md`. Se revisaron todos los archivos Java y la configuración de construcción disponibles. El enunciado original no está en el repositorio; por eso este documento evalúa el alcance descrito en el análisis y no presupone requisitos adicionales de la asignatura.

## 1. Diagnóstico

El proyecto tiene una base del dominio, pero todavía no constituye una aplicación utilizable. Existen habitaciones, tipos, usuarios, tarjeta virtual, reservas con Builder, un Singleton y Factory Method. `Main` únicamente imprime el ejemplo inicial. Tras las fases 1 y 2 hay validaciones, autenticación, sesiones, autorización y administración de usuarios/catálogos mediante una fachada Java. Siguen pendientes pantallas, controladores y los flujos transaccionales de reserva/pago.

La recuperación vuelve a conectar las fases 1 y 2 y conserva State/Strategy de GitHub (`52f986d`, RF-011/refund-strategy). La compilación inicial estaba rota (30 errores en fuentes principales); por ello ninguna prueba podía ejecutarse. La verificación actual con OpenJDK 21 y Gradle 8.14.3 ejecutó `clean test build` correctamente: **137 pruebas satisfactorias, 0 fallos y 0 omisiones** (132 originales + 5 de compatibilidad). Véase la evidencia de recuperación en el apartado 11. Las restricciones de Gradle descritas en las evidencias antiguas corresponden a esas ejecuciones históricas.

## 2. Estado por requisito

El estado corresponde a la entrega integral de la aplicación. «Parcial — negocio verificado» distingue requisitos con operaciones Java completas, autorizadas y probadas, cuya integración en la interfaz de la fase 5 está pendiente. No se marca ningún RF como terminado para el usuario final mientras esa integración siga ausente.

| Requisito | Estado | Evidencia y pendiente |
|---|---|---|
| RF-001 Registro | Parcial — negocio verificado | `HotelService.register` crea exclusivamente clientes con tarjeta, valida datos y duplicidad de ID/correo; probado. Integración de interfaz pendiente. |
| RF-002 Inicio de sesión | Parcial — negocio verificado | Login, logout, credenciales genéricas, hash con sal, tokens aleatorios, caducidad, revocación y rol del servidor probados. Integración de interfaz pendiente. |
| RF-003 Consultar tipos | Parcial | Hay tipos, características y tarifas consultables en el modelo. Falta la interfaz. |
| RF-004 Disponibilidad | Parcial — negocio verificado | Consulta Java autenticada con fechas válidas; excluye habitaciones/tipos inactivos y superposiciones. Falta integración de interfaz. |
| RF-005 Crear reserva | Parcial | Builder y registro en `Hotel` con referencias registradas, capacidad y duplicidad validadas; pruebas de superposición concurrente. Falta coordinar disponibilidad, pago y confirmación. |
| RF-006 Servicios | Parcial | Builder suma servicios disponibles; nulidad y disponibilidad se validan también al registrar. Falta selección desde la aplicación y control al confirmar/pagar. |
| RF-007 Promociones | Parcial | Porcentaje finito entre 0 y 100 validado y probado. Aún se aplica sin evaluar condiciones. |
| RF-008 Pago | Parcial | `VirtualCard.charge` valida importes, usa saldo decimal y evita sobregiros entre cobros concurrentes; probado. No está conectado a la reserva ni impide cobrarla dos veces. |
| RF-009 Saldo y movimientos | Parcial — negocio verificado | Consulta autenticada del propietario o administrador mediante vista inmutable; accesos ajenos y sin sesión rechazados. Falta integración de interfaz. |
| RF-010 Historial | Parcial — negocio verificado | Historial filtrado por cliente; administrador puede consultar y cliente solo su propio historial. Pruebas con reservas reales del modelo. Falta integración de interfaz. |
| RF-011 Cancelar y reembolsar | Parcial — código GitHub conservado | Existen State, políticas Strategy de 100/80/0 % y selector por anticipación; límites probados. Falta integración autorizada, transaccional e idempotente con el pago original; no se declara terminado. |
| RF-012 Consultar puntos | Pendiente | No existen `PointsAccount` ni `PointsBatch`. |
| RF-013 Redimir puntos | Pendiente | No existen redención, beneficios ni control de vencimiento. |
| RF-014 Gestionar clientes | Parcial — negocio verificado | Alta, consulta, edición y eliminación autorizadas. Correo único incluso en setters y concurrencia; eliminación bloqueada con reservas o movimientos/saldo. Falta interfaz. |
| RF-015 Gestionar habitaciones | Parcial — negocio verificado | CRUD, activación, Factory Method y referencias protegidas; edición de piso/estado con número y categoría estables. Falta interfaz. |
| RF-016 Gestionar tipos | Parcial — negocio verificado | CRUD y activación de tipos dentro de STANDARD/FAMILY/SUITE; nombres/categorías únicos y referencias protegidas. Falta interfaz. |
| RF-017 Servicios y paquetes | Parcial | CRUD autorizado de servicios simples, UUID estable, nombre único y disponibilidad implementados/probados. Paquetes pendientes de fase 4 e interfaz de fase 5. |
| RF-018 Gestionar promociones | Parcial | CRUD básico autorizado, UUID estable, nombre único y porcentaje válido probados. Faltan condiciones/vigencia/beneficios de fase 4 e interfaz. |
| RF-019 Gestionar reservas | Parcial | Listados protegidos y filtrados por propietario; integridad de referencias probada. Estados/transacciones siguen pendientes de fase 3; no hay flujo de pago confirmado. |
| RF-020 Recargar tarjeta | Parcial — negocio verificado | Solo administrador puede recargar tarjetas registradas; valida estado/importe y genera un movimiento. Probado también sin pasar por la fachada y bajo concurrencia. Falta interfaz. |
| RF-021 Cancelación por hotel | Parcial — código GitHub conservado | Existe `ReservationService.cancelByHotel` con `FullRefundPolicy`; falta flujo integral autorizado y ligado al pago original. |
| RF-022 Estadísticas | Pendiente | No hay cálculo de indicadores. |
| RF-023 Exportar TXT | Pendiente | No hay generación ni exportación de reportes. |
| RF-024 Gráficos por fechas | Pendiente | JavaFX está configurado, pero no hay gráficos ni filtros. |
| RF-025 Acumulación automática | Pendiente | No existe cuenta de puntos ni integración con pagos. |
| RF-026 Notificaciones | Pendiente | No existen eventos, observadores ni bandeja de notificaciones. |

## 3. Hallazgos del diagnóstico y seguimiento

Los hallazgos siguientes se conservan como referencia. Los apartados 9 y 10 distinguen correcciones verificadas y pendientes; no todos siguen presentes.

### Reservas y disponibilidad

- Sustituir el setter libre `Reservation.setStatus` por operaciones con transiciones explícitas. Actualmente es posible confirmar sin pagar, reabrir una cancelada o asignar un estado nulo.
- Definir si una reserva PENDING bloquea la habitación y, en tal caso, cuánto dura ese bloqueo. Hoy todas las reservas salvo CANCELLED bloquean su intervalo indefinidamente.
- Validar fechas también al consultar disponibilidad, no únicamente al construir reservas.
- Verificar que habitación y cliente pertenezcan al hotel. Actualmente pueden registrarse reservas con objetos externos a los catálogos.
- Hacer indivisible el proceso de comprobar disponibilidad, cobrar y confirmar. Un error no debe dejar un cobro sin reserva ni una reserva confirmada sin cobro.
- Proteger `Hotel.getReservations`: actualmente devuelve la lista interna modificable y permite saltarse `addReservation`.
- Preservar el detalle contratado: el total ya queda calculado en la reserva, pero servicios, promoción, habitación y cliente mantienen referencias a objetos modificables. Guardar los datos históricos necesarios para explicar el importe pagado aunque cambien los catálogos.

### Dinero y tarjeta

- Usar `BigDecimal` o una unidad monetaria entera y definir redondeo. `double` puede introducir diferencias en descuentos, saldos y reembolsos.
- Rechazar cantidades no finitas (`NaN`, infinitos), además de cero y negativos. `checkPositive` actualmente no rechaza `NaN` ni infinito positivo.
- Relacionar pagos y reembolsos con la reserva y el pago original; impedir pagos y devoluciones duplicados o devoluciones superiores a lo cobrado.
- Restringir la recarga a una operación autorizada. El comentario situado sobre `charge` habla de administradores, pero el permiso corresponde a `recharge`.
- Evitar que código externo fabrique movimientos con `VirtualCard.register`, que hoy es público.
- Corregir el nombre `InsuficientBalanceException` a `InsufficientBalanceException` y actualizar sus referencias.
- Validar precios no negativos y descuentos entre 0 y 100. Definir cómo confirmar una reserva con importe cero tras redimir beneficios, pues `charge(0)` actualmente falla.

### Usuarios y catálogos

- Validar campos obligatorios, textos vacíos, correo, capacidad y número de habitación.
- Sustituir contraseñas en texto plano por un hash con sal y verificación apropiada.
- Mantener la unicidad del correo al editar: `setEmail` permite eludir la comprobación de `Hotel.addUser`.
- Evitar categorías de habitación duplicadas y verificar la coherencia entre la subclase y su `RoomType`.
- Definir identificadores y reglas de duplicidad de servicios y promociones.
- Impedir eliminaciones que rompan reservas históricas; utilizar desactivación cuando corresponda.
- Si se usa concurrencia, proteger tanto la creación del Singleton como las operaciones sobre sus colecciones. Resolver la inicialización del Singleton por sí sola no evita reservas simultáneas incompatibles.

## 4. Componentes por implementar

Los nombres siguientes son una propuesta de organización; las responsabilidades y reglas son lo necesario.

| Componente | Responsabilidad |
|---|---|
| `HotelService` + sesiones en `Hotel` | Implementado en fase 2: registrar, autenticar, cerrar sesión y comprobar identidad/rol en cada operación. |
| `ReservationFacade` | Coordinar cotización, disponibilidad, pago, confirmación, puntos y notificación. |
| Operaciones de estado | Permitir PENDING → CONFIRMED/CANCELLED y CONFIRMED → CANCELLED/COMPLETED; rechazar transiciones inválidas. Definir cuándo se permite completar una estancia. |
| `RefundPolicy` | Strategy y selector de GitHub conservados. Pendiente integrar con pago original, autorización, idempotencia y reloj/zona del hotel. |
| `PointsAccount` / `PointsBatch` | Registrar lotes, vencimiento anual, saldo vigente y consumo por vencimiento más cercano. |
| Redención de beneficios | Canjear descuentos, servicios y paquetes sin exceder saldo ni reutilizar puntos. |
| `PromotionCondition` | Evaluar estadía larga, suite dentro de fechas y cliente frecuente según el análisis. |
| `ServicePackage` | Componer servicios y paquetes, calcular precio y disponibilidad; impedir ciclos. |
| `HotelService` (administración) | Implementado en fase 2 para usuarios y catálogos simples; paquetes/condiciones de promociones siguen pendientes. |
| Eventos y observadores | Notificar confirmación, cancelación, pago, recarga y reembolso una sola vez por operación exitosa. |
| `ReportService` | Calcular indicadores con filtros, producir TXT y entregar datos para gráficos. |
| Aplicación JavaFX | Presentar los flujos de cliente y administrador, validaciones y mensajes de error. |

No hace falta crear clases vacías para representar todos los patrones de la sección 7 del análisis: allí se describen como posibles. Si la rúbrica exige patrones específicos, verificar ese punto contra el enunciado original.

## 5. Precisiones funcionales pendientes

El análisis establece las reglas principales, pero deja estas decisiones abiertas. Deben acordarse y documentarse antes de cerrar su implementación:

1. **Hora de entrada:** las reservas usan `LocalDate`, mientras los reembolsos dependen de horas. Definir hora de check-in y zona horaria del hotel.
2. **Límites del reembolso:** interpretar más de 72 horas como 100 %, de 24 a 72 horas inclusive como 80 % y menos de 24 como 0 %. Confirmar qué sucede tras iniciar la estancia.
3. **Servicios:** establecer si se cobran una vez por reserva, por noche o por huésped. El código actual los cobra una vez.
4. **Promociones:** determinar vigencia, fechas que deben caer dentro de la temporada y base del descuento. El código actual descuenta sobre habitación más servicios y admite una sola promoción.
5. **Puntos y cancelaciones:** definir si se revierten puntos ganados, cómo se recuperan puntos canjeados y qué ocurre si ya se gastaron los puntos de una reserva cancelada.
6. **Beneficios:** precisar si servicios canjeados quedan ligados a una reserva, cómo se valora un paquete y si se acumulan promociones y descuentos por puntos.
7. **Finalización:** definir quién marca COMPLETED y desde qué fecha; esto determina la promoción de cliente frecuente.
8. **Notificaciones:** definir si basta una bandeja dentro de la aplicación o se requiere un canal externo. El análisis no exige correo electrónico.
9. **Reportes:** acordar indicadores, fecha usada para filtrar e ingresos brutos/netos. Para ocupación, contar noches efectivamente solapadas con el rango y definir el denominador de habitaciones activas.
10. **Persistencia:** no está exigida explícitamente en el análisis. Actualmente los datos se pierden al cerrar. Confirmar si la entrega necesita almacenamiento en archivo/base de datos y restauración al iniciar.

## 6. Interfaz mínima para cubrir los casos de uso

- Acceso: registro e inicio/cierre de sesión.
- Cliente: consulta de tipos y disponibilidad por fechas; selección de huéspedes, servicios, promoción y beneficios; revisión del total y pago; historial y cancelación; tarjeta, movimientos, puntos con vencimientos y notificaciones.
- Administrador: gestión de clientes, habitaciones, tipos, servicios, paquetes y promociones; consulta y gestión de reservas; recargas; cancelación por hotel; estadísticas, filtros, gráficos y exportación TXT.

Ocultar un botón no constituye autorización: las operaciones de negocio deben verificar también los permisos y la propiedad de cada reserva.

## 7. Plan de implementación en cinco fases

| Fase | Alcance y dependencias | Estado |
|---|---|---|
| 1. Estabilizar el modelo | Validaciones en constructores/setters; datos finitos; integridad de altas y disponibilidad; colecciones protegidas; hash de contraseñas; pruebas y correcciones de precisión sin elegir redondeo comercial. No requiere las decisiones de cancelación o beneficios. | Alcance de código implementado y verificado con Java 21/JUnit. Gradle verificado en la recuperación; véase apartado 11. |
| 2. Identidad y gestión | Sobre fase 1: autenticación, sesión, autorización por rol/propietario; recargas solo por administrador; edición de correo con unicidad y gestión íntegra de catálogos. | Implementada en negocio y probada en fase 2; integración visual pendiente. |
| 3. Reserva y dinero | Sobre fases 1–2: cotización y detalle histórico, coordinación de disponibilidad/cobro/confirmación, transiciones, pagos y devoluciones idempotentes, cancelaciones y finalización. Requiere acordar hora/zona de entrada, reglas tras iniciar estancia y representación/redondeo monetario. | Pendiente. |
| 4. Beneficios y eventos | Sobre fases 2–3: condiciones de promociones, paquetes, lotes y redención de puntos, notificaciones. Requiere decisiones de acumulación, canjes y cancelaciones del apartado 5. | Pendiente. |
| 5. Aplicación y reportes | Sobre los servicios probados: JavaFX para ambos roles, estadísticas, TXT, barras/pastel/líneas, aceptación integral y README. Persistencia solo si se confirma su alcance. | Pendiente. |

Ya se ejecutaron las fases 1 y 2. No se implementaron las fases 3–5. Los bloqueos comerciales no impidieron avanzar en validaciones y operaciones independientes. La evidencia de cada fase se conserva por separado.

## 8. Pruebas y criterios de aceptación

JUnit Jupiter 5.11.4 y `useJUnitPlatform()` ya están configurados en Gradle; existen diez clases con pruebas en `src/test/java`, además de la preparación compartida de fase 2. La matriz siguiente sigue siendo el objetivo de cobertura para las fases restantes, no una afirmación de que todos estos flujos estén implementados. Los apartados 9 y 10 registran la cobertura realmente ejecutada.

| Área | Casos mínimos |
|---|---|
| Identidad y permisos | Registro válido; ID/correo duplicados; credenciales incorrectas; cliente no puede recargar ni administrar; cliente no consulta/cancela reservas ajenas. |
| Disponibilidad | Superposición parcial/total; salida y entrada el mismo día permitidas; habitación inactiva; intervalo inválido; reserva cancelada libera disponibilidad. |
| Reserva y pago | Capacidad inválida; servicio no disponible; saldo insuficiente; tarjeta inactiva; confirmación exitosa con un solo débito; segundo pago rechazado; fallo sin cambios parciales. |
| Cancelación | Más de 72 h, exactamente 72 h, exactamente 24 h y menos de 24 h; hotel devuelve 100 %; cancelación repetida sin segunda devolución; devolución a la tarjeta original. |
| Promociones | Cada condición cumplida e incumplida; límites de temporada; porcentaje inválido; cambios de catálogo no alteran el importe histórico. |
| Puntos | Acumulación por cada $1.000 pagados; vencimiento anual; consumo por vencimiento más cercano; puntos insuficientes/vencidos; canje repetido; descuento no supera el valor aplicable. |
| Paquetes | Composición anidada; hijo no disponible; ciclo rechazado; precio y beneficio según reglas acordadas. |
| Reportes | Rango vacío/inválido; límites de fechas; cancelaciones y devoluciones; datos consistentes entre TXT y gráficos. |
| Aplicación | Recorrido manual completo de ambos roles, errores comprensibles y resultados visibles tras cada operación. |

La entrega se puede considerar completa cuando RF-001 a RF-026 sean ejecutables desde la aplicación, las reglas RN-01 a RN-23 y las propuestas adoptadas estén verificadas, los reportes funcionen y el README explique instalación, ejecución y pruebas. Conservar en esta carpeta las decisiones y evidencias de verificación.


## 9. Ejecución de la fase 1 (registro histórico)

### Componentes identificados antes de intervenir

Se revisaron el modelo (`User`, `Client`, `Administrator`, `Room`, sus tres subclases, `RoomType`, `Reservation`, `VirtualCard`, `CardMovement`, `AdditionalService`, `Promotion`), `Hotel`, las fábricas, las excepciones y `build.gradle`. No había servicios, controladores, pantallas ni pruebas. Se conservan Singleton, Factory Method y Builder; `Client`, `Administrator`, fábricas y `Main` conservan su código.

### Correcciones y validaciones verificadas

- `DomainValidation` centraliza campos obligatorios, normalización de texto/correo, capacidad/número positivos, importes finitos y validación de intervalos.
- Constructores y setters rechazan datos inválidos antes de cambiar estado. Los precios aceptan cero, pero no negativos, NaN o infinitos; las promociones aceptan los límites 0 y 100.
- Las subclases de habitación comprueban su categoría. `Hotel` rechaza tipos duplicados por categoría, habitaciones con tipos externos y reservas con habitación/cliente externos. La pertenencia se comprueba por el objeto registrado; un objeto nuevo con el mismo identificador no lo sustituye.
- Registro de una misma reserva duplicada rechazado; capacidad, actividad y disponibilidad de servicios se revisan de nuevo al registrarla.
- Consultas de disponibilidad y `overlaps` rechazan fechas nulas, iguales o invertidas; conservan intervalos de salida exclusivos. Las habitaciones inactivas y la superposición siguen excluidas.
- `Hotel` se inicializa mediante holder estático; sus operaciones sobre colecciones están sincronizadas. Dos registros concurrentes superpuestos no pueden superar la comprobación y el alta simultáneamente. Esto **no** equivale a una transacción de reserva/pago ni protege todas las mutaciones directas de las entidades.
- Las listas internas de catálogos, reservas y movimientos se entregan como copias inmodificables. Los objetos contenidos siguen siendo entidades modificables; aún falta el detalle histórico de la fase 3. `getClients` ya devolvía una copia independiente y conserva ese comportamiento.
- `VirtualCard.register` pasa a ser privado: solo operaciones reales de tarjeta generan movimientos. No tenía consumidores fuera de la tarjeta en el código existente.
- Saldo interno decimal y operaciones de tarjeta sincronizadas; los errores de importe, descripción, saldo, actividad u overflow no cambian saldo/movimientos. Se conserva la recepción de devoluciones en tarjetas inactivas.
- `Reservation` calcula importes con aritmética decimal y rechaza resultados no representables como `double` finito, evitando también overflow intermedio en descuentos. Conserva la fórmula existente: servicio una vez por reserva y promoción sobre el subtotal completo. Los getters públicos conservan `double`; aún no hay política comercial de escala/redondeo ni migración integral del contrato monetario.
- `InsufficientBalanceException` es el nuevo nombre utilizado; extiende la excepción antigua `InsuficientBalanceException`, de modo que las capturas existentes siguen funcionando.
- `User` conserva únicamente hash PBKDF2-HMAC-SHA256 de 256 bits y sal aleatoria de 16 bytes; usa 210.000 iteraciones y comparación con `MessageDigest.isEqual`. Constructor y cambio de contraseña comparten validación; no se recortan espacios de la contraseña ni se impone longitud/composición comercial nueva. No se modificaron credenciales ni configuración de producción.
- `Reservation.setStatus(null)` se rechaza. Las transiciones y su vínculo obligatorio con el pago siguen pendientes de la fase 3; no se presentan como resueltos.

### Archivos modificados o añadidos

Rutas relativas a la carpeta que contiene `build.gradle`:

- Modificados: `build.gradle`; `src/main/java/edu/hoteluq/singleton/Hotel.java`; `src/main/java/edu/hoteluq/model/{User,Room,RoomType,StandardRoom,FamilyRoom,SuiteRoom,AdditionalService,Promotion,CardMovement,VirtualCard,Reservation}.java`.
- Nuevos: `src/main/java/edu/hoteluq/validation/DomainValidation.java`; `src/main/java/edu/hoteluq/exceptions/InsufficientBalanceException.java`.
- Nuevas pruebas: `src/test/java/edu/hoteluq/model/{CatalogValidationTest,UserTest,VirtualCardTest,ReservationTest}.java` y `src/test/java/edu/hoteluq/singleton/HotelTest.java`.
- Verificación y documentación: `scripts/verify-offline.py`, `docs/pendientes.md` y `docs/evidencia-fase1.txt`.

### Compilación y pruebas

Comando habitual, desde la carpeta de `build.gradle`, usando Java 21:

```bash
./gradlew --no-daemon clean test build
```

Se intentó con `GRADLE_USER_HOME=/tmp/hoteluq-gradle-home`, para no escribir en la caché del usuario. El wrapper necesita descargar Gradle 8.14.3 y falla con `java.net.SocketException: Operation not permitted`. Una consulta independiente a la distribución también falló por DNS. La distribución Gradle 9.8.0 disponible localmente se probó en modo offline con una copia temporal de las dependencias: falla antes de configurar el proyecto porque no puede determinar una IP wildcard utilizable. No se cambió el wrapper, la versión de JavaFX ni configuraciones externas para sortear estas restricciones.

Alternativa **ejecutada satisfactoriamente**:

```bash
python3 scripts/verify-offline.py
```

El script usa `java`/`javac` del PATH (Java 21), compila código principal y pruebas con `--release 21` y ejecuta las mismas pruebas con JUnit Platform Launcher y Jupiter reales. Necesita los JAR de JUnit 5.11.4 / Platform 1.11.4 en la caché local; permite pasar la ruta de esa caché como argumento. No descarga ni simula dependencias. Limpia únicamente su salida propia en `build/offline-tests`. No reemplaza la validación pendiente de la construcción/distribución Gradle ni una prueba visual de JavaFX.

| Clase de pruebas | Casos ejecutados, incluyendo parámetros | Cobertura principal |
|---|---:|---|
| `CatalogValidationTest` | 13 | Precios no finitos/negativos, límites de descuentos, campos, capacidad, números, categorías, movimientos. |
| `UserTest` | 13 | Ausencia de contraseña en texto plano, sales y hashes distintos, credenciales correctas/incorrectas/nulas, cambio válido/inválido, datos obligatorios, correo, espacios y conservación de tarjeta. |
| `VirtualCardTest` | 11 | Importes inválidos, precisión decimal, saldo insuficiente, inactividad, descripciones, overflow, movimientos protegidos, cobros concurrentes. |
| `ReservationTest` | 8 | Fórmula de precio, decimales, overflow, servicios, capacidad, fechas, superposición, estado nulo, reutilización del Builder. |
| `HotelTest` | 10 | Singleton/fábricas, duplicidad, integridad referencial, disponibilidad, cambios antes del registro, copias protegidas y altas concurrentes. |
| **Total** | **55** | **55 satisfactorios; 0 fallos, omitidos o abortados.** |

Evidencia resumida de la ejecución: `docs/evidencia-fase1.txt`. El control de espacios/errores de parche `git diff --check` también fue satisfactorio.

### Estado de los requisitos y límites

RF-001, RF-002, RF-004–RF-009, RF-015, RF-016, RF-018–RF-020 tienen mejoras o regresiones verificadas del modelo; **continúan parciales**, como indica la matriz del apartado 2. RF-014 y RF-017 se benefician de validaciones en sus entidades, pero continúan parciales porque falta la gestión. Ningún RF se ha marcado como completo. Los demás mantienen su estado anterior.

Pendientes al cierre de la fase 1 (el apartado 10 actualiza lo resuelto después):

- **Fase 2:** autorización (incluida recarga solo por administrador), propiedad de operaciones y unicidad del correo al editar un usuario ya registrado. `setEmail` valida formato, pero todavía puede crear un duplicado tras el alta. Este defecto debe cerrarse al centralizar la edición autenticada, evitando introducir ahora una dependencia de `User` hacia el Singleton.
- **Fase 3:** estados libres distintos de null, confirmación sin pago, cobro/reembolso genéricos sin vínculo idempotente a la reserva, conservación de detalle histórico y representación monetaria integral. No usar el setter de estado como si confirmara un pago.
- **Negocio por confirmar:** los diez puntos del apartado 5 permanecen abiertos. En particular, no se inventaron vencimiento de reservas PENDING, hora de check-in, reglas de cancelación tardía, reversión de puntos, redondeo o alcance de persistencia. Se conserva el bloqueo de disponibilidad que PENDING ya tenía; su plazo sigue pendiente.
- **Entorno:** repetir la construcción completa con el wrapper Gradle 8.14.3 cuando se disponga de la distribución y permisos de red adecuados.

**Continuación recomendada:** ejecutar únicamente la fase 2 después de revisar esta entrega: servicio de autenticación/sesión, autorización por roles y por propietario, recargas administrativas y edición de catálogos/usuarios sin saltarse unicidad ni integridad. Preparar pruebas de acceso denegado, edición de correo duplicado y operaciones válidas de cada rol antes de integrar pagos y confirmación de reservas.


## 10. Ejecución de la fase 2

### Alcance revisado y arquitectura

Se revisaron nuevamente ambos documentos y todos los puntos de entrada del modelo/fábricas. No hay API REST, servidor ni pantallas implementadas. Se conserva Java 21, el Singleton público `Hotel`, Factory Method y Builder. La nueva `HotelService` es una fachada Java local con resultados inmutables (`Views`); no introduce framework ni dependencias. Las sesiones y el estado siguen siendo exclusivamente de memoria.

Los requisitos directamente relacionados son RF-001, RF-002, RF-014–RF-018 y RF-020; también se implementan consultas autorizadas para RF-004, RF-009 y RF-010 y protección de listados para RF-019. No se implementaron pagos/confirmación/cancelaciones, puntos, paquetes ni condiciones promocionales.

### Funcionalidades implementadas y contrato de acceso

- **Registro:** `register` acepta solo datos de cliente, nunca un rol. Cada registro construye su propia tarjeta inicialmente vacía; no admite objetos de usuario/tarjeta suministrados por el solicitante. ID y correo únicos se comprueban bajo el bloqueo del agregado.
- **Login/logout:** verificación PBKDF2 heredada de fase 1; errores genéricos para correo ausente/desconocido, contraseña errónea o datos inválidos. Una cuenta ficticia interna con contraseña aleatoria permite ejecutar el mismo tipo de comprobación para un correo desconocido. No es una cuenta registrada ni una credencial de acceso.
- **Sesiones:** tokens de 32 bytes aleatorios con `SecureRandom`, codificados en Base64 URL; identidad y versión de credenciales conservadas en el servidor. `Session.toString()` oculta el token. Un objeto `Session` fabricado por el llamador no crea autoridad: cada operación consulta el registro interno.
- **Caducidad:** vencimiento absoluto configurable al iniciar; valor predeterminado técnico de 30 minutos. Pruebas usan cinco minutos y reloj controlable. Se rechaza exactamente en el instante de vencimiento, sin esperas reales. No se ha inventado un plazo comercial de reserva PENDING.
- **Revocación:** logout invalida ese token; cambiar/resetear contraseña o eliminar un cliente invalida todas sus sesiones. El cambio de correo conserva la identidad y la sesión existente, pero el siguiente login exige el correo nuevo. Una sesión vencida/revocada tampoco sirve dentro de un scope abierto previamente.
- **Roles:** CLIENT y ADMINISTRATOR derivados del usuario registrado; no hay setter ni parámetro para elevar un rol. Solo el administrador gestiona clientes/catálogos y recarga/activa tarjetas. Los clientes consultan y editan únicamente sus datos, tarjeta e historial; el administrador puede consultar esos datos para gestión.
- **Perfiles:** edición atómica de nombre/correo, con formato y unicidad insensible a mayúsculas y espacios externos. El setter directo de un usuario registrado aplica la misma comprobación y bloqueo. Los datos y credenciales rechazados no reemplazan valores anteriores.
- **Entidades registradas:** `ManagedEntity` recibe una política del agregado que solo se instala una vez. Los setters no pueden saltarse sesión, permisos, unicidad o referencias. El registro de una reserva solo admite el estado inicial PENDING, evitando que un cliente inserte una confirmación preparada en un objeto separado. La comprobación, instalación de política y alta mantienen también el bloqueo de esa reserva, para cerrar una carrera contra su setter mientras todavía está separada. Usuarios, tarjetas y datos sensibles de reservas también controlan lectura. Los objetos separados todavía pueden construirse y probarse sin sesión, pero un cliente no puede insertarlos en el agregado mediante un alta administrativa.
- **Bloqueos:** el orden es agregado → entidad, sin propagar privilegios a hilos trabajadores. `withSession` delimita operaciones y restaura el contexto incluso con excepciones; las comprobaciones se repiten al usar una entidad. Los workers deben presentar explícitamente su token.
- **Consultas:** la fachada devuelve vistas sin contraseñas, hashes, sales ni referencias a entidades modificables. Sus colecciones son inmutables. Los catálogos no contienen datos privados y sus getters de dominio mantienen consulta pública; toda mutación exige administrador. Las consultas de la fachada exigen sesión.
- **Clientes:** CRUD real para el administrador, edición propia para el cliente. La eliminación se rechaza cuando hay reservas, saldo o movimientos, para no perder referencias/auditoría; en los demás casos elimina el acceso y revoca sesiones. Los requisitos no definen usuarios inactivos, por lo que no se añadió un estado comercial nuevo.
- **Tipos y habitaciones:** CRUD, activación de habitaciones y tipos; un tipo inactivo excluye sus habitaciones de nuevas disponibilidades sin cancelar reservas existentes. Se conservan las tres categorías del enum. Categoría de un tipo, número/categoría de una habitación e ID de usuario son identidades estables; se editan los datos descriptivos, tarifas/capacidad y piso/estado que correspondan.
- **Servicios y promociones simples:** alta, consulta, edición y eliminación con UUID estable. Nombre normalizado único dentro de cada catálogo, incluyendo renombrado; porcentajes/precios conservan validaciones. Disponibilidad de servicios editable. Esto no implementa paquetes ni elegibilidad/vigencia de promociones.
- **Referencias:** tipos usados por habitaciones y elementos usados por cualquier reserva no pueden eliminarse, incluso tras cancelación. Las reservas tampoco admiten servicios/promociones externos al catálogo. Los detalles referenciados de habitaciones/tipos/servicios/promociones se protegen contra edición mientras no existan copias históricas; su disponibilidad sí se puede cambiar. Esta protección evita corromper datos existentes y no sustituye los snapshots de fase 3.
- **Recargas:** la regla RN-10 se comprueba también en `VirtualCard.recharge` para tarjetas registradas. Solo un administrador autenticado puede ejecutarla. Cada recarga válida genera un movimiento real; errores de estado/importe/permisos no alteran saldo ni movimientos. Las operaciones concurrentes conservan ambos movimientos y el saldo decimal.

### Inicialización administrativa y uso

Antes de habilitar registro o aceptar usuarios, el código de arranque local confiable debe llamar una sola vez a:

```java
HotelService application = new HotelService();
application.initializeAdministrator(adminId, adminName, adminEmail, adminPassword, sessionLifetime);
```

Los cinco argumentos deben provenir del operador/configuración confiable; no hay credenciales fijas ni cuenta automática en `Main`. La inicialización se bloquea tras el primer administrador y no permite a un cliente convertirse en administrador. **No debe publicarse como una operación de registro anónimo en una futura interfaz remota.** La gestión de otros administradores no está en RF-014 y no se habilitó mediante CRUD de clientes.

Flujo de aplicación: `register(...)` → `login(email, password)` → operaciones usando `session.token()` → `logout(token)`. `me`, `getUser`, `card` y `history` verifican propietario/rol; `clients`, `createClient`, `deleteClient`, `recharge` y CRUD de catálogos verifican administrador. `changePassword` invalida la sesión utilizada, por lo que se requiere nuevo login. Logout con token inválido, vencido o ya cerrado se rechaza.

Las APIs de dominio anteriores conservan sus nombres y algoritmos, pero las mutaciones registradas y consultas privadas ahora requieren contexto autenticado: usar la fachada o `try (var scope = hotel.openSession(token)) { ... }`. No hay modo de omitir permisos para pruebas. La preparación de `HotelTest` se adaptó a ese contrato; se conservaron sus diez pruebas y sus comprobaciones, incluida concurrencia con token explícito.

### Archivos de esta fase

Rutas relativas a la carpeta de `build.gradle`. Los cambios de fase 1 se conservaron, sin revertir controles ni eliminar sus pruebas.

| Tipo | Archivos |
|---|---|
| Nuevos de negocio | `service/HotelService.java`, `service/Views.java` bajo `src/main/java/edu/hoteluq/`. |
| Nuevos de seguridad | `enums/Role.java`, `exceptions/AuthenticationException.java`, `exceptions/AuthorizationException.java`, `security/Action.java`, `security/EntityGuard.java`, `security/ManagedEntity.java`, `security/Session.java` bajo ese mismo paquete raíz. |
| Modelo/registro modificados | `singleton/Hotel.java`; `model/User.java`, `Client.java`, `Administrator.java`, `Room.java`, `RoomType.java`, `AdditionalService.java`, `Promotion.java`, `VirtualCard.java`, `Reservation.java`; `factory/RoomCreator.java`. |
| Pruebas nuevas | `src/test/java/edu/hoteluq/singleton/Phase2Fixture.java`, `AuthenticationSessionTest.java`, `UserAuthorizationTest.java`, `CatalogAdministrationTest.java`, `AuthorizationConcurrencyTest.java`. |
| Pruebas adaptadas | `src/test/java/edu/hoteluq/singleton/HotelTest.java`: preparación con sesión de administrador y scope explícito en workers. Ninguna prueba omitida. |
| Verificación/documentación | `scripts/verify-offline.py` añade resultados por clase; `docs/pendientes.md`; `docs/evidencia-fase2.txt`. |

No se cambiaron dependencias, wrapper, JavaFX, credenciales ni configuraciones de producción. No se hizo despliegue ni se añadieron controladores web.

### Verificación reproducible

Desde la carpeta de `build.gradle`, se intentó:

```bash
GRADLE_USER_HOME=/tmp/hoteluq-phase2-gradle-home ./gradlew --no-daemon clean test build
```

Resultado: código de salida 1, antes de ejecutar tareas. El wrapper intenta descargar Gradle 8.14.3 y falla con `java.net.SocketException: Operation not permitted`. No se presenta como compilación o empaquetado satisfactorio mediante Gradle.

Alternativa ejecutada:

```bash
python3 scripts/verify-offline.py
```

Resultado: código de salida 0 con OpenJDK 21.0.13.0.3, compilación de fuentes principales/pruebas mediante `javac --release 21`, JUnit Jupiter 5.11.4 y Platform 1.11.4 reales de caché. El script permite indicar la ruta de caché como argumento. Evidencia exacta y desglose: `docs/evidencia-fase2.txt`.

| Grupo | Casos aprobados | Alcance |
|---|---:|---|
| Regresión de fase 1 | 55 | Catálogos 13, usuarios 13, tarjeta 11, reservas 8, hotel/fábricas 10. |
| `AuthenticationSessionTest` | 21 | Login válido/inválido, tokens falsificados, logout, límites de caducidad, revocación por cambio/reset/eliminación, scopes y arranque. |
| `UserAuthorizationTest` | 30 | Operaciones administrativas permitidas/denegadas, propiedad, escalamiento, edición/alta duplicada, acceso directo al dominio y datos sensibles. |
| `CatalogAdministrationTest` | 15 | CRUD, duplicidad, validación atómica, disponibilidad, eliminación/edición con referencias, consultas, vistas inmutables y rechazo de confirmación preparada antes del registro. |
| `AuthorizationConcurrencyTest` | 11 | ID/correo/nombres simultáneos, renombrado, creación/eliminación con referencias, registro de reservas frente a eliminación, recargas concurrentes y carrera contra mutación de estado en objetos separados. |
| **Total** | **132** | **132 satisfactorios; 0 fallos, omitidos o abortados.** |

También se comprobó `git diff --check`. Las pruebas ejercitan objetos reales del dominio, sus sesiones y operaciones, sin servicios simulados. El reloj controlable solo evita esperas en las pruebas de caducidad.

### Requisitos, límites y decisiones pendientes

- **Módulos completos y probados en negocio:** registro/login/sesión/roles, CRUD de clientes, habitaciones, tipos y servicios simples; CRUD básico de promociones; consultas de tarjeta/historial y recarga administrativa. La matriz del apartado 2 mantiene el estado de entrega integral para no confundir estas APIs Java con una interfaz terminada.
- **Negocio verificado para RF-001, RF-002, RF-004, RF-009, RF-010, RF-014, RF-015, RF-016 y RF-020.** Su interfaz sigue pendiente; ninguno se declara terminado para el usuario final en esta fase.
- **Parciales en negocio:** RF-003 (consulta de tipos disponible; integración de características/presentación pendiente), RF-005–RF-008, RF-017–RF-019. Servicios simples y catálogos de promociones no completan paquetes ni condiciones comerciales.
- **Pendientes:** RF-011–RF-013 y RF-021–RF-026; no fueron adelantados. Las operaciones de pago/devolución y estado heredadas ya están restringidas, pero aún no coordinan reserva/pago ni validan transiciones. Un administrador todavía puede usar el setter de estado sin prueba de pago; no es un flujo de confirmación y debe cerrarse en fase 3.
- **Límite de concurrencia:** operaciones de administración y entidades registradas se serializan por agregado. No equivale a persistencia transaccional ni al futuro proceso compuesto de cobrar/confirmar/reembolsar. Los registros siguen en memoria y las sesiones se pierden al reiniciar.
- **Decisiones técnicas adoptadas:** tokens aleatorios de 256 bits, vencimiento absoluto configurable (30 minutos predeterminados), revocación de sesiones por contraseña/eliminación, nombres únicos normalizados en cada catálogo, IDs estables y rechazo de eliminación con historial. No se introdujeron bloqueos comerciales de cuentas, vencimiento de reservas, cargos ni reglas de canje nuevas.
- **Por confirmar antes de fase 3:** hora y zona del check-in, tratamiento tras iniciar estancia, momento/actor de COMPLETED y representación/escala/redondeo del dinero. Los otros puntos del apartado 5 siguen abiertos para sus fases respectivas. Si se requieren usuarios inactivos, provisión de administradores adicionales o categorías arbitrarias fuera del enum, hace falta definir ese alcance; no se simularon esas funciones.
- **Entorno pendiente:** repetir `./gradlew clean test build` cuando pueda obtenerse la distribución y abrirse la red requerida por Gradle. La evidencia offline no verifica distribuciones ni interfaz JavaFX.

**Recomendación para fase 3:** conservar la fachada y los controles del agregado; definir primero los puntos comerciales bloqueantes y después implementar una operación de reserva/pago con snapshots, dinero decimal de extremo a extremo, estados explícitos, idempotencia de cobros/devoluciones y política de cancelación con reloj. Añadir pruebas de fallo sin cambios parciales, reintentos y concurrencia antes de exponer confirmaciones en una interfaz.


## 11. Recuperación de integridad tras actualización y reset (14 de noviembre de 2026)

### Diagnóstico y procedencia

- HEAD inicial: `ec2c242` (respaldo de archivos nuevos), sobre `52f986d` (merge RF-011/refund-strategy). Árbol de trabajo inicialmente limpio.
- `git reflog` y `git fsck --no-reflogs --unreachable` permitieron identificar el respaldo local `d15ab30b6fcfdcde1e0d82cb26cab6ba5fb14203` (stash previo a actualizar). Se leyeron sus contenidos sin reset, checkout, aplicación global de stash ni cambios de rama.
- Se comparó ese respaldo con su base `069d8ff` y con GitHub. Los cambios recientes de GitHub se concentran en `Reservation`, `ReservationService`, `state/` y `strategy/`. Los otros 15 archivos recuperados no tenían cambios nuevos de GitHub respecto a la base y se restauraron desde el respaldo. `Reservation` se combinó manualmente.
- La evidencia histórica de 132 pruebas no describía el código después del reset: se habían perdido validaciones, hash, guardas y métodos de sesión/CRUD. El intento inicial `python3 scripts/verify-offline.py` terminó con 30 errores de compilación principal (por ejemplo, `Hotel.login`, `registerClient`, `withSession`, `findUser`, eliminaciones/consultas de catálogos, `RoomType.updateDetails/setActive` y la sobrecarga de Factory Method). No hubo pruebas individuales fallidas: **la compilación impedía iniciarlas**. También faltaba la configuración JUnit de Gradle.

### Integración recuperada

- `Hotel`: Singleton seguro, arranque administrativo, registro, login/logout, sesiones con caducidad/revocación, autorización por rol/propietario, colecciones protegidas, CRUD, unicidad, referencias y serialización de operaciones.
- Usuarios: validaciones, PBKDF2 con sal, roles del servidor, perfil atómico, correo único y revocación por cambio de contraseña. Tarjetas: saldo decimal, importes finitos, movimientos inmutables y recarga/reembolso administrativo.
- Catálogos y Factory Method: validación de categorías/capacidad/precios, UUID, nombres únicos, estado activo/disponible, edición atómica y protección de objetos históricos referenciados.
- Reservas: Builder validado, cálculo decimal intermedio, servicios no nulos, intervalos válidos, propietario, pertenencia al agregado y altas exclusivamente PENDING. `getStatus/setStatus` y `getState/setState` usan una sola representación State; se mantienen las transiciones y políticas incorporadas desde GitHub. Las nuevas entradas `pay/cancel/complete`, lectura del estado y fecha/hora reciben las guardas de fase 2.
- `state/` y `strategy/` permanecen sin cambios respecto a GitHub. Se verificaron >72 h, exactamente 72 h, exactamente 24 h, <24 h y tiempo negativo (100/80/0 % según selector existente). No se inventaron reglas comerciales nuevas.
- Se corrigió una incompatibilidad con efectos monetarios: la secuencia anterior de `ReservationService.confirm` cobraba antes de intentar registrar un estado CONFIRMED que fase 2 rechaza. Ahora esa entrada rechaza explícitamente la confirmación **antes de cualquier débito o mutación**, para reservas separadas y registradas. Se conserva su firma y se deja la operación compuesta para fase 3; no se presenta como un flujo de pago funcional.

### Archivos modificados

Rutas relativas a `hotel-management-system/` (carpeta de `build.gradle`):

| Grupo | Archivos |
|---|---|
| Construcción | `build.gradle` (JUnit 5.11.4, launcher y `useJUnitPlatform`, recuperados del respaldo) |
| Agregado y fábrica | `src/main/java/edu/hoteluq/singleton/Hotel.java`, `src/main/java/edu/hoteluq/factory/RoomCreator.java` |
| Usuarios y dinero | `src/main/java/edu/hoteluq/model/User.java`, `Client.java`, `Administrator.java`, `VirtualCard.java`, `CardMovement.java` (los nombres siguientes al primero corresponden al mismo directorio) |
| Catálogos | `src/main/java/edu/hoteluq/model/Room.java`, `RoomType.java`, `StandardRoom.java`, `FamilyRoom.java`, `SuiteRoom.java`, `AdditionalService.java`, `Promotion.java` |
| Compatibilidad de reservas | `src/main/java/edu/hoteluq/model/Reservation.java`, `src/main/java/edu/hoteluq/service/ReservationService.java` |
| Pruebas nuevas | `src/test/java/edu/hoteluq/singleton/RecoveryIntegrationTest.java` (5 casos) |
| Documentación | `docs/pendientes.md`, `docs/evidencia-recuperacion.txt` |

Las nueve clases de pruebas originales no se modificaron, omitieron ni eliminaron. Las clases nuevas de seguridad, validación, fachada y vistas ya conservadas en HEAD no se sobrescribieron.

### Verificación actual

**`./gradlew --no-daemon clean test build`: BUILD SUCCESSFUL (51 s), salida 0; 137 pruebas satisfactorias, 0 fallos/errores/omisiones según los diez XML de Gradle.** Se verificaron compilación principal/pruebas, JAR y distribuciones ZIP/TAR. `git diff --check` también terminó con salida 0. Resultado definitivo registrado en `docs/evidencia-recuperacion.txt`. Informe navegable local: `build/reports/tests/test/index.html`. Esto cierra el pendiente de construcción Gradle; no verifica interfaz JavaFX. La primera ejecución tras recuperar el modelo ejecutó las 132 pruebas originales mediante Java 21/JUnit real: 132 satisfactorias. Se añadieron cinco pruebas de compatibilidad para State/estado legado, permisos de las nuevas entradas, rechazo de alta confirmada, ausencia de débito ante confirmación incompatible y límites de Strategy.

### Pendientes reales y alcance detenido

- Fases 1 y 2 recuperadas en negocio; pantallas y aceptación visual siguen pendientes. `Main` continúa siendo el ejemplo inicial. No hay persistencia nueva.
- La fase 3 **no se ha iniciado**. El código State/Strategy recibido de GitHub se preserva como trabajo parcial existente; no equivale a completar esa fase.
- La confirmación integral sigue pendiente y `ReservationService.confirm` falla antes de cobrar. `PendingState.pay` consulta el Singleton y una reserva registrada se superpone consigo misma; falta coordinar disponibilidad, pago y registro. No debe usarse como alternativa a una futura operación atómica.
- Los setters administrativos de estado aún pueden cambiarlo sin demostrar pago; las guardas impiden hacerlo al cliente, pero no sustituyen transiciones de negocio completas. Las APIs de cancelación existentes no constituyen todavía un flujo de cliente autorizado: fase 2 reserva las mutaciones de estado/reembolsos al administrador.
- Falta vincular cobros/reembolsos al pago original, impedir duplicados y devoluciones sin cobro, snapshots y atomicidad bajo concurrencia. No se garantiza un flujo de cancelación de extremo a extremo solo por conservar Strategy.
- GitHub ya fija 15:00 como hora de entrada local. Se preservó ese valor; antes de fase 3 hay que confirmar hora/zona y regla tras iniciar estancia. El selector devuelve 0 % con tiempo negativo, comportamiento existente probado, no una decisión comercial nueva de esta recuperación.
- Continúan pendientes redondeo/escala monetaria integral, reglas de finalización, promociones condicionadas, paquetes, puntos, eventos, reportes e interfaz, según apartados 5 y 7.
- No hubo despliegues, commits, push ni publicación en GitHub.
