package com.projects.wtg;

import com.projects.wtg.dto.UserRegistrationDto;
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
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    private UserRegistrationDto registrationDto;
    private User mockUser;
    private Account mockAccount;

    @BeforeEach
    void setUp() {
        // Inicializa os objetos antes de cada teste para evitar repetição
        registrationDto = new UserRegistrationDto();
        registrationDto.setFullName("Test User");
        registrationDto.setUserName("test_user");
        registrationDto.setEmail("test@email.com");
        registrationDto.setPassword("senha123");

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setFullName("Test User");

        mockAccount = new Account();
        mockAccount.setId(1L);
        mockAccount.setEmail("test@email.com");
        mockAccount.setUserName("test_user");
        mockUser.setAccount(mockAccount);
        mockAccount.setUser(mockUser);
    }

    @Test
    void createUserWithAccount_shouldCreateUser_whenEmailIsUnique() {
        // Cenário 1: E-mail não existe, a criação deve ser bem-sucedida.
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Ação: Chamar o método com o DTO
        User createdUser = userService.createUserWithAccount(registrationDto);

        // Verificação: O usuário e a conta foram criados e salvos
        assertNotNull(createdUser);
        assertNotNull(createdUser.getAccount());
        assertEquals("test@email.com", createdUser.getAccount().getEmail());

        // Verificação: Garante que os métodos foram chamados
        verify(accountRepository, times(1)).findByEmail(anyString());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUserWithAccount_shouldThrowException_whenEmailAlreadyExists() {
        // Cenário 2: E-mail já existe, uma exceção deve ser lançada.
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(mockAccount));

        // Ação e Verificação: Confirma se a exceção é lançada
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userService.createUserWithAccount(registrationDto));

        assertEquals("Email já cadastrado!", exception.getMessage());

        // Verificação: O método save NUNCA deve ser chamado
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserWithAccount_shouldThrowException_whenUserDoesNotExist() {
        // Cenário 3: Usuário não existe, uma exceção deve ser lançada.
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Ação e Verificação: Confirma se a exceção é lançada
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userService.updateUserWithAccount(99L, new User(), new Account(), List.of()));

        assertEquals("Usuário não encontrado", exception.getMessage());
    }

    @Test
    void updateUserWithAccount_shouldUpdateUserAndAccount_whenTheyExist() {
        // Cenário 4: Usuário e conta existem e devem ser atualizados.
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setFullName("Old Name");

        Account existingAccount = new Account();
        existingAccount.setUserName("old_user");
        existingUser.setAccount(existingAccount);

        // Dados para atualização
        User newUserdata = new User();
        newUserdata.setFullName("New Name");

        Account newAccountData = new Account();
        newAccountData.setUserName("new_user");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Ação: Chamar o método com os dados de atualização
        User updatedUser = userService.updateUserWithAccount(1L, newUserdata, newAccountData, List.of());

        // Verificação: Garante que os dados foram atualizados
        assertNotNull(updatedUser);
        assertEquals("New Name", updatedUser.getFullName());
        assertEquals("new_user", updatedUser.getAccount().getUserName());

        // Verificação: Garante que os métodos foram chamados
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(existingUser);
    }
}