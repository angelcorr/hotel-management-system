package edu.hoteluq.service;

import edu.hoteluq.model.Reservation;
import edu.hoteluq.model.BookingRecords.BookingView;
import edu.hoteluq.singleton.Hotel;
import edu.hoteluq.exceptions.InvalidReservationException;
import java.time.LocalDateTime;
import java.util.*;

/** Authenticated booking facade; uses the same aggregate as identity/catalog operations. */
public final class ReservationService {
  private final Hotel hotel;
  public ReservationService() { this(Hotel.getInstance()); }
  public ReservationService(Hotel hotel) { this.hotel = Objects.requireNonNull(hotel); }
  public void configure(String token, BookingRules rules) {
    hotel.withSession(token, () -> { hotel.configureBookingRules(rules); return null; });
  }
  public void configureCheckoutWindow(String token, BookingRules.CheckoutWindow window) {
    hotel.withSession(token, () -> { hotel.configureCheckoutWindow(window); return null; });
  }
  public void configureCancellation(String token, edu.hoteluq.strategy.CancellationSchedule policy) {
    hotel.withSession(token, () -> { hotel.configureCancellation(policy); return null; });
  }
  public List<edu.hoteluq.model.BookingRecords.PolicyChange> policyHistory(String token) {
    return hotel.withSession(token, hotel::cancellationPolicyHistory);
  }
  public BookingView checkout(String token, UUID id, String operationKey) {
    return hotel.withSession(token, () -> hotel.checkoutReservation(id, operationKey));
  }
  public edu.hoteluq.model.BookingRecords.ModificationProposal prepareConfirmedModification(
      String token, UUID id, long version, String reason, BookingRequest request) {
    return hotel.withSession(token, () -> hotel.prepareConfirmedModification(id, version, reason, request));
  }
  public void applyConfirmedModification(String token, UUID id, long version, String reason, BookingRequest request) {
    hotel.withSession(token, () -> { hotel.applyConfirmedModification(id, version, reason, request); return null; });
  }
  public BookingView create(String token, String clientId, String idempotencyKey, BookingRequest request) {
    return hotel.withSession(token, () -> hotel.createReservation(clientId, idempotencyKey, request));
  }
  public BookingView get(String token, UUID id) {
    return hotel.withSession(token, () -> hotel.findReservation(id).view());
  }
  public List<BookingView> list(String token) {
    return hotel.withSession(token, () -> hotel.getReservations().stream().map(Reservation::view).toList());
  }
  public BookingView modify(String token, UUID id, long expectedVersion, BookingRequest request) {
    return hotel.withSession(token, () -> hotel.modifyReservation(id, expectedVersion, request));
  }
  public edu.hoteluq.model.BookingRecords.Quote previewModification(String token,UUID id,long version,BookingRequest request) { return hotel.withSession(token,()->hotel.previewPendingModification(id,version,request)); }
  public BookingRules configuration(String token) { return hotel.withSession(token,hotel::bookingConfiguration); }
  public edu.hoteluq.model.BookingRecords.Quote preview(String token,String clientId,BookingRequest request) { return hotel.withSession(token,()->hotel.previewBooking(clientId,request)); }
  public List<Hotel.PromotionOffer> offersModification(String token,String clientId,UUID id,long version,BookingRequest request) { return hotel.withSession(token,()->hotel.promotionOffers(clientId,request,id,version)); }
  public List<Hotel.PromotionOffer> offers(String token,String clientId,BookingRequest request) { return hotel.withSession(token,()->hotel.promotionOffers(clientId,request)); }
  public BookingView confirm(String token,UUID id,long version) { return hotel.withSession(token,()->hotel.confirmReservation(id,version)); }
  public Hotel.CancellationQuote cancellationQuote(String token,UUID id,boolean byHotel) { return hotel.withSession(token,()->hotel.previewCancellation(id,byHotel)); }
  public BookingView cancel(String token,Hotel.CancellationQuote accepted) { return hotel.withSession(token,()->hotel.cancelQuoted(accepted)); }
  public BookingView confirm(String token, UUID id) {
    return hotel.withSession(token, () -> hotel.confirmReservation(hotel.findReservation(id)));
  }
  public BookingView cancelByClient(String token, UUID id) {
    return hotel.withSession(token, () -> hotel.cancelReservation(hotel.findReservation(id), false));
  }
  public BookingView cancelByHotel(String token, UUID id) {
    return hotel.withSession(token, () -> hotel.cancelReservation(hotel.findReservation(id), true));
  }
  public BookingView complete(String token, UUID id) {
    return hotel.withSession(token, () -> hotel.completeReservation(id));
  }
  /** Legacy callers must open an authenticated scope; detached/foreign objects are rejected before debit. */
  public void confirm(Reservation reservation) { hotel.confirmReservation(reservation); }
  public void cancelByClient(Reservation reservation) { hotel.cancelReservation(reservation, false); }
  public void cancelByHotel(Reservation reservation) { hotel.cancelReservation(reservation, true); }
  /** Never trust a requester-supplied cancellation timestamp. Use an injected Clock on the aggregate in tests. */
  @Deprecated public void cancelByClient(Reservation reservation, LocalDateTime when) {
    throw new InvalidReservationException("Caller-supplied cancellation time is not accepted");
  }
}
