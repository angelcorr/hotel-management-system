package edu.hoteluq.factory;

import edu.hoteluq.enums.RoomCategory;
import edu.hoteluq.exceptions.HotelException;

import java.util.EnumMap;
import java.util.Map;

public class RoomCreatorRegistry {
  private final Map<RoomCategory, RoomCreator> creators = new EnumMap<>(RoomCategory.class);

  public RoomCreatorRegistry() {
    creators.put(RoomCategory.STANDARD, new StandardRoomCreator());
    creators.put(RoomCategory.FAMILY, new FamilyRoomCreator());
    creators.put(RoomCategory.SUITE, new SuiteRoomCreator());
  }

  public RoomCreator get(RoomCategory category) {
    RoomCreator creator = creators.get(category);
    if (creator == null) throw new HotelException("There is not creator for " + category);
    return creator;
  }
}
