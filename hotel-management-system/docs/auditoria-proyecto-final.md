# Auditoría integral del Proyecto Final HotelUQ

Auditoría original: 14 de noviembre de 2026, exclusivamente documental. **Actualización posterior autorizada: H-01 cerrado mediante corrección funcional de cancelación y regresión de 272 pruebas; H-02 añade negocio de puntos/paquetes y evidencia independiente.** Esta actualización no inicia otras fases ni realiza commits, push o despliegue. La evidencia original de auditoría permanece en `evidencia-auditoria.txt`; la corrección tiene evidencia separada.

## 1. Fuente principal y criterio de evaluación

Fuente oficial leída íntegramente: [Proyecto Final (HotelUQ) PGII - 2026-2.pdf](</home/luis/Descargas/Proyecto Final (HotelUQ) PGII - 2026-2.pdf>), SHA-256 `c61012dd8c9c544e63844acfba7c3993bf9405553e197202dc6985b15242d9a0`. Tiene 18 páginas físicas y numeración impresa 1–16, con títulos genéricos heredados en encabezados; se toman como normativas las secciones del **Sistema Integral de Gestión Hotelera**, no los encabezados de logística/eventos. Las citas § remiten a sus secciones.

El PDF prevalece sobre `analisis.md`, `pendientes.md`, `configuracion-comercial.md` y propuestas anteriores. Se revisaron esos archivos, `propuesta-persistencia.md`, `evidencia-fase3b.txt`, las fuentes Java y clases de pruebas del árbol local (la auditoría original cubría 57 fuentes/15 suites; tras H-05 hay 94 fuentes principales, 25 suites independientes de pantalla, dos suites gráficas y dos fixtures), además de fixtures/build/historial local. La auditoría inicial registró divergencias sin modificar esos documentos; la corrección posterior de H-01 actualiza ahora análisis, pendientes y configuración para restablecer la prioridad normativa. No se usa una mención en Markdown como prueba de implementación.

**Hallazgo original (histórico, actualizado por H-04 al cierre):** hay lógica de identidad, catálogos y reservas/dinero probada, pero no una aplicación gráfica académicamente terminada. H-02 incorpora puntos y paquetes en negocio con configuración comercial explícita pendiente; H-03 incorpora promociones elegibles con configuración explícita; siguen ausentes reportes y aplicación gráfica. La incompatibilidad de cancelación 3B detectada en H-01 fue corregida: el runtime solo acepta la función oficial 100/80/0. Una compilación verde no certifica esos requisitos ausentes o contradictorios.

### Estados y prioridades

- **COMPLETO:** se verificó el criterio delimitado, con artefacto/código y evidencia. Para RF de usuario exige poder realizarlo en la aplicación requerida, no solo mediante una API Java.
- **PARCIAL:** hay implementación comprobada, pero falta integración, regla o componente necesario.
- **PENDIENTE:** no se encontró implementación/artefacto del criterio. No significa que una declaración conceptual lo complete.
- **BLOQUEADO:** no se puede cerrar la comprobación sin información externa concreta (roster, plantilla o evaluación del equipo). Una función ausente no se etiqueta bloqueada solo porque aún no fue programada.
- **P0:** requisito mínimo/corrección normativa o entrega que puede comprometer seriamente la evaluación. **P1:** integración, consistencia/diseño o demostración importante. **P2:** mejora/alcance adicional no mínimo explícito. Son prioridades de auditoría; el PDF no asigna pesos por apartado, por lo que no se inventa una nota ni porcentaje de avance.

### Conteo reproducible

| Ámbito | COMPLETO | PARCIAL | PENDIENTE | BLOQUEADO | Total |
|---|---:|---:|---:|---:|---:|
| 26 RF existentes | 12 | 13 | 1 | 0 | 26 |
| Criterios técnicos/académicos/SOLID/patrones | 9 | 7 | 2 | 3 | 21 |
| Matriz principal de indicadores auditados | 21 | 20 | 3 | 3 | 47 |

Los 47 son indicadores desglosados por esta auditoría, no una numeración oficial de 47 RF. Los desgloses de puntos/reportes y las dos propuestas de RF posteriores no se suman otra vez: refinan la cobertura de la matriz. En la auditoría inicial ningún RF se cerraba como aplicación final por faltar JavaFX/MVC. Tras H-04 se cierran nueve recorridos locales; los demás conservan las limitaciones específicas indicadas, sin inferir completitud a partir de compilar.

## 2. Matriz funcional: RF-001 a RF-026

