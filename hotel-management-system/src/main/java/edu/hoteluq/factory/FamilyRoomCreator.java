package edu.hoteluq.factory;

import edu.hoteluq.enums.RoomCategory;
import edu.hoteluq.model.FamilyRoom;
import edu.hoteluq.model.Room;
import edu.hoteluq.model.RoomType;

public class FamilyRoomCreator extends RoomCreator {
  @Override
  protected Room createRoom(int number, int floor, RoomType type) {
    return new FamilyRoom(number, floor, type);
  }

  @Override
  protected RoomCategory category() {
    return RoomCategory.FAMILY;
  }
}
