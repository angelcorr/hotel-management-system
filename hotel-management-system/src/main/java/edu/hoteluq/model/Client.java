package edu.hoteluq.model;

public class Client extends User {

  private final VirtualCard card;

  public Client(String id, String name, String email, String password) {
    super(id, name, email, password);
    this.card = new VirtualCard("CARD-" + id); // This can be improved by using UUID generator
  }

  public VirtualCard getCard() {
    return card;
  }
}
