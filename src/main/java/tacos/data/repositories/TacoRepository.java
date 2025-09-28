package tacos.data.repositories;

import org.springframework.data.repository.CrudRepository;
import tacos.models.Taco;

public interface TacoRepository extends CrudRepository<Taco, Long> {

}