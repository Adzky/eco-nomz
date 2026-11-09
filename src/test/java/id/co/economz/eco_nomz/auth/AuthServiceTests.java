package id.co.economz.eco_nomz.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.co.economz.eco_nomz.user.User;
import id.co.economz.eco_nomz.user.UserRepository;
import id.co.economz.eco_nomz.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder);
    }

    @Test
    void registersCustomerWithNormalizedEmailAndEncodedPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setName("  Customer Name  ");
        request.setEmail(" CUSTOMER@Example.com ");
        request.setPassword("correct-horse-battery");
        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(false);
        when(passwordEncoder.encode("correct-horse-battery")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User registered = authService.registerCustomer(request);

        assertThat(registered.getName()).isEqualTo("Customer Name");
        assertThat(registered.getEmail()).isEqualTo("customer@example.com");
        assertThat(registered.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(registered.getRole()).isEqualTo(UserRole.CUSTOMER);
        verify(userRepository).saveAndFlush(registered);
    }

    @Test
    void registersRestaurantOwnerWhenThatOnboardingTypeIsSelected() {
        RegisterRequest request = new RegisterRequest();
        request.setAccountType(RegistrationType.RESTAURANT_OWNER);
        request.setName("Restaurant Owner");
        request.setEmail("owner@example.com");
        request.setPassword("correct-horse-battery");
        when(userRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(false);
        when(passwordEncoder.encode("correct-horse-battery")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User registered = authService.registerAccount(request);

        assertThat(registered.getRole()).isEqualTo(UserRole.RESTAURANT_OWNER);
        verify(userRepository).saveAndFlush(registered);
    }

    @Test
    void rejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Customer Name");
        request.setEmail("customer@example.com");
        request.setPassword("correct-horse-battery");
        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerCustomer(request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void rejectsPasswordExceedingBcryptUtf8Limit() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Customer Name");
        request.setEmail("customer@example.com");
        request.setPassword("😀".repeat(19));
        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.registerCustomer(request))
                .isInstanceOf(PasswordTooLongException.class);
    }
}
