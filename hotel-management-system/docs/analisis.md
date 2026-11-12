# HotelUQ: análisis y diseño

Documento de análisis previo a la implementación (sección 9 del enunciado). Los nombres de clases están en inglés porque así se escribe el código.

> Los valores marcados como **propuesta** (reglas adicionales, puntos, promociones) son decisiones del grupo y se pueden cambiar.

---

## 1. Actores del sistema

| Actor | Descripción |
|---|---|
| **Cliente** | Se registra, consulta habitaciones, reserva, contrata servicios, paga con su tarjeta virtual, cancela reservas y usa sus puntos. |
| **Administrador** | Gestiona clientes, habitaciones, tipos de habitación, servicios, promociones y reservas. Recarga tarjetas, cancela reservas a nombre del hotel y genera reportes. |

---

## 2. Requisitos funcionales

### Cliente

| Código | Requisito |
|---|---|
| RF-001 | Registrarse en el sistema. |
| RF-002 | Iniciar sesión. |
| RF-003 | Consultar los tipos de habitación, sus características y tarifas. |
| RF-004 | Seleccionar fecha de entrada y de salida y consultar las habitaciones disponibles en ese periodo. |
| RF-005 | Seleccionar una habitación y crear una reserva. |
| RF-006 | Agregar servicios adicionales a la reserva. |
| RF-007 | Aplicar una promoción cuando se cumplen sus condiciones. |
| RF-008 | Pagar la reserva con la tarjeta virtual. |
| RF-009 | Consultar el saldo y los movimientos de la tarjeta. |
| RF-010 | Consultar el historial de reservas. |
| RF-011 | Cancelar una reserva y recibir el reembolso que corresponda. |
| RF-012 | Consultar los puntos acumulados y su fecha de vencimiento. |
| RF-013 | Redimir puntos por descuentos, servicios o paquetes de servicios. |

### Administrador

| Código | Requisito |
|---|---|
| RF-014 | Gestionar clientes (crear, consultar, editar, eliminar). |
| RF-015 | Gestionar habitaciones. |
| RF-016 | Gestionar tipos de habitación. |
| RF-017 | Gestionar servicios adicionales y paquetes de servicios. |
| RF-018 | Gestionar promociones. |
| RF-019 | Consultar y gestionar las reservas realizadas. |
| RF-020 | Recargar la tarjeta virtual de un cliente. |
| RF-021 | Cancelar una reserva a nombre del hotel (reembolso del 100 %). |
| RF-022 | Consultar las estadísticas del hotel. |
| RF-023 | Generar reportes exportables en formato TXT. |
| RF-024 | Generar reportes visuales (barras, pastel, líneas) con rango de fechas. |

### Sistema

| Código | Requisito |
|---|---|
| RF-025 | Acumular puntos automáticamente al aprobar un pago. |
| RF-026 | Notificar al cliente cuando se confirma o cancela una reserva, o se registra un pago, recarga o reembolso. |

---

## 3. Reglas de negocio

### Definidas en el enunciado

| Código | Regla |
|---|---|
| RN-01 | Cada habitación pertenece a un tipo de habitación. |
| RN-02 | Una habitación no puede reservarse para periodos que se superponen. |
| RN-03 | La fecha de salida debe ser posterior a la fecha de entrada. |
| RN-04 | El valor de la reserva depende del tipo de habitación, el número de noches y los servicios contratados. |
| RN-05 | Se verifica la disponibilidad antes de confirmar una reserva. |
| RN-06 | Una reserva confirmada conserva habitación, cliente, fechas, servicios y valor pagado. |
| RN-07 | Cada cliente tiene una tarjeta virtual con saldo. |
| RN-08 | Antes de confirmar se verifica que la tarjeta esté activa y tenga saldo suficiente. |
| RN-09 | Un pago aprobado se descuenta del saldo y genera un movimiento. |
| RN-10 | Solo el administrador puede recargar una tarjeta; cada recarga genera un movimiento. |
| RN-11 | Cancelación con más de 72 horas de anticipación: reembolso del 100 %. |
| RN-12 | Cancelación entre 24 y 72 horas: reembolso del 80 %. |
| RN-13 | Cancelación con menos de 24 horas: sin reembolso. |
| RN-14 | Cancelación hecha por el hotel: reembolso del 100 %. |
| RN-15 | El reembolso vuelve a la tarjeta con la que se pagó, y un pago no se reembolsa más de una vez. |
| RN-16 | Una reserva puede tener varios servicios; su valor se suma al total. |
| RN-17 | No se pueden agregar servicios que no estén disponibles. |
| RN-18 | Los puntos vencen un año después de obtenerse. |
| RN-19 | No se pueden usar puntos vencidos, ni más puntos de los disponibles, ni los mismos puntos dos veces. |
| RN-20 | Al redimir se usan primero los puntos más próximos a vencer. |
| RN-21 | Una promoción solo se aplica si el cliente cumple sus condiciones. |

### Adicionales del grupo (propuesta)

| Código | Regla |
|---|---|
| RN-22 | El número de huéspedes de una reserva no puede superar la capacidad de la habitación. |
| RN-23 | Una habitación fuera de servicio (inactiva) no aparece como disponible ni se puede reservar. |

### Programa de puntos (propuesta)

- **Acumulación:** 1 punto por cada $1.000 pagados (100 puntos por cada $100.000).
- **Vigencia:** un año desde la fecha de obtención.

| Beneficio | Puntos |
|---|---|
| Descuento en reserva | 1 punto = $50 de descuento |
| Desayuno | 400 |
| Parqueadero | 300 |
| Spa | 1.000 |
| Paquete "Relax" (spa + desayuno) | 1.200 |

