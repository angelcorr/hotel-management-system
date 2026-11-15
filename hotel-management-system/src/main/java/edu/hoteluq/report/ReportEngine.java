package edu.hoteluq.report;

import edu.hoteluq.config.HotelTime;
import edu.hoteluq.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import static edu.hoteluq.report.ReportResult.*;

/** Pure aggregations over one immutable, authorized snapshot. No toolkit, filesystem or mutable entities. */
public final class ReportEngine {
  private ReportQuery q;
  private ReportSnapshot data;
  private List<ReportSnapshot.Booking> bookings;
  private final List<Section> sections=new ArrayList<>();
  private final List<Metric> totals=new ArrayList<>();
  private final List<ChartSpec> charts=new ArrayList<>();
  private final List<String> criteria=new ArrayList<>();
  public ReportResult generate(ReportSnapshot snapshot,ReportQuery query){
    if(q!=null)throw new IllegalStateException("Use a fresh report engine");q=Objects.requireNonNull(query);data=Objects.requireNonNull(snapshot);
    bookings=data.bookings().stream().filter(this::matches).sorted(Comparator.comparing(b->b.id().toString())).toList();
    criteria.add("Fechas inclusivas; operaciones en America/Bogota [00:00 inicial,00:00 posterior al final). Estado y última cotización/contrato al corte; no reconstruye dimensiones al cierre histórico del periodo.");
    switch(q.kind()){
      case RESERVATIONS->reservations();case ROOM_RANKING->ranking();case OCCUPANCY->occupancy();case REVENUE->revenue();
      case SERVICES->services();case RECHARGES->recharges();case CANCELLATIONS->cancellations();case POINTS->points();
    }
    return new ReportResult(q,data.at(),criteria,sections,totals,charts);
  }
  private boolean matches(ReportSnapshot.Booking b){return q.client(b.clientId()) && (q.category()==null || q.category()==b.category()) && (q.roomNumber()==null || q.roomNumber()==b.room()) && (q.status()==null || q.status()==b.status());}
  private void metric(String key,String label,BigDecimal value,Unit unit){totals.add(new Metric(key,label,value,unit));}
  private BigDecimal n(long value){return BigDecimal.valueOf(value);}
  private LocalDate date(Instant at){return at.atZone(HotelTime.ZONE).toLocalDate();}
  private void section(String title,List<String> columns,List<Row> rows){sections.add(new Section(title,columns,rows));}
  private Map<String,BigDecimal> days(){Map<String,BigDecimal> result=new TreeMap<>();for(LocalDate d=q.from();!d.isAfter(q.through());d=d.plusDays(1))result.put(d.toString(),BigDecimal.ZERO);return result;}
  private void add(Map<String,BigDecimal> map,String key,BigDecimal value){map.merge(key,value,BigDecimal::add);}
  private Series series(String name,Map<String,BigDecimal> values){return new Series(name,values.entrySet().stream().map(e->new Point(e.getKey(),e.getValue())).toList());}
  private void chart(String title,Unit unit,Series... series){charts.add(new ChartSpec(title,unit,List.of(series)));}
  private List<ReportSnapshot.Booking> created(){return bookings.stream().filter(b->q.contains(b.createdAt())).sorted(Comparator.comparing(ReportSnapshot.Booking::createdAt).thenComparing(b->b.id().toString())).toList();}
  private void reservations(){
    criteria.add("Cohorte por evento CREATED, todos los estados salvo filtro explícito. Estado actual al corte; no estado reconstruido al final del periodo.");
    var selected=created();Map<String,BigDecimal> daily=days(),states=new TreeMap<>();List<Row> rows=new ArrayList<>();
    for(var b:selected){add(daily,date(b.createdAt()).toString(),BigDecimal.ONE);add(states,b.status().name(),BigDecimal.ONE);rows.add(row(b.id(),b.clientId(),b.room(),b.category(),b.createdAt(),b.checkIn(),b.checkOut(),b.status()));}
    section("Reservas creadas",List.of("Reserva","Cliente","Habitación","Tipo","Creación Bogotá","Entrada","Salida","Estado al corte"),rows);
    section("Distribución diaria",List.of("Fecha Bogotá","Cantidad"),daily.entrySet().stream().map(e->row(e.getKey(),e.getValue())).toList());
    metric("reservations","Reservas",n(selected.size()),Unit.COUNT);metric("undated","Registros sin fecha CREATED excluidos",n(bookings.stream().filter(b->b.createdAt()==null).count()),Unit.COUNT);
    chart("Reservas por fecha",Unit.COUNT,series("Reservas",daily));chart("Estados actuales de la cohorte",Unit.COUNT,series("Reservas",states));
    if(q.comparePrevious()){
      LocalDate start=q.from().minusDays(q.days()),end=q.from().minusDays(1);Instant a=start.atStartOfDay(HotelTime.ZONE).toInstant(),z=q.from().atStartOfDay(HotelTime.ZONE).toInstant();
      long previous=bookings.stream().filter(b->b.createdAt()!=null && !b.createdAt().isBefore(a) && b.createdAt().isBefore(z)).count();
      metric("previous","Reservas periodo anterior",n(previous),Unit.COUNT);metric("difference","Diferencia absoluta",n(selected.size()-previous),Unit.COUNT);
      criteria.add("Comparación anterior de igual longitud: "+start+" a "+end+". Sin porcentaje si la base es cero.");
      Map<String,BigDecimal> comparison=new LinkedHashMap<>();comparison.put("Anterior",n(previous));comparison.put("Seleccionado",n(selected.size()));chart("Comparación de periodos",Unit.COUNT,series("Reservas",comparison));
    }
  }
  private void ranking(){
    criteria.add("Ranking por reservas CREATED en el periodo, todos los estados salvo filtro. Empates: número de habitación ascendente.");
    Map<Integer,List<ReportSnapshot.Booking>> groups=new TreeMap<>();for(var b:created())groups.computeIfAbsent(b.room(),k->new ArrayList<>()).add(b);
    var ranked=groups.entrySet().stream().sorted(Comparator.<Map.Entry<Integer,List<ReportSnapshot.Booking>>>comparingInt(e->e.getValue().size()).reversed().thenComparingInt(Map.Entry::getKey)).toList();
    List<Row> rows=new ArrayList<>();Map<String,BigDecimal> counts=new LinkedHashMap<>();int position=0;
    for(var entry:ranked){var first=entry.getValue().getFirst();rows.add(row(++position,entry.getKey(),first.category(),first.typeName(),entry.getValue().size()));counts.put("Hab. "+entry.getKey(),n(entry.getValue().size()));}
    section("Ranking",List.of("Posición","Habitación","Categoría","Tipo histórico","Reservas"),rows);metric("reservations","Reservas",n(created().size()),Unit.COUNT);metric("rooms","Habitaciones con reservas",n(groups.size()),Unit.COUNT);chart("Habitaciones más reservadas",Unit.COUNT,series("Reservas",counts));
  }
  private void occupancy(){
    criteria.add("Ocupación contractual de noches [entrada,salida), CONFIRMED/COMPLETED pagadas; excluye CANCELLED. No mide presencia física ni horas reales de check-in/out.");
    criteria.add("Capacidad por noche: inventario activo actual (habitación y tipo), más habitaciones con compromiso confirmado esa noche aunque hoy inactivas. No hay historial de activaciones: no se presenta como capacidad histórica observada.");
    criteria.add("PENDING bloquea disponibilidad pero no es ocupación confirmada. Filtros de cliente/estado afectan compromisos, no reducen el inventario del hotel.");
    List<Row> rows=new ArrayList<>();Map<String,BigDecimal> occupiedSeries=new TreeMap<>(),availableSeries=new TreeMap<>(),pendingSeries=new TreeMap<>();long capacityTotal=0,occupiedTotal=0,pendingTotal=0,availableTotal=0;
    for(LocalDate day=q.from();!day.isAfter(q.through());day=day.plusDays(1)){
      Set<Integer> capacity=new HashSet<>(),occupied=new HashSet<>(),pending=new HashSet<>();
      for(var room:data.rooms())if(room.active() && (q.category()==null || q.category()==room.category()) && (q.roomNumber()==null || q.roomNumber()==room.number()))capacity.add(room.number());
      for(var b:bookings)if(!day.isBefore(b.checkIn()) && day.isBefore(b.checkOut())){
        if(b.payment()!=null && (b.status()==ReservationStatus.CONFIRMED || b.status()==ReservationStatus.COMPLETED)){occupied.add(b.room());capacity.add(b.room());}
        else if(b.status()==ReservationStatus.PENDING && capacity.contains(b.room()))pending.add(b.room());
      }
      pending.removeAll(occupied);int available=capacity.size()-occupied.size()-pending.size();
      BigDecimal percent=capacity.isEmpty()?null:n(occupied.size()).multiply(n(100)).divide(n(capacity.size()),2,RoundingMode.HALF_UP);
      rows.add(row(day,capacity.size(),occupied.size(),pending.size(),available,percent==null?"N/A":percent));capacityTotal+=capacity.size();occupiedTotal+=occupied.size();pendingTotal+=pending.size();availableTotal+=available;
      occupiedSeries.put(day.toString(),n(occupied.size()));pendingSeries.put(day.toString(),n(pending.size()));availableSeries.put(day.toString(),n(available));
    }
    section("Habitaciones por noche",List.of("Fecha","Capacidad","Ocupadas contractuales","Bloqueadas PENDING","Disponibles","Ocupación %"),rows);
    metric("capacityNights","Capacidad habitación-noche",n(capacityTotal),Unit.NIGHTS);metric("occupiedNights","Ocupación habitación-noche",n(occupiedTotal),Unit.NIGHTS);metric("pendingNights","Bloqueo PENDING habitación-noche",n(pendingTotal),Unit.NIGHTS);metric("availableNights","Disponibilidad habitación-noche",n(availableTotal),Unit.NIGHTS);
    metric("occupancyPercent","Ocupación ponderada",capacityTotal==0?null:n(occupiedTotal).multiply(n(100)).divide(n(capacityTotal),2,RoundingMode.HALF_UP),Unit.PERCENT);
    chart("Habitaciones por noche",Unit.COUNT,series("Ocupadas",occupiedSeries),series("Pendientes",pendingSeries),series("Disponibles",availableSeries));
    Map<String,BigDecimal> totals=new LinkedHashMap<>();totals.put("Ocupadas",n(occupiedTotal));totals.put("Bloqueadas PENDING",n(pendingTotal));totals.put("Disponibles",n(availableTotal));chart("Distribución habitación-noche",Unit.NIGHTS,series("Noches",totals));
  }

