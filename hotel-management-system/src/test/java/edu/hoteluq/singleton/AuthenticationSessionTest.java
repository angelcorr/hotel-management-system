package edu.hoteluq.singleton;

import edu.hoteluq.enums.Role;
import edu.hoteluq.exceptions.*;
import edu.hoteluq.security.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class AuthenticationSessionTest extends Phase2Fixture {
  @Test void validLoginNormalizesEmailAndUsesStoredRole() {
    var session = service.login(" ANA@EXAMPLE.COM ", ownerPassword);
    assertEquals("client-1", service.me(session.token()).id());
    assertEquals(Role.CLIENT, service.me(session.token()).role());
    assertEquals(Role.ADMINISTRATOR, service.me(admin).role());
    assertEquals(clock.instant().plus(Duration.ofMinutes(5)), session.expiresAt());
  }

  @ParameterizedTest
  @CsvSource({"ana@example.com,incorrect", "missing@example.com,incorrect", "bad-email,incorrect", "ana@example.com,''"})
  void invalidCredentialsProduceGenericErrorsWithoutPassword(String email, String password) {
    var error = assertThrows(AuthenticationException.class, () -> service.login(email, password));
    assertEquals("Invalid credentials", error.getMessage());
    assertEquals("Ana", service.me(owner).name());
  }

  @Test void nullCredentialsAreRejected() {
    assertThrows(AuthenticationException.class, () -> service.login(null, ownerPassword));
    assertThrows(AuthenticationException.class, () -> service.login("ana@example.com", null));
  }

  @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "invented-token"})
  void invalidSessionsCannotReadOrWrite(String token) {
    assertThrows(AuthenticationException.class, () -> service.me(token));
    assertThrows(AuthenticationException.class, () -> service.updateUser(token, "client-1", "Changed", "changed@example.com"));
    assertThrows(AuthenticationException.class, () -> service.recharge(token, "client-1", 1, "Reload"));
    assertThrows(AuthenticationException.class, () -> service.logout(token));
    assertEquals("Ana", service.me(owner).name());
  }

  @Test void tokensAreDistinctOpaqueAndRedacted() {
    Session first = service.login("ana@example.com", ownerPassword);
    Session second = service.login("ana@example.com", ownerPassword);
    assertNotEquals(first.token(), second.token());
    assertTrue(first.token().length() >= 43);
    assertFalse(first.toString().contains(first.token()));
    assertFalse(first.toString().contains(ownerPassword));
    assertThrows(AuthenticationException.class, () -> service.me(new Session("forged", Instant.MAX).token()));
  }

  @Test void logoutRevokesOnlyTheSelectedSession() {
    String second = service.login("ana@example.com", ownerPassword).token();
    service.logout(owner);
    assertThrows(AuthenticationException.class, () -> service.me(owner));
    assertThrows(AuthenticationException.class, () -> service.logout(owner));
    assertEquals("client-1", service.me(second).id());
    assertEquals(Role.ADMINISTRATOR, service.me(admin).role());
  }

  @Test void sessionExpiresAtTheExactConfiguredInstant() {
    clock.advance(Duration.ofMinutes(5).minusMillis(1));
    assertEquals("client-1", service.me(owner).id());
    clock.advance(Duration.ofMillis(1));
    assertThrows(AuthenticationException.class, () -> service.me(owner));
    assertThrows(AuthenticationException.class, () -> service.createService(admin, "Late", 10));
    String fresh = service.login("ana@example.com", ownerPassword).token();
    assertEquals("client-1", service.me(fresh).id());
  }

  @Test void existingScopeDoesNotBypassExpiryOrLogout() {
    var user = rawClient("client-1");
    try (var scope = hotel.openSession(owner)) {
      clock.advance(Duration.ofMinutes(5));
      assertThrows(AuthenticationException.class, () -> user.setName("Expired"));
      assertThrows(AuthenticationException.class, user::getEmail);
    }
    String fresh = service.login("ana@example.com", ownerPassword).token();
    assertEquals("Ana", service.me(fresh).name());
    try (var scope = hotel.openSession(fresh)) {
      service.logout(fresh);
      assertThrows(AuthenticationException.class, () -> user.setName("Logged out"));
    }
  }

  @Test void passwordChangeRevokesEveryOldSessionAndPreservesOtherUsers() {
    String second = service.login("ana@example.com", ownerPassword).token();
    service.changePassword(owner, "new password");
    assertThrows(AuthenticationException.class, () -> service.me(owner));
    assertThrows(AuthenticationException.class, () -> service.me(second));
    assertThrows(AuthenticationException.class, () -> service.login("ana@example.com", ownerPassword));
    assertEquals("client-1", service.me(service.login("ana@example.com", "new password").token()).id());
    assertEquals("client-2", service.me(other).id());
    assertEquals(Role.ADMINISTRATOR, service.me(admin).role());
  }

  @Test void rejectedPasswordChangePreservesSessionsAndCredentials() {
    assertThrows(HotelException.class, () -> service.changePassword(owner, " "));
    assertEquals("client-1", service.me(owner).id());
    assertNotNull(service.login("ana@example.com", ownerPassword));
  }

  @Test void administrativeResetRevokesClientSessionsWithoutRevokingAdministrator() {
    service.resetClientPassword(admin, "client-1", "reset password");
    assertThrows(AuthenticationException.class, () -> service.me(owner));
    assertThrows(AuthenticationException.class, () -> service.login("ana@example.com", ownerPassword));
    assertEquals("client-1", service.me(service.login("ana@example.com", "reset password").token()).id());
    assertEquals(Role.ADMINISTRATOR, service.me(admin).role());
  }

  @Test void deletionRevokesSessionsAndPreventsLogin() {
    service.deleteClient(admin, "client-1");
    assertThrows(AuthenticationException.class, () -> service.me(owner));
    assertThrows(AuthenticationException.class, () -> service.login("ana@example.com", ownerPassword));
    assertEquals(1, service.clients(admin).size());
  }

  @Test void nestedScopesRestoreIdentityAfterSuccessAndFailure() {
    try (var scope = hotel.openSession(admin)) {
      assertEquals(Role.ADMINISTRATOR, hotel.currentUser().getRole());
      assertEquals(Role.CLIENT, service.me(owner).role());
      assertThrows(AuthorizationException.class, () -> service.clients(owner));
      assertThrows(AuthenticationException.class, () -> service.me("invalid"));
      assertEquals(Role.ADMINISTRATOR, hotel.currentUser().getRole());
    }
    assertThrows(AuthenticationException.class, hotel::currentUser);
  }

  @Test void scopesAreThreadBoundAndDoNotGrantWorkerThreadsPermissions() throws Exception {
    try (var scope = hotel.openSession(admin); ExecutorService executor = Executors.newSingleThreadExecutor()) {
      executor.submit(() -> {
        assertThrows(AuthenticationException.class, hotel::getClients);
        assertThrows(IllegalStateException.class, scope::close);
      }).get(5, TimeUnit.SECONDS);
      assertEquals(Role.ADMINISTRATOR, hotel.currentUser().getRole());
    }
  }

  @Test void startupIsExplicitOneTimeAndHasNoFixedCredentials() {
    Hotel fresh = new Hotel(clock, Duration.ofMinutes(5));
    assertThrows(HotelException.class, () -> fresh.registerClient("a", "A", "a@example.com", "secret"));
    assertThrows(AuthenticationException.class, () -> fresh.addUser(new edu.hoteluq.model.Administrator("a", "A", "a@example.com", "secret")));
    assertThrows(AuthorizationException.class, () -> hotel.initializeAdministrator("evil", "Evil", "evil@example.com", "secret"));
    assertThrows(HotelException.class, () -> new Hotel(clock, Duration.ZERO));
    assertThrows(HotelException.class, () -> new Hotel(clock, Duration.ofSeconds(-1)));
  }
}
