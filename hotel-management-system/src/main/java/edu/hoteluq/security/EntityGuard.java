package edu.hoteluq.security;

import java.util.function.Supplier;

/** Installed once by the owning aggregate; runs checks and mutations under its lock. */
public interface EntityGuard {
  void mutate(Action action, Object candidate, Runnable mutation);
  <T> T read(Supplier<T> query);
}
