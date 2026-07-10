package tacos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tacos.domain.TacoOrder;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<TacoOrder, Long>, JpaSpecificationExecutor<TacoOrder> {

    Optional<TacoOrder> findByOrderUuid(UUID orderUuid);

    Optional<TacoOrder> findByOrderUuidAndUserUsername(UUID orderUuid, String username);

    boolean existsByTacos_Id(Long tacoId);
}
