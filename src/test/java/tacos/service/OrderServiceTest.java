package tacos.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tacos.domain.DeliveryAddress;
import tacos.domain.Ingredient;
import tacos.domain.Ingredient.Type;
import tacos.domain.OrderStatus;
import tacos.domain.Taco;
import tacos.domain.TacoOrder;
import tacos.domain.User;
import tacos.dto.OrderForm;
import tacos.dto.TacoSummary;
import tacos.event.OrderCancelled;
import tacos.event.OrderCreated;
import tacos.event.OrderEvent;
import tacos.event.OrderStatusChanged;
import tacos.mapper.ApiMapper;
import tacos.mapper.OrderMapper;
import tacos.mapper.UserMapper;
import tacos.outbox.OrderEventOutboxService;
import tacos.repository.OrderRepository;
import tacos.repository.TacoRepository;
import tacos.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TacoRepository tacoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderEventOutboxService orderEventOutboxService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                tacoRepository,
                userRepository,
                new OrderMapper(),
                new UserMapper(),
                new ApiMapper(),
                orderEventOutboxService);
    }

    @Test
    void prepareOrderFillsMissingDeliveryFields() {
        OrderForm form = new OrderForm();
        form.setDeliveryCity("Custom City");
        when(userRepository.findByUsername("user")).thenReturn(user());

        orderService.prepareOrder(form, "user");

        assertEquals("Test User", form.getDeliveryName());
        assertEquals("1 Test Street", form.getDeliveryStreet());
        assertEquals("Custom City", form.getDeliveryCity());
        assertEquals("TS", form.getDeliveryState());
        assertEquals("12345", form.getDeliveryZip());
    }

    @Test
    void placeOrderReloadsTacosAndSavesOrder() {
        OrderForm form = orderForm();
        Taco taco = new Taco();
        taco.setId(10L);
        taco.setName("Test Taco");
        taco.setIngredients(List.of(
                new Ingredient("FLTO", "Flour Tortilla", Type.WRAP, new BigDecimal("1.00")),
                new Ingredient("GRBF", "Ground Beef", Type.PROTEIN, new BigDecimal("2.50"))));
        when(userRepository.findByUsername("user")).thenReturn(user());
        when(tacoRepository.findAllById(List.of(10L))).thenReturn(List.of(taco));
        when(orderRepository.save(any(TacoOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID orderUuid = orderService.placeOrder(form, "user");

        ArgumentCaptor<TacoOrder> orderCaptor = ArgumentCaptor.forClass(TacoOrder.class);
        verify(orderRepository).save(orderCaptor.capture());
        TacoOrder savedOrder = orderCaptor.getValue();
        assertEquals(savedOrder.getOrderUuid(), orderUuid);
        assertEquals(List.of(taco), savedOrder.getTacos());
        assertEquals("user", savedOrder.getUser().getUsername());
        assertEquals(OrderStatus.CREATED, savedOrder.getStatus());
        assertEquals(new BigDecimal("3.50"), savedOrder.getTotalPrice());
        assertEquals("Test User", savedOrder.getDeliveryAddress().getRecipientName());
        verify(orderEventOutboxService).append(any(OrderCreated.class));
    }

    @Test
    void placeOrderRejectsMissingTaco() {
        OrderForm form = orderForm();
        when(userRepository.findByUsername("user")).thenReturn(user());
        when(tacoRepository.findAllById(List.of(10L))).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> orderService.placeOrder(form, "user"));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void transitionOrderDelegatesToDomainModelWhenVersionMatches() {
        UUID orderUuid = UUID.randomUUID();
        TacoOrder order = mock(TacoOrder.class);
        when(order.getVersion()).thenReturn(3L);
        when(order.getOrderUuid()).thenReturn(orderUuid);
        when(order.getStatus()).thenReturn(OrderStatus.ACCEPTED);
        when(order.getCreatedAt()).thenReturn(Instant.parse("2026-07-10T00:00:00Z"));
        when(order.getUpdatedAt()).thenReturn(Instant.parse("2026-07-10T00:00:00Z"));
        when(order.getTotalPrice()).thenReturn(BigDecimal.ZERO);
        when(order.getDeliveryAddress()).thenReturn(new DeliveryAddress(
                "Test User",
                "1 Test Street",
                "Test City",
                "TS",
                "12345"));
        when(order.getTacos()).thenReturn(List.of());
        when(orderRepository.findByOrderUuid(orderUuid)).thenReturn(Optional.of(order));

        orderService.transitionOrder(orderUuid, OrderStatus.ACCEPTED, 3L);

        verify(order).transitionTo(OrderStatus.ACCEPTED);
        verify(orderRepository).flush();
        verify(orderEventOutboxService).append(any(OrderStatusChanged.class));
    }

    @Test
    void transitionOrderRejectsStaleVersion() {
        UUID orderUuid = UUID.randomUUID();
        TacoOrder order = mock(TacoOrder.class);
        when(order.getVersion()).thenReturn(4L);
        when(order.getOrderUuid()).thenReturn(orderUuid);
        when(orderRepository.findByOrderUuid(orderUuid)).thenReturn(Optional.of(order));

        assertThrows(
                OrderVersionConflictException.class,
                () -> orderService.transitionOrder(orderUuid, OrderStatus.ACCEPTED, 3L));

        verify(order, never()).transitionTo(any());
    }

    @Test
    void cancelOrderPublishesCancellationEvent() {
        UUID orderUuid = UUID.randomUUID();
        TacoOrder order = mock(TacoOrder.class);
        when(order.getVersion()).thenReturn(2L);
        when(order.getOrderUuid()).thenReturn(orderUuid);
        when(order.getStatus()).thenReturn(
                OrderStatus.CREATED,
                OrderStatus.CANCELLED,
                OrderStatus.CANCELLED);
        when(order.getUser()).thenReturn(user());
        when(order.getCreatedAt()).thenReturn(Instant.parse("2026-07-10T00:00:00Z"));
        when(order.getUpdatedAt()).thenReturn(Instant.parse("2026-07-10T00:00:00Z"));
        when(order.getTotalPrice()).thenReturn(new BigDecimal("3.50"));
        when(order.getDeliveryAddress()).thenReturn(new DeliveryAddress(
                "Test User",
                "1 Test Street",
                "Test City",
                "TS",
                "12345"));
        when(order.getTacos()).thenReturn(List.of());
        when(orderRepository.findByOrderUuidAndUserUsername(orderUuid, "user"))
                .thenReturn(Optional.of(order));

        orderService.cancelOrder(orderUuid, 2L, "user");

        verify(order).transitionTo(OrderStatus.CANCELLED);
        verify(orderRepository).flush();
        ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(orderEventOutboxService).append(eventCaptor.capture());
        OrderCancelled event = assertInstanceOf(OrderCancelled.class, eventCaptor.getValue());
        assertEquals(orderUuid, event.orderId());
        assertEquals("user", event.username());
        assertEquals(OrderStatus.CREATED, event.previousStatus());
        assertEquals(OrderStatus.CANCELLED, event.status());
    }

    private OrderForm orderForm() {
        OrderForm form = new OrderForm();
        form.setDeliveryName("Test User");
        form.setDeliveryStreet("1 Test Street");
        form.setDeliveryCity("Test City");
        form.setDeliveryState("TS");
        form.setDeliveryZip("12345");
        form.setCcNumber("4111111111111111");
        form.setCcExpiration("12/30");
        form.setCcCVV("123");
        form.setTacos(List.of(new TacoSummary(10L, "Test Taco")));
        return form;
    }

    private User user() {
        return new User(
                "user",
                "encoded-password",
                "Test User",
                "1 Test Street",
                "Test City",
                "TS",
                "12345",
                "5551234567");
    }
}
