# HotelUQ: qué falta para completar el proyecto

Revisión actualizada el 14 de noviembre de 2026 tras cerrar H-01 de la auditoría oficial: cancelación 100/80/0, 24 y 72 h inclusivas al 80 %, y hotel 100 %. La fuente principal es el PDF oficial del Proyecto Final HotelUQ (§3.3), disponible en la ruta indicada en `auditoria-proyecto-final.md`; `analisis.md` es documentación secundaria. Los apartados 9–13 conservan cierres históricos. El apartado 14 conserva H-01; el apartado 15 registra H-02.

## 1. Diagnóstico actual

H-04 incorpora una aplicación JavaFX/MVC funcional sobre las fachadas existentes. El arranque ya no es el ejemplo de consola; hay acceso/registro, pantallas de cliente y administración y operaciones reales de reserva/dinero/puntos/promociones. Las decisiones comerciales no aprobadas continúan explícitas y bloqueadas cuando faltan. Los estados y cifras históricos de fases anteriores se conservan en sus apartados y no sustituyen el cierre actual del apartado 17.

La aplicación funciona en memoria: no existe recuperación durable tras cerrar la JVM. H-05 incorpora reportes/estadísticas/TXT; diagramas finales y persistencia durable siguen pendientes. Guía: ejecucion-javafx-h04.md; comprobación/inventario: evidencia-h04.txt.

## 2. Estado por requisito

El estado corresponde a la entrega integral de la aplicación. «Parcial — negocio verificado» distingue requisitos con operaciones Java completas, autorizadas y probadas, cuya integración en la interfaz de la fase 5 está pendiente. No se marca ningún RF como terminado para el usuario final mientras esa integración siga ausente.

| Requisito | Estado | Evidencia y pendiente |
|---|---|---|
| RF-001 Registro | Completo — aplicación local | H-04 integra fachada, pantalla y controles de sesión/rol. Ver guía JavaFX y evidencia H-04. Persistencia durable se sigue como riesgo transversal. |
| RF-002 Inicio de sesión | Completo — aplicación local | H-04 integra fachada, pantalla y controles de sesión/rol. Ver guía JavaFX y evidencia H-04. Persistencia durable se sigue como riesgo transversal. |
| RF-003 Consultar tipos | Completo — aplicación local | H-04 integra fachada, pantalla y controles de sesión/rol. Ver guía JavaFX y evidencia H-04. Persistencia durable se sigue como riesgo transversal. |
| RF-004 Disponibilidad | Completo — aplicación local | H-04 integra fachada, pantalla y controles de sesión/rol. Ver guía JavaFX y evidencia H-04. Persistencia durable se sigue como riesgo transversal. |
| RF-005 Crear reserva | Completo — aplicación local | H-04 integra fachada, pantalla y controles de sesión/rol. Ver guía JavaFX y evidencia H-04. Persistencia durable se sigue como riesgo transversal. |
| RF-006 Servicios | Parcial — negocio verificado | Selección por UUID registrado/disponible, sin duplicados, suma decimal precisa antes del redondeo final; cambios PENDING auditados. Se conserva cobro una vez por reserva. Falta interfaz. |
| RF-007 Promociones | Parcial — negocio implementado | H-03: condiciones AND por fecha/tipo/noches/frecuencia/servicios/paquetes, vigencia y versión revalidadas al confirmar; COP y combinación con puntos solo explícita. Faltan aprobación comercial e interfaz. |
| RF-008 Pago | Parcial — negocio verificado | Solo tarjeta virtual del cliente registrado, COP entero HALF_UP y total positivo; comprobante/movimiento vinculados y atómicos, moneda validada, sin duplicados. Falta interfaz. |
| RF-009 Saldo y movimientos | Completo — aplicación local | H-04 integra fachada, pantalla y controles de sesión/rol. Ver guía JavaFX y evidencia H-04. Persistencia durable se sigue como riesgo transversal. |
| RF-010 Historial | Completo — aplicación local | H-04 integra fachada, pantalla y controles de sesión/rol. Ver guía JavaFX y evidencia H-04. Persistencia durable se sigue como riesgo transversal. |
| RF-011 Cancelar y reembolsar | Completo — aplicación local | H-04 integra fachada, pantalla y controles de sesión/rol. Ver guía JavaFX y evidencia H-04. Persistencia durable se sigue como riesgo transversal. |
| RF-012 Consultar puntos | Completo — aplicación local | H-04 integra fachada, pantalla y controles de sesión/rol. Ver guía JavaFX y evidencia H-04. Persistencia durable se sigue como riesgo transversal. |
| RF-013 Redimir puntos | Parcial — negocio implementado | Descuentos/servicios/paquetes, FEFO, idempotencia, concurrencia y rollback. Requiere equivalencias y política de reversión explícitas; no hay valores activos por defecto. Falta interfaz. |
| RF-014 Gestionar clientes | Parcial — negocio verificado | Alta, consulta, edición y eliminación autorizadas. Correo único incluso en setters y concurrencia; eliminación bloqueada con reservas o movimientos/saldo. Falta interfaz. |
| RF-015 Gestionar habitaciones | Parcial — negocio verificado | CRUD, activación, Factory Method y referencias protegidas; edición de piso/estado con número y categoría estables. Falta interfaz. |
| RF-016 Gestionar tipos | Parcial — negocio verificado | CRUD y activación de tipos dentro de STANDARD/FAMILY/SUITE; nombres/categorías únicos y referencias protegidas. Falta interfaz. |
| RF-017 Servicios y paquetes | Parcial | CRUD de servicios conservado; Composite de paquetes inmutables con precio/disponibilidad y canje real. Falta interfaz y reglas de venta autónoma si se requiere. |
| RF-018 Gestionar promociones | Parcial — negocio implementado | PromotionService administra definiciones/versiones, actividad y auditoría; autorización y referencias verificadas. Catálogo anterior conservado, edición directa de promociones versionadas bloqueada. Falta UI. |
| RF-019 Gestionar reservas | Parcial — negocio verificado, liquidación pendiente | PENDING versionado/auditado; cambios confirmados preparables con autorización/motivo/disponibilidad/diferencia, aplicación económica bloqueada. Finalización solo por check-out explícito con responsable/UUID/horario; horas/ventana pendientes. Falta interfaz. |
| RF-020 Recargar tarjeta | Parcial — negocio verificado | Solo administrador puede recargar tarjetas registradas; valida estado/importe y genera un movimiento. Probado también sin pasar por la fachada y bajo concurrencia. Falta interfaz. |
| RF-021 Cancelación por hotel | Parcial — negocio verificado | Solo administrador, 100 % del pago elegible original, política identificada, COP e idempotencia; no cancela COMPLETED ni duplica devoluciones. Falta interfaz. |
| RF-022 Estadísticas | Completo — aplicación local | H-05: ocho familias, agregación administrativa con corte consistente, filtros, criterios explícitos, totales y datos reales. Limitaciones de ocupación/atribución documentadas en reportes-h05.md. |
| RF-023 Exportación TXT | Completo — aplicación local | TXT UTF-8 con título/corte/periodo/filtros/criterios/registros/totales/COP; FileChooser y autorización, sin sobrescribir sin aceptación. Abstracción para PDF futuro. |
| RF-024 Gráficos por fechas | Completo — aplicación local | H-05: BarChart/PieChart/LineChart, tablas, filtros y selección de series; ReportChartAdapter sin cálculos económicos en controladores. |
| RF-025 Acumulación automática | Parcial — negocio implementado | Integrada atómicamente al pago real de reserva/servicios; sin tasa deja origen diferido, no puntos ficticios. Faltan aprobación comercial, compras independientes e interfaz. |
| RF-026 Notificaciones | Pendiente | No existen eventos, observadores ni bandeja de notificaciones. |

