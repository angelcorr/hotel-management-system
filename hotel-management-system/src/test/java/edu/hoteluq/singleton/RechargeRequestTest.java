package edu.hoteluq.singleton;

import edu.hoteluq.exceptions.*;
import edu.hoteluq.enums.MovementType;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RechargeRequestTest extends Phase2Fixture {
  @Test void clientRequestsWithoutCreditingCardAndCannotApprove() {
    var request=service.requestRecharge(owner,UUID.randomUUID(),new BigDecimal("15000"),"Necesito saldo");
    assertEquals(Hotel.RechargeStatus.PENDING,request.status());
    assertEquals(0,service.card(owner,"client-1").balance());
    assertEquals(0,service.card(owner,"client-1").movements().size());
    assertThrows(AuthorizationException.class,()->service.resolveRecharge(owner,request.id(),true));
    assertEquals(1,service.rechargeRequests(owner).size());
    assertTrue(service.rechargeRequests(other).isEmpty());
    assertEquals(1,service.rechargeRequests(admin).size());
  }

  @Test void approvingOncePreventsDuplicateCreditAndRejectingNeverCredits() {
    var request=service.requestRecharge(owner,UUID.randomUUID(),new BigDecimal("25000"),"Evento");
    assertEquals(Hotel.RechargeStatus.APPROVED,service.resolveRecharge(admin,request.id(),true).status());
    service.resolveRecharge(admin,request.id(),true);
    assertEquals(new BigDecimal("25000"),service.card(owner,"client-1").exactBalance());
    assertEquals(1,service.card(owner,"client-1").movements().size());
    assertEquals(MovementType.RECHARGE,service.card(owner,"client-1").movements().getFirst().getType());
    assertThrows(HotelException.class,()->service.resolveRecharge(admin,request.id(),false));
    var reject=service.requestRecharge(other,UUID.randomUUID(),new BigDecimal("1000"),"Sin saldo");
    assertEquals(Hotel.RechargeStatus.REJECTED,service.resolveRecharge(admin,reject.id(),false).status());
    assertEquals(0,service.card(other,"client-2").balance());
  }

  @Test void retriesCannotMutateRequestAndInvalidAmountNeverCreatesRequest() {
    UUID id=UUID.randomUUID();
    var first=service.requestRecharge(owner,id,new BigDecimal("500"),"Recargar");
    assertEquals(first,service.requestRecharge(owner,id,new BigDecimal("500"),"Recargar"));
    assertThrows(HotelException.class,()->service.requestRecharge(owner,id,new BigDecimal("600"),"Recargar"));
    assertThrows(HotelException.class,()->service.requestRecharge(owner,UUID.randomUUID(),BigDecimal.ZERO,"Cero"));
    assertThrows(HotelException.class,()->service.requestRecharge(owner,UUID.randomUUID(),new BigDecimal("-2"),"Negativo"));
    assertThrows(HotelException.class,()->service.requestRecharge(owner,UUID.randomUUID(),new BigDecimal("0.4"),"Redondea a cero"));
    assertThrows(AuthorizationException.class,()->service.requestRecharge(admin,UUID.randomUUID(),BigDecimal.ONE,"No cliente"));
    assertEquals(1,service.rechargeRequests(admin).size());
  }

  @Test void failedApprovalRemainsPendingForRetry() {
    var request=service.requestRecharge(owner,UUID.randomUUID(),new BigDecimal("1200"),"Recarga");
    service.setCardActive(admin,"client-1",false);
    assertThrows(InactiveCardException.class,()->service.resolveRecharge(admin,request.id(),true));
    assertEquals(Hotel.RechargeStatus.PENDING,service.rechargeRequests(admin).getFirst().status());
    assertEquals(0,service.card(owner,"client-1").movements().size());
    service.setCardActive(admin,"client-1",true);
    assertEquals(Hotel.RechargeStatus.APPROVED,service.resolveRecharge(admin,request.id(),true).status());
  }
}
