package edu.hoteluq.singleton;

import edu.hoteluq.enums.RoomCategory;
import edu.hoteluq.exceptions.DuplicateRoomException;
import edu.hoteluq.exceptions.HotelException;
import edu.hoteluq.model.Room;
import edu.hoteluq.model.RoomType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hotel {
  private static Hotel instance;

  private final List<RoomType> roomTypes = new ArrayList<>();
  private final List<Room> rooms = new ArrayList<>();

  private Hotel() {}

  public static Hotel getInstance() {
    if (instance == null) {
      instance = new Hotel();
    }

    return instance;
  }

  public void addRoomType(RoomType type) {
    roomTypes.add(type);
  }

  public RoomType getRoomType(RoomCategory category) {
    for (RoomType type : roomTypes) {
      if (type.getCategory() == category) return type;
    }

    throw new HotelException("There is no room type for " + category);
  }

  public List<RoomType> getRoomTypes() {
    return Collections.unmodifiableList(roomTypes);
  }

  public void addRoom(Room room) {
    // Early return when room already exists
    if (findRoom(room.getNumber()) != null) {
      throw new DuplicateRoomException("Room " + room.getNumber() + " already exists");
    }

    rooms.add(room);
  }

  public Room findRoom(int number) {
    for (Room room : rooms) {
      if (room.getNumber() == number) return room;
    }

    return null;
  }

  public List<Room> getRooms() {
    return Collections.unmodifiableList(rooms);
  }
}