## 3. Hallazgos actuales y seguimiento

Los diagnósticos originales y sus correcciones se conservan en los apartados 9–11. El estado actual es:

- **Resuelto:** validaciones de campos, fechas/capacidad/actividad, importes finitos y descuentos; Singleton seguro, colecciones protegidas y referencias registradas.
- **Resuelto:** hash de contraseñas, sesiones, revocación, autorización por rol/propietario y correo único incluso mediante setters.
- **Resuelto en fase 3:** confirmación/pago y cancelación/reembolso atómicos en memoria; idempotencia; vínculo con pago/tarjeta original; prohibición de alterar estados registrados por setters; detalle histórico copiado y cálculos decimales exactos.
- **Resuelto en fase 3B:** zona Bogotá, COP entero HALF_UP, total positivo y cambios PENDING; devolución por ventanas versionadas, check-out explícito y auditoría transaccional. **Corrección posterior H-01:** devolución oficial activa por defecto y configuraciones incompatibles rechazadas. **Pendiente comercial:** horas, tolerancia de check-out y liquidación de modificaciones CONFIRMED, no tasas de cancelación.
- **Pendiente operativo:** PENDING bloquea indefinidamente; no hay persistencia ni recuperación tras cierre del proceso. No se eligió un plazo comercial de bloqueo.
- **Negocio H-02/H-03:** puntos, paquetes y promociones condicionadas implementados con configuración comercial explícita pendiente. Promociones antiguas sin condiciones siguen bloqueadas. Eventos/notificaciones y JavaFX pendientes.
- **Implementado H-04/H-05:** JavaFX/MVC y reportes reales. Persistencia/recuperación y cierre académico siguen pendientes; ver los apartados 17–18.

## 4. Componentes por implementar

Los nombres siguientes son una propuesta de organización; las responsabilidades y reglas son lo necesario.

| Componente | Responsabilidad |
|---|---|
| `HotelService` + sesiones en `Hotel` | Implementado en fase 2: registrar, autenticar, cerrar sesión y comprobar identidad/rol en cada operación. |
| `ReservationService` + `Hotel` | Implementados cotización, disponibilidad, modificación PENDING, pago/confirmación, cancelación/reembolso y finalización. Puntos/notificaciones corresponden a fase 4. |
| Operaciones de estado | State conserva PENDING → CONFIRMED/CANCELLED y CONFIRMED → CANCELLED/COMPLETED. Cambios registrados solo dentro de transacción; finalización solo mediante check-out administrativo explícito, auditable e idempotente, dentro de ventana autorizada. Hora/ventana pendientes de configurar. |
| `RefundPolicy` | Strategy de GitHub integrada con cálculo decimal, pago original, autorización e idempotencia. Reloj inyectable, Bogotá, perfil oficial 100/80/0, equivalencia validada y snapshot/versiones. Horas contractuales pendientes; no porcentajes. |
| `PointsLedger.Account/Lot` | Implementado H-02: cuenta lógica por cliente, lotes por pago, vencimiento anual, saldo/deuda, FEFO y snapshots. |
| `LoyaltyService` / `Benefit` | H-02: canje atómico de descuentos, servicios y paquetes; equivalencias versionadas explícitas. |
| `PromotionCondition` / `PromotionProgram` | H-03: Specifications, condiciones reales, versiones/auditoría y validación autorizada. Propuestas numéricas de análisis no activas. |
| `ServiceComponent.Leaf/Package` | H-02: Composite inmutable, precio/disponibilidad, referencias válidas y duplicados/ciclos no permitidos. |
| `HotelService` (administración) | Implementado en fase 2 para usuarios y catálogos simples; paquetes H-02 y condiciones H-03 implementados; falta configuración comercial e interfaz. |
| Eventos y observadores | Notificar confirmación, cancelación, pago, recarga y reembolso una sola vez por operación exitosa. |
| `ReportService` | Calcular indicadores con filtros, producir TXT y entregar datos para gráficos. |
| Aplicación JavaFX | Presentar los flujos de cliente y administrador, validaciones y mensajes de error. |

No hace falta crear clases vacías para representar todos los patrones de la sección 7 del análisis: allí se describen como posibles. Si la rúbrica exige patrones específicos, verificar ese punto contra el enunciado original.

## 5. Precisiones funcionales pendientes

El análisis establece las reglas principales, pero deja estas decisiones abiertas. Deben acordarse y documentarse antes de cerrar su implementación:

1. **Horas:** zona oficial ya aprobada: `America/Bogota`. Definir horas de entrada/salida; se guardan fechas de estadía e instantes contractuales por separado, sin desplazar historia.
2. **Cancelación resuelta:** prevalecen las tasas oficiales 100/80/0; se incluyen 24 y 72 h en 80 %. El perfil oficial tiene ID/versión propios y se activa por defecto. Se extiende <24 h a cero y tiempos negativos para estados aún cancelables, sin atribuir al docente una regla expresa de elegibilidad después de llegar. No se inventa una penalización tardía.
3. **Servicios:** establecer si se cobran una vez por reserva, por noche o por huésped. El código actual los cobra una vez.
4. **Promociones:** determinar vigencia, fechas que deben caer dentro de la temporada y base del descuento. El Builder conserva descuento sobre habitación más servicios y una sola promoción; el nuevo flujo pagado rechaza promociones hasta verificar sus condiciones en fase 4.
5. **Puntos y cancelaciones:** definir si se revierten puntos ganados, cómo se recuperan puntos canjeados y qué ocurre si ya se gastaron los puntos de una reserva cancelada.
6. **Beneficios:** precisar si servicios canjeados quedan ligados a una reserva, cómo se valora un paquete y si se acumulan promociones y descuentos por puntos.
7. **Finalización:** solo check-out explícito del administrador, nunca por fecha automática. Definir hora y ventana anticipada/tardía; sin autorización temporal explícita se bloquea. COMPLETED servirá a la promoción de cliente frecuente en fase 4.
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
| 3. Reserva y dinero | Sobre fases 1–2: cotización y detalle histórico, coordinación de disponibilidad/cobro/confirmación, transiciones, pagos y devoluciones idempotentes, cancelaciones y finalización. Fase 3B fija Bogotá/COP/HALF_UP/total positivo y check-out explícito; H-01 fija cancelación oficial. | Implementada y verificada: 272 pruebas. Horas/check-out y liquidación de cambios confirmados pendientes. Ver apartado 14. |
| 4. Beneficios y eventos | Puntos/paquetes H-02 y promociones H-03 implementados con configuración por aprobar; notificaciones pendientes. | Parcial; no se inició JavaFX. |
| 5. Aplicación y reportes | Sobre los servicios probados: JavaFX para ambos roles, estadísticas, TXT, barras/pastel/líneas, aceptación integral y README. Persistencia solo si se confirma su alcance. | Pendiente. |

