package edu.hoteluq.model;

import java.util.List;
import edu.hoteluq.enums.RoomCategory;
import edu.hoteluq.exceptions.HotelException;

public class SuiteRoom extends Room {
  public SuiteRoom(int number, int floor, RoomType type) {
    super(number, floor, type);
    if (type.getCategory() != RoomCategory.SUITE) {
      throw new HotelException("Room category does not match its type");
    }
  }

  @Override
  public List<String> getFeatures() {
    return List.of("King bed", "Living room", "Jacuzzi", "Wi-Fi", "TV", "Minibar", "Balcony");
  }
}
