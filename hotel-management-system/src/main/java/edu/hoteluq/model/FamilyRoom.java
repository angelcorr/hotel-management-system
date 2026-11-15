package edu.hoteluq.model;

import java.util.List;
import edu.hoteluq.enums.RoomCategory;
import edu.hoteluq.exceptions.HotelException;

public class FamilyRoom extends Room {
  public FamilyRoom(int number, int floor, RoomType type) {
    super(number, floor, type);
    if (type.getCategory() != RoomCategory.FAMILY) {
      throw new HotelException("Room category does not match its type");
    }
  }

  @Override
  public List<String> getFeatures() {
    return List.of("Double bed", "Two single beds", "Private bathroom", "Wi-Fi", "TV", "Mini fridge", "Balcony");
  }
}
