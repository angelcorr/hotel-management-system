package edu.hoteluq.factory;

import edu.hoteluq.enums.RoomCategory;
import edu.hoteluq.model.Room;
import edu.hoteluq.model.RoomType;
import edu.hoteluq.singleton.Hotel;

public abstract class RoomCreator {
  // Factory Method
  protected abstract Room createRoom(int number, int floor, RoomType type);

  protected abstract RoomCategory category();

  public Room create(int number, int floor) {
    Hotel hotel = Hotel.getInstance();
    RoomType type = hotel.getRoomType(category());
    Room room = createRoom(number, floor, type);
    hotel.addRoom(room);
    return room;
  }
}
