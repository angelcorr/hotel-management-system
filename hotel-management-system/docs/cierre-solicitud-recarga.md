# HotelUQ — Solicitud de recarga por cliente

## Alcance implementado

- El cliente autenticado puede crear una solicitud por importe positivo en COP y motivo.
- Crear una solicitud **no cambia el saldo** ni crea un movimiento financiero.
- Cada cliente consulta sus propias solicitudes; un administrador puede consultar todas.
- Solo un administrador puede aprobar o rechazar solicitudes pendientes.
- Aprobar genera una recarga real en la tarjeta original y deja el estado APROBADO.
- Si el cargo falla (por ejemplo, tarjeta inactiva), la solicitud permanece PENDIENTE.
- Repetir una aprobación con el mismo ID no causa una segunda recarga.
- Reutilizar una clave de solicitud con otros datos se rechaza.
- La interfaz ofrece formulario para clientes y tabla de solicitudes con acciones para administradores.

## Validación y limitaciones

Las clases de negocio modificadas compilaron con `javac --release 21`; una prueba manual Java verificó solicitud, aprobación y reintento idempotente. No se ejecutó aquí la suite JUnit ni JavaFX por falta de sus dependencias locales. Ejecutar en Ubuntu:

```bash
./gradlew --no-daemon clean test build fxTest
git diff --check
./gradlew run
```

Hacer comprobación visual de la pantalla **Tarjeta y movimientos** para cliente y administrador.

**Limitación actual:** solicitudes, pagos y registros se conservan exclusivamente en memoria. No sobreviven a reinicios de la JVM. La funcionalidad de compras de servicios autónomas (fuera de una reserva) no fue modificada en este parche.
