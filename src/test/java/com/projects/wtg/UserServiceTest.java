package com.projects.wtg;

import com.projects.wtg.dto.UserRegistrationDto;
import com.projects.wtg.exception.EmailAlreadyExistsException;
import com.projects.wtg.model.Account;
import com.projects.wtg.model.User;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.UserRepository;
import com.projects.wtg.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserRegistrationDto registrationDto;

    @BeforeEach
    void setUp() {
        registrationDto = new UserRegistrationDto();
        registrationDto.setFullName("Test User");
        registrationDto.setUserName("test_user");
        registrationDto.setEmail("test@email.com");
        registrationDto.setPassword("Password@123");
        registrationDto.setConfirmPassword("Password@123");
    }

    @Test
    void createUserWithAccount_shouldSucceed_whenDataIsValid() {
        // Arrange
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User createdUser = userService.createUserWithAccount(registrationDto);

        // Assert
        assertNotNull(createdUser);
        assertNotNull(createdUser.getAccount());
        assertEquals("test@email.com", createdUser.getAccount().getEmail());
        assertEquals("hashedPassword", createdUser.getAccount().getPassword());

        verify(accountRepository, times(1)).findByEmail("test@email.com");
        verify(passwordEncoder, times(1)).encode("Password@123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUserWithAccount_shouldThrowException_whenEmailAlreadyExists() {
        // Arrange
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(new Account()));

        // Act & Assert
        EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class, () ->
                userService.createUserWithAccount(registrationDto));

        assertEquals("Este e-mail já está cadastrado. Por favor, faça o login.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUserWithAccount_shouldThrowException_whenPasswordIsWeak() {
        // Arrange
        registrationDto.setPassword("123");
        registrationDto.setConfirmPassword("123");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userService.createUserWithAccount(registrationDto));

        assertTrue(exception.getMessage().contains("A senha não atende aos critérios de segurança"));
    }

    @Test
    void createUserWithAccount_shouldThrowException_whenPasswordsDoNotMatch() {
        // Arrange
        registrationDto.setConfirmPassword("AnotherPassword@123");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userService.createUserWithAccount(registrationDto));

        assertEquals("As senhas não coincidem.", exception.getMessage());
    }
}