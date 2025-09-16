package com.projects.wtg;

import com.projects.wtg.model.Account;
import com.projects.wtg.model.User;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.UserRepository;
import com.projects.wtg.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Test
    void createUserWithAccount_shouldSetAccountAndSaveUser() {
        // Cenário: Novos objetos de usuário e conta
        User user = new User();
        Account account = new Account();

        // Simular o comportamento do repositório
        // Quando qualquer User for salvo, retorne o próprio User.
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Chamar o método a ser testado
        User createdUser = userService.createUserWithAccount(user, account);

        // Verificar os resultados
        assertNotNull(createdUser);
        assertEquals(account, createdUser.getAccount()); // Verifica se a conta foi associada
        assertEquals(user, account.getUser()); // Verifica a associação bidirecional

        // Verificar se o método save do repositório foi chamado
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void updateUserWithAccount_shouldThrowException_whenUserDoesNotExist() {
        // Cenário: Usuário não existe
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Verificar se a exceção é lançada
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userService.updateUserWithAccount(99L, new User(), new Account(), List.of()));

        assertEquals("Usuário não encontrado", exception.getMessage());
    }

    @Test
    void updateUserWithAccount_shouldUpdateUserAndAccount_whenTheyExist() {
        // Cenário: Usuário e conta existentes
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

        // Simular o repositório
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Chamar o método a ser testado
        User updatedUser = userService.updateUserWithAccount(1L, newUserdata, newAccountData, List.of());

        // Verificar os resultados
        assertNotNull(updatedUser);
        assertEquals("New Name", updatedUser.getFullName());
        assertEquals("new_user", updatedUser.getAccount().getUserName());

        // Verificar as chamadas
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(existingUser);
    }

    @Test
    void updateUserWithAccount_shouldCreateAccount_whenUserExistsButHasNoAccount() {
        // Cenário: Usuário existe, mas não tem conta
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setAccount(null); // Sem conta

        Account newAccountData = new Account();
        newAccountData.setUserName("new_user");

        // Simular o repositório
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Chamar o método a ser testado
        User updatedUser = userService.updateUserWithAccount(1L, new User(), newAccountData, List.of());

        // Verificar os resultados
        assertNotNull(updatedUser);
        assertNotNull(updatedUser.getAccount()); // A conta deve ter sido criada
        assertEquals("new_user", updatedUser.getAccount().getUserName());

        // O usuário deve ter a referência da nova conta e a conta a referência do usuário
        assertEquals(updatedUser.getAccount().getUser(), updatedUser);
    }
}
