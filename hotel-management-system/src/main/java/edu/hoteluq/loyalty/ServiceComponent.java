package edu.hoteluq.loyalty;

import java.math.BigDecimal;
import java.util.*;
import edu.hoteluq.model.AdditionalService;
import edu.hoteluq.money.CopMoney;
import edu.hoteluq.exceptions.HotelException;

/** Composite: both an individual service and a nested package supply the same operations. */
public sealed interface ServiceComponent permits ServiceComponent.Leaf, ServiceComponent.Package {
  List<AdditionalService> services();
  default BigDecimal rawPrice() { return services().stream().map(AdditionalService::getPriceAmount).reduce(BigDecimal.ZERO, BigDecimal::add); }
  default BigDecimal price() { return CopMoney.rounded(rawPrice()); }
  default boolean available() { return services().stream().allMatch(AdditionalService::isAvailable); }
  record Leaf(AdditionalService service) implements ServiceComponent {
    public Leaf { Objects.requireNonNull(service); }
    public List<AdditionalService> services() { return List.of(service); }
  }
  record Package(UUID id, String name, String description, List<ServiceComponent> children) implements ServiceComponent {
    public Package {
      Objects.requireNonNull(id);
      if (name == null || name.isBlank() || description == null || description.isBlank() || children == null || children.isEmpty()) throw new HotelException("Incomplete service package");
      name = name.trim(); description = description.trim();
      children = List.copyOf(children);
      Set<UUID> seen = new HashSet<>();
      for (ServiceComponent child : children) for (AdditionalService service : child.services()) {
        if (!seen.add(service.getId())) throw new HotelException("Duplicate service in package");
      }
    }
    public List<AdditionalService> services() { return children.stream().flatMap(c -> c.services().stream()).toList(); }
  }
}
