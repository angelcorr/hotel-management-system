package edu.hoteluq.factory;

import edu.hoteluq.enums.RoomCategory;
import edu.hoteluq.model.Room;
import edu.hoteluq.model.RoomType;
import edu.hoteluq.model.SuiteRoom;

public class SuiteRoomCreator extends RoomCreator {
  @Override
  protected Room createRoom(int number, int floor, RoomType type) {
    return new SuiteRoom(number, floor, type);
  }

  @Override
  protected RoomCategory category() {
    return RoomCategory.SUITE;
  }
}
