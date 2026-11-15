package edu.hoteluq.security;

import edu.hoteluq.exceptions.HotelException;
import java.util.Objects;
import java.util.function.Supplier;

/** Detached objects can be assembled freely; registered objects cannot bypass their aggregate. */
public abstract class ManagedEntity {
  private volatile EntityGuard guard;

  public final synchronized boolean isManaged() { return guard != null; }

  public final synchronized void attachGuard(EntityGuard guard) {
    if (this.guard != null) throw new HotelException("Entity already belongs to an aggregate");
    this.guard = Objects.requireNonNull(guard);
  }

  protected final void change(Action action, Object candidate, Runnable mutation) {
    EntityGuard policy = guard;
    if (policy == null) {
      synchronized (this) {
        // Registration can win the race while this thread waits for the monitor.
        policy = guard;
        if (policy == null) { mutation.run(); return; }
      }
    }
    EntityGuard checked = policy;
    checked.mutate(action, candidate, () -> { synchronized (this) { mutation.run(); } });
  }

  protected final <T> T readProtected(Supplier<T> query) {
    EntityGuard policy = guard;
    if (policy == null) {
      synchronized (this) {
        policy = guard;
        if (policy == null) return query.get();
      }
    }
    return policy.read(() -> { synchronized (this) { return query.get(); } });
  }
}