Se ejecutaron las fases 1 y 2 y se implementó el negocio de fase 3 con configuración comercial pendiente. No se implementaron las fases 4–5. Los bloqueos comerciales no impidieron avanzar en validaciones y operaciones independientes. La evidencia de cada fase se conserva por separado.

## 8. Pruebas y criterios de aceptación

JUnit Jupiter 5.11.4 y `useJUnitPlatform()` ya están configurados en Gradle; existen dieciséis clases con pruebas en `src/test/java`, además de la preparación compartida de fase 2. La matriz siguiente sigue siendo el objetivo de cobertura para las fases restantes, no una afirmación de que todos estos flujos estén implementados. Los apartados 9 y 10 registran la cobertura realmente ejecutada.

| Área | Casos mínimos |
|---|---|
| Identidad y permisos | Registro válido; ID/correo duplicados; credenciales incorrectas; cliente no puede recargar ni administrar; cliente no consulta/cancela reservas ajenas. |
| Disponibilidad | Superposición parcial/total; salida y entrada el mismo día permitidas; habitación inactiva; intervalo inválido; reserva cancelada libera disponibilidad. |
| Reserva y pago | Capacidad inválida; servicio no disponible; saldo insuficiente; tarjeta inactiva; confirmación exitosa con un solo débito; segundo pago rechazado; fallo sin cambios parciales. |
| Cancelación | Más de 72 h, exactamente 72 h, exactamente 24 h y menos de 24 h; hotel devuelve 100 %; cancelación repetida sin segunda devolución; devolución a la tarjeta original. |
| Promociones | Cada condición cumplida e incumplida; límites de temporada; porcentaje inválido; cambios de catálogo no alteran el importe histórico. |
| Puntos | Acumulación con tasa explícita aprobada (sin valor predeterminado); vencimiento anual; consumo por vencimiento más cercano; puntos insuficientes/vencidos; canje repetido; descuento no supera el valor aplicable. |
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


## 10. Ejecución de la fase 2 (registro histórico)

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


## 11. Recuperación de integridad tras actualización y reset (registro histórico, 14 de noviembre de 2026)

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


## 12. Fase 3: reservas, pagos y reembolsos en memoria (registro histórico)

### Diagnóstico, alcance y reglas

Fuentes revisadas: `analisis.md` (RF-004–RF-011, RF-019 y RF-021; RN-02–RN-17, RN-21–RN-23), este seguimiento y `evidencia-recuperacion.txt`. Se parte de las 137 pruebas de recuperación. El proyecto usa un Singleton y colecciones en memoria: **no existe una base de datos ni un procesador de pagos externo**. No se añadieron dependencias, pantallas, puntos, paquetes ni eventos.

RN-06 impide alterar lo contratado en CONFIRMED. La modificación implementada se limita a PENDING y conserva UUID/cliente; cambiar una confirmada exige cancelación y nueva reserva. Su habilitación requiere configuración explícita. Se conservan el cobro de servicios una vez por reserva y el bloqueo indefinido de disponibilidad por PENDING; no se inventa un vencimiento de bloqueo.

Se solicitaron al usuario definiciones de zona/hora de entrada, cancelación después del inicio, actor/momento de finalización, moneda/escala/redondeo, total cero y ratificación de modificación/unidad de servicios. **No se recibió una definición durante esta implementación.** `BookingRules` exige zona, hora y opciones explícitas del operador; no carga defaults en producción. `completionTime == null` bloquea finalización. Los valores UTC/15:00/12:00 y America/Bogota usados en pruebas son fixtures y no acuerdos comerciales.

### Funcionalidad implementada y uso

`ReservationService` comparte el mismo `Hotel` que `HotelService` (Singleton por defecto, inyección para pruebas). Operaciones con token:

- `configure(token, BookingRules)`: solo administrador, una vez y antes de reservas. La política no puede cambiar bajo reservas existentes.
- `create(token, clientId, idempotencyKey, BookingRequest)`: valida cliente/propiedad, habitación registrada y activa, fechas antes del inicio configurado, capacidad y UUID de servicios disponibles; crea PENDING sin débito. Clave única por cliente; un reintento con iguales datos retorna la misma reserva, y reutilizarla con distintos datos se rechaza. No se aceptan importes, tarjetas, roles ni estados del solicitante.
- `get/list`: detalle e historial inmutables, filtrados por propietario o administrador; `HotelService.history` conserva el contrato anterior.
- `modify(token, id, expectedVersion, request)`: solo PENDING, con versión vigente y modificación habilitada. Comprueba disponibilidad excluyendo la propia reserva antes de reemplazar sus condiciones; un conflicto o versión obsoleta no altera nada. CONFIRMED/CANCELLED/COMPLETED son inmodificables.
- `confirm(token, id)`: autoriza propietario/administrador, revalida disponibilidad, actividad de habitación/tipo, servicios, capacidad y fecha; cobra exactamente el total contratado en la tarjeta del cliente y confirma una sola vez. Reintentar una confirmación ya exitosa retorna el mismo comprobante sin débito adicional. No confirma canceladas ni completadas. Las reservas separadas o de otro agregado son rechazadas.
- `cancelByClient`: solo propietario, política Strategy de anticipación calculada con `Clock` del servidor y zona/hora configuradas. Los límites de 72/24 h se conservan exactamente. El solicitante no puede elegir la política ni falsear la fecha de cancelación.
- `cancelByHotel`: solo administrador; usa `FullRefundPolicy`. Ambas cancelaciones son atómicas y retornan la primera decisión de cancelación en reintentos, incluso si el segundo intento es del otro actor; no hay devolución adicional ni aumento posterior de una devolución parcial.
- `complete`: solo administrador; exige pago y CONFIRMED y permite finalizar desde la hora configurada de la fecha de salida. Reintento de COMPLETED es inocuo. Sin hora definida se rechaza explícitamente.

Las firmas antiguas de `ReservationService` para objetos se conservan y requieren scope autenticado del mismo agregado; ya no cobran sobre objetos sin registrar. La sobrecarga con `LocalDateTime` del llamador se conserva deprecada, pero rechaza su uso: las pruebas controlan `Clock`, no una fecha de cliente falsificable.

### State, Strategy y datos históricos

