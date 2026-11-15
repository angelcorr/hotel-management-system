package edu.hoteluq.security;

/** Mutation categories used by the aggregate's access policy. */
public enum Action {
  UPDATE, NAME, EMAIL, PASSWORD, AVAILABILITY, FLOOR,
  RECHARGE, CHARGE, REFUND, CARD_STATE, RESERVATION_STATE, BOOKING_TRANSACTION
}
