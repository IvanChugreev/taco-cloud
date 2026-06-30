package tacos.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tacos.domain.User;
import tacos.dto.RegistrationForm;
import tacos.mapper.UserMapper;
import tacos.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, userMapper);
    }

    @Test
    void registerEncodesPasswordAndSavesMappedUser() {
        RegistrationForm form = registrationForm();
        User user = user("encoded-password");
        when(userRepository.findByUsername("user")).thenReturn(null);
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(userMapper.toEntity(form, "encoded-password")).thenReturn(user);

        userService.register(form);

        verify(passwordEncoder).encode("password");
        verify(userMapper).toEntity(form, "encoded-password");
        verify(userRepository).save(user);
    }

    @Test
    void registerRejectsDuplicateUsername() {
        RegistrationForm form = registrationForm();
        when(userRepository.findByUsername("user")).thenReturn(user("existing-password"));

        assertThrows(DuplicateUsernameException.class, () -> userService.register(form));

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(passwordEncoder, userMapper);
    }

    private RegistrationForm registrationForm() {
        RegistrationForm form = new RegistrationForm();
        form.setUsername("user");
        form.setPassword("password");
        form.setConfirm("password");
        form.setFullname("Test User");
        form.setStreet("1 Test Street");
        form.setCity("Test City");
        form.setState("TS");
        form.setZip("12345");
        form.setPhone("5551234567");
        return form;
    }

    private User user(String password) {
        return new User(
                "user",
                password,
                "Test User",
                "1 Test Street",
                "Test City",
                "TS",
                "12345",
                "5551234567");
    }
}
