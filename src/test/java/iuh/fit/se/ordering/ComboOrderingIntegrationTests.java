package iuh.fit.se.ordering;

import iuh.fit.se.TestcontainersConfiguration;
import iuh.fit.se.menu.api.dto.admin.UpsertFixedComboRequest;
import iuh.fit.se.menu.api.dto.admin.UpsertPickComboRequest;
import iuh.fit.se.menu.application.MenuService;
import iuh.fit.se.menu.infrastructure.MenuItemRepository;
import iuh.fit.se.ordering.api.dto.CreateOrderRequest;
import iuh.fit.se.ordering.api.dto.OrderResponse;
import iuh.fit.se.ordering.application.OrderingService;
import iuh.fit.se.ordering.domain.OrderItem;
import iuh.fit.se.ordering.infrastructure.OrderItemRepository;
import iuh.fit.se.shared.security.JwtPrincipal;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ComboOrderingIntegrationTests {

    @Autowired
    OrderingService orderingService;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    MenuItemRepository menuItemRepository;

    @Autowired
        MenuService menuService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrder_fixedCombo_createsParentAndChildrenAndBillsOnlyParent() {
        authenticateAsWaiter(2L);

        Long comboId = menuItemRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .filter(mi -> "Seafood Duo Combo".equals(mi.getName()))
                .findFirst()
                .orElseThrow()
                .getId();

        Long salmonId = menuItemRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .filter(mi -> "Cá Hồi Áp Chảo".equals(mi.getName()))
                .findFirst()
                .orElseThrow()
                .getId();

        Long seabassId = menuItemRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .filter(mi -> "Cá Chẽm Sốt Mù Tạt".equals(mi.getName()))
                .findFirst()
                .orElseThrow()
                .getId();

        // Ensure combo configuration exists (tests should not rely on seed ordering)
        menuService.upsertFixedComboConfig(comboId, new UpsertFixedComboRequest(List.of(
                new UpsertFixedComboRequest.Component(salmonId, 1),
                new UpsertFixedComboRequest.Component(seabassId, 1)
        )));

        BigDecimal comboPrice = menuService.getItem(comboId).price();

        CreateOrderRequest request = new CreateOrderRequest(
                "1-001",
                null,
                false,
                List.of(new CreateOrderRequest.OrderItemRequest(comboId, 1, null, null))
        );

        OrderResponse response = orderingService.createOrder(request, UUID.randomUUID().toString(), null);

        assertThat(response.totalAmount()).isEqualByComparingTo(comboPrice);

        List<OrderItem> orderItems = orderItemRepository.findAllByRevisionIdOrderByIdAsc(
                response.items().getFirst().revisionId()
        );

        OrderItem parent = orderItems.stream().filter(OrderItem::isComboParent).findFirst().orElseThrow();
        List<OrderItem> children = orderItems.stream().filter(i -> !i.isComboParent()).toList();

        assertThat(parent.getMenuItemId()).isEqualTo(comboId);
        assertThat(parent.isBillable()).isTrue();
        assertThat(parent.getParentOrderItemId()).isNull();
        assertThat(parent.getComboSnapshot()).isNotNull();

        assertThat(children).isNotEmpty();
        assertThat(children).allSatisfy(child -> {
            assertThat(child.isBillable()).isFalse();
            assertThat(child.getParentOrderItemId()).isEqualTo(parent.getId());
            assertThat(child.getUnitPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    @Test
    void createOrder_pickCombo_validatesAndCreatesChildrenFromSelection() {
        authenticateAsWaiter(3L);

        Long comboId = menuItemRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .filter(mi -> "Lunch Set".equals(mi.getName()))
                .findFirst()
                .orElseThrow()
                .getId();

        Long main1 = menuItemRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .filter(mi -> "Ức Vịt Áp Chảo Sốt Cam".equals(mi.getName()))
                .findFirst()
                .orElseThrow()
                .getId();
        Long drink1 = menuItemRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .filter(mi -> "Trà & Trà Thảo Mộc".equals(mi.getName()))
                .findFirst()
                .orElseThrow()
                .getId();

        menuService.upsertPickComboConfig(comboId, new UpsertPickComboRequest(List.of(
                new UpsertPickComboRequest.Slot("Main", 1, 1, 1, List.of(main1)),
                new UpsertPickComboRequest.Slot("Drink", 1, 1, 2, List.of(drink1))
        )));

        var detail = menuService.getMenuItemAdminDetail(comboId);
        assertThat(detail.getPickCombo()).isNotNull();
        assertThat(detail.getPickCombo().getSlots()).hasSizeGreaterThanOrEqualTo(2);

        // choose 1 allowed item per slot (slots are configured min=1,max=1)
        List<CreateOrderRequest.SlotSelection> selections = detail.getPickCombo().getSlots().stream()
                .map(slot -> new CreateOrderRequest.SlotSelection(
                        slot.getId(),
                        List.of(new CreateOrderRequest.SelectedItem(slot.getAllowedItemIds().getFirst(), 1))
                ))
                .toList();

        CreateOrderRequest request = new CreateOrderRequest(
                "1-001",
                null,
                false,
                List.of(new CreateOrderRequest.OrderItemRequest(
                        comboId,
                        1,
                        null,
                        new CreateOrderRequest.ComboSelection(selections)
                ))
        );

        OrderResponse response = orderingService.createOrder(request, UUID.randomUUID().toString(), null);
        assertThat(response.totalAmount()).isEqualByComparingTo(menuService.getItem(comboId).price());

        List<OrderItem> saved = orderItemRepository.findAllByRevisionIdOrderByIdAsc(
                response.items().getFirst().revisionId()
        );
        OrderItem parent = saved.stream().filter(OrderItem::isComboParent).findFirst().orElseThrow();
        List<OrderItem> children = saved.stream().filter(i -> !i.isComboParent()).toList();

        assertThat(children).hasSize(selections.size());
        assertThat(children).allSatisfy(child -> assertThat(child.getParentOrderItemId()).isEqualTo(parent.getId()));
    }

    private static void authenticateAsWaiter(Long staffId) {
        JwtPrincipal principal = new JwtPrincipal("waiter-test", staffId, "WAITER", null, null);
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_WAITER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}