### Promociones (propuesta)

| Promoción | Condición | Descuento |
|---|---|---|
| Estadía larga | 5 noches o más | 15 % |
| Suite de temporada | Habitación tipo suite dentro de un rango de fechas | 10 % |
| Cliente frecuente | 3 o más reservas completadas | 10 % |

---

## 4. Entidades del dominio

| Entidad | Responsabilidad |
|---|---|
| `HotelUQ` | Punto central que guarda habitaciones, clientes, servicios, promociones y reservas, y verifica disponibilidad. |
| `RoomType` | Nombre, tarifa por noche, capacidad y descripción de un tipo de habitación. |
| `Room` (abstracta) | Habitación con número, piso y tipo. Subclases: `StandardRoom`, `FamilyRoom`, `SuiteRoom`. |
| `Client` | Datos del cliente y su tarjeta virtual. |
| `VirtualCard` | Saldo, estado y movimientos. Cobra, recarga y reembolsa. |
| `CardMovement` | Un pago, recarga o reembolso con fecha y valor. |
| `Reservation` | Cliente, habitación, fechas, huéspedes, servicios, promoción, estado y valor. |
| `AdditionalService` | Servicio con precio y disponibilidad. |
| `ServicePackage` | Paquete compuesto por servicios u otros paquetes. |
| `Promotion` | Descuento con condiciones. |
| `PointsAccount` | Cuenta de puntos del cliente. |
| `PointsBatch` | Lote de puntos con fecha de obtención y de vencimiento. |
| `Administrator` | Usuario con permisos de gestión. |

**Enumeraciones:** `RoomCategory` (STANDARD, FAMILY, SUITE), `ReservationStatus` (PENDING, CONFIRMED, CANCELLED, COMPLETED), `MovementType` (PAYMENT, RECHARGE, REFUND).

**Excepciones del dominio:** `HotelException` (base), `InvalidReservationException`, `RoomNotAvailableException`, `InsufficientBalanceException`, `InactiveCardException`, `DuplicateRoomException`.

---

## 5. Relaciones y multiplicidades

| Relación | Tipo | Multiplicidad |
|---|---|---|
| `HotelUQ` – `Room` | Composición | 1 a muchos |
| `HotelUQ` – `Client` | Agregación | 1 a muchos |
| `HotelUQ` – `Reservation` | Composición | 1 a muchos |
| `Room` – `RoomType` | Asociación | muchos a 1 |
| `StandardRoom`, `FamilyRoom`, `SuiteRoom` – `Room` | Herencia | |
| `Client` – `VirtualCard` | Composición | 1 a 1 |
| `VirtualCard` – `CardMovement` | Composición | 1 a muchos |
| `Client` – `PointsAccount` | Composición | 1 a 1 |
| `PointsAccount` – `PointsBatch` | Composición | 1 a muchos |
| `Reservation` – `Client` | Asociación | muchos a 1 |
| `Reservation` – `Room` | Asociación | muchos a 1 |
| `Reservation` – `AdditionalService` | Agregación | muchos a muchos |
| `Reservation` – `Promotion` | Asociación | muchos a 0..1 |
| `ServicePackage` – `AdditionalService` | Agregación | 1 a muchos |

---

## 6. Casos de uso principales

| Código | Caso de uso | Actor | Resumen |
|---|---|---|---|
| CU-01 | Registrarse e iniciar sesión | Cliente | Crea su cuenta, recibe una tarjeta virtual y entra al sistema. |
| CU-02 | Buscar habitaciones disponibles | Cliente | Elige fechas y el sistema muestra las habitaciones libres con su tarifa. |
| CU-03 | Realizar una reserva | Cliente | Elige habitación, servicios y promoción; el sistema valida, cobra, confirma y suma puntos. |
| CU-04 | Cancelar una reserva | Cliente | El sistema calcula el reembolso según el tiempo restante y lo devuelve a la tarjeta. |
| CU-05 | Redimir puntos | Cliente | Usa puntos vigentes, empezando por los más próximos a vencer. |
| CU-06 | Recargar una tarjeta | Administrador | Agrega saldo a la tarjeta de un cliente y queda el movimiento. |
| CU-07 | Gestionar habitaciones y tipos | Administrador | Crea, edita y desactiva habitaciones y tipos. |
| CU-08 | Generar reportes | Administrador | Elige el reporte y el rango de fechas, y lo exporta o lo ve en un gráfico. |

---

## 7. Posibles patrones de diseño

| Categoría | Patrón | Problema que resuelve |
|---|---|---|
| Creacional | Singleton | Una sola fuente de datos del hotel (`HotelUQ`). |
| Creacional | Factory Method | Crear habitaciones de distintos tipos sin que la interfaz conozca las clases concretas. |
| Creacional | Builder | Armar una reserva con muchos datos opcionales y validarla antes de crearla. |
| Estructural | Decorator | Sumar servicios adicionales al costo de una reserva. |
| Estructural | Composite | Paquetes de servicios formados por servicios u otros paquetes. |
| Estructural | Facade | Un solo punto para reservar: disponibilidad, pago, puntos y notificación. |
| Estructural | Proxy | Permitir recargas de tarjeta solo al administrador. |
| Comportamiento | State | Estados de una reserva y sus transiciones. |
| Comportamiento | Strategy | Políticas de reembolso y condiciones de las promociones. |
| Comportamiento | Observer | Notificaciones de reservas, pagos y reembolsos. |


Esta planificación puede someterse a cambios a medida que se vaya desarrollando el código.