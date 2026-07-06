package tacos.repository;

import org.springframework.data.repository.CrudRepository;
import tacos.domain.TacoOrder;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends CrudRepository<TacoOrder, Long> {

    Optional<TacoOrder> findByOrderUuid(UUID orderUuid);
}