- `PendingState` y `ConfirmedState` conservan las transiciones de GitHub. Se retiraron de los estados los efectos monetarios y el acceso global al Singleton: ahora la transacción coordina efectos y State decide las transiciones válidas. `CancelledState`, `CompletedState`, la interfaz State y el selector de anticipación se conservan.
- Los setters/operaciones de estado de entidades registradas ya no son un modo de confirmar gratis, reabrir canceladas o devolver dinero. Ni siquiera el administrador puede utilizarlos fuera de la operación transaccional. Los objetos separados conservan el contrato de construcción/pruebas y no pueden darse de alta si su estado no es el PendingState original.
- `Quote` copia cliente, nombre/correo, habitación/tipo/descripción/características, tarifa/noches/huéspedes/fechas, servicios con UUID/nombre/precio, promoción e importes. Sus listas son inmutables. Se preserva la cotización del borrador aceptada al confirmar; no se recalcula silenciosamente el precio.
- `Payment` incluye UUID, reserva, tarjeta original, importe decimal e instante. `Refund` incluye UUID, reserva, pago original, tarjeta, importe, política, actor e instante. En PENDING se registra una decisión de cancelación por cero y sin pago ficticio; en una cancelación sin devolución tampoco se crea un movimiento monetario de cero.
- Los movimientos de pago/reembolso incluyen UUID propios, UUID de reserva y pago original e importe decimal exacto. La tarjeta del cliente es final e insustituible. Las tarjetas inactivas siguen aceptando el reembolso original (contrato preexistente).
- Se mantienen las protecciones conservadoras de edición/eliminación de catálogos referenciados; los snapshots no se usan como excusa para borrar referencias históricas. El perfil del cliente puede editarse sin alterar el contrato copiado.
- Dinero de cotización, pago, saldo y reembolso se procesa con `BigDecimal` sin redondeo implícito. Strategy tiene una API decimal exacta para 100/80/0 %; la API `double` se conserva por compatibilidad. Los catálogos y recargas aún aceptan los `double` finitos del contrato previo, convertidos con `BigDecimal.valueOf`; vistas y movimientos exponen importes exactos adicionales. No se afirma haber adoptado una moneda/escala comercial.
- Las promociones no tienen condiciones verificables todavía. Se conserva su catálogo y fórmula del Builder, pero el nuevo flujo pagado las rechaza para cumplir RN-21; no se inventan condiciones ni se adelanta fase 4.

### Consistencia transaccional y fallos parciales

`Hotel` mantiene su monitor durante autorización, validación, débito/reembolso, transición State, registro del comprobante y resultado. Las guardas de todas las entidades registradas usan ese mismo monitor. Otros lectores, recargas, administraciones y operaciones concurrentes no pueden observar ni modificar un estado intermedio. La comprobación de disponibilidad excluye la propia reserva, corrigiendo la autosuperposición del PendingState anterior.

Antes de escribir se capturan checkpoints privados del estado completo de reserva (condiciones, State, pago, reembolso y versión) y tarjeta (saldo exacto y movimientos). Ante `RuntimeException` o un `Error` recuperable se restaura el estado exacto antes de propagar el fallo. Esto es compensación en memoria bajo exclusión mutua: **no se añade un movimiento de reembolso para compensar un cobro fallido**, ni quedan comprobantes de operaciones que no se completaron. Restauración y transferencias solo funcionan dentro de la transacción interna del agregado; el solicitante no puede usarlas para fabricar saldo.

Idempotencia de creación por cliente/clave, de confirmación por UUID de reserva y de cancelación por la decisión de reembolso guardada. El comprobante no se registra antes de completar los pasos de estado y tarjeta. Tras fallo se permite reintentar; tras éxito se devuelve la misma identidad de pago/reembolso. El reembolso se calcula sobre el importe efectivamente pagado, no sobre el catálogo, y se limita a ese importe. Un reembolso por reserva como máximo, incluso si era parcial o cero.

No hay garantía de recuperación ante cierre de JVM, fallo de máquina u agotamiento de memoria que impida restaurar: reservas, saldo, sesiones e idempotencia se pierden al reiniciar. Esta solución sirve al proceso único y almacenamiento en memoria actuales; no equivale a transacción distribuida ni durable. Si luego se integra persistencia o una pasarela externa, se debe rediseñar commit/compensación/idempotencia durables antes de conectar esos efectos.

### Pruebas y regresión

**Verificación final: `./gradlew --no-daemon clean test build` con Java 21 y Gradle 8.14.3 terminó en BUILD SUCCESSFUL (1 min 4 s), salida 0: 181 ejecutadas, 181 aprobadas, 0 fallidas/errores/omitidas.** Se compilaron fuentes/pruebas y se generaron JAR y distribuciones. `git diff --check` también terminó con salida 0. Evidencia definitiva y conteos por clase en `evidencia-fase3.txt`; informe HTML en `build/reports/tests/test/index.html`. Se conservan los 137 casos anteriores y se agregan 44: 37 de `BookingTransactionTest` y 7 de `BookingRulesTest` (incluyen parámetros). No se eliminaron ni omitieron pruebas.

Cuatro casos antiguos dependían de los contratos intencionalmente cerrados: cancelación mediante setter en `HotelTest` y `CatalogAdministrationTest`, y setters/confirmación deshabilitada en dos casos de `RecoveryIntegrationTest`. Se adaptaron para ejercer la cancelación transaccional, verificar el rechazo de confirmación gratis y comprobar el nuevo cobro válido. `Phase2Fixture` y `HotelTest` declaran políticas explícitas solo para pruebas.

Casos nuevos: datos/capacidad/fechas/catálogos inválidos; superposición y estancias adyacentes; pago válido/saldo insuficiente/tarjeta inactiva/servicio inactivo; claves duplicadas y reintentos; modificación versionada/conflicto; snapshot histórico; acceso ajeno y bloqueo de mutaciones monetarias/estado/restauración; fronteras 72/24 h; cancelación pendiente, parcial, completa, cero y reintentos cruzados cliente/hotel; tarjeta original inactiva; fecha de cancelación falsificada; finalización y estados terminales; decimales con más de dos posiciones; política explícita, total cero y zona horaria.

La inyección de fallo es un hook package-private del agregado, inaccesible a solicitudes de aplicación, en tres puntos de confirmación y cancelación: después del movimiento, después del State y después del comprobante. En todos se comprueba igualdad del estado/saldo/movimientos previos y un único efecto tras reintentar. Se prueba además un Error recuperable y que un lector concurrente queda bloqueado hasta rollback. Las carreras cubren altas superpuestas, alta/confirmación idénticas, dos pagos con saldo para uno, dos cancelaciones, confirmación contra cancelación y dos modificaciones de la misma versión.

### Requisitos y pendientes

