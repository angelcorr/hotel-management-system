# H-04 — Aplicación JavaFX / MVC

## Ejecución

Requisitos: JDK 21, Gradle Wrapper incluido, bibliotecas nativas GTK de JavaFX y una sesión gráfica accesible. Desde la raíz Git:

```bash
cd hotel-management-system
./gradlew run
```

La clase lanzadora es `edu.hoteluq.Main`; delega en `HotelApplication`. JavaFX 21.0.2 y los módulos controls/fxml ya estaban configurados; no se añadió biblioteca de UI, TestFX ni base de datos. El CSS y los FXML se empaquetan como recursos.

Primer inicio: pantalla **Inicializar e ingresar**, con ID/nombre/correo/contraseña elegidos por el operador. Solo se habilita antes de inicializar al administrador. No existen credenciales predeterminadas, semillas de producción, promociones de ejemplo ni equivalencias de puntos activas. Después del primer acceso, el registro público crea exclusivamente clientes. No es un mecanismo de aprovisionamiento remoto: es el arranque local confiable previsto por el servicio existente.

**Todos los datos son de memoria.** Cerrar el proceso pierde usuarios, sesiones, catálogos, parámetros, reservas, pagos, puntos e historial. Al volver a abrir, se inicializa otro hotel vacío. No se incorporó persistencia ni exportación de datos en H-04.

### Preparación por el administrador

1. En **Parámetros comerciales**, ingresar horarios y ventana de check-out aprobados. No se proponen horas ni tolerancias por defecto. COP, Bogotá y cancelación oficial son fijos y no se editan.
2. Crear tipos y tarifas, habitaciones y servicios desde sus pantallas administrativas.
3. Registrar clientes o permitir que se registren; recargar sus tarjetas desde **Tarjeta y movimientos**, indicando su identificación.
4. Configurar promociones y puntos/beneficios solo si sus valores y tratamiento comercial están aprobados. Los campos y opciones empiezan vacíos. El checkbox de aprobación es una confirmación del operador, no sustituye una decisión del responsable ni los permisos del servicio.
5. Los paquetes se crean en **Servicios y paquetes**, seleccionando hojas y/o paquetes existentes. Se conserva Composite y su inmutabilidad histórica.

No hace falta habilitar promociones ni puntos para una reserva ordinaria con pago COP. Sin horario contractual, el servicio rechaza reservas y la UI informa del parámetro faltante.

## Pantallas y flujos reales

| Pantalla | Acciones | Fachadas |
|---|---|---|
| Inicio / registro / arranque | Inicialización local, registro de cliente, login y logout | HotelService |
| Habitaciones | Tabla de habitaciones, tipos/tarifas/capacidad/características, fechas, disponibilidad, selección de servicios/paquetes | HotelService, LoyaltyService |
| Cotización | Promociones elegibles/no elegibles con condiciones, cotización sin efectos, borrador y edición PENDING versionada | ReservationService |
| Reservas | Historial autorizado, contrato, estado/versión, comprobantes/auditoría, confirmación del cargo, cancelación con porcentaje/importe previo, canje de puntos con revisión del neto | ReservationService, LoyaltyService |
| Tarjeta y movimientos | Saldo COP exacto, estado, movimientos fechados; administrador puede recargar y cambiar actividad | HotelService |
| Puntos | Disponible/deuda, lotes con fechas de obtención/vencimiento, movimientos y política; explica configuración ausente | LoyaltyService |
| Clientes | Alta, edición, eliminación protegida, restablecimiento de contraseña | HotelService |
| Habitaciones / tipos | Alta, edición permitida, actividad y eliminación protegida | HotelService |
| Servicios / paquetes | Catálogo CRUD, disponibilidad, composición y precio de paquetes | HotelService, LoyaltyService |
| Promociones | Alta/edición con versiones, condiciones, vigencia, combinación/acumulación explícitas, activación/desactivación y auditoría | PromotionService |
| Parámetros | Horarios/ventana autorizados, política de puntos, beneficios versionados y liquidación explícita de acumulaciones diferidas | ReservationService, LoyaltyService |
| Administración de reservas | Consulta total, cancelación por hotel, check-out con confirmación y operación idempotente | ReservationService |