  private void revenue() {
    criteria.add(
            "Flujo operativo por fecha de pago y devolución. "
                    + "Incluye compras independientes de servicios. "
                    + "Las recargas no constituyen ingresos."
    );

    criteria.add(
            "Los pagos por reservas y por servicios independientes "
                    + "se muestran por separado. Los reembolsos de reservas "
                    + "se descuentan del ingreso operativo."
    );

    BigDecimal paid = BigDecimal.ZERO;
    BigDecimal refunded = BigDecimal.ZERO;
    BigDecimal room = BigDecimal.ZERO;
    BigDecimal services = BigDecimal.ZERO;
    BigDecimal standalone = BigDecimal.ZERO;

    List<Row> rows = new ArrayList<>();
    Map<String, BigDecimal> daily = days();

    Set<UUID> payments = new HashSet<>();
    Set<UUID> refunds = new HashSet<>();

    // Pagos y reembolsos de reservas.
    for (var b : bookings) {
      if (b.payment() != null
              && q.contains(b.payment().at())
              && payments.add(b.payment().id())) {

        var allocation = MoneyAllocation.payment(b);

        BigDecimal roomAmount = allocation.get("ROOM");
        BigDecimal serviceAmount =
                b.payment().amount().subtract(roomAmount);

        paid = paid.add(b.payment().amount());
        room = room.add(roomAmount);
        services = services.add(serviceAmount);

        add(
                daily,
                date(b.payment().at()).toString(),
                b.payment().amount()
        );

        rows.add(row(
                "PAGO RESERVA",
                b.payment().id(),
                b.id(),
                b.clientId(),
                b.payment().at(),
                b.payment().amount(),
                roomAmount,
                serviceAmount,
                b.payment().amount()
        ));
      }

      if (b.refund() != null
              && q.contains(b.refund().at())
              && refunds.add(b.refund().id())) {

        refunded = refunded.add(b.refund().amount());

        add(
                daily,
                date(b.refund().at()).toString(),
                b.refund().amount().negate()
        );

        rows.add(row(
                "REEMBOLSO",
                b.refund().id(),
                b.id(),
                b.clientId(),
                b.refund().at(),
                b.refund().amount(),
                "—",
                "—",
                b.refund().amount().negate()
        ));
      }
    }

    // Compras independientes: no tienen habitación ni reserva.
    // Los filtros de habitación, categoría y estado no aplican.
    if (q.category() == null
            && q.roomNumber() == null
            && q.status() == null) {

      Set<UUID> purchases = new HashSet<>();

      for (var purchase : data.servicePurchases()) {
        if (!q.client(purchase.clientId())
                || !q.contains(purchase.at())
                || !purchases.add(purchase.id())) {
          continue;
        }

        standalone = standalone.add(purchase.amount());

        add(
                daily,
                date(purchase.at()).toString(),
                purchase.amount()
        );

        rows.add(row(
                "SERVICIO SIN RESERVA",
                purchase.id(),
                "—",
                purchase.clientId(),
                purchase.at(),
                purchase.amount(),
                BigDecimal.ZERO,
                purchase.amount(),
                purchase.amount()
        ));
      }
    }

    rows.sort(
            Comparator.comparing(
                    (Row r) -> r.cells().get(4).display()
            ).thenComparing(
                    r -> r.cells().get(1).display()
            )
    );

    section(
            "Flujos operativos",
            List.of(
                    "Tipo",
                    "Operación",
                    "Reserva",
                    "Cliente",
                    "Fecha Bogotá",
                    "Importe COP",
                    "Habitación atribuido COP",
                    "Servicios atribuido COP",
                    "Flujo neto COP"
            ),
            rows
    );

    BigDecimal totalIncome = paid.add(standalone);
    BigDecimal net = totalIncome.subtract(refunded);

    metric("payments", "Pagos de reservas", paid, Unit.COP);
    metric("roomRevenue", "Habitaciones", room, Unit.COP);

    metric(
            "serviceRevenue",
            "Servicios incluidos en reservas",
            services,
            Unit.COP
    );

    metric(
            "standaloneServiceRevenue",
            "Servicios comprados sin reserva",
            standalone,
            Unit.COP
    );

    metric(
            "totalIncome",
            "Ingresos brutos operativos",
            totalIncome,
            Unit.COP
    );

    metric("refunds", "Reembolsos", refunded, Unit.COP);
    metric("net", "Neto operativo", net, Unit.COP);

    chart(
            "Neto por fecha",
            Unit.COP,
            series("Neto", daily)
    );
  }