- **Completos en negocio con políticas explícitas configuradas:** RF-004, RF-005, RF-006, RF-008–RF-011, RF-019 y RF-021 para los flujos descritos. La entrega integral sigue parcial por interfaz y ratificación comercial pendiente. RF-019 no incluye alterar una confirmada porque RN-06 lo prohíbe.
- **Parciales:** RF-007 (catálogo sin elegibilidad), RF-017/RF-018 (paquetes y condiciones pendientes), RF-025/RF-026 (puntos/eventos sin implementar). Ningún RF nuevo se declara completo para el usuario final desde JavaFX.
- **Definiciones aún requeridas:** hora/zona del hotel; cancelación tras inicio; momento de finalización; moneda/escala/redondeo; total cero; ratificación de cambios PENDING y de servicios una vez por reserva. `BookingRules` permite habilitar decisiones explícitas sin cambiarlas silenciosamente. Los porcentajes y límites documentados no se hicieron configurables ni se alteraron.
- **Operación:** bloqueos PENDING sin vencimiento, proceso único, sin persistencia y crecimiento de claves de idempotencia/historial en memoria. No se añadió expiración comercial ni poda que permita reutilizar una clave exitosa.
- **Recomendación para fase 4:** ratificar primero esas decisiones y las reglas de puntos al cancelar/canjear; luego añadir condiciones de promociones, paquetes, puntos y eventos sobre el resultado confirmado de esta transacción. Los observadores externos solo deben ejecutarse después del commit y necesitarán idempotencia/outbox si se adopta persistencia. **La fase 4 no se inició.**

### Archivos de fase 3

Rutas relativas a la carpeta de `build.gradle`:

- Nuevos: `src/main/java/edu/hoteluq/model/BookingRecords.java`, `src/main/java/edu/hoteluq/service/BookingRequest.java`, `BookingRules.java`; `src/test/java/edu/hoteluq/singleton/BookingTransactionTest.java`, `BookingRulesTest.java`; `docs/evidencia-fase3.txt`.
- Modificados: `src/main/java/edu/hoteluq/model/Reservation.java`, `VirtualCard.java`, `CardMovement.java`; `security/Action.java`; `service/ReservationService.java`, `Views.java`; `singleton/Hotel.java`; `state/PendingState.java`, `ConfirmedState.java`; `strategy/RefundPolicy.java`, `FullRefundPolicy.java`, `PartialRefundPolicy.java`, `NoRefundPolicy.java` (todos bajo `src/main/java/edu/hoteluq/`).
- Pruebas adaptadas: `src/test/java/edu/hoteluq/singleton/Phase2Fixture.java`, `HotelTest.java`, `CatalogAdministrationTest.java`, `RecoveryIntegrationTest.java`.
- Documentación actualizada: `docs/pendientes.md`. No se cambió `build.gradle`, versiones, wrapper ni dependencias. No se ejecutaron despliegues, commits ni push.


## 13. Fase 3B: reglas comerciales y cierre funcional (registro histórico)

**Vigencia:** la política de cancelación descrita en este cierre histórico fue sustituida por las reglas oficiales del apartado 14. Las tasas parciales tardías y la ausencia de perfil predeterminado ya no corresponden al código actual.

### Reglas implementadas

1. **Tiempo:** `HotelTime` centraliza Bogotá y reloj inyectable. Instantes de operaciones y sesiones son inequívocos; calendario de estadía es `LocalDate`. Entrada/salida se capturan como `Schedule` al registrar/modificar PENDING y no se desplazan por activar políticas nuevas ni por cambiar la zona del servidor. Las recargas también usan el reloj del agregado. No se añadió TTL de PENDING ni vencimientos de puntos de fase 4.
2. **Cancelación:** `CancellationSchedule` valida partición completa ordenada [inferior incluido, superior excluido], ID/versión y porcentajes [0,100]. Las ventanas posteriores al inicio contractual exigen porcentaje parcial. No hay política de cliente activa por defecto. Las activaciones se autorizan y auditan; versiones publicadas no se reescriben ni reactivan. `Refund` conserva ID/versión/porcentaje/configuración completa, anticipación, instante, moneda, pago y tarjeta originales. Un único instante de referencia se usa en cálculo/comprobante/auditoría. Se calcula sobre lo cobrado elegible no devuelto, redondea una vez y no lo supera. El hotel mantiene RN-14 de 100 %. Se conservan las Strategies legadas; el selector 100/80/0 es ejemplo, no ruta productiva.
3. **Check-out:** operación explícita autorizada, State CONFIRMED y pago obligatorios. Guarda UUID, clave idempotente, usuario, instante real/programado, zona y ventana autorizada; evento con mismo UUID/instante. Reintento con igual clave devuelve la misma operación; otra clave se rechaza. No se finaliza por alcanzar una fecha. Sin hora/ventana definida se bloquea; una ventana ausente puede completarse explícitamente una vez si ya había hora contractual, sin mover fechas.
4. **Dinero:** `CopMoney` centraliza COP, BigDecimal, HALF_UP y escala 0 final. Tarifas/servicios y porcentajes se almacenan/calculan decimalmente; firmas double antiguas son adaptadores de entrada/consulta. Quote guarda brutos y finales conciliados; el total se redondea una vez tras sumar servicios/descontar. Pago, saldo, movimientos, recargas y reembolsos son enteros COP; se rechazan otras monedas. No se recalculó historia existente ni se instaló una migración monetaria.
5. **Total positivo:** Builder y confirmación exigen total final mayor que cero, incluido después del redondeo. Descuento completo, total negativo o fracción que redondee a cero no producen reserva/pago/movimiento. No se puede rehabilitar total cero mediante el constructor legado de reglas.
6. **Cambios:** PENDING mantiene UUID/versión, recalcula precio/disponibilidad/capacidad/fechas/servicios y audita antes/después. Reservas confirmadas solo permiten preparar una propuesta administrativa con motivo, versión, disponibilidad y diferencia COP; `applyConfirmedModification` permanece bloqueado por falta de regla de liquidación. No hay cargos/devoluciones adicionales. Canceladas/finalizadas se rechazan.

### Integridad y alcance

Se conserva el monitor del agregado, las guardas de fases 1–2 y State/Strategy. Los checkpoints ahora incluyen horarios, check-out y auditoría además de saldo/condiciones/versión/comprobantes. Fallos al modificar o registrar check-out restauran también el historial. Un fallo después de registrar el alta retira la reserva para no dejar habitación bloqueada ni consumir una clave idempotente sin resultado. Las operaciones contables de objetos separados se rechazan: no se puede preparar un pago ficticio y dar de alta un PENDING aparentemente pagado.

No se añadieron observadores/notificaciones, puntos, paquetes, promociones condicionadas, pantallas ni reportes. No se instaló una base de datos, no hubo migración y no se hicieron commits, push ni despliegues. Se preservaron los cambios locales de fase 3 presentes al iniciar; no se aplicó reset ni se sobrescribieron desde GitHub.

### Verificación y evidencia

