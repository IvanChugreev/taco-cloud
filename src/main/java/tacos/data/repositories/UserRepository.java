package tacos.data.repositories;

import org.springframework.data.repository.CrudRepository;
import tacos.models.User;

public interface UserRepository extends CrudRepository<User, Long> {

    User findByUsername(String username);

}
