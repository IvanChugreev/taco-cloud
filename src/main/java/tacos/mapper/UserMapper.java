package tacos.mapper;

import org.springframework.stereotype.Component;
import tacos.domain.User;
import tacos.dto.RegistrationForm;
import tacos.dto.UserProfile;

@Component
public class UserMapper {

    public User toEntity(RegistrationForm form, String encodedPassword) {
        return new User(
                form.getUsername(),
                encodedPassword,
                form.getFullname(),
                form.getStreet(),
                form.getCity(),
                form.getState(),
                form.getZip(),
                form.getPhone());
    }

    public UserProfile toProfile(User user) {
        return new UserProfile(
                user.getFullname(),
                user.getStreet(),
                user.getCity(),
                user.getState(),
                user.getZip());
    }
}