**`./gradlew --no-daemon clean test build` con Java 21 / Gradle 8.14.3 y `JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Tokyo`: BUILD SUCCESSFUL (1 min 47 s), salida 0. 242 ejecutadas, 242 aprobadas, 0 fallidas, errores u omitidas.** Son los 181 casos anteriores (sin eliminar ni omitir pruebas, con expectativas ajustadas a zona/COP/total positivo/check-out) y 61 nuevos: 20 de `CommercialPolicyValidationTest`, 36 de `Commercial3BTest` y 5 de `CommercialConfigurationTest`. `git diff --check`: salida 0. Informe local: `build/reports/tests/test/index.html`; detalle por clase: `evidencia-fase3b.txt`.

Cobertura nueva: día/mes/año en Bogotá; JVM con Asia/Tokyo; reloj mutable e instantes concurrentes en cambio de año; fronteras exactas y nanosegundos de ventanas; políticas faltantes/inválidas/solapadas/incompletas, versiones y traza histórica; devolución tardía parcial con fallos/reintentos/concurrencia; cambio de política concurrente con cancelación; check-out válido/ajeno/duplicado/pendiente/cancelado/anticipado/tardío/concurrente y su rollback; ausencia de finalización automática; configuración temporal pendiente; HALF_UP/0,5/servicios/descuentos y conciliación monetaria; cero/negativo/100 %/redondeo a cero; rechazo de otra moneda; propuestas confirmadas bloqueadas, auditoría PENDING y rollback de modificación/alta; rechazo de contabilidad falsa en objeto separado.

Las fixtures 100/80/0 y 25/35 % usadas en pruebas no representan aprobación de valores de producción. Los horarios 15:00/12:00 y ventanas de prueba tampoco se activan en `Main`. La nota de deprecación de compilación proviene de pruebas deliberadas de compatibilidad/rechazo de APIs antiguas.

### Configuración pendiente, requisitos y siguiente paso

- **Fijos y aprobados:** Bogotá, COP, HALF_UP a cero decimales, total positivo, modificación válida PENDING, finalización solo por check-out, protección del contrato confirmado.
- **Sin valores aprobados aún:** horas de entrada/salida; ventanas y porcentajes concretos de cancelación; tolerancia de check-out anticipado/tardío; liquidación económica de modificaciones CONFIRMED. Se consultaron al responsable durante esta ejecución. La configuración Java existe y se valida; no se rellenan valores por suposición.
- **Completos en negocio bajo configuración explícita:** RF-004–RF-006, RF-008–RF-011 y RF-021 para los flujos descritos. **RF-019 parcial:** el control/consulta/modificación PENDING y check-out están implementados; liquidar cambios confirmados queda deliberadamente bloqueado. RF-007, RF-017/RF-018 y RF-025/RF-026 mantienen pendientes de fase 4. Ninguno se declara entrega final de interfaz.
- **Riesgos técnicos:** estado, claves, políticas, ledger e historial siguen en memoria; se pierden con la JVM. PENDING sigue sin TTL aprobado. Los catálogos referenciados mantienen protección conservadora de edición/eliminación. La persistencia propuesta exige resolver representación de importes/timestamps históricos y restricciones transaccionales antes de migrar.
- **Propuesta durable:** repositorios JDBC y unidad de trabajo relacional para reservar/cobrar/cancelar/auditar/idempotencia en un commit. Motor y dependencias requieren autorización. Esquema, riesgos y pruebas de cierre/reinicio en `propuesta-persistencia.md`; no se implementó esa arquitectura.
- **Recomendación antes de fase 4:** completar la configuración comercial y definir reversión de puntos/canjes tras cancelación. Revisar y autorizar por separado persistencia; después implementar condiciones, paquetes, puntos y eventos usando el commit como frontera. La fase 4 no se inició.

La documentación operativa y el ejemplo parametrizado de configuración están en `configuracion-comercial.md`. El inventario exacto de archivos nuevos/modificados de **fase 3B respecto a los contenidos de fase 3 al iniciar**, junto con los resultados, está en `evidencia-fase3b.txt`; las evidencias de fases 1–3 y recuperación permanecen intactas.


## 14. Corrección de cancelaciones oficiales — cierre H-01

### Reglas y decisiones interpretativas

Fuente: enunciado oficial §3.3 y solicitud de corrección posterior a auditoría. El cliente recibe 100 % si faltan más de 72 h, 80 % si faltan de 24 a 72 h inclusive y 0 % si faltan menos de 24 h. El hotel devuelve 100 % para estados cancelables sin depender de la anticipación. A las **24 h exactas** se adopta la interpretación inclusiva de la franja 24–72 h; es una interpretación documentada por el proyecto, no una aclaración atribuida al docente. Las **72 h exactas** devuelven 80 %.

No se introducen nuevos plazos de elegibilidad. Se conserva PENDING/CONFIRMED como conjunto cancelable y rechazo de COMPLETED. Para el dominio ya admitido después de llegada (`timeLeft < 0`) se aplica 0 %, extensión técnica de <24 h; el PDF no se presenta como si hubiera regulado expresamente ese derecho. El hotel mantiene 100 % aunque ya se haya alcanzado la llegada, mientras la reserva no esté finalizada. CANCELLED retorna la primera decisión sin un abono nuevo, incluso al reintentar desde el otro tipo de operación.

### Código y transacción

- `CancellationSchedule.official()` fija `HOTELUQ-ACADEMIC-CANCELLATION/v1` y particiona sin huecos/solapamientos: (-infinito,24h)→0; [24h,72h+1ns)→80; [72h+1ns,+infinito)→100. El nanosegundo es la resolución de Duration para representar estrictamente >72 h, no una tolerancia comercial.
- `Hotel` activa/registra ese perfil desde su construcción. Ya no requiere un administrador que invente tasas para habilitar la cancelación; las horas contractuales siguen configurándose por separado.
- `configureCancellation` conserva autorización/registro/historial, pero exige `requireOfficial()` antes de cualquier cambio. La comprobación cruza todas las ventanas con las oficiales: no se elude con segmentos remotos o muy pequeños. Particiones equivalentes y escalas 80/80.00 se aceptan; ID/versión pueden documentar nuevas representaciones técnicas, sin cambiar derechos de devolución.
- La ruta de cancelación vuelve a validar el perfil activo. `RefundPolicySelector` comparte los mismos límites oficiales. No se modificaron clases State ni las estrategias de cálculo Full/Partial/No.
- Se retiró únicamente la restricción 3B de parcial obligatorio para tiempos negativos. Los snapshots genéricos históricos siguen siendo legibles/calculables; no pueden activarse si contradicen el perfil. Los recibos previos no se recalculan ni se reescriben en un reintento.
- Se conservan sin cambios `bookingAtomic`, tarjeta/ledger, comprobantes y guardas: pago/tarjeta originales, importe elegible no reembolsado, límite al importe cobrado, COP/BigDecimal/HALF_UP, un instante de referencia, estado/recibo/auditoría juntos y restauración exacta tras fallo. La cancelación de un PENDING no pagado no crea dinero ni un movimiento de devolución.

### Regresión y evidencia

