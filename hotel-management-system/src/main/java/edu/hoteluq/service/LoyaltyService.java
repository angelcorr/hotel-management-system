package edu.hoteluq.service;

import edu.hoteluq.singleton.Hotel;
import edu.hoteluq.loyalty.*;
import edu.hoteluq.model.BookingRecords.BookingView;
import java.util.*;

/** Authenticated loyalty facade. Redemption grants its benefit and confirms payment atomically. */
public final class LoyaltyService {
  private final Hotel hotel;
  public LoyaltyService() { this(Hotel.getInstance()); }
  public LoyaltyService(Hotel hotel) { this.hotel=Objects.requireNonNull(hotel); }
  public void configure(String token,LoyaltyPolicy policy) { hotel.withSession(token,()->{hotel.configureLoyalty(policy);return null;}); }
  public LoyaltyPolicy policy(String token) { return hotel.withSession(token,hotel::loyaltyPolicy); }
  public void benefit(String token,Benefit benefit) { hotel.withSession(token,()->{hotel.configureBenefit(benefit);return null;}); }
  public List<Benefit> benefits(String token) { return hotel.withSession(token,hotel::loyaltyBenefits); }
  public PointsLedger.Account account(String token,String clientId) { return hotel.withSession(token,()->hotel.pointsAccount(clientId)); }
  public List<PointsLedger.Deferred> deferred(String token,String clientId) { return hotel.withSession(token,()->hotel.deferredPoints(clientId)); }
  public void settle(String token,String clientId,UUID paymentId) { hotel.withSession(token,()->{hotel.settleDeferredPoints(clientId,paymentId);return null;}); }
  public LoyaltyCatalog.PackageView createPackage(String token,String name,String description,List<UUID> serviceIds,List<UUID> childIds) {
    return hotel.withSession(token,()->hotel.createServicePackage(name,description,serviceIds,childIds));
  }
  public List<LoyaltyCatalog.PackageView> packages(String token) { return hotel.withSession(token,hotel::servicePackages); }
  public edu.hoteluq.model.BookingRecords.Quote preview(String token,UUID reservation,long version,UUID benefit) {
    return hotel.withSession(token,()->hotel.previewPointRedemption(reservation,version,benefit));
  }
  public BookingView redeem(String token,UUID reservation,long version,String key,UUID benefit) {
    return hotel.withSession(token,()->hotel.redeemPoints(reservation,version,key,benefit));
  }
}
