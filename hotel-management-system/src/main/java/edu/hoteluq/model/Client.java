package edu.hoteluq.model;

public final class Client extends User {

  private final VirtualCard card;

  public Client(String id, String name, String email, String password) {
    super(id, name, email, password);
    this.card = new VirtualCard("CARD-" + getId()); // This can be improved by using UUID generator
  }

  public VirtualCard getCard() {
    return readProtected(() -> card);
  }
}