`./gradlew --no-daemon clean test build` con Java 21: **BUILD SUCCESSFUL en 1 min 46 s, salida 0; 272 ejecutadas/aprobadas, 0 fallos/errores/omisiones**. `git diff --check`: salida 0. Se conservan los 242 casos anteriores: se adaptan solo expectativas y fixtures que asumían política ausente, porcentajes alternativos o 72 h al 100 %. No se eliminan ni deshabilitan pruebas.

`OfficialCancellationTest` agrega 30 casos: 9 límites exactos/adyacentes y llegada pasada, 4 anticipaciones de cancelación por hotel, 5 configuraciones incompatibles (incluido un nanosegundo remoto), 5 escenarios de día/mes/año y 7 casos de saldo insuficiente/no pagado, devolución con saldo cero, equivalencia/versiones, concurrencia, autorización y datos históricos no activables. Los casos anteriores de fallo parcial se mantienen y ahora prueban una devolución oficial real del 80 %, con rollback de saldo, estado, recibo e historial.

Evidencia e inventario por ejecución: `evidencia-cancelacion-oficial.txt`. Se modifican tres fuentes principales (`Hotel`, `CancellationSchedule`, `RefundPolicySelector`), cinco archivos de pruebas/fixture y documentación; se añade una clase de prueba. El resto de las fuentes y pruebas permanece igual respecto al inicio de esta corrección.

### RF y pendientes restantes

RF-011 corregido y probado en negocio; RF-021 confirma 100 % por hotel; RF-008/RF-009/RF-010/RF-019 conservan coherencia entre pago, tarjeta, historial y estado. Todos siguen parciales como aplicación final por ausencia de JavaFX. El hallazgo H-01 queda cerrado en código; los demás hallazgos de auditoría permanecen.

No se iniciaron puntos, promociones, JavaFX ni otras fases. No hay persistencia durable nueva, despliegues, commits ni push. Siguen pendientes horas/ventana de check-out y liquidación de modificaciones confirmadas, además del alcance académico ya auditado; no se vuelve a pedir al grupo elegir porcentajes que el docente ya definió.

## 15. H-02 — Puntos y paquetes (implementación de negocio)

**Verificación histórica H-02:** Java 21 / Gradle 8.14.3; `./gradlew --no-daemon clean test build`: BUILD SUCCESSFUL (2 min 9 s), 351 pruebas aprobadas, 0 fallidas/errores/omitidas; 272 anteriores conservadas sin editar + 79 nuevas. `git diff --check`: salida 0. Evidencia: [evidencia-h02.txt](evidencia-h02.txt).

Se implementan `LoyaltyService`, `PointsLedger`, `LoyaltyPolicy`, `Benefit`, `LoyaltyCatalog` y Composite `ServiceComponent.Leaf/Package`; `Hotel` solo coordina autorización, monitor y checkpoints con reserva/tarjeta. `Reservation.applyPointsBenefit` mantiene contrato histórico y validación de total positivo. No se duplican clientes, sesiones, cobros ni reglas oficiales de cancelación.

Cuenta lógica por cliente consultable por propietario/administrador; créditos por UUID de pago real, vencimiento individual por aniversario Bogotá, eventos, FEFO, idempotencia y canjes de los tres tipos. El pago de servicios dentro de la reserva integra la base efectiva, sin otorgar puntos por recarga, rechazo o parte bonificada. Los reembolsos ajustan el pago original bajo su política histórica; puntos usados pueden producir deuda y la restauración conserva la caducidad original. Todos los efectos se restauran con reserva/tarjeta si falla la transacción. Las 272 pruebas anteriores se conservan sin editar sus archivos.

**Configuración de producción pendiente:** tasa, equivalencias y reglas de reversión/compensación/restauración. Sin tasa, se registran orígenes diferidos; no se activan tasas de ejemplo ni beneficios implícitos. La aprobación de una política y la liquidación retroactiva son operaciones administrativas diferentes. Los orígenes cancelados no se liquidan retroactivamente. Ver contrato, opciones y límites exactos en [programa-puntos-h02.md](programa-puntos-h02.md).

RF-012/013/017/025 avanzan en negocio; siguen parciales como aplicación final. Falta compra autónoma de servicios fuera de reserva, configuración aprobada e interfaz. Se incorpora un patrón estructural real: Composite; inventario pasa a 3 creacionales / 2 estructurales / 2 de comportamiento. No se añaden patrones artificiales, promociones ni JavaFX.

Verificación definitiva y lista de archivos en [evidencia-h02.txt](evidencia-h02.txt). Continúa el riesgo de pérdida tras reinicio: memoria sin durabilidad, incluida configuración/ledger/idempotencia. No se incorpora persistencia nueva; se mantiene la propuesta para aprobación. No se realizaron commits, push ni despliegues.

## 16. H-03 — Promociones condicionadas y descuentos

**Verificación histórica H-03:** Java 21 / Gradle 8.14.3; `./gradlew --no-daemon clean test build`: BUILD SUCCESSFUL (2 min 27 s), 416 pruebas ejecutadas/aprobadas, 0 fallidas, 0 errores y 0 omitidas. Se conservan las 351 anteriores sin editar sus archivos y se añaden 65 (22 unitarias de reglas/precio y 43 de integración). `git diff --check`: salida 0. Evidencia: [evidencia-h03.txt](evidencia-h03.txt).

RF-007/RF-018 implementados en negocio con `PromotionService`, `PromotionDefinition`, `PromotionProgram`, `PromotionCondition` y `PriceComponent`. Configuración comercial concreta y UI siguen pendientes, por lo que los RF no se declaran completos como aplicación final. Se conserva catálogo previo; porcentajes antiguos sin reglas siguen sin poder pagarse.

Las configuraciones exigen descripción, actividad, condiciones, porcentaje BigDecimal, periodo completo cuando aplica y decisiones de combinación/acumulación. Ventana de operación [desde,hasta); fechas de estadía separadas; noches/categoría/frecuencia/servicios/paquetes se evalúan con hechos autoritativos. Frecuencia soportada: reservas COMPLETED pagadas y no canceladas del mismo cliente, con umbral explícito sin predeterminado.

Una promoción por reserva. Combinación con un beneficio por puntos solo por opción explícita EXCLUSIVE / PROMOTION_THEN_POINTS / POINTS_THEN_PROMOTION, con base y fórmulas documentadas en [promociones-h03.md](promociones-h03.md). También se exige EARN_ON_NET_PAYMENT o EXCLUDED para acumulación. No se activan porcentajes, plazos ni equivalencias de puntos de ejemplo.

`BookingRequest` admite paquetes y conserva constructor anterior. Rechaza duplicación de servicios al combinar selecciones. Cotizaciones/BookingView y auditoría guardan definición/versiones y selección de paquetes. Reglas cambiadas en PENDING exigen aceptar nueva cotización versionada; contratos pagados y reintentos se conservan aunque la configuración cambie o expire.

