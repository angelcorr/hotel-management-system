package edu.hoteluq.model;

import java.util.List;

public class SuiteRoom extends Room {
  public SuiteRoom(int number, int floor, RoomType type) {
    super(number, floor, type);
  }

  @Override
  public List<String> getFeatures() {
    return List.of("King bed", "Living room", "Jacuzzi", "Wi-Fi", "TV", "Minibar", "Balcony");
  }
}
