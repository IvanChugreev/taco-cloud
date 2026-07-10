package tacos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
import tacos.dto.api.CreateOrderRequest;
import tacos.dto.api.OrderResponse;
import tacos.dto.api.PageResponse;
import tacos.mapper.ApiMapper;
import tacos.mapper.OrderMapper;
import tacos.mapper.UserMapper;
import tacos.repository.OrderRepository;
import tacos.repository.TacoRepository;
import tacos.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
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
    private final ApiMapper apiMapper;

    public OrderService(
            OrderRepository orderRepository,
            TacoRepository tacoRepository,
            UserRepository userRepository,
            OrderMapper orderMapper,
            UserMapper userMapper,
            ApiMapper apiMapper) {
        this.orderRepository = orderRepository;
        this.tacoRepository = tacoRepository;
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.apiMapper = apiMapper;
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
        List<Taco> tacos = loadTacos(form.tacoIds());
        BigDecimal totalPrice = calculateTotalPrice(tacos);
        TacoOrder savedOrder = orderRepository.save(orderMapper.toEntity(form, user, tacos, totalPrice));
        return savedOrder.getOrderUuid();
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String username) {
        User user = findUser(username);
        List<Taco> tacos = loadTacos(request.tacoIds());
        if (tacos.stream().anyMatch(taco -> orderRepository.existsByTacos_Id(taco.getId()))) {
            throw new TacoUnavailableException();
        }
        BigDecimal totalPrice = calculateTotalPrice(tacos);
        TacoOrder order = orderMapper.toEntity(
                user,
                apiMapper.toAddress(request.deliveryAddress()),
                request.comment(),
                request.ccNumber(),
                request.ccExpiration(),
                request.ccCvv(),
                tacos,
                totalPrice);
        return apiMapper.toOrderResponse(orderRepository.saveAndFlush(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderUuid, String username) {
        return apiMapper.toOrderResponse(findOwnedOrder(orderUuid, username));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrders(
            String username,
            OrderStatus status,
            Instant createdFrom,
            Instant createdTo,
            Pageable pageable) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new InvalidOrderFilterException("createdFrom must not be after createdTo");
        }
        Specification<TacoOrder> specification = (root, query, builder) ->
                builder.equal(root.get("user").get("username"), username);
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        if (createdFrom != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }
        if (createdTo != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        }
        Page<OrderResponse> orders = orderRepository.findAll(specification, pageable)
                .map(apiMapper::toOrderResponse);
        return PageResponse.from(orders);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderUuid, long expectedVersion, String username) {
        TacoOrder order = findOwnedOrder(orderUuid, username);
        verifyVersion(order, expectedVersion);
        order.transitionTo(OrderStatus.CANCELLED);
        orderRepository.flush();
        return apiMapper.toOrderResponse(order);
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

    private TacoOrder findOwnedOrder(UUID orderUuid, String username) {
        return orderRepository.findByOrderUuidAndUserUsername(orderUuid, username)
                .orElseThrow(() -> new ResourceNotFoundException("Order '" + orderUuid + "' not found"));
    }

    private List<Taco> loadTacos(List<Long> requestedIds) {
        List<Long> tacoIds = requestedIds.stream().distinct().toList();
        if (tacoIds.size() != requestedIds.size()) {
            throw new InvalidOrderRequestException("Each taco may appear only once in an order");
        }
        List<Taco> tacos = toList(tacoRepository.findAllById(tacoIds));
        if (tacos.size() != tacoIds.size()) {
            throw new ResourceNotFoundException("One or more tacos were not found");
        }
        return tacos;
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
