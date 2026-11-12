package edu.hoteluq.model;

import java.util.List;

public class StandardRoom extends Room {
  public StandardRoom(int number, int floor, RoomType type) {
    super(number, floor, type);
  }

  @Override
  public List<String> getFeatures() {
    return List.of("Double bed", "Private bathroom", "Wi-Fi", "TV");
  }
}
