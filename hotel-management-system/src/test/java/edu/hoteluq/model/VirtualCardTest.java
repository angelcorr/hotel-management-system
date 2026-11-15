package edu.hoteluq.model;

import edu.hoteluq.enums.MovementType;
import edu.hoteluq.exceptions.*;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class VirtualCardTest {
  @ParameterizedTest
  @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void invalidOperationsDoNotChangeBalanceOrMovements(double amount) {
    VirtualCard card = new VirtualCard("CARD-1");
    card.recharge(100, "Initial");
    assertThrows(HotelException.class, () -> card.charge(amount, "Pay"));
    assertThrows(HotelException.class, () -> card.recharge(amount, "Reload"));
    assertThrows(HotelException.class, () -> card.refund(amount, "Refund"));
    assertEquals(100, card.getBalance());
    assertEquals(1, card.getMovements().size());
  }

  @Test
  void decimalOperationsDoNotLeaveResidualBalance() {
    VirtualCard card = new VirtualCard("CARD-1");
    card.recharge(0.1, "First");
    card.recharge(0.2, "Second");
    card.charge(0.3, "Payment");
    assertEquals(0, card.getBalance());
    assertEquals(3, card.getMovements().size());
    assertEquals(MovementType.PAYMENT, card.getMovements().get(2).getType());
    assertEquals(0.3, card.getMovements().get(2).getAmount());
    card.refund(0.3, "Refund");
    assertEquals(0.3, card.getBalance());
    assertEquals(MovementType.REFUND, card.getMovements().get(3).getType());
  }

  @Test
  void insufficientBalanceIsCompatibleWithLegacyExceptionAndDoesNotMutateCard() {
    VirtualCard card = new VirtualCard("CARD-1");
    assertThrows(InsufficientBalanceException.class, () -> card.charge(1, "Payment"));
    assertThrows(InsuficientBalanceException.class, () -> card.charge(1, "Payment"));
    assertEquals(0, card.getBalance());
    assertTrue(card.getMovements().isEmpty());
  }

  @Test
  void inactiveCardRejectsPaymentAndRechargeButStillReceivesRefund() {
    VirtualCard card = new VirtualCard("CARD-1");
    card.recharge(10, "Initial");
    card.setActive(false);
    assertThrows(InactiveCardException.class, () -> card.charge(1, "Pay"));
    assertThrows(InactiveCardException.class, () -> card.recharge(1, "Reload"));
    assertEquals(10, card.getBalance());
    assertEquals(1, card.getMovements().size());
    card.refund(1, "Refund");
    assertEquals(11, card.getBalance());
    assertFalse(card.isActive());
  }

  @Test
  void invalidDescriptionsAndOverflowAreRejectedBeforeMutation() {
    assertThrows(HotelException.class, () -> new VirtualCard(" "));
    VirtualCard card = new VirtualCard("CARD-1");
    card.recharge(10, "Initial");
    assertThrows(HotelException.class, () -> card.charge(1, null));
    assertThrows(HotelException.class, () -> card.recharge(1, " "));
    assertThrows(HotelException.class, () -> card.refund(1, ""));
    assertEquals(10, card.getBalance());
    assertEquals(1, card.getMovements().size());
    VirtualCard large = new VirtualCard("CARD-2");
    large.recharge(Double.MAX_VALUE, "Initial");
    assertThrows(HotelException.class, () -> large.recharge(Double.MAX_VALUE, "Overflow"));
    assertThrows(HotelException.class, () -> large.refund(Double.MAX_VALUE, "Overflow"));
    assertEquals(Double.MAX_VALUE, large.getBalance());
    assertEquals(1, large.getMovements().size());
  }

  @Test
  void movementsCannotBeInjectedAndSnapshotsRemainStable() {
    VirtualCard card = new VirtualCard("CARD-1");
    var movements = card.getMovements();
    assertThrows(UnsupportedOperationException.class, () -> movements.add(
        new CardMovement(MovementType.PAYMENT, 1, "Fake", java.time.LocalDateTime.now())));
    card.recharge(1, "Real");
    assertTrue(movements.isEmpty());
    assertEquals(1, card.getMovements().size());
  }
  @Test
  void concurrentPaymentsCannotOverdrawCard() throws Exception {
    VirtualCard card = new VirtualCard("CARD-1");
    card.recharge(1, "Initial");
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    Callable<Boolean> payment = () -> {
      ready.countDown();
      if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Payment start timed out");
      try {
        card.charge(1, "Payment");
        return true;
      } catch (InsufficientBalanceException expected) {
        return false;
      }
    };
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<Boolean> a = executor.submit(payment);
      Future<Boolean> b = executor.submit(payment);
      try {
        assertTrue(ready.await(5, TimeUnit.SECONDS));
      } finally {
        start.countDown();
      }
      assertNotEquals(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS));
    }
    assertEquals(0, card.getBalance());
    assertEquals(2, card.getMovements().size());
  }

}
