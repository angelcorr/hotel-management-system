package edu.hoteluq.service;

import java.time.LocalDate;
import java.util.*;
import edu.hoteluq.validation.DomainValidation;
import edu.hoteluq.exceptions.InvalidReservationException;

/** Value request: no caller-supplied prices, card, role or status. */
public record BookingRequest(int roomNumber, LocalDate checkIn, LocalDate checkOut,
    int guests, List<UUID> serviceIds, UUID promotionId, List<UUID> packageIds) {
  public BookingRequest(int roomNumber, LocalDate checkIn, LocalDate checkOut, int guests, List<UUID> serviceIds, UUID promotionId) {
    this(roomNumber,checkIn,checkOut,guests,serviceIds,promotionId,List.of());
  }
  public BookingRequest {
    DomainValidation.positive(roomNumber, "Room number");
    DomainValidation.period(checkIn, checkOut);
    DomainValidation.positive(guests, "Guests");
    serviceIds = List.copyOf(DomainValidation.required(serviceIds, "Services"));
    packageIds=List.copyOf(DomainValidation.required(packageIds,"Packages"));
    if(new HashSet<>(packageIds).size()!=packageIds.size()) throw new InvalidReservationException("Select each package once");
    if (new HashSet<>(serviceIds).size() != serviceIds.size()) {
      throw new InvalidReservationException("Select each service only once");
    }
  }
}
