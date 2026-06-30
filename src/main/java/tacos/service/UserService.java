package tacos.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tacos.domain.User;
import tacos.dto.RegistrationForm;
import tacos.dto.UserProfile;
import tacos.mapper.UserMapper;
import tacos.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional
    public void register(RegistrationForm form) {
        if (userRepository.findByUsername(form.getUsername()) != null) {
            throw new DuplicateUsernameException(form.getUsername());
        }
        String encodedPassword = passwordEncoder.encode(form.getPassword());
        userRepository.save(userMapper.toEntity(form, encodedPassword));
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(String username) {
        return userMapper.toProfile(findByUsername(username));
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User '" + username + "' not found");
        }
        return user;
    }

    private User findByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("User '" + username + "' not found");
        }
        return user;
    }
}
