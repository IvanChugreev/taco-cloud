package tacos.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tacos.domain.Taco;
import tacos.domain.TacoOrder;
import tacos.domain.User;
import tacos.dto.OrderForm;
import tacos.dto.UserProfile;
import tacos.mapper.OrderMapper;
import tacos.mapper.UserMapper;
import tacos.repository.OrderRepository;
import tacos.repository.TacoRepository;
import tacos.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

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
    public Long placeOrder(OrderForm form, String username) {
        User user = findUser(username);
        List<Long> tacoIds = form.tacoIds().stream().distinct().toList();
        List<Taco> tacos = toList(tacoRepository.findAllById(tacoIds));
        if (tacos.size() != tacoIds.size()) {
            throw new ResourceNotFoundException("One or more tacos were not found");
        }
        TacoOrder savedOrder = orderRepository.save(orderMapper.toEntity(form, user, tacos));
        return savedOrder.getId();
    }

    private User findUser(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("User '" + username + "' not found");
        }
        return user;
    }

    private static <T> List<T> toList(Iterable<T> values) {
        List<T> result = new ArrayList<>();
        values.forEach(result::add);
        return result;
    }
}
