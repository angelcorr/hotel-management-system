package edu.hoteluq.model;

public abstract class User {
  private final String id;
  private String name;
  private String email;
  private String password;

  protected User(String id, String name, String email, String password) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.password = password;
  }

  // TODO: consider implementing hashing
  public boolean checkPassword(String passwordAttempt) {
    return password.equals(passwordAttempt);
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String toString() {
    return name;
  }
}
