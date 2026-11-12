package edu.hoteluq.model;

import java.util.List;

public class FamilyRoom extends Room {
  public FamilyRoom(int number, int floor, RoomType type) {
    super(number, floor, type);
  }

  @Override
  public List<String> getFeatures() {
    return List.of("Double bed", "Two single beds", "Private bathroom", "Wi-Fi", "TV", "Mini fridge", "Balcony");
  }
}
