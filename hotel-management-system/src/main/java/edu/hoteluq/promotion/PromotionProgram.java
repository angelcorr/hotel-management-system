package edu.hoteluq.promotion;

import edu.hoteluq.exceptions.HotelException;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/** Version registry and audit, owned by Hotel's synchronized aggregate; never exposes mutable entities. */
public final class PromotionProgram {
  public record Change(UUID operationId,String actorId,Instant at,PromotionDefinition before,PromotionDefinition after) {}
  private final Map<UUID,PromotionDefinition> current=new LinkedHashMap<>();
  private final List<Change> history=new ArrayList<>();
  public boolean configured(UUID id) { return current.containsKey(id); }
  public PromotionDefinition get(UUID id) {
    var p=current.get(id);if(p==null) throw new edu.hoteluq.exceptions.InvalidReservationException("Promotion eligibility not configured");return p;
  }
  public List<PromotionDefinition> list() { return List.copyOf(current.values()); }
  public List<Change> history() { return List.copyOf(history); }
  public PromotionDefinition prepare(UUID id,long expected,PromotionDefinition.Settings settings,Consumer<PromotionCondition> referenceValidator) {
    var previous=current.get(id); long version=previous==null?0:previous.version();
    if(version!=expected) throw new HotelException("Stale promotion version");
    if(settings==null) throw new HotelException("Promotion settings required");
    settings.conditions().forEach(referenceValidator);
    return new PromotionDefinition(id,Math.addExact(version,1),settings);
  }
  public void publish(PromotionDefinition definition,String actor,Instant at) {
    var old=current.put(definition.id(),definition);history.add(new Change(UUID.randomUUID(),actor,at,old,definition));
  }
  public void validate(PromotionDefinition accepted,PromotionCondition.Context context,Instant now) {
    if(accepted==null || !get(accepted.id()).equals(accepted)) throw new HotelException("Promotion changed: accept a new pending quote");
    accepted.requireEligible(context,now);
  }
  public boolean referencesService(UUID id) {
    return history.stream().map(Change::after).flatMap(d->d.settings().conditions().stream())
        .anyMatch(c->c instanceof PromotionCondition.Services s && s.ids().contains(id));
  }
}
