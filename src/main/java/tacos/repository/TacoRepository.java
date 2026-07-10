package tacos.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;
import tacos.domain.Taco;

import java.util.List;

public interface TacoRepository extends CrudRepository<Taco, Long> {

    @Query(value = """
            SELECT t.*
            FROM tacos t
            WHERE NOT EXISTS (
                SELECT 1 FROM order_items oi WHERE oi.taco_id = t.id
            )
            """, nativeQuery = true)
    List<Taco> findAvailableForOrdering();
}
