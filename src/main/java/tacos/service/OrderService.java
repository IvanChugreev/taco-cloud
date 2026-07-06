package tacos.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tacos.domain.Ingredient;
import tacos.domain.OrderStatus;
import tacos.domain.Taco;
import tacos.domain.TacoOrder;
import tacos.domain.User;
import tacos.dto.OrderForm;
import tacos.dto.OrderUpdateCommand;
import tacos.dto.UserProfile;
import tacos.mapper.OrderMapper;
import tacos.mapper.UserMapper;
import tacos.repository.OrderRepository;
import tacos.repository.TacoRepository;
import tacos.repository.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final TacoRepository tacoRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    public OrderService(
            OrderRepository orderRepository,
            TacoRepository tacoRepository,
            UserRepository userRepository,
            OrderMapper orderMapper,
            UserMapper userMapper) {
        this.orderRepository = orderRepository;
        this.tacoRepository = tacoRepository;
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public void prepareOrder(OrderForm form, String username) {
        UserProfile profile = userMapper.toProfile(findUser(username));
        if (!StringUtils.hasText(form.getDeliveryName())) {
            form.setDeliveryName(profile.getFullname());
        }
        if (!StringUtils.hasText(form.getDeliveryStreet())) {
            form.setDeliveryStreet(profile.getStreet());
        }
        if (!StringUtils.hasText(form.getDeliveryCity())) {
            form.setDeliveryCity(profile.getCity());
        }
        if (!StringUtils.hasText(form.getDeliveryState())) {
            form.setDeliveryState(profile.getState());
        }
        if (!StringUtils.hasText(form.getDeliveryZip())) {
            form.setDeliveryZip(profile.getZip());
        }
    }

    @Transactional
    public UUID placeOrder(OrderForm form, String username) {
        User user = findUser(username);
        List<Long> tacoIds = form.tacoIds().stream().distinct().toList();
        List<Taco> tacos = toList(tacoRepository.findAllById(tacoIds));
        if (tacos.size() != tacoIds.size()) {
            throw new ResourceNotFoundException("One or more tacos were not found");
        }
        BigDecimal totalPrice = calculateTotalPrice(tacos);
        TacoOrder savedOrder = orderRepository.save(orderMapper.toEntity(form, user, tacos, totalPrice));
        return savedOrder.getOrderUuid();
    }

    @Transactional
    public void transitionOrder(UUID orderUuid, OrderStatus targetStatus, long expectedVersion) {
        TacoOrder order = findOrder(orderUuid);
        verifyVersion(order, expectedVersion);
        order.transitionTo(targetStatus);
    }

    @Transactional
    public void updateOrder(UUID orderUuid, OrderUpdateCommand command) {
        TacoOrder order = findOrder(orderUuid);
        verifyVersion(order, command.getVersion());
        order.updateDeliveryDetails(orderMapper.toAddress(command), command.getComment());
    }

    private User findUser(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("User '" + username + "' not found");
        }
        return user;
    }

    private TacoOrder findOrder(UUID orderUuid) {
        return orderRepository.findByOrderUuid(orderUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Order '" + orderUuid + "' not found"));
    }

    private void verifyVersion(TacoOrder order, Long expectedVersion) {
        if (!Objects.equals(order.getVersion(), expectedVersion)) {
            throw new OrderVersionConflictException(
                    order.getOrderUuid(), expectedVersion, order.getVersion());
        }
    }

    private BigDecimal calculateTotalPrice(List<Taco> tacos) {
        return tacos.stream()
                .flatMap(taco -> taco.getIngredients().stream())
                .map(Ingredient::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static <T> List<T> toList(Iterable<T> values) {
        List<T> result = new ArrayList<>();
        values.forEach(result::add);
        return result;
    }
}
