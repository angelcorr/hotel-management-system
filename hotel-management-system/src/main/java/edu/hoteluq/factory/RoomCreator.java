package edu.hoteluq.factory;

import edu.hoteluq.enums.RoomCategory;
import edu.hoteluq.model.Room;
import edu.hoteluq.model.RoomType;
import edu.hoteluq.singleton.Hotel;

public abstract class RoomCreator {
  /** Factory Method: subclasses choose the concrete Room product.
   * Template Method: create(Hotel,...) fixes resolution -> construction -> registration.
   * These are distinct design responsibilities sharing the same participants.
   */
  protected abstract Room createRoom(int number, int floor, RoomType type);

  protected abstract RoomCategory category();

  public Room create(int number, int floor) {
    return create(Hotel.getInstance(), number, floor);
  }

  public final Room create(Hotel hotel, int number, int floor) {
    RoomType type = hotel.getRoomType(category());
    Room room = createRoom(number, floor, type);
    hotel.addRoom(room);
    return room;
  }
}