**Recorrido de cliente:** ingresar → consultar habitación/fechas/servicios → consultar promociones si interesa → cotizar → crear borrador (bloquea disponibilidad, no cobra) → abrir reserva → revisar total → confirmar cargo. Puede consultar tarjeta, puntos e historial, editar PENDING o solicitar cancelación. El canje se hace desde una reserva PENDING, seleccionando beneficio y confirmando puntos y pago residual; queda bloqueado si falta política/equivalencia o no alcanza el saldo.

Los borradores no vencen automáticamente: se mantiene la regla anterior, todavía sin plazo aprobado. Una cotización pura no bloquea habitación ni crea un pago/borrador. Los formularios extensos tienen desplazamiento vertical. Para selección múltiple se usan los controles estándar JavaFX (Ctrl/Shift).

## MVC y clases

- **Modelo:** entidades, reglas, patrones y las cuatro fachadas existentes. La UI no recibe referencias mutables para efectuar cambios. `Views` incorpora importes BigDecimal exactos además de sus accesores double de compatibilidad: editar tarifas no exige un viaje de ida/vuelta por coma flotante.
- **Vista:** `shell.fxml`, `login.fxml`, `hotel.css` y componentes JavaFX construidos con `Ui`. Tablas, formularios, calendarios y diálogos reales; las pantallas extensas se componen programáticamente.
- **Controladores:** `ShellController` navega; `LoginController` gestiona acceso; `BookingScreen`, `ClientScreens`, `AdminScreens`, `CommercialScreens` conectan eventos de sus vistas a fachadas. `DesktopController` mantiene sesión, rol, navegación y exclusión de envíos; no contiene reglas monetarias. La composición programática y los handlers conviven en las clases de pantalla, sin trasladar negocio a ellas; una separación FXML más granular es mejora futura, no pantallas inexistentes.
- **Soporte de presentación:** `FormInput` convierte entradas; `UiText` centraliza formatos/mensajes; `FxActions` ejecuta una sola tarea de fondo y retorna actualizaciones al hilo JavaFX. `FxCapture` solo guarda la propia escena para diagnóstico optativo.

El token no se muestra ni se escribe en evidencias. Cerrar sesión lo revoca y limpia navegación/identidad. Caducidad de sesión redirige al ingreso. La navegación también comprueba rol/sesión; ocultar botones no sustituye autorización en las fachadas. Cambiar parámetros/promociones usa las reglas y versiones reales del modelo.

## Adaptaciones del modelo necesarias para la UI

- Consultas `preview`, `offers` y `previewModification`: cotización/eligibilidad sin registrar una reserva. La edición excluye su propio bloqueo de disponibilidad.
- Confirmación con `expectedVersion`, comprobada dentro del monitor del agregado; conserva idempotencia de reservas ya pagadas.
- `cancellationQuote` y `cancel(quote)`: comparten la misma función de decisión oficial con la ejecución. Si cambian versión, porcentaje o importe al cruzar una ventana, el servicio exige revisar de nuevo. No se confía en una hora enviada por la UI; al ejecutar se usa un único instante del reloj del agregado.
- `LoyaltyService.preview`: cálculo puro del beneficio reutilizando el cálculo de Terms de Reservation, sin consumir puntos/cobrar; mantiene autorización, saldo vigente y elegibilidad. El pago posterior vuelve a validar estado/versión/configuración.
- Sobrecargas decimales de edición y DTOs monetarios exactos, manteniendo las firmas anteriores.

Estas adaptaciones no cambian State/Strategy, Composite, Decorator, Specification, tasas de cancelación ni equivalencias. El precio/reembolso/puntos se calculan en modelo, nunca en controladores. La UI solo forma solicitudes, presenta importes y pide confirmación.

## Seguridad, consistencia y UX

Durante cada tarea se deshabilita la raíz de la ventana y existe un gate adicional en DesktopController. Las reservas usan sus claves idempotentes existentes; los canjes/check-out usan claves estables por operación. La recarga conserva su servicio anterior: la UI impide envíos simultáneos y exige confirmación, pero una segunda recarga explícita posterior sigue siendo otra operación válida; no se promete idempotencia durable de recargas.

