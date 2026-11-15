package edu.hoteluq.model;

import java.util.List;
import edu.hoteluq.enums.RoomCategory;
import edu.hoteluq.exceptions.HotelException;

public class StandardRoom extends Room {
  public StandardRoom(int number, int floor, RoomType type) {
    super(number, floor, type);
    if (type.getCategory() != RoomCategory.STANDARD) {
      throw new HotelException("Room category does not match its type");
    }
  }

  @Override
  public List<String> getFeatures() {
    return List.of("Double bed", "Private bathroom", "Wi-Fi", "TV");
  }
}
