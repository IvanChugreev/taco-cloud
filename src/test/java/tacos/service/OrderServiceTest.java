package tacos.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tacos.domain.Taco;
import tacos.domain.TacoOrder;
import tacos.domain.User;
import tacos.dto.OrderForm;
import tacos.dto.TacoSummary;
import tacos.mapper.OrderMapper;
import tacos.mapper.UserMapper;
import tacos.repository.OrderRepository;
import tacos.repository.TacoRepository;
import tacos.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                tacoRepository,
                userRepository,
                new OrderMapper(),
                new UserMapper());
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
        when(userRepository.findByUsername("user")).thenReturn(user());
        when(tacoRepository.findAllById(List.of(10L))).thenReturn(List.of(taco));
        when(orderRepository.save(any(TacoOrder.class))).thenAnswer(invocation -> {
            TacoOrder order = invocation.getArgument(0);
            order.setId(99L);
            return order;
        });

        Long orderId = orderService.placeOrder(form, "user");

        ArgumentCaptor<TacoOrder> orderCaptor = ArgumentCaptor.forClass(TacoOrder.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(99L, orderId);
        assertEquals(List.of(taco), orderCaptor.getValue().getTacos());
        assertEquals("user", orderCaptor.getValue().getUser().getUsername());
    }

    @Test
    void placeOrderRejectsMissingTaco() {
        OrderForm form = orderForm();
        when(userRepository.findByUsername("user")).thenReturn(user());
        when(tacoRepository.findAllById(List.of(10L))).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> orderService.placeOrder(form, "user"));

        verify(orderRepository, never()).save(any());
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
