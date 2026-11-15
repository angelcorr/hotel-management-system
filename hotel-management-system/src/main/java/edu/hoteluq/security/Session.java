package edu.hoteluq.security;

import java.time.Instant;

/** Bearer credential: pass token explicitly; never log it. Only the server's registry grants authority. */
public record Session(String token, Instant expiresAt) {
  @Override public String toString() { return "Session[expiresAt=" + expiresAt + ", token=REDACTED]"; }
}
