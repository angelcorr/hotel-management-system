package edu.hoteluq.loyalty;

import edu.hoteluq.exceptions.HotelException;
import edu.hoteluq.model.AdditionalService;
import edu.hoteluq.model.BookingRecords.ServiceLine;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

/** Versioned benefits and immutable, acyclic package catalog; access controlled by its aggregate owner. */
public final class LoyaltyCatalog {
  public record PackageView(UUID id, String name, String description, List<ServiceLine> services,
      BigDecimal rawPrice, BigDecimal price, boolean available, String currency) {
    public PackageView { services=List.copyOf(services); }
  }
  private final Map<UUID,ServiceComponent.Package> packages=new LinkedHashMap<>();
  private final Map<UUID,Benefit> benefits=new LinkedHashMap<>();
  private final Map<String,Benefit> versions=new HashMap<>();
  private final Function<UUID,AdditionalService> service;
  public LoyaltyCatalog(Function<UUID,AdditionalService> service) { this.service=service; }
  public PackageView create(String name,String description,List<UUID> services,List<UUID> children) {
    if (services == null || children == null) throw new HotelException("Service and child package lists required");
    List<ServiceComponent> components=new ArrayList<>();
    for (UUID id:services) components.add(new ServiceComponent.Leaf(service.apply(id)));
    for (UUID id:children) components.add(get(id));
    var p=new ServiceComponent.Package(UUID.randomUUID(),name,description,components);
    if (!p.available()) throw new HotelException("Package contains unavailable services");
    if (packages.values().stream().anyMatch(v -> v.name().equalsIgnoreCase(name.trim()))) throw new HotelException("Duplicate package name");
    PackageView result=view(p); // Price validation must succeed before registering the package.
    packages.put(p.id(),p); return result;
  }
  private ServiceComponent.Package get(UUID id) {
    var p=packages.get(id); if(p==null) throw new HotelException("Package not found"); return p;
  }
  public List<AdditionalService> packageServices(UUID id) { return get(id).services(); }
  public List<AdditionalService> services(Benefit b) {
    return switch(b.kind()) {
      case RESERVATION_DISCOUNT -> List.of();
      case SERVICE -> List.of(service.apply(b.targetId()));
      case PACKAGE -> get(b.targetId()).services();
    };
  }
  public void configure(Benefit b) {
    Objects.requireNonNull(b); services(b);
    String key=b.id()+"/"+b.version();
    if (versions.containsKey(key) && !versions.get(key).equals(b)) throw new HotelException("Benefit version is immutable");
    Benefit old=benefits.get(b.id());
    if(old!=null && b.version()<old.version()) throw new HotelException("Cannot downgrade a benefit");
    versions.put(key,b); benefits.put(b.id(),b);
  }
  public Benefit benefit(UUID id) {
    Benefit b=benefits.get(id); if(b==null) throw new HotelException("Benefit equivalence not configured"); return b;
  }
  public List<Benefit> benefits() { return List.copyOf(benefits.values()); }
  public List<PackageView> packages() { return packages.values().stream().map(this::view).toList(); }
  private PackageView view(ServiceComponent.Package p) {
    return new PackageView(p.id(),p.name(),p.description(),p.services().stream().map(s -> new ServiceLine(s.getId(),s.getName(),s.getPriceAmount())).toList(),p.rawPrice(),p.price(),p.available(),"COP");
  }
  public boolean references(UUID serviceId) {
    return packages.values().stream().anyMatch(p -> p.services().stream().anyMatch(s -> s.getId().equals(serviceId)))
        || versions.values().stream().anyMatch(b -> b.kind()==Benefit.Kind.SERVICE && b.targetId().equals(serviceId));
  }
}
