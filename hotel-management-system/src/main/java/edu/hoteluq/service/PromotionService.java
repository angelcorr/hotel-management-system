package edu.hoteluq.service;

import edu.hoteluq.promotion.*;
import edu.hoteluq.singleton.Hotel;
import java.util.*;

/** Authenticated promotion administration. Version checks prevent lost updates. */
public final class PromotionService {
  private final Hotel hotel;
  public PromotionService() { this(Hotel.getInstance()); }
  public PromotionService(Hotel hotel) { this.hotel=Objects.requireNonNull(hotel); }
  public PromotionDefinition create(String token,PromotionDefinition.Settings settings) {
    return hotel.withSession(token,()->hotel.createConditionalPromotion(settings));
  }
  /** expectedVersion=0 explicitly configures an existing legacy percentage for the first time. */
  public PromotionDefinition update(String token,UUID id,long expectedVersion,PromotionDefinition.Settings settings) {
    return hotel.withSession(token,()->hotel.configurePromotion(id,expectedVersion,settings));
  }
  public PromotionDefinition setActive(String token,UUID id,long expectedVersion,boolean active) {
    return hotel.withSession(token,()->hotel.setPromotionActive(id,expectedVersion,active));
  }
  public List<PromotionDefinition> list(String token) { return hotel.withSession(token,hotel::conditionalPromotions); }
  public List<PromotionProgram.Change> history(String token) { return hotel.withSession(token,hotel::promotionHistory); }
}
