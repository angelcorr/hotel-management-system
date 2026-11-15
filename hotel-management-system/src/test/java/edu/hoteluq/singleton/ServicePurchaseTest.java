
package edu.hoteluq.singleton;

import edu.hoteluq.exceptions.*;
import edu.hoteluq.model.VirtualCard;
import edu.hoteluq.service.Views.ServiceView;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServicePurchaseTest extends Phase2Fixture {

    private ServiceView createService() {
        return service.createService(
                admin,
                "Desayuno especial",
                new BigDecimal("25000"),
                "COP"
        );
    }

    @Test
    void clientBuysServiceWithoutReservation() {
        ServiceView selected = createService();

        service.recharge(admin, "client-1", 100000, "Fixture");

        UUID operationId = UUID.randomUUID();

        Hotel.ServicePurchase purchase = service.purchaseService(
                owner,
                operationId,
                selected.id()
        );

        assertEquals(operationId, purchase.id());
        assertEquals("client-1", purchase.clientId());
        assertEquals(selected.id(), purchase.serviceId());
        assertEquals(
                0,
                new BigDecimal("25000").compareTo(purchase.amount())
        );

        assertEquals(
                0,
                new BigDecimal("75000").compareTo(
                        service.card(owner, "client-1").exactBalance()
                )
        );

        assertEquals(1, service.servicePurchases(owner).size());
        assertEquals(0, hotel.withSession(
                owner,
                () -> hotel.getReservations().size()
        ));
    }

    @Test
    void retryDoesNotChargeTwice() {
        ServiceView selected = createService();

        service.recharge(admin, "client-1", 100000, "Fixture");

        UUID key = UUID.randomUUID();

        Hotel.ServicePurchase first = service.purchaseService(
                owner, key, selected.id()
        );

        Hotel.ServicePurchase retry = service.purchaseService(
                owner, key, selected.id()
        );

        assertEquals(first, retry);

        assertEquals(
                0,
                new BigDecimal("75000").compareTo(
                        service.card(owner, "client-1").exactBalance()
                )
        );

        assertEquals(1, service.servicePurchases(owner).size());
        assertEquals(
                2,
                service.card(owner, "client-1").movements().size()
        );
    }

    @Test
    void insufficientBalanceDoesNotRecordPurchase() {
        ServiceView selected = createService();

        assertThrows(
                InsufficientBalanceException.class,
                () -> service.purchaseService(
                        owner,
                        UUID.randomUUID(),
                        selected.id()
                )
        );

        assertTrue(service.servicePurchases(owner).isEmpty());

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        service.card(owner, "client-1").exactBalance()
                )
        );
    }

    @Test
    void unavailableServiceCannotBePurchased() {
        ServiceView selected = createService();

        service.recharge(admin, "client-1", 100000, "Fixture");
        service.setServiceAvailable(admin, selected.id(), false);

        assertThrows(
                HotelException.class,
                () -> service.purchaseService(
                        owner,
                        UUID.randomUUID(),
                        selected.id()
                )
        );

        assertTrue(service.servicePurchases(owner).isEmpty());
    }

    @Test
    void clientCannotSeeAnotherClientsPurchases() {
        ServiceView selected = createService();

        service.recharge(admin, "client-1", 100000, "Fixture");

        UUID key = UUID.randomUUID();

        service.purchaseService(owner, key, selected.id());

        assertEquals(1, service.servicePurchases(owner).size());
        assertTrue(service.servicePurchases(other).isEmpty());
        assertEquals(1, service.servicePurchases(admin).size());

        assertThrows(
                AuthorizationException.class,
                () -> service.purchaseService(
                        admin,
                        UUID.randomUUID(),
                        selected.id()
                )
        );
    }

    @Test
    void directCardPurchaseIsBlocked() {
        VirtualCard card = rawCard("client-1");

        assertThrows(
                AuthorizationException.class,
                () -> hotel.withSession(
                        owner,
                        () -> {
                            card.purchaseService(
                                    new BigDecimal("1000"),
                                    "Unauthorized purchase"
                            );
                            return null;
                        }
                )
        );
    }
}
