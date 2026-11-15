package edu.hoteluq.model;

import edu.hoteluq.security.ManagedEntity;
import edu.hoteluq.security.Action;

import edu.hoteluq.validation.DomainValidation;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public abstract class User extends ManagedEntity {
  private final String id;
  private String name;
  private String email;
  private byte[] passwordHash;
  private byte[] passwordSalt;
  private long credentialVersion;

  protected User(String id, String name, String email, String password) {
    this.id = DomainValidation.text(id, "Id");
    this.name = DomainValidation.text(name, "Name");
    this.email = DomainValidation.email(email);
    assignPassword(password);
  }

  public boolean checkPassword(String passwordAttempt) {
    return readProtected(() -> passwordAttempt != null && MessageDigest.isEqual(
        passwordHash, hash(passwordAttempt, passwordSalt)));
  }

  public final edu.hoteluq.enums.Role getRole() {
    return this instanceof Administrator ? edu.hoteluq.enums.Role.ADMINISTRATOR : edu.hoteluq.enums.Role.CLIENT;
  }

  public synchronized long getCredentialVersion() { return credentialVersion; }

  private static byte[] hash(String password, byte[] salt) {
    var spec = new PBEKeySpec(password.toCharArray(), salt, 210_000, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
          .generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Password hashing is unavailable", ex);
    } finally {
      spec.clearPassword();
    }
  }

  public String getId() {
    return readProtected(() -> id);
  }

  public String getName() {
    return readProtected(() -> name);
  }

  public String getEmail() {
    return readProtected(() -> email);
  }

  public void setName(String name) {
    String valid = DomainValidation.text(name, "Name");
    change(Action.UPDATE, valid, () -> this.name = valid);
  }

  public void setEmail(String email) {
    String valid = DomainValidation.email(email);
    change(Action.EMAIL, valid, () -> this.email = valid);
  }

  public void updateProfile(String name, String email) {
    String validName = DomainValidation.text(name, "Name");
    String validEmail = DomainValidation.email(email);
    change(Action.EMAIL, validEmail, () -> { this.name = validName; this.email = validEmail; });
  }

  public void setPassword(String password) {
    DomainValidation.text(password, "Password");
    change(Action.PASSWORD, null, () -> assignPassword(password));
  }

  private void assignPassword(String password) {
    DomainValidation.text(password, "Password");
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    byte[] hash = hash(password, salt);
    this.passwordSalt = salt;
    this.passwordHash = hash;
    credentialVersion++;
  }

  @Override
  public String toString() {
    return readProtected(() -> name);
  }
}
