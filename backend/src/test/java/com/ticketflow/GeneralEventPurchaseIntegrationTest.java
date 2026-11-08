package com.ticketflow;

import com.ticketflow.common.UserContext;
import com.ticketflow.common.Ids;
import com.ticketflow.dto.event.EventRequest;
import com.ticketflow.dto.order.CreateGeneralOrderRequest;
import com.ticketflow.dto.session.SessionRequest;
import com.ticketflow.dto.tier.TierRequest;
import com.ticketflow.dto.venue.VenueRequest;
import com.ticketflow.entity.auth.UserEntity;
import com.ticketflow.service.IAuthService;
import com.ticketflow.service.IEventService;
import com.ticketflow.service.IOrderService;
import com.ticketflow.service.IOrganizerService;
import com.ticketflow.service.ISessionService;
import com.ticketflow.service.ITierService;
import com.ticketflow.service.IVenueService;
import com.ticketflow.vo.event.EventView;
import com.ticketflow.vo.order.OrderView;
import com.ticketflow.vo.session.SessionView;
import com.ticketflow.vo.tier.TierView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/** 使用本地测试库验证多场次发布、公开展示和无座票下单；事务结束后回滚测试数据。 */
@SpringBootTest
@Transactional
class GeneralEventPurchaseIntegrationTest {
    @Autowired private IAuthService users;
    @Autowired private IOrganizerService organizers;
    @Autowired private IVenueService venues;
    @Autowired private IEventService events;
    @Autowired private ISessionService sessions;
    @Autowired private ITierService tiers;
    @Autowired private IOrderService orders;
    @Autowired private Ids ids;

    @AfterEach
    void clearUser() { UserContext.clear(); }

    @Test
    void publishesThreeGeneralSessionsWithThreePricesAndAllowsPurchase() {
        // 独立的主办方和购票账号验证对象归属；两者均随测试事务回滚。
        long ownerId = saveUser();
        long buyerId = saveUser();
        organizers.create(ownerId, "测试主办方");
        UserContext.setUserId(ownerId);

        VenueRequest venueRequest = new VenueRequest();
        venueRequest.setName("测试场馆");
        long venueId = venues.create(venueRequest).getId();
        EventRequest eventRequest = new EventRequest();
        eventRequest.setTitle("多场次无座票测试演出");
        EventView event = events.create(eventRequest);

        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        LocalDateTime saleStart = now.minusMinutes(5);
        List<Long> sessionIds = new ArrayList<>();
        List<Long> tierIds = new ArrayList<>();
        BigDecimal[] prices = {new BigDecimal("199.00"), new BigDecimal("399.00"), new BigDecimal("699.00")};
        String[] names = {"普通票", "优享票", "VIP票"};

        for (int i = 0; i < 3; i++) {
            LocalDateTime start = now.plusDays(8 + i);
            LocalDateTime saleEnd = start.minusDays(1);
            SessionRequest sessionRequest = new SessionRequest();
            sessionRequest.setVenueId(venueId);
            sessionRequest.setSaleMode("GENERAL");
            sessionRequest.setSaleStart(saleStart);
            sessionRequest.setSaleEnd(saleEnd);
            sessionRequest.setStartTime(start);
            sessionRequest.setEndTime(start.plusHours(3));
            sessionRequest.setCheckinStart(start.minusHours(2));
            sessionRequest.setCheckinEnd(start.plusHours(1));
            SessionView session = sessions.create(event.getId(), sessionRequest);
            sessionIds.add(session.getId());

            for (int j = 0; j < 3; j++) {
                TierRequest tierRequest = new TierRequest();
                tierRequest.setName(names[j]);
                tierRequest.setPrice(prices[j]);
                tierRequest.setTotalStock(20 + j * 10);
                tierRequest.setPerUserLimit(4);
                tierRequest.setSaleStart(saleStart);
                tierRequest.setSaleEnd(saleEnd);
                TierView tier = tiers.create(session.getId(), tierRequest);
                tierIds.add(tier.getId());
                assertEquals("ON_SALE", tiers.activate(tier.getId()).getStatus());
            }
            assertEquals("ON_SALE", sessions.activate(session.getId()).getStatus());
        }

        assertEquals("PUBLISHED", events.publish(event.getId()).getStatus());
        assertEquals(event.getId(), events.publicDetail(event.getId()).getId());
        assertEquals(3, sessions.publicList(event.getId()).size());
        for (long sessionId : sessionIds) {
            List<TierView> publicTiers = tiers.publicList(sessionId);
            assertEquals(3, publicTiers.size());
            assertEquals(List.of(prices), publicTiers.stream().map(TierView::getPrice).toList());
        }

        UserContext.setUserId(buyerId);
        CreateGeneralOrderRequest purchase = new CreateGeneralOrderRequest();
        purchase.setRequestId("test-" + UUID.randomUUID());
        purchase.setTierId(tierIds.get(0));
        purchase.setQuantity(1);
        OrderView order = orders.createGeneral(purchase);
        assertEquals("PENDING_PAYMENT", order.getStatus());
        assertEquals(prices[0], order.getAmount());
        assertEquals(sessionIds.get(0), order.getSessionId());
        assertEquals(1, order.getItems().size());
        assertEquals(19, tiers.getById(tierIds.get(0)).getAvailableStock());
    }

    private long saveUser() {
        UserEntity user = new UserEntity();
        user.setId(ids.next());
        user.setPhone("139" + ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000));
        user.setPasswordHash("integration-test-only");
        user.setStatus("ACTIVE");
        assertTrue(users.save(user));
        return user.getId();
    }
}