La transacción reserva/tarjeta/puntos, State/Strategy y Composite se conservan. Cancelaciones oficiales sin cambios: se devuelven porcentajes sobre el pago neto real, enlazado al pago original. Las pruebas cubren fallos después de transferencia, State y recibo con restauración de todos los efectos.

Se incorpora Decorator estructural para composición económica y Specification como patrón de dominio para predicados AND. Conteo GoF conservador: 3 creacionales / 3 estructurales / 2 comportamientos; Specification se documenta aparte, sin certificar por sí sola el mínimo académico de comportamiento. Faltan un estructural y un comportamiento GoF inequívoco, además de diagramas finales PDF.

Evidencia reproducible e inventario exclusivo de H-03: [evidencia-h03.txt](evidencia-h03.txt). No se inició JavaFX ni se incorporó persistencia durable, dependencias nuevas, commits, push o despliegues. Se conserva la compra autónoma fuera de reservas como pendiente previo, junto con las decisiones comerciales de H-02 y las nuevas configuraciones de H-03.

## 17. H-04 — Interfaz JavaFX/MVC

**Verificación H-04 (15/11/2026):** Java 21 / Gradle 8.14.3, BUILD SUCCESSFUL. Regresión sin pantalla: 457 aprobadas (416 anteriores intactas + 41 nuevas). Integración gráfica real: 3 aprobadas. **Total: 460 ejecutadas/aprobadas, 0 fallidas, 0 errores y 0 omitidas.** Arranque real con `run --args=--smoke-ui`: salida 0 y `HOTELUQ_UI_STARTED`. `git diff --check`: salida 0. Detalle: [evidencia-h04.txt](evidencia-h04.txt).

Aplicación Java 21 ejecutable con `./gradlew run` desde hotel-management-system/. Main ya no imprime el ejemplo inicial: abre HotelApplication, FXML y CSS reales. El primer arranque pide administrador local; no hay credenciales, horarios, promociones ni equivalencias de producción sembradas. Todos los datos siguen en memoria y se pierden al cerrar.

Se implementan registro/acceso/logout; tablas de habitaciones/tipos, fechas/disponibilidad, detalle, servicios/paquetes/promociones elegibles, cotización sin efectos, borrador/edición PENDING, confirmación del pago, historial, cancelación con preconsulta del porcentaje/importe, tarjeta/movimientos y puntos/lotes/vencimientos/canjes. Administración: clientes, habitaciones, tipos, servicios/paquetes, promociones versionadas/auditoría, recargas, reservas/cancelaciones del hotel, check-out y parámetros comerciales permitidos.

FXML y componentes visuales no calculan negocio. DesktopController gestiona sesión/rol/navegación/exclusión de envíos; FxActions ejecuta tareas fuera del hilo gráfico y refresca desde servicios. Las fachadas conservan su autorización, aunque alguien intente saltarse un botón. FormInput y UiText centralizan conversiones, mensajes, COP y Bogotá. Los DTO monetarios añaden BigDecimal exacto con compatibilidad anterior.

Consultas de cotización/promociones/reembolso y canje previo se incorporan a las fachadas/modelo sin cargos ni borradores implícitos. Confirmación verifica versión; cancelación verifica de nuevo la devolución aceptada y exige revisión si cruza una ventana. No se cambia la política oficial ni se inventan configuraciones. Regresión anterior conservada; ejecución gráfica y capturas reales separadas de pruebas sin pantalla.

Guía completa, pantallas, controladores, servicios, preparación del hotel y límites: [ejecucion-javafx-h04.md](ejecucion-javafx-h04.md). Resultado reproducible, conteos/inventario/capturas: [evidencia-h04.txt](evidencia-h04.txt).

Estado integral actual: 9 RF completos en alcance local, 13 parciales y 4 pendientes. Para RF parciales con nueva UI falta aprobación comercial, recorridos previamente bloqueados o campaña visual exhaustiva; no se afirma ausencia de pantallas. Siguen pendientes compras autónomas fuera de reservas, liquidación de cambios CONFIRMED, vencimiento comercial PENDING, persistencia durable y entregables académicos. H-05 debe abordar reportes/estadísticas sin mover cálculos a la UI. No se inició H-05, no se hicieron diagramas finales, commits, push ni despliegues.

## 18. H-05 — Reportes, estadísticas y exportación

**Verificación H-05 (15/11/2026):** Java 21 / Gradle 8.14.3; `clean test build fxTest run --args=--smoke-ui`: BUILD SUCCESSFUL (3 min 11 s), salida 0. Regresión sin pantalla: 506 aprobadas; JavaFX real: 12 aprobadas. **Total: 518 ejecutadas/aprobadas, 0 fallidas, 0 errores y 0 omitidas**, incluidas las 460 anteriores sin editar y 58 nuevas. Inicio real con `HOTELUQ_UI_STARTED`; `git diff --check`: salida 0. Detalle: [evidencia-h05.txt](evidencia-h05.txt).

Implementadas ocho familias administrativas: reservas por creación, ranking de habitaciones, ocupación contractual por habitaciones-noches, pagos/ingresos/reembolsos/neto, servicios, recargas, cancelaciones y puntos. Fechas inclusivas Bogotá y filtros tipados; datos tomados de un corte inmutable bajo el monitor del agregado. No se inventan ingresos de PENDING ni se consideran recargas ingresos operativos.

ReportService/ReportEngine/ReportSnapshot/ReportResult separan consulta, agregación y transporte. MoneyAllocation concilia atribuciones analíticas COP, no nuevas tarifas comerciales. PointsReportData reconstruye vencimientos sin modificar el ledger; saldo/deuda al corte se distinguen de movimientos del periodo. Los precios/porcentajes históricos proceden de comprobantes y snapshots.

JavaFX añade ReportScreen, tablas y BarChart/PieChart/LineChart mediante el puerto ReportChartView y ReportChartAdapter. Solo administración; permisos comprobados también en el servicio. Exportación TXT UTF-8 usa el resultado mostrado, encabezados/filtros/criterios/registros/totales, y escritura temporal con confirmación para reemplazar. PDF queda como extensión opcional de ReportExporter.

Criterios/formulas/rangos/límites: [reportes-h05.md](reportes-h05.md). Pruebas, inventario, TXT y capturas reales: [evidencia-h05.txt](evidencia-h05.txt). Los fixtures de prueba nunca se incorporan al arranque de Main.

RF-022/023/024 pasan a completos en el alcance local documentado. Totales RF: 12 completos / 13 parciales / 1 pendiente (RF-026 notificaciones propias del grupo). Adapter aporta el cuarto estructural real: 3 creacionales / 4 estructurales / 2 comportamientos GoF, más Specification de dominio sin inflar el tercer comportamiento.

Límites: ocupación contractual, sin historial de inventario/presencia física; atribución monetaria analítica y no fiscal; saldos de puntos actuales, no cierres históricos supuestos; datos en memoria. Persistencia durable, compra autónoma, decisiones comerciales previas, documentos/diagramas finales, aceptación y sustentación siguen pendientes. No se realizaron commits, push ni despliegues.
