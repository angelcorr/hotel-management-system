package edu.hoteluq.model;

import edu.hoteluq.exceptions.HotelException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {
  private Client client() { return new Client("123", "Ana", "ana@example.com", " secret "); }

  @Test
  void passwordStorageDoesNotRetainPlaintextAndUsesDistinctSaltedHashes() throws Exception {
    String password = "same password for both accounts";
    Client first = new Client("1", "Ana", "ana@example.com", password);
    Client second = new Client("2", "Luis", "luis@example.com", password);
    for (var field : User.class.getDeclaredFields()) {
      if (field.getType() == String.class) {
        field.setAccessible(true);
        assertNotEquals(password, field.get(first), "User must not retain the plaintext password");
      }
    }
    // Storage checks deliberately inspect private credentials without exposing them in production.
    var salt = User.class.getDeclaredField("passwordSalt");
    var hash = User.class.getDeclaredField("passwordHash");
    salt.setAccessible(true);
    hash.setAccessible(true);
    assertFalse(java.util.Arrays.equals((byte[]) salt.get(first), (byte[]) salt.get(second)));
    assertFalse(java.util.Arrays.equals((byte[]) hash.get(first), (byte[]) hash.get(second)));
    assertTrue(first.checkPassword(password));
    assertTrue(second.checkPassword(password));
  }

  @Test
  void passwordsRemainExactAndCanBeChangedWithoutBreakingCard() {
    Client client = client();
    assertTrue(client.checkPassword(" secret "));
    assertFalse(client.checkPassword("secret"));
    assertFalse(client.checkPassword(null));
    assertFalse(client.checkPassword("incorrect"));
    assertFalse(client.checkPassword(""));
    VirtualCard card = client.getCard();
    client.setPassword("new password");
    assertFalse(client.checkPassword(" secret "));
    assertTrue(client.checkPassword("new password"));
    assertSame(card, client.getCard());
    assertEquals(0, card.getBalance());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void missingFieldsAreRejectedAndFailedChangesPreserveUser(String invalid) {
    assertThrows(HotelException.class, () -> new Client(invalid, "Ana", "a@b.co", "secret"));
    assertThrows(HotelException.class, () -> new Client("1", invalid, "a@b.co", "secret"));
    assertThrows(HotelException.class, () -> new Client("1", "Ana", "a@b.co", invalid));
    Client client = client();
    assertThrows(HotelException.class, () -> client.setName(invalid));
    assertThrows(HotelException.class, () -> client.setEmail(invalid));
    assertThrows(HotelException.class, () -> client.setPassword(invalid));
    assertEquals("Ana", client.getName());
    assertEquals("ana@example.com", client.getEmail());
    assertTrue(client.checkPassword(" secret "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ana", "ana@", "@example.com", "ana@example", "a b@c.co", "a@b@c.co"})
  void malformedEmailsAreRejected(String email) {
    assertThrows(HotelException.class, () -> new Administrator("1", "Admin", email, "secret"));
  }

  @Test
  void surroundingWhitespaceIsNormalizedForIdentityButNotPasswords() {
    Client client = new Client(" 1 ", " Ana ", " Ana@example.com ", " secret ");
    assertEquals("1", client.getId());
    assertEquals("Ana", client.getName());
    assertEquals("Ana@example.com", client.getEmail());
    client.setName(" Maria ");
    client.setEmail(" maria@example.com ");
    assertEquals("Maria", client.getName());
    assertEquals("maria@example.com", client.getEmail());
  }
}