Se capturan entradas antes de la tarea de fondo; solo sus resultados actualizan controles en el hilo JavaFX. Después de mutaciones se consultan nuevamente las listas/saldos. Confirmaciones de pago y cancelación muestran total, porcentaje o importe desde el modelo. El detalle conserva recibos e historial. Fechas de operación se muestran en Bogotá; fechas de estadía mediante calendario DD/MM/AAAA; los calendarios no aceptan texto ambiguo. Los importes se rotulan COP y las tarifas conservan sus decimales crudos si existen.

Las excepciones se traducen centralmente: sesión, autorización, datos/versiones cambiadas, configuración ausente y saldo insuficiente tienen mensajes comprensibles; para rechazos de dominio no traducidos se conserva la explicación técnica original. Una localización completa de todos los mensajes heredados sigue siendo una mejora pendiente. No se muestran detalles de errores inesperados ni contraseñas.

Las modificaciones CONFIRMED continúan bloqueadas económicamente; la UI explica el motivo y solo permite editar PENDING. Canceladas/finalizadas se rechazan por los servicios. La fecha programada de salida no finaliza automáticamente reservas: check-out explícito administrativo, sujeto a la ventana aprobada.

## Pruebas y ejecución gráfica

Regresión independiente de pantalla:

```bash
./gradlew --no-daemon clean test build
```

Pruebas opcionales con toolkit y pantalla reales (no se incluyen en `test` para que la compilación sea posible en servidores sin entorno gráfico):

```bash
./gradlew --no-daemon fxTest
```

`JavaFxIntegrationTest` carga FXML/CSS, dispara controles reales, confirma diálogos y espera servicios asíncronos. Recorre registro/acceso, cotización/borrador/pago/cancelación/logout y navegación administrativa. Capturas de su propia escena en `build/h04/`. Los datos de esas pruebas son fixtures y nunca se cargan desde Main.

Arranque real verificable que captura la primera pantalla y termina, sin crear usuarios/datos:

```bash
./gradlew --no-daemon run --args='--smoke-ui'
```

El modo diagnóstico imprime `HOTELUQ_UI_STARTED` y escribe `build/h04/lanzamiento.png`; un arranque interactivo normal se hace con `./gradlew run` sin ese argumento. Si DISPLAY/GTK no están disponibles, fxTest/run pueden fallar aunque compile/test/build sean satisfactorios: no se debe declarar ejecución visual a partir de la compilación. El resultado real de esta ejecución, conteos e inventario están en [evidencia-h04.txt](evidencia-h04.txt).

## Pendientes y H-05

Sigue pendiente aprobar configuraciones comerciales reales y la compra autónoma fuera de reservas. La UI permite configurar valores explícitos, no los aprueba automáticamente. Falta una campaña visual exhaustiva de todos los formularios CRUD y combinaciones de promociones/paquetes/canjes con usuarios finales; las suites de negocio existentes siguen cubriendo esas reglas.

Persistencia durable/recuperación tras reinicio y expiración comercial de PENDING continúan pendientes previos. H-04 no introduce reportes, estadísticas, exportadores ni diagramas finales. H-05 debería crear servicios de consulta agregada y luego vistas/gráficos/exportación autorizados, sobre los mismos DTO/ledgers y con filtros/criterios conciliados; no poner cálculos de ingresos/ocupación en controladores.

## Evidencia gráfica de esta ejecución

**Verificación H-04 (15/11/2026):** Java 21 / Gradle 8.14.3, BUILD SUCCESSFUL. Regresión sin pantalla: 457 aprobadas (416 anteriores intactas + 41 nuevas). Integración gráfica real: 3 aprobadas. **Total: 460 ejecutadas/aprobadas, 0 fallidas, 0 errores y 0 omitidas.** Arranque real con `run --args=--smoke-ui`: salida 0 y `HOTELUQ_UI_STARTED`. `git diff --check`: salida 0. Detalle: [evidencia-h04.txt](evidencia-h04.txt).

Capturas de la propia aplicación, con datos de prueba salvo el arranque vacío:

- [acceso](h04/acceso.png)
- [administrador parametros](h04/administrador-parametros.png)
- [administrador promociones](h04/administrador-promociones.png)
- [cliente cotizacion](h04/cliente-cotizacion.png)
- [cliente pago](h04/cliente-pago.png)
- [cliente puntos](h04/cliente-puntos.png)
- [cliente reembolso](h04/cliente-reembolso.png)
- [cliente tarjeta](h04/cliente-tarjeta.png)
- [lanzamiento](h04/lanzamiento.png)
