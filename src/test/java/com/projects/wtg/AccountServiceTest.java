package com.projects.wtg;

import com.projects.wtg.model.Account;
import com.projects.wtg.model.User;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.UserRepository;
import com.projects.wtg.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)//Habilita a integração com o Mockito.
public class AccountServiceTest {
    @InjectMocks //Cria uma instância real do AccountService e injeta os objetos "mockados" nele.
    private AccountService accountService;

    @Mock //Cria versões simuladas das dependências (AccountRepository e UserRepository).
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void createAccountForUsers_shouldCreateAccount_whenUserExists() {
        // Cenário: O usuário existe no banco de dados.
        User mockUser = new User();
        mockUser.setId(1L);

        Account mockAccount = new Account();
        mockAccount.setUserName("testUser");

        // Simular o comportamento do repositório Quando findById for chamado com qualquer Long, retorne o mockUser.
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));
        // Quando o save for chamado para qualquer Account, retorne a conta mockada.
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);

        // Chamar o método a ser testado
        Account createdAccount = accountService.createAccountForUsers(new Account(), 1L);

        // Verificar os resultados
        assertNotNull(createdAccount);
        assertEquals("testUser", createdAccount.getUserName());

        // Verificar se os métodos dos repositórios foram chamados
        verify(userRepository, times(1)).findById(1L);
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void createAccountForUsers_shouldThrowException_whenUserDoesNotExist() {
        // Cenário: O usuário NÃO existe.
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Verificar se a exceção é lançada
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                accountService.createAccountForUsers(new Account(), 99L));

        // Verificar a mensagem da exceção
        assertEquals("Usuário não encontrado", exception.getMessage());
    }
}