| ID | Requisito oficial | Estado | Evidencia en código | Prueba existente | Pendiente | Prioridad |
|---|---|---|---|---|---|---|
| RF-001 | Registro de cliente (§2.1) | COMPLETO | H-04: fachadas existentes + ui/ y recursos FXML/CSS; ver ejecucion-javafx-h04.md | DesktopIntegrationTest/FormInputTest y JavaFxIntegrationTest; regresión anterior | Recorrido local integrado; datos en memoria. Validación comercial/operativa y persistencia se siguen por separado. | P0 |
| RF-002 | Inicio de sesión de cliente/administrador (§2) | COMPLETO | H-04: fachadas existentes + ui/ y recursos FXML/CSS; ver ejecucion-javafx-h04.md | DesktopIntegrationTest/FormInputTest y JavaFxIntegrationTest; regresión anterior | Recorrido local integrado; datos en memoria. Validación comercial/operativa y persistencia se siguen por separado. | P0 |
| RF-003 | Consultar tipos, características y tarifas de habitaciones (§2.1) | COMPLETO | H-04: fachadas existentes + ui/ y recursos FXML/CSS; ver ejecucion-javafx-h04.md | DesktopIntegrationTest/FormInputTest y JavaFxIntegrationTest; regresión anterior | Recorrido local integrado; datos en memoria. Validación comercial/operativa y persistencia se siguen por separado. | P1 |
| RF-004 | Elegir entrada/salida y consultar disponibilidad (§2.1, §3.1) | COMPLETO | H-04: fachadas existentes + ui/ y recursos FXML/CSS; ver ejecucion-javafx-h04.md | DesktopIntegrationTest/FormInputTest y JavaFxIntegrationTest; regresión anterior | Recorrido local integrado; datos en memoria. Validación comercial/operativa y persistencia se siguen por separado. | P0 |
| RF-005 | Seleccionar habitación y reservar (§2.1, §3.1) | COMPLETO | H-04: fachadas existentes + ui/ y recursos FXML/CSS; ver ejecucion-javafx-h04.md | DesktopIntegrationTest/FormInputTest y JavaFxIntegrationTest; regresión anterior | Recorrido local integrado; datos en memoria. Validación comercial/operativa y persistencia se siguen por separado. | P0 |
| RF-006 | Agregar servicios con precio/disponibilidad al total (§2.1, §3.4) | PARCIAL | [Reservation.java](../src/main/java/edu/hoteluq/model/Reservation.java#L165); [Hotel.confirmReservation](../src/main/java/edu/hoteluq/singleton/Hotel.java#L604) | [BookingTransactionTest.serviceAvailabilityIsRecheckedAtConfirmation](../src/test/java/edu/hoteluq/singleton/BookingTransactionTest.java#L93); [CommercialPolicyValidationTest.java](../src/test/java/edu/hoteluq/money/CommercialPolicyValidationTest.java#L55) | Servicios/paquetes de negocio verificados; falta selección visual. Unidad de cobro una vez/reserva es decisión del grupo, no texto obligatorio. | P1 |
| RF-007 | Promociones solo cuando se cumplen condiciones (§3.6) | PARCIAL | PromotionCondition; PromotionProgram; Hotel.draft/confirmReservation; PriceComponent | PromotionRulesTest; PromotionIntegrationTest | Negocio H-03: condiciones autoritativas, vigencia/versiones y cálculo neto. Faltan configuración comercial aprobada e interfaz. | P0 |
| RF-008 | Compras y reservas con tarjeta virtual (§2.1, §3.2) | PARCIAL | [Hotel.confirmReservation](../src/main/java/edu/hoteluq/singleton/Hotel.java#L604); [VirtualCard.java](../src/main/java/edu/hoteluq/model/VirtualCard.java#L65) | [BookingTransactionTest.createsPendingSnapshotAndConfirmsWithOneLinkedPayment](../src/test/java/edu/hoteluq/singleton/BookingTransactionTest.java#L35); saldo insuficiente/fallos parciales/concurrencia | Cobro de reservas probado; no existe flujo identificado de compras ajenas a reserva. Falta UI; aclarar/ampliar con propuesta RF-027. | P0 |
| RF-009 | Consultar saldo/movimientos (§2.1, §3.2) | COMPLETO | H-04: fachadas existentes + ui/ y recursos FXML/CSS; ver ejecucion-javafx-h04.md | DesktopIntegrationTest/FormInputTest y JavaFxIntegrationTest; regresión anterior | Recorrido local integrado; datos en memoria. Validación comercial/operativa y persistencia se siguen por separado. | P1 |
| RF-010 | Historial de reservas (§2.1) | COMPLETO | H-04: fachadas existentes + ui/ y recursos FXML/CSS; ver ejecucion-javafx-h04.md | DesktopIntegrationTest/FormInputTest y JavaFxIntegrationTest; regresión anterior | Recorrido local integrado; datos en memoria. Validación comercial/operativa y persistencia se siguen por separado. | P1 |
| RF-011 | Cancelación del cliente y devolución oficial (§3.3) | COMPLETO | H-04: fachadas existentes + ui/ y recursos FXML/CSS; ver ejecucion-javafx-h04.md | DesktopIntegrationTest/FormInputTest y JavaFxIntegrationTest; regresión anterior | Recorrido local integrado; datos en memoria. Validación comercial/operativa y persistencia se siguen por separado. | P1 |
| RF-012 | Cuenta/saldo, obtención y vencimiento anual (§4) | COMPLETO | H-04: fachadas existentes + ui/ y recursos FXML/CSS; ver ejecucion-javafx-h04.md | DesktopIntegrationTest/FormInputTest y JavaFxIntegrationTest; regresión anterior | Recorrido local integrado; datos en memoria. Validación comercial/operativa y persistencia se siguen por separado. | P0 |
| RF-013 | Redimir descuentos, servicios y paquetes (§4) | PARCIAL | `PointsLedger.redeem`, `Reservation.applyPointsBenefit`, `LoyaltyCatalog` | LoyaltyIntegrationTest; BenefitValidationTest | Tres beneficios operativos, atómicos e idempotentes con configuración explícita. Equivalencias/reversión por aprobar; falta interfaz. | P0 |
| RF-014 | Gestionar clientes (§2.2) | PARCIAL | [HotelService.java](../src/main/java/edu/hoteluq/service/HotelService.java#L42); [Hotel.removeClient](../src/main/java/edu/hoteluq/singleton/Hotel.java#L307) | [UserAuthorizationTest.administrativeClientCrudIsRealAndReturnsNoCredentials](../src/test/java/edu/hoteluq/singleton/UserAuthorizationTest.java#L70); unicidad/propiedad/eliminación de esa clase | CRUD Java protegido comprobado; falta pantalla administrativa. | P1 |
| RF-015 | Gestionar habitaciones (§2.2) | PARCIAL | [RoomCreator.java](../src/main/java/edu/hoteluq/factory/RoomCreator.java#L1); [HotelService.java](../src/main/java/edu/hoteluq/service/HotelService.java#L135); [Hotel.removeRoom](../src/main/java/edu/hoteluq/singleton/Hotel.java#L359) | [CatalogAdministrationTest.roomCrudUsesFactoryAndRejectsDuplicateNumbersAndMissingTypes](../src/test/java/edu/hoteluq/singleton/CatalogAdministrationTest.java#L37) | Falta interfaz CRUD/activación. Tres categorías elegidas por el grupo son válidas; el PDF no exige categorías arbitrarias. | P1 |
| RF-016 | Gestionar tipos (§2.2) | PARCIAL | [RoomType.java](../src/main/java/edu/hoteluq/model/RoomType.java#L1); [HotelService.java](../src/main/java/edu/hoteluq/service/HotelService.java#L111) | [CatalogAdministrationTest.roomTypeCrudValidatesCategoryAndPreservesStableIdentity](../src/test/java/edu/hoteluq/singleton/CatalogAdministrationTest.java#L23) | Falta UI y documentación de catálogo elegido; mantener protección de referencias. | P1 |
| RF-017 | Gestión de servicios y paquetes (§2.2, §4) | PARCIAL | AdditionalService; `LoyaltyCatalog`, `ServiceComponent.Leaf/Package` | CatalogAdministrationTest; LoyaltyIntegrationTest.nestedPackageIsCompositeAndRoundsOnce | CRUD simple conservado; paquetes inmutables con composición, precio/disponibilidad y canje. Falta UI y venta autónoma si se requiere. | P0 |
| RF-018 | Gestión administrativa de promociones (§2.2) | PARCIAL | PromotionService; PromotionDefinition; PromotionProgram.Change | PromotionIntegrationTest.unauthorizedAdministrationAndHistoryAreRejected; concurrentAdministrativeUpdatesCannotLoseVersions | Alta/consulta/edición/activación/desactivación y auditoría implementadas; UI pendiente. | P1 |
| RF-019 | Consultar/gestionar reservas (§2.2) | PARCIAL | [Hotel.modifyReservation](../src/main/java/edu/hoteluq/singleton/Hotel.java#L584); [Hotel.checkoutReservation](../src/main/java/edu/hoteluq/singleton/Hotel.java#L682); [Hotel.prepareConfirmedModification](../src/main/java/edu/hoteluq/singleton/Hotel.java#L709) | [BookingTransactionTest.pendingModificationIsAtomicVersionedAndPreservesIdentity](../src/test/java/edu/hoteluq/singleton/BookingTransactionTest.java#L133); [Commercial3BTest.checkoutIsExplicitAuditedIdempotentAndNotAClockTimer](../src/test/java/edu/hoteluq/singleton/Commercial3BTest.java#L144) | Falta UI; horas/ventana para check-out de la solución del grupo. Liquidar cambios confirmados está bloqueado y NO es una exigencia explícita adicional del PDF. | P1 |
| RF-020 | Recargar tarjetas solo como administrador y registrar movimiento (§2.2, §3.2) | PARCIAL | [HotelService.java](../src/main/java/edu/hoteluq/service/HotelService.java#L66); [VirtualCard.java](../src/main/java/edu/hoteluq/model/VirtualCard.java#L1) | [UserAuthorizationTest.rechargeIsAdministratorOnlyAndCreatesExactlyOneMovement](../src/test/java/edu/hoteluq/singleton/UserAuthorizationTest.java#L153); [AuthorizationConcurrencyTest.concurrentAdministrativeRechargesPreserveBalanceAndEveryMovement](../src/test/java/edu/hoteluq/singleton/AuthorizationConcurrencyTest.java#L80) | Negocio probado; falta UI y solicitud del cliente (propuesta RF-028). | P1 |
| RF-021 | Cancelación por hotel: 100 % original, una devolución (§3.3) | PARCIAL | [Hotel.cancelReservation](../src/main/java/edu/hoteluq/singleton/Hotel.java#L641); [FullRefundPolicy.java](../src/main/java/edu/hoteluq/strategy/FullRefundPolicy.java#L1) | [BookingTransactionTest.hotelReturnsFullPaymentToOriginalInactiveCard](../src/test/java/edu/hoteluq/singleton/BookingTransactionTest.java#L242); reintentos cruzados y concurrencia | Regla Java comprobada, falta pantalla autorizada. Un reintento de cancelación previa no es una nueva cancelación ni un segundo abono. | P1 |
| RF-022 | Estadísticas administrativas (§2.2, §7) | COMPLETO | ReportService, ReportEngine, ReportSnapshot y ReportResult | ReportEngineTest; ReportIntegrationTest; ReportsJavaFxTest | Criterios y límites explícitos en reportes-h05.md; persistencia/ocupación física histórica no inferidas. | P1 |
| RF-023 | Exportación TXT o PDF (§7.1) | COMPLETO | ReportExporter, TxtReportExporter, ReportFiles y FileChooser en ReportScreen | UTF-8/metadatos/permisos/sobrescritura/corte inmutable en ReportIntegrationTest; TXT de recorridos gráficos | TXT cumple formato mínimo. PDF opcional futuro sin duplicar agregación. | P1 |
| RF-024 | Reportes visuales/filtros (§7.2) | COMPLETO | ReportScreen, ReportChartView y ReportChartAdapter: BarChart/PieChart/LineChart | ReportsJavaFxTest: ocho familias con tablas y tres gráficos, y rechazo de pastel negativo | Rango largo puede requerir reducir fechas para legibilidad; no se fabrican sectores/valores. | P1 |
| RF-025 | Acumulación automática por pagos (§4) | PARCIAL | Hotel.confirmReservation → PointsLedger.paid; cancelReservation → refunded | LoyaltyIntegrationTest.paidRoomAndServicesAccrueOnlyActualCopPayment; PointsLedgerTest | Reserva y servicios vinculados integrados. Sin tasa solo diferidos; faltan aprobación, compra autónoma fuera de reserva e interfaz. | P0 |
| RF-026 | Notificaciones de la solución del grupo (relación §6.2) | PENDIENTE | Revisión actual: no se añadió componente implementador en H-04. | Ninguna prueba del flujo requerido. | No hay observadores ni entrega de notificaciones. No es mínimo funcional explícito: el PDF las enumera como posible problema de diseño. Conservar como alcance propio o justificar su priorización. | P2 |

## 3. Matriz técnica, académica, SOLID y patrones

| ID | Requisito oficial | Estado | Evidencia en código | Prueba existente | Pendiente | Prioridad |
|---|---|---|---|---|---|---|
| TEC-01 | Desarrollarse en Java (§8) | COMPLETO | [build.gradle](../build.gradle#L16); 94 fuentes principales Java tras H-05, con separación modelo/reportes/UI. | Compilación Java 21; suite JUnit actual (evidencia al cierre). | Mantener toolchain y ejecución reproducibles. | P1 |
| TEC-02 | JavaFX funcional (§8) | COMPLETO | HotelApplication, Main, shell.fxml/login.fxml, hotel.css y pantallas ui/ | fxTest con toolkit/pantalla real y arranque run --args=--smoke-ui | Ampliar campaña visual de CRUD y combinaciones complejas. | P1 |
| TEC-03 | Arquitectura MVC (§8) | COMPLETO | Modelo/fachadas, vista FXML/componentes, controladores de sesión/navegación/pantalla separados de reglas de negocio | DesktopIntegrationTest y JavaFxIntegrationTest | Mantener negocio fuera de handlers; composición programática puede separarse en FXML más granular. | P1 |
| TEC-04 | Excepciones propias del dominio (§8) | COMPLETO | [HotelException.java](../src/main/java/edu/hoteluq/exceptions/HotelException.java#L1) y ocho subtipos/compatibilidad. | CatalogValidationTest, VirtualCardTest, BookingTransactionTest: assertThrows de dominio. | Normalizar uso puntual de IllegalArgumentException/UnsupportedOperationException en contratos cuando corresponda; no invalida la existencia exigida. | P2 |
| TEC-05 | Interfaces, abstracciones, herencia y polimorfismo cuando proceda (§5, §8) | COMPLETO | [Room.java](../src/main/java/edu/hoteluq/model/Room.java#L1); [User.java](../src/main/java/edu/hoteluq/model/User.java#L1); [RoomCreator.java](../src/main/java/edu/hoteluq/factory/RoomCreator.java#L1); [ReservationState.java](../src/main/java/edu/hoteluq/state/ReservationState.java#L1); [RefundPolicy.java](../src/main/java/edu/hoteluq/strategy/RefundPolicy.java#L1) | [HotelTest.singletonAndFactoryRemainFunctionalForAllCategories](../src/test/java/edu/hoteluq/singleton/HotelTest.java#L54); pruebas de estados y policies. | Extender la demostración con paquetes/puntos; no confundir con certificación de los cinco SOLID. | P1 |
| TEC-06 | Diagrama de clases del proyecto en PDF (§8, §10) | PENDIENTE | No existe PDF del modelo en el repositorio; el PDF docente es el enunciado. | No aplica JUnit; no hay artefacto para inspeccionar. | Diagrama real actualizado, relaciones/multiplicidades y exportación PDF. Los Mermaid de auditoría no lo reemplazan. | P0 |
| TEC-07 | Gestionarse mediante repositorio Git (§8, §10) | COMPLETO | Repositorio local con 30 commits alcanzables en --all, ramas y origin configurado. | git log --all / branch -avv / remote -v (solo lectura). | No implica que los cambios locales o la última versión estén entregados en remoto. | P1 |
| TEC-08 | Participación continua de TODOS, ramas por estudiante y requerimiento (§8, §10) | BLOQUEADO | Metadatos locales muestran contribuciones y PR/ramas por RF; roster completo no disponible. | git shortlog: 3 nombres de autor, dos son variantes Ángeles/Angeles; no inferir tres estudiantes. | Equipo debe aportar integrantes y evidencia remota vigente. Revisar cambios locales sin seguimiento antes de futura entrega autorizada. | P0 |
| TEC-09 | Datos iniciales suficientes para demostrar (§8) | PENDIENTE | [Main.java](../src/main/java/edu/hoteluq/Main.java#L1) no inicializa Hotel; TestCommercialPolicies/Phase2Fixture son solo tests. | Pruebas tienen datos, pero no existe un arranque de demostración. | Escenario demo con catálogos, clientes, reservas/pagos/puntos/reportes y credenciales de operador seguras. | P0 |
| TEC-10 | Documento final según plantilla; un integrante entrega (§10) | BLOQUEADO | Hay análisis/seguimiento/evidencias; no documento final conforme a plantilla suministrada. | Revisión de archivos locales; no hay plantilla final ni documento conectado. | Obtener plantilla y responsable de entrega; componer documento final. Su ausencia no significa que no pueda haber copia externa del equipo. | P0 |
| TEC-11 | Análisis previo: actores, RF codificados, entidades/relaciones/multiplicidades/CU/patrones (§5, §9) | PARCIAL | [analisis.md](../docs/analisis.md#L1); commit 8ec9b15 anterior a 6af6bc3 (dominio habitaciones). | Historial local respalda análisis antes del dominio, no antes de todo esqueleto Gradle/Main. | Reconciliar cobertura con el PDF, marcar entidades propuestas vs implementadas y completar el modelo final. | P1 |
| TEC-12 | Al menos dos reglas adicionales documentadas (§3.7) | COMPLETO | RN-22 capacidad; RN-23 habitación inactiva, en analisis.md y validaciones reales. | ReservationTest.guestCountMustFitCapacity; CatalogAdministrationTest.inactiveTypeExcludesItsRoomsWithoutDeletingOrCancellingAnything. | Preservar reglas adicionales compatibles; no redefinir porcentajes oficiales. | P1 |
| TEC-13 | Un integrante sustenta TODO; factor 0–1 multiplica nota 0–5 (§11) | BLOQUEADO | Es una condición de evaluación, no una clase implementable. No se realizó simulación del equipo. | No corresponde a JUnit; entrevista/sustentación no aportada. | Todos deben poder explicar flujos, invariantes, SOLID, patrones, pruebas y límites; elegir expositor no depende del equipo según PDF. | P0 |
| SOLID-SRP | Demostrar SRP (§6.1) | PARCIAL | HotelTime/CopMoney/DTOs tienen foco; Hotel reúne sesiones, CRUD, políticas y transacciones (734 líneas). | Evidencia y pruebas indirectas identificadas en auditoria-diseno.md; no existe suite completa de contratos SOLID. | Separar responsabilidades manteniendo una frontera transaccional; ver informe de diseño. | P1 |
| SOLID-OCP | Demostrar OCP (§6.1) | PARCIAL | RefundPolicy/RoomCreator habilitan variación; Registry/enum y downcast de ConfiguredRefundPolicy limitan extensiones. | Evidencia y pruebas indirectas identificadas en auditoria-diseno.md; no existe suite completa de contratos SOLID. | Documentar extensión concreta y reducir conocimiento de implementaciones por el agregado. | P1 |
| SOLID-LSP | Demostrar LSP (§6.1) | PARCIAL | Room se usa polimórficamente y subclases satisfacen consultas; API decimal por defecto de RefundPolicy puede lanzar UnsupportedOperationException. | Evidencia y pruebas indirectas identificadas en auditoria-diseno.md; no existe suite completa de contratos SOLID. | Definir contrato común decimal y comprobar sustitución para todas las implementaciones; no declarar violación solo por rechazo legítimo de estados terminales. | P1 |
| SOLID-ISP | Demostrar ISP (§6.1) | PARCIAL | EntityGuard tiene dos operaciones cohesionadas; RefundPolicy mezcla contrato legado double y exacto decimal. | Evidencia y pruebas indirectas identificadas en auditoria-diseno.md; no existe suite completa de contratos SOLID. | Separar compatibilidad de contrato de cálculo sin multiplicar interfaces artificialmente. | P1 |
| SOLID-DIP | Demostrar DIP (§6.1) | PARCIAL | Se inyecta Clock y se depende de EntityGuard/RefundPolicy; fachadas dependen de Hotel concreto y dominio importa DTO/config de service. | Evidencia y pruebas indirectas identificadas en auditoria-diseno.md; no existe suite completa de contratos SOLID. | Introducir puertos estrechos cuando haya implementaciones alternativas; no basta inyectar una clase concreta. | P1 |
| PAT-C | Al menos 3 patrones creacionales (§6.2) | COMPLETO | Singleton (Hotel), Factory Method (RoomCreator), Builder (Reservation.Builder). | HotelTest.singletonAndFactoryRemainFunctionalForAllCategories; ReservationTest. | Documentar defensa y diagramas en el documento final; implementación real inventariada aquí. | P1 |
| PAT-E | Al menos 4 estructurales (§6.2) | COMPLETO | Facade, Composite, Decorator y Adapter ReportChartAdapter/ReportChartView | ReportsJavaFxTest y suites anteriores | Defender necesidad/participantes y preparar diagrama final cuando se autorice ese cierre. | P1 |
| PAT-B | Al menos 3 comportamientos (§6.2) | PARCIAL | State y Strategy comprobados; H-03 añade Specification como patrón de dominio, separado del conteo GoF conservador | PromotionRulesTest.allOfComposesSpecificationsWithoutChangingPredicates; suites State/Strategy existentes | No certificar el mínimo GoF solo por Specification; falta un comportamiento inequívoco o validar criterio de rúbrica. | P0 |

## 4. Cobertura del PDF y correcciones propuestas a los 26 RF

Los 26 cubren buena parte de las funciones nominales, pero **no son una cobertura suficiente tal como están redactados**. Compras y solicitudes de recarga no tienen un flujo identificado; puntos/reportes están demasiado agregados y RF-026 no es un mínimo funcional explícito del enunciado. Las siguientes son propuestas de requisitos/aceptación, no nuevas funcionalidades implementadas ni modificaciones al código o al análisis anterior.

| ID propuesto / refinado | Exigencia y fuente | Diagnóstico | Incorporación propuesta | Prioridad |
|---|---|---|---|---|
| RF-027 (propuesto) | Compras con tarjeta, además de reservas (§2.1, §4) | No hay Purchase/Order/PurchaseService. La tarjeta registrada rechaza CHARGE genérico y exige pago vinculado a reserva. | Modelar compras de servicios/beneficios si ese es el alcance esperado; confirmar con docente si contratar servicios dentro de una reserva satisface toda la noción de compra. No declarar completo sin resolver y demostrar ese recorrido. | P0 |
| RF-028 (propuesto) | Cliente sin saldo puede solicitar recarga (§3.2) | RF-020 solo cubre que un administrador abone. No hay acción/canal de solicitud desde la aplicación. | Definir solicitud y resultado visible para cliente/administrador. El PDF no prescribe una cola de aprobación, estados concretos ni una pasarela: no inventarlos como obligatorios. | P1 |
| RF-003 | Tipos, características y tarifas (§2.1) | La descripción actual solo nombra tipos. | Incluir explícitamente características/tarifas de habitaciones y su relación con el tipo; UI con información coherente. | P1 |
| RF-012/013/025 | Programa completo (§4) | H-02 implementa cuenta/lotes/vencimiento/FEFO/tres beneficios; RF-025 integra servicios dentro de reservas, sin compra autónoma. | Añadir criterios PTS-01 a PTS-10 y acumulación por reservas **y compras**, sin fijar equivalencias que el docente deja al grupo. | P0 |
| RF-017/013 | Paquetes como beneficio mínimo (§4) | H-02 implementa ServiceComponent.Leaf/Package y canje, con pruebas de precio/disponibilidad/referencias. | Composición, precio/disponibilidad y canje de paquetes; relacionarlo con servicios sin duplicar un RF ficticio. | P0 |
| RF-022/023/024 | Reportes mínimos (§7, §8) | Los tres RF no enumeran familias/cabecera/filtros/seguridad. | Criterios REP-01 a REP-13; exportar al menos TXT **o** PDF. No imponer ambos ni afirmar que los tres gráficos son obligatorios. | P0 |
| RF-026 | Notificaciones | §6.2 las presenta como problema posible para seleccionar patrones, no como función mínima de §2. | Conservar como requisito propio justificado (puede soportar Observer), pero no atribuir obligatoriedad literal al docente. La auditoría interna no lo cumple. | P2 |
| RF-019 | Check-out y modificaciones controladas del grupo | El PDF exige gestionar reservas y conservar su información confirmada; no prescribe una operación económica de cambio confirmado ni check-out. | Documentar esos casos propios como criterios de RF-019. Mantener bloqueo económico sin convertir esa ampliación en bloqueo de todos los mínimos docentes. | P1 |

Hay dos reglas adicionales reales del grupo, RN-22/RN-23; cumplen §3.7. Bogotá/COP/HALF_UP/total positivo son decisiones suplementarias, no reglas monetarias literales del PDF. La tasa de acumulación y las equivalencias sí están delegadas al grupo; los porcentajes de cancelación **no**.

## 5. H-01 — Cancelaciones: cerrado en código tras corrección autorizada

Fuente: §3.3, página impresa/física 5. La auditoría inicial detectó 72 h al 100 %, menos de 24 h al 40 % en una fixture y la obligación de devolución parcial para tiempo negativo. Esas expectativas se corrigieron, sin eliminar sus casos ni alterar los comprobantes históricos. La evidencia original describe el árbol anterior; no es el estado actual.

| Anticipación del cliente | Regla actual | Decisión de límites |
|---|---:|---|
| Más de 72 h | 100 % | Estrictamente mayor; un nanosegundo más ya es 100 %. |
| Exactamente 72 h | 80 % | Pertenece a la franja 24–72 h. |
| Entre 24 y 72 h | 80 % | Sin solapamientos ni vacíos. |
| Exactamente 24 h | 80 % | Interpretación inclusiva y consistente adoptada por el proyecto; no se atribuye a una aclaración del docente. |
| Menos de 24 h, cero y tiempo negativo | 0 % | Se conserva elegibilidad por estado. Extender el intervalo a negativos no se presenta como un derecho posterior a llegada expresamente regulado por el PDF. |
| Hotel, cualquier anticipación | 100 % | Para estados cancelables; reintento sobre CANCELLED no crea una segunda devolución. |

[CancellationSchedule.official](../src/main/java/edu/hoteluq/strategy/CancellationSchedule.java#L22) activa por defecto el perfil `HOTELUQ-ACADEMIC-CANCELLATION/v1`: (-infinito,24h)→0, [24h,72h+1ns)→80, [72h+1ns,+infinito)→100. El nanosegundo es resolución discreta de Duration, no una tolerancia comercial.

[Hotel.configureCancellation](../src/main/java/edu/hoteluq/singleton/Hotel.java#L472) llama a [requireOfficial](../src/main/java/edu/hoteluq/strategy/CancellationSchedule.java#L39) antes de cambiar registro/historial/política activa. Se comprueban todas las intersecciones de ventanas; solo se aceptan particiones equivalentes, incluidas escalas 80/80.00. ID/versiones siguen siendo metadatos, no permiso para elegir otros derechos de devolución. [Hotel.cancelReservation](../src/main/java/edu/hoteluq/singleton/Hotel.java#L641) vuelve a validar antes de escribir. RefundPolicySelector comparte los límites oficiales; State y las estrategias Full/Partial/No se conservan.

Los snapshots no oficiales pueden seguir representándose para lectura histórica, pero no activarse para nuevos reembolsos. Un reintento conserva el primer recibo, pago, tarjeta, ID/versión y configuración originales. Hotel sigue usando FullRefundPolicy independientemente del tiempo restante. Saldo, estado, comprobantes e historial se actualizan/restauran dentro del bloqueo transaccional existente; no hubo cambios al ledger ni a esa frontera.

[OfficialCancellationTest.officialClientWindowsAreExactAndRetriesNeverCreditTwice](../src/test/java/edu/hoteluq/singleton/OfficialCancellationTest.java#L52) prueba límites y nanosegundos; [incompatibleActivationHasNoFinancialOrRegistrySideEffects](../src/test/java/edu/hoteluq/singleton/OfficialCancellationTest.java#L99) rechaza ventanas incompatibles, incluida una de un nanosegundo a 1.000 días; otros casos cubren hotel, pagos fallidos/saldo cero, concurrencia, calendario Bogotá y permisos.

**Validación de cierre:** `./gradlew --no-daemon clean test build`, Java 21, BUILD SUCCESSFUL en 1 min 46 s; **272 ejecutadas/aprobadas, 0 fallidas/errores/omisiones**, con los 242 anteriores conservados/adaptados y 30 nuevos. `git diff --check`: salida 0. Detalle: [evidencia-cancelacion-oficial.txt](evidencia-cancelacion-oficial.txt). H-01 queda cerrado en negocio; RF-011 sigue PARCIAL como aplicación integral por ausencia de UI. Horas de entrada/salida siguen pendientes; los porcentajes no.

## 6. Programa de puntos: evidencia y criterios mínimos

Fuente: §4, página impresa/física 7. **Actualización H-02:** existe implementación de negocio en `loyalty/`, integrada con las transacciones reales del hotel. No se requiere un campo duplicado en Client: las cuentas se identifican por el ID del cliente registrado y solo se consultan tras autorización. Ver [contrato y configuración H-02](programa-puntos-h02.md).

| ID de aceptación | Obligación | RF relacionados | Estado | Evidencia / prueba actual | Trabajo requerido |
|---|---|---|---|---|---|
| PTS-01 | Cuenta asociada a cada cliente | 012/025 | COMPLETO | PointsLedger.Account; LoyaltyIntegrationTest.everyClientHasAnIsolatedEmptyAccount | Criterio delimitado a negocio; UI sigue pendiente. |
| PTS-02 | Acumular por reservas/compras | 025/008/027 propuesto | PARCIAL | paidRoomAndServicesAccrueOnlyActualCopPayment; paidReservationCreditsActualPaymentOnce | Tasa por aprobar; compras autónomas no existentes. Sin tasa hay origen diferido seguro. |
| PTS-03 | Fecha individual de acumulación | 012/025 | COMPLETO | Lot.obtainedAt/expiresAt/paymentId/policy; deferredSettlementPreservesOriginalExpiryAndIsIdempotent | Conservar en persistencia futura. |
| PTS-04 | Vencimiento anual | 012/013 | COMPLETO | expiryExactBoundaryAndTraceAreIdempotent; anniversaryUsesBogotaCalendar; concurrentExpiryQueriesRecordOneExpirationEvent | Interpretación calendario/bisiesto documentada. |
| PTS-05 | Descuento por puntos | 013 | PARCIAL | discountIsGrantedAndCashAndAccrualUseReducedAmount | Operativo; aprobar equivalencia, un beneficio por reserva y reversión. |
| PTS-06 | Servicios por puntos | 013 | PARCIAL | serviceBenefitIncludesExactlyOneFreeServiceWithRealSnapshot | Operativo; equivalencias/reversión por aprobar. |
| PTS-07 | Paquetes por puntos | 013/017 | PARCIAL | nestedPackageIsCompositeAndRoundsOnce; ServiceComponent | Operativo; equivalencias/reversión por aprobar. |
| PTS-08 | Vencidos/saldo insuficiente | 013 | COMPLETO | expiredCreditsCannotFundRedemptionAtExactAnniversary; insufficientPointsCannotBorrowPointsFromCurrentPayment | Mantener guardas en UI futura. |
| PTS-09 | Doble uso/idempotencia | 013 | COMPLETO | concurrentDifferentReservationsCannotOverspend; concurrentRedemptionRetriesProduceOneGrantAndPayment | Persistir claves/transacciones para sobrevivir reinicios. |
| PTS-10 | FEFO | 013 | COMPLETO | consumesEarliestExpiryNotInsertionOrder; asignaciones por lote | Desempate vencimiento/obtención/UUID documentado. |

Estos diez criterios de negocio no se suman a los 47 indicadores principales. Las declaraciones COMPLETO se limitan a la lógica comprobada; no certifican UI ni durabilidad.

### Definiciones del grupo que faltan cerrar

`analisis.md` **sí contiene propuestas numéricas**, por lo que no se declaran inexistentes: 1 punto por 1.000 COP; 1 punto = 50 COP de descuento; desayuno 400, parqueadero 300, spa 1.000 y Relax 1.200. Coincidir con el ejemplo del docente no convierte una propuesta en regla implementada. El docente deja la equivalencia al grupo y no impone exactamente esos valores.

El grupo debe adoptar/documentar la regla final de acumulación y cada beneficio, además de precisar: fracciones de punto y redondeo, base efectivamente pagada y servicios/compras, acumulación con descuentos, combinabilidad con promociones, uso de beneficios ligados a reserva, canje de paquetes anidados, instante de vencimiento/aniversario y año bisiesto, reversión de puntos al cancelar/reembolsar y tratamiento si ya fueron consumidos. Los descuentos de puntos no pueden evadir la regla propia de COP/total positivo. H-02 incorpora algoritmos y configuración explícita, pero no activa ninguna equivalencia por defecto. Su contrato documenta el redondeo entero por pago, aniversario local, compensación dinámica de deuda y restauración con caducidad original; las decisiones comerciales deben aprobarse antes de configurar producción.

## 7. SOLID y patrones reales

Informe verificable con problemas, clases, evidencia y diagramas: [auditoria-diseno.md](auditoria-diseno.md). Ningún principio se declara globalmente satisfecho solo por tener interfaces. Los cinco están **PARCIALES** en esta revisión: hay ejemplos concretos y limitaciones.

| Categoría | Mínimo docente | Conteo conservador comprobado | Patrones | Déficit seguro |
|---|---:|---:|---|---:|
| Creacionales | 3 | 3 | Singleton, Factory Method, Builder | 0 |
| Estructurales | 4 | 4 | Facade, Composite, Decorator y Adapter (H-05) | 0 |
| Comportamiento GoF | 3 | 2 | State, Strategy; Specification de dominio documentada aparte | 1 |

`RoomCreator.create` tiene un esqueleto con hooks compatible con Template Method: se documenta como candidato y no se esconde. Es el mismo recorrido del Factory Method y no hay justificación/prueba específica de su contrato como patrón adicional en el proyecto; no se usa para asegurar el mínimo académico. Si se demuestra formalmente y el docente acepta ambos problemas distintos, el conteo de comportamiento podría ser 3. La alternativa más sólida funcionalmente es Observer para notificaciones post-commit del RF-026 propio.

`ManagedEntity`/`EntityGuard` resuelven protección, pero no presentan un Proxy separado con el mismo contrato de un RealSubject; se inventarían participantes al dibujarlo como si existieran. No se computan Proxy, Adapter, Composite, Decorator, Observer ni Abstract Factory por aparecer sugeridos o por una conversión double/BigDecimal. Las clases de auditoría/Consumer de inyección de fallos no constituyen una bandeja ni un sistema Observer productivo.

## 8. JavaFX/MVC y pantallas (diagnóstico original; cierre H-04 al final)

Comprobaciones: build.gradle configura plugin OpenJFX y módulos `javafx.controls`/`javafx.fxml`; `Main` no extiende Application, no crea Stage/Scene ni carga FXML. No hay imports JavaFX en fuentes principales, controladores, FXML ni recursos de pantallas. Ejecutar Main solo produjo `Hello and welcome!i = 1` y los restantes números hasta 5. `Views` son records de transferencia; `HotelService`/`ReservationService` son fachadas de negocio, no controladores gráficos. **JavaFX funcional y MVC están pendientes.**

| Orden de pantallas | Cliente | Administrador | Dependencia y criterio de cierre |
|---|---|---|---|
| 1 | Registro, login/logout y perfil | Login y navegación por rol | Arranque Application y controlador fino; sesiones reales y mensajes de error. |
| 2 | Tipos, tarifas, características y disponibilidad con fechas | CRUD clientes, tipos, habitaciones, servicios y recarga | Fachadas ya disponibles; no acceder a listas del Singleton desde la vista. |
| 3 | Borrador, servicios, total COP, pago, saldo/movimientos e historial | Listado/detalle, cancelación por hotel y check-out | Perfil oficial corregido (H-01); configurar horas; refresco tras commit y errores sin dobles clics económicos. |
| 4 | Cancelación del cliente y solicitud de recarga | Atención a solicitudes según alcance acordado | Límites/reembolso oficiales y autorización en backend, no solo botón oculto. |
| 5 | Puntos por lote/vencimiento/canjes, promociones y paquetes | Gestión de condiciones, paquetes y beneficios | Depende del negocio de puntos/promociones; no maqueta que simule saldo. |
| 6 | — | Estadísticas, selección de reporte, fechas, gráficos y exportación | Mismo resultado de cálculo para totales, gráfico y archivo; permisos reales. |

No se realizó prueba visual de pantallas inexistentes. Después de implementarlas deben probarse ambos roles, accesos ajenos, ventanas cerradas/sesión expirada, errores de saldo/fechas, doble clic, reintentos y consistencia entre pantalla y comprobante.

## 9. Reportes y estadísticas (diagnóstico original; cierre H-05 al final)

Fuente: §7, §8. **Todos los criterios siguientes están PENDIENTES y carecen de pruebas de aplicación.** Existen datos fuente parciales en memoria, no un generador de reportes. Los TXT de evidencias de JUnit y el informe HTML de Gradle no son reportes operativos de HotelUQ.

| ID | Familia/criterio oficial | Datos existentes | Pendiente y prueba de aceptación requerida | RF |
|---|---|---|---|---|
| REP-01 | Reservas por periodo | Fechas de estadía, creación en AuditEvent y estado | Elegir fecha de filtro, contar correctamente, extremos/rango vacío; archivo/gráfico con mismos registros. | 022–024 |
| REP-02 | Habitaciones más reservadas | Room/Reservation | Ranking con empates, canceladas y selección de periodo definida. | 022–024 |
| REP-03 | Ocupación hotelera | Intervalos y habitaciones activas | Definir numerador/denominador; noches solapadas con el periodo, no contar toda la estancia por tocar un día. | 022–024 |
| REP-04 | Ingresos por periodo | Payment/Refund en COP | Definir bruto/neto y fecha de operación; no sumar totales de PENDING como ingresos; devolución cruzada de periodo. | 022–024 |
| REP-05 | Servicios contratados / más contratados | Quote.services | Cantidades/importes históricos, servicios/paquetes/cancelaciones con criterio documentado. | 022–024 |
| REP-06 | Recargas realizadas | CardMovement.RECHARGE/Instant | Filtros por operación/cliente, totales exactos y permisos; no confundir saldo con recargas. | 022–024 |
| REP-07 | Cancelaciones y reembolsos | Refund/AuditEvent | Decisión de cancelación vs dinero devuelto; casos de importe cero, política histórica y no duplicación. | 022–024 |
| REP-08 | Puntos acumulados/redimidos | Ledger/lotes/canjes/eventos H-02 disponibles; sin reporte calculado | Consumir ledger existente en reportes; diferenciar acumulación, redención, vencimiento y reversión. | 012/013/025/022–024 |
| REP-09 | Exportación al menos TXT o PDF | No existe exportador | Crear un formato válido y legible desde la aplicación, comprobar contenido/errores de escritura. **No son obligatorios ambos formatos.** | 023 |
| REP-10 | Visualizaciones de barras/pastel/líneas | Dependencia JavaFX sin charts | El PDF pide algunos tipos, no cuantifica cuántos ni obliga literalmente los tres. Acordar con docente; plan prudente: barras y líneas con datos reales, pastel si aporta información. | 024 |
| REP-11 | Selección de tipo y fechas cuando aplique | No existe operación de reporte | Validar selección, fechas inclusivas/exclusivas, mes/año/Bogotá y periodo inválido. | 022–024 |
| REP-12 | Solo administrador desde la aplicación | Hay seguridad base, no endpoint de reportes | Verificar token/rol en servicio real y controller; el patrón existente no prueba permiso en un método inexistente. | 022–024 |
| REP-13 | Título, generación, periodo, registros y totales cuando corresponda | Instantes/importe disponibles parcialmente | Metadatos de reporte y conciliación con movimientos; contenido vacío claro y fecha generada con Clock. | 023/024 |

## 10. Repositorio, entregables y verificaciones externas

Comprobado localmente: HEAD `53df80f`, rama `respaldo-fases-1-2`; 30 commits alcanzables con `git rev-list --all --count`. Hay PR/merges y nombres por RF. `shortlog` registra 14 commits como Angeles Rondon, 14 como Ángeles Rondón y 2 como Luis Velez: son nombres de autor, **no una verificación de tres personas**. Hay variantes de una identidad y falta la lista oficial de integrantes.

El análisis se incorporó en `8ec9b15` (2026-11-12 00:04:49 -05:00), antes del modelo de habitaciones `6af6bc3` (10:00:33 -05:00); el esqueleto Gradle/Main era anterior. Eso respalda planificación previa al dominio, pero no acredita por sí solo el documento final ni toda la actividad académica.

`origin` apunta a `https://github.com/angelcorr/hotel-management-system.git`. La referencia local `origin/main` está en `52f986d`; **no se consultó ni actualizó el estado remoto**. No puede afirmarse desde esa caché qué está publicado hoy, quién tiene permisos o qué ramas fueron borradas tras merge. Los cambios locales de fases 3/3B incluyen fuentes y pruebas sin commit/sin seguimiento: clonar el remoto no garantiza reproducir las 272 pruebas actuales de este árbol (242 al auditar inicialmente). No se hizo fetch/pull/reset/commit/push.

Hace falta información del equipo/remoto para confirmar roster, contribución continua de todos, correspondencia de ramas/commits por estudiante y requerimiento, plantilla del documento final, enlace de entrega y responsable. No se recomienda fabricar historia retrospectiva ni atribuir autorías; futuras incorporaciones deben reflejar trabajo/revisión reales y contar con autorización.

No se encontró diagrama de clases PDF, documento final según plantilla ni datos demo cargables en la aplicación. Las fixtures no sustituyen estos entregables. Según §10 un integrante realiza la entrega; según §11 **el docente selecciona a un integrante para sustentar toda la solución** y el factor de sustentación se aplica al grupo entero. Preparar a todos, no solo repartir diapositivas por módulo.

La persistencia durable no aparece exigida explícitamente por el PDF. Es un riesgo operativo real de pérdida tras reinicio, no un mínimo de base de datos inventado por esta auditoría. Mantener la propuesta de persistencia pendiente de aprobación; los reportes pueden calcularse sobre datos en memoria para el alcance académico, sin presentar eso como recuperación durable.

## 11. Plan priorizado por dependencias (solo propuesta)

| Paso | Prioridad | Trabajo y dependencias | Criterio verificable de cierre |
|---|---|---|---|
| 1 | P0 | H-01 de cancelación ya corregido y probado. Confirmar horas, compras/solicitud de recarga, cantidad de tipos de gráfico y plantilla/roster; mantener la regresión oficial. | Matriz acordada; nuevas pruebas oficiales 24/72 h y no regresión; no porcentajes comerciales incompatibles activos. |
| 2 | P0 | Implementar programa de puntos: cuenta/lotes/fecha/vencimiento anual y acumulación por pago/compra aprobados. Adoptar equivalencias del grupo y reglas de reversión. | Pruebas de aniversario/leap year, saldo vigente, acumulación única y cancelación/reembolso consistente. |
| 3 | P0 | Paquetes (Composite) y redenciones por descuentos, servicios y paquetes; FEFO, concurrencia e idempotencia. | Las tres familias de beneficio ejecutables; canje repetido/vencido/insuficiente rechazado sin cambios parciales. |
| 4 | P0 | Condiciones de promociones y aplicación al flujo pagado; definir vigencia/combinación con canjes. Pricing extensible (Decorator si aporta) conservando snapshots y COP. | Elegibilidad verdadera/falsa, límites temporales, combo descuento/servicios/canjes y un único total exigible. |
| 5 | P0 | JavaFX/MVC: iniciar shell/acceso en paralelo conceptual a negocio independiente; integrar luego pantallas en el orden §8. | Recorridos cliente/administrador con servicios reales, errores visibles y seguridad del backend conservada. |
| 6 | P0 | Estadísticas y ocho familias de reportes sobre ledger/snapshots/puntos; filtros y un exportador TXT o PDF. Adapter de datos a charts si se usa ese diseño. | Conciliación entre UI, gráfico y archivo; solo administrador; metadatos, vacíos, límites y totales probados. |
| 7 | P0/P1 | Completar/justificar déficit de patrones con necesidades de pasos 3–6. Observer post-commit si se mantiene RF-026, no antes de conocer semántica de reintentos. Evaluar/refactorizar SOLID sin romper atomicidad. | ≥3/4/3 patrones defendibles por problema/clases/diagrama/test; 5 principios con ejemplos y límites reconocidos. No posponer todo el diseño para el final. |
| 8 | P0 | Diagrama de clases PDF, documento final en plantilla, datos demo y revisión Git por integrante/requerimiento. Se pueden elaborar incrementalmente. | Artefactos consistentes con código entregado y revisión real de contribuciones; no fuentes sin seguimiento en la versión entregable. |
| 9 | P0 | Pruebas integrales JavaFX y compilación limpia; ensayo aleatorio de sustentación para todos. | Escenario completo reserva/pago/promoción/puntos/canje/cancelación/reporte y defensa del total del sistema por cada integrante. |
| 10 | P2 operativo | Revisar propuesta durable separadamente si se autoriza; no introducir DB para completar cantidades de patrones. | Decisión explícita de alcance y, si se implementa, pruebas reales de cierre/reinicio y commit idempotente. |

Dependencias clave: reportes de puntos necesitan ledger; redenciones de paquetes necesitan composición; promociones/puntos necesitan reglas compatibles con total positivo y cancelación oficial; GUI puede iniciar con acceso/catálogos existentes sin simular funciones pendientes. La corrección posterior únicamente cierra H-01; no inicia los pasos pendientes ni la fase 4.

## 12. Riesgos para evaluación y sustentación

1. **Aplicación no demostrable:** Main de ejemplo, sin JavaFX/MVC ni datos iniciales; bloquea demostrar casi todos los RF pese a las pruebas Java.
2. **H-01 corregido:** mantener el perfil oficial y sus pruebas de 24/72 h; las configuraciones antiguas solo son datos históricos, no alternativas activables. Explicar esta corrección en sustentación.
3. **Funciones mínimas ausentes:** puntos completos, paquetes, promociones elegibles y ocho reportes, no solo detalles de UI.
4. **Mínimo de patrones no acreditado:** falta estructural clara y conteo de comportamiento por defender; guardas/records/listas no se deben presentar como patrones inexistentes.
5. **SOLID parcial:** Hotel concentra responsabilidades y hay acoplamientos de capas/contratos; nombrar interfaces no basta ante preguntas de diseño.
6. **Entrega incompleta/versionado:** diagrama PDF/documento final/roster no aportados y fuentes locales sin commit; evidencia de build no equivale a código publicado reproducible.
7. **Factor de sustentación grupal:** desconocer un módulo ajeno puede afectar a todos según la fórmula del PDF. No se asigna una nota estimada sin evaluar al equipo.
8. **Historia y dinero en memoria:** cierre pierde todo y las claves idempotentes; reconocerlo durante demostración, sin prometer durabilidad ni atribuir al docente una base de datos no exigida.

## 13. Validación y límites de esta auditoría

**Validación original de auditoría (registro histórico):** `./gradlew --no-daemon clean test build`, Java 21 / Gradle 8.14.3, salida 0, **BUILD SUCCESSFUL en 1 min 33 s**. Los 15 XML de JUnit registran **242 ejecutadas y aprobadas, 0 fallidas, 0 errores y 0 omitidas**. `git diff --check` terminó con salida 0. Hubo un intento previo bloqueado por permisos de caché y otro interrumpido con código 143; no se contabilizan como pruebas aprobadas ni como fallos de aserción. Resultado reproducible, pruebas por clase, intentos interrumpidos y control de integridad de archivos: [evidencia-auditoria.txt](evidencia-auditoria.txt). Los resultados JUnit no son cobertura de requisitos no implementados. No hay prueba de aceptación JavaFX ni de reinicio durable.

Durante la auditoría original solo se crearon `auditoria-proyecto-final.md`, `auditoria-diseno.md` y `evidencia-auditoria.txt`; su comprobación SHA-256 corresponde a ese momento. La corrección posterior H-01 modifica exclusivamente la política/activación de cancelación, sus pruebas y documentación, con inventario separado en `evidencia-cancelacion-oficial.txt`. No se inició ninguna otra funcionalidad ni se hicieron commits, push o despliegues.

## Actualización H-02: alcance y plan vigente

H-02 implementa lógica real de puntos y paquetes: [programa-puntos-h02.md](programa-puntos-h02.md), [evidencia-h02.txt](evidencia-h02.txt). Se conservan intactos los archivos de las 272 pruebas anteriores y las estrategias/State oficiales. El inventario actual es **3 creacionales, 2 estructurales y 2 de comportamiento**: se añade Composite, sin atribuir nuevos patrones a simples records/enums.

RF-012/013/025 pasan de PENDIENTE a PARCIAL; RF-017 amplía su evidencia. Totales RF: 0 completos / 22 parciales / 4 pendientes (022/023/024/026). La matriz general mantiene 6 completos técnicos, 30 parciales, 8 pendientes y 3 bloqueados. Cero RF de aplicación final completos obedece al criterio JavaFX/MVC original, no a ausencia de negocio.

Orden recomendado: (1) aprobar configuración de puntos y reversión; (2) delimitar/implementar compra autónoma si se exige separada de reserva; (3) promociones con elegibilidad real, sin habilitarlas por catálogo; (4) JavaFX/MVC con las fachadas existentes; (5) reportes/estadísticas/exportador, incluido ledger de puntos; (6) dos estructurales y un comportamiento adicionales solo por necesidades reales; (7) documento/diagrama PDF, datos demo, pruebas integrales y sustentación. No se inició ninguno de esos pasos en H-02.

Persistencia/idempotencia después del reinicio siguen pendientes de arquitectura aprobada. Los conteos/pruebas originales de auditoría y H-01 son históricos; el resultado actual y archivos efectivamente cambiados se registran en evidencia-h02.txt.

**Verificación histórica H-02:** Java 21 / Gradle 8.14.3; `./gradlew --no-daemon clean test build`: BUILD SUCCESSFUL (2 min 9 s), 351 pruebas aprobadas, 0 fallidas/errores/omitidas; 272 anteriores conservadas sin editar + 79 nuevas. `git diff --check`: salida 0. Evidencia: [evidencia-h02.txt](evidencia-h02.txt).

## Actualización H-03: promociones verificadas

**Verificación histórica H-03:** Java 21 / Gradle 8.14.3; `./gradlew --no-daemon clean test build`: BUILD SUCCESSFUL (2 min 27 s), 416 pruebas ejecutadas/aprobadas, 0 fallidas, 0 errores y 0 omitidas. Se conservan las 351 anteriores sin editar sus archivos y se añaden 65 (22 unitarias de reglas/precio y 43 de integración). `git diff --check`: salida 0. Evidencia: [evidencia-h03.txt](evidencia-h03.txt).

Se implementan condiciones, versiones/auditoría administrativa, actividad, vigencias, cálculo COP y combinaciones explícitas con H-02: [promociones-h03.md](promociones-h03.md). No se activan las propuestas comerciales de análisis ni tasas de puntos nuevas. Evidencia e inventario actuales: [evidencia-h03.txt](evidencia-h03.txt).

RF-007/RF-018 continúan PARCIAL como aplicación final: el negocio ahora existe y tiene pruebas; falta configuración comercial concreta e interfaz. Totales de matriz se mantienen: 26 RF (0 completos / 22 parciales / 4 pendientes), 47 indicadores (6 completos / 30 parciales / 8 pendientes / 3 bloqueados). Los criterios técnicos de UI/reportes/entrega no cambian por añadir lógica Java.

Los contratos pagados incorporan PromotionDefinition en Quote/BookingView e historial anterior/posterior. Los cambios de promoción exigen nueva aceptación en PENDING; confirmar no recalcula ofertas silenciosamente. Pagos, devoluciones oficiales y puntos usan importe efectivo. Paquetes seleccionados se integran con Composite, sin dobles cobros por hojas repetidas.

Patrones actuales GoF: 3 creacionales / 3 estructurales / 2 comportamientos; Decorator añade un estructural real. Specification documenta otro patrón de dominio, sin asumir su aceptación como tercer comportamiento GoF. Faltan un estructural, un comportamiento inequívoco y diagramas de entrega PDF. SOLID permanece parcial con mejoras específicas descritas en auditoria-diseno.md.

Siguiente integración JavaFX recomendada: pantallas administrativas mediante PromotionService y expectedVersion; selección de una promoción/beneficio/paquetes mediante ReservationService/LoyaltyService; revisión de cotización con condiciones, versión y total; tratamiento de conflictos pidiendo nueva aceptación. No trasladar autorización ni elegibilidad a controles visuales. No se implementó JavaFX.

Persistencia durable y compras autónomas fuera de reservas siguen pendientes previos. Aprobar condiciones/porcentajes/periodos, criterio y umbral de frecuencia, combinación/orden y acumulación; conservar las decisiones pendientes de H-02. Las evidencias de H-01/H-02 son históricas; la ejecución actual se registra en evidencia-h03.txt.

## Actualización H-04 — JavaFX/MVC implementados

**Verificación H-04 (15/11/2026):** Java 21 / Gradle 8.14.3, BUILD SUCCESSFUL. Regresión sin pantalla: 457 aprobadas (416 anteriores intactas + 41 nuevas). Integración gráfica real: 3 aprobadas. **Total: 460 ejecutadas/aprobadas, 0 fallidas, 0 errores y 0 omitidas.** Arranque real con `run --args=--smoke-ui`: salida 0 y `HOTELUQ_UI_STARTED`. `git diff --check`: salida 0. Detalle: [evidencia-h04.txt](evidencia-h04.txt).

Existe aplicación de escritorio ejecutable: Main → HotelApplication → FXML/CSS y controladores de pantallas. Guía y flujos: [ejecucion-javafx-h04.md](ejecucion-javafx-h04.md). Evidencia actual: [evidencia-h04.txt](evidencia-h04.txt). La conclusión original «no hay aplicación gráfica» es histórica y queda reemplazada por esta comprobación.

RF-001/002/003/004/005/009/010/011/012 se cierran en el alcance local: acceso/registro, consulta real de habitaciones/tipos/disponibilidad, reserva/pago, tarjeta/historial, cancelación y consulta de puntos están integrados a pantallas funcionales. Estado actual: 9 completos, 13 parciales y 4 pendientes. TEC-02/TEC-03 completos; matriz total 17 completos / 21 parciales / 6 pendientes / 3 bloqueados. Estos conteos no certifican durabilidad ni todas las decisiones comerciales/entregables académicos.

RF-006/007/013/014–021/025 disponen de integración gráfica para sus operaciones existentes: servicios/paquetes, promociones, canjes, CRUD, recargas, check-out y parámetros. Se mantienen parciales por aprobación de reglas comerciales, recorridos no cerrados (compras autónomas/modificaciones confirmadas) y campaña visual completa de formularios/combinaciones. Su pendiente ya no es «no existe pantalla». RF-008 conserva pago real en UI pero compra autónoma fuera de reservas pendiente. RF-022/023/024/026 siguen sin reportes/estadísticas/notificaciones nuevos.

La prueba gráfica carga FXML real, ejecuta eventos y servicios asíncronos y captura únicamente escenas del hotel. No se deduce funcionamiento visual solo de compilar. Las consultas previas de precio/reembolso viven en el modelo; versión y condiciones se vuelven a comprobar dentro del monitor antes de cobrar/cancelar. La UI no muta entidades ni calcula descuentos/puntos/reembolsos.

Los porcentajes oficiales, State/Strategy, Composite, Decorator y Specification se conservan. MVC se documenta como arquitectura, no como otro patrón GoF para inflar el mínimo. No se realizaron diagramas finales, reportes, estadísticas, base de datos, commits, push o despliegues.

H-05 recomendado: servicios de reportes/estadísticas con criterios/periodos/totales conciliados sobre datos existentes, luego vistas/gráficos/exportación autorizados; mantener cálculos fuera de controladores. Persistencia/recuperación durable sigue requiriendo autorización de arquitectura y no se confunde con atomicidad en memoria.

## Actualización H-05 — Ocho familias, gráficos y TXT

**Verificación H-05 (15/11/2026):** Java 21 / Gradle 8.14.3; `clean test build fxTest run --args=--smoke-ui`: BUILD SUCCESSFUL (3 min 11 s), salida 0. Regresión sin pantalla: 506 aprobadas; JavaFX real: 12 aprobadas. **Total: 518 ejecutadas/aprobadas, 0 fallidas, 0 errores y 0 omitidas**, incluidas las 460 anteriores sin editar y 58 nuevas. Inicio real con `HOTELUQ_UI_STARTED`; `git diff --check`: salida 0. Detalle: [evidencia-h05.txt](evidencia-h05.txt).

Reportes/estadísticas/exportación se implementan realmente en negocio y JavaFX: [reportes-h05.md](reportes-h05.md), [evidencia-h05.txt](evidencia-h05.txt). Solo administradores; un corte de reservas/contratos/movimientos/ledger bajo el mismo monitor garantiza que no se observan pagos parciales. Los resultados inmutables alimentan tablas, gráficos y TXT sin recalcular en controladores ni al exportar un corte mostrado.

RF-022/023/024 completos dentro de los criterios publicados: ocho familias, filtros, comparación de reservas, ranking, noches/capacidad, pagos/reembolsos/neto, servicios, recargas, cancelaciones y puntos. Los gráficos de barras/pastel/líneas fueron ejercitados en pantalla real. TXT incluye metadatos/registros/totales UTF-8 y comprobación administrativa; PDF no es obligatorio adicional y queda como adaptador futuro.

No se ocultan carencias de datos: ocupación es contractual con inventario actual más compromisos (no capacidad/presencia física histórica observada); habitación/servicios se atribuyen analíticamente sobre el pago real y concilian por mayor resto, sin alterar precios/penalizaciones; puntos presentan movimientos del rango y saldos al corte, sin fingir saldos históricos. Se reconstruyen vencimientos desde lotes/asignaciones/ajustes, no se confunden con observación tardía de EXPIRED.

Totales actuales: 26 RF = 12 completos / 13 parciales / 1 pendiente. Matriz general = 21 completos / 20 parciales / 3 pendientes / 3 bloqueados. Los desgloses REP no se suman como RF adicionales. Estos conteos corresponden al alcance local y no afirman persistencia durable, aprobación comercial ni entrega académica final.

Adapter se acredita como cuarto estructural, con Target ReportChartView, cliente ReportScreen, adaptee ReportResult.ChartSpec y contratos JavaFX incompatibles transformados por ReportChartAdapter. GoF: 3 creacionales / 4 estructurales / 2 comportamientos; Specification sigue documentado aparte. No se suma la nueva fachada como otro patrón ni se inventa Observer para cerrar cantidades.

Siguiente etapa recomendada: auditoría final de reglas/rúbrica, aceptación manual con equipo, datos demo explícitos, documento/diagramas finales y sustentación. Persistencia durable debe aprobarse como arquitectura separada. Se conservan State/Strategy y políticas oficiales; no se hicieron commits, push ni despliegues.