  private void services(){
    criteria.add("Contratación: una unidad por línea de servicio en reserva pagada, por fecha de pago, incluidos servicios bonificados (ingreso asignado cero). No se multiplican por noches.");
    criteria.add("Precio bruto desde Quote histórico; ingreso COP atribuido con mayor resto. Reembolsos del periodo se atribuyen proporcionalmente al pago original, aunque el pago fuera anterior. No altera comprobantes.");
    record Values(UUID id,String name,long count,BigDecimal raw,BigDecimal revenue,BigDecimal refunds){}
    Map<UUID,Values> values=new TreeMap<>();Set<UUID> seenPayments=new HashSet<>(),seenRefunds=new HashSet<>();
    var ordered=bookings.stream().sorted(Comparator.comparing((ReportSnapshot.Booking b)->b.payment()==null?Instant.MIN:b.payment().at()).thenComparing(b->b.id().toString())).toList();
    for(var b:ordered)if(b.payment()!=null){boolean purchase=q.contains(b.payment().at()) && seenPayments.add(b.payment().id());boolean refund=b.refund()!=null && q.contains(b.refund().at()) && seenRefunds.add(b.refund().id());if(!purchase && !refund)continue;
      var allocation=MoneyAllocation.payment(b);var returned=refund?MoneyAllocation.allocate(b.refund().amount(),allocation):Map.<String,BigDecimal>of();
      for(var service:b.services())if(q.serviceId()==null || q.serviceId().equals(service.id())){
        var old=values.getOrDefault(service.id(),new Values(service.id(),service.name(),0,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO));
        values.put(service.id(),new Values(service.id(),service.name(),old.count()+(purchase?1:0),old.raw().add(purchase?service.rawPrice():BigDecimal.ZERO),old.revenue().add(purchase?allocation.get(service.id().toString()):BigDecimal.ZERO),old.refunds().add(refund?returned.get(service.id().toString()):BigDecimal.ZERO)));
      }
    }

    criteria.add(
            "Incluye servicios de reservas pagadas y compras "
                    + "independientes. Las compras independientes solo "
                    + "se incluyen cuando no hay filtros de habitación, "
                    + "categoría o estado de reserva."
    );

    if (q.category() == null
            && q.roomNumber() == null
            && q.status() == null) {

      Set<UUID> seenPurchases = new HashSet<>();

      for (var purchase : data.servicePurchases()) {

        if (!q.client(purchase.clientId())
                || !q.contains(purchase.at())
                || (q.serviceId() != null
                && !q.serviceId().equals(purchase.serviceId()))
                || !seenPurchases.add(purchase.id())) {
          continue;
        }

        var old = values.getOrDefault(
                purchase.serviceId(),
                new Values(
                        purchase.serviceId(),
                        purchase.serviceName(),
                        0,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                )
        );

        values.put(
                purchase.serviceId(),
                new Values(
                        purchase.serviceId(),
                        purchase.serviceName(),
                        old.count() + 1,
                        old.raw().add(purchase.amount()),
                        old.revenue().add(purchase.amount()),
                        old.refunds()
                )
        );
      }
    }

    var sorted=values.values().stream().sorted(Comparator.comparingLong(Values::count).reversed().thenComparing(v->v.id().toString())).toList();List<Row> rows=new ArrayList<>();Map<String,BigDecimal> counts=new LinkedHashMap<>();BigDecimal quantity=BigDecimal.ZERO,revenue=BigDecimal.ZERO,refunded=BigDecimal.ZERO,raw=BigDecimal.ZERO;
    for(var value:sorted){rows.add(row(value.id(),value.name(),value.count(),value.raw(),value.revenue(),value.refunds(),value.revenue().subtract(value.refunds())));counts.put(value.name()+" ["+value.id()+"]",n(value.count()));quantity=quantity.add(n(value.count()));revenue=revenue.add(value.revenue());refunded=refunded.add(value.refunds());raw=raw.add(value.raw());}
    section("Servicios por ID histórico",List.of("Servicio ID","Nombre histórico","Cantidad","Bruto crudo COP","Ingreso atribuido COP","Reembolso atribuido COP","Neto atribuido COP"),rows);
    metric("quantity","Unidades contratadas",quantity,Unit.COUNT);metric("raw","Bruto contractual sin descuentos",raw,Unit.COP);metric("revenue","Ingreso atribuido",revenue,Unit.COP);metric("refunds","Reembolso atribuido",refunded,Unit.COP);metric("net","Neto atribuido",revenue.subtract(refunded),Unit.COP);chart("Servicios más solicitados",Unit.COUNT,series("Unidades",counts));
  }
  private void recharges(){
    criteria.add("Solo movimientos RECHARGE reales por fecha del movimiento; no son ingresos operativos. Identificadores de tarjeta/credenciales omitidos.");
    Set<UUID> ids=new HashSet<>();List<Row> rows=new ArrayList<>();BigDecimal total=BigDecimal.ZERO;Map<String,BigDecimal> daily=days();
    for(var m:data.recharges().stream().sorted(Comparator.comparing(ReportSnapshot.Recharge::at).thenComparing(v->v.id().toString())).toList())if(q.client(m.clientId()) && q.contains(m.at()) && ids.add(m.id())){rows.add(row(m.id(),m.clientId(),m.at(),m.amount()));total=total.add(m.amount());add(daily,date(m.at()).toString(),m.amount());}
    section("Movimientos de recarga",List.of("Movimiento","Cliente","Fecha Bogotá","Importe COP"),rows);metric("count","Recargas",n(rows.size()),Unit.COUNT);metric("recharged","Total recargado (no ingreso)",total,Unit.COP);chart("Recargas por fecha",Unit.COP,series("Recargado",daily));
  }
  private void cancellations(){
    criteria.add("Cancelación por Refund.at, incluidas reservas PENDING no pagadas y reembolsos de cero. Un registro por UUID de cancelación; tasas/pago original históricos.");
    List<Row> rows=new ArrayList<>();Set<UUID> seen=new HashSet<>();long hotel=0,client=0;BigDecimal total=BigDecimal.ZERO;Map<String,BigDecimal> distribution=new LinkedHashMap<>();
    for(var b:bookings)if(b.refund()!=null && q.contains(b.refund().at()) && seen.add(b.refund().id())){var f=b.refund();if(f.byHotel())hotel++;else client++;total=total.add(f.amount());rows.add(row(f.id(),b.id(),b.clientId(),f.byHotel()?"HOTEL":"CLIENTE",f.at(),f.paymentId(),f.percent(),f.amount()));}
    rows.sort(Comparator.comparing((Row r)->r.cells().get(4).display()).thenComparing(r->r.cells().getFirst().display()));
    section("Cancelaciones",List.of("Cancelación","Reserva","Cliente","Origen","Fecha Bogotá","Pago original","Porcentaje %","Reembolso COP"),rows);metric("count","Cancelaciones",n(rows.size()),Unit.COUNT);metric("hotel","Por hotel",n(hotel),Unit.COUNT);metric("client","Por cliente",n(client),Unit.COUNT);metric("refunds","Reembolsado",total,Unit.COP);distribution.put("Hotel",n(hotel));distribution.put("Cliente",n(client));chart("Origen de cancelaciones",Unit.COUNT,series("Cancelaciones",distribution));
  }
  private void points(){
    criteria.add("Movimientos por fecha efectiva, no por observación tardía. ACUMULADOS/REDIMIDOS son brutos; ajustes y restauraciones se muestran separados. Saldos vigentes/deuda al corte actual, no saldo histórico al final del rango.");
    criteria.add("Vencimientos reconstruidos por aniversario individual y asignaciones/reversiones anteriores; no se suman observaciones EXPIRED perezosas. Restaurar puntos ya vencidos no los vuelve gastables; se informa por separado.");
    List<Row> rows=new ArrayList<>(),balances=new ArrayList<>();BigDecimal earned=BigDecimal.ZERO,used=BigDecimal.ZERO,expired=BigDecimal.ZERO,restored=BigDecimal.ZERO,revoked=BigDecimal.ZERO,available=BigDecimal.ZERO,debt=BigDecimal.ZERO;
    for(var m:PointsReportData.movements(data))if(q.client(m.client()) && q.contains(m.at())){
      rows.add(row(m.id(),m.client(),m.type(),m.source(),m.at(),m.amount()));
      switch(m.type()){case "ACCRUED"->earned=earned.add(m.amount());case "REDEEMED"->used=used.add(m.amount());case "EXPIRED_CALENDAR","RESTORED_EXPIRED"->expired=expired.add(m.amount());case "REDEMPTION_RESTORED"->restored=restored.add(m.amount());case "REFUND_ADJUSTMENT"->revoked=revoked.add(m.amount());default->{}}
    }
    Map<String,BigDecimal> balanceChart=new TreeMap<>();
    for(var account:data.points().stream().filter(a->q.client(a.clientId())).sorted(Comparator.comparing(a->a.clientId())).toList()){
      available=available.add(n(account.available()));debt=debt.add(n(account.debt()));balanceChart.put(account.clientId(),n(account.available()));balances.add(row(account.clientId(),data.at(),account.available(),account.debt()));
    }
    section("Movimientos de puntos",List.of("Movimiento","Cliente","Tipo","Origen","Fecha efectiva Bogotá","Puntos"),rows);section("Saldos actuales al corte",List.of("Cliente","Corte Bogotá","Disponibles","Deuda"),balances);
    metric("earned","Acumulados brutos",earned,Unit.POINTS);metric("redeemed","Redimidos brutos",used,Unit.POINTS);metric("expired","Vencidos (incluye restaurados ya vencidos)",expired,Unit.POINTS);metric("restored","Restaurados",restored,Unit.POINTS);metric("revoked","Ajustes por reembolso",revoked,Unit.POINTS);metric("available","Saldo disponible al corte",available,Unit.POINTS);metric("debt","Deuda al corte",debt,Unit.POINTS);chart("Saldo disponible actual por cliente",Unit.POINTS,series("Disponibles",balanceChart));
  }
}
