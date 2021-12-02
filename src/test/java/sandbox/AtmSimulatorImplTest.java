package sandbox;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sandbox.model.Account;
import sandbox.model.Balance;
import sandbox.model.Session;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AtmSimulatorImplTest {
    @Mock
    SqliteDao sqliteDao;

    @InjectMocks
    AtmSimulatorImpl underTest;

    @BeforeEach
    void init_mocks() {
        MockitoAnnotations.openMocks(this);
        underTest = new AtmSimulatorImpl(sqliteDao);
    }

    @Test
    void login_makeANewSession() {
        // given
        String userName = "tester";
        String pin = "1234";
        Integer accountId = 1;

        when(sqliteDao.getAccount(userName, pin))
                .thenReturn(Optional.of(Account.builder()
                        .username(userName)
                        .pin(pin)
                        .id(accountId)
                        .build()));

        when(sqliteDao.getSession(accountId))
                .thenReturn(Optional.empty());
        // when
        Optional<String> tokenOpt = underTest.login(userName, pin);

        // verify
        assertTrue(tokenOpt.isPresent(), "token was found");
        assertNotNull(tokenOpt.get());

        verify(sqliteDao).getAccount(userName, pin);
        verify(sqliteDao).getSession(accountId);

        ArgumentCaptor<Timestamp> argumentCaptor = ArgumentCaptor.forClass(Timestamp.class);
        verify(sqliteDao).persistSession(eq(accountId), any(UUID.class), argumentCaptor.capture());

        Timestamp expiryTime = argumentCaptor.getValue();
        assertTrue(expiryTime.after(Timestamp.from(Instant.now())));
    }

    @Test
    void login_oldSessionExpired() {
        // given
        String userName = "tester";
        String pin = "1234";
        Integer accountId = 1;
        UUID token = UUID.randomUUID();
        Timestamp expiryTime = Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES));

        when(sqliteDao.getAccount(userName, pin))
                .thenReturn(Optional.of(Account.builder()
                        .username(userName)
                        .pin(pin)
                        .id(accountId)
                        .build()));

        when(sqliteDao.getSession(accountId))
                .thenReturn(Optional.of(Session.builder()
                        .token(token)
                        .accountId(accountId)
                        .expires(expiryTime)
                        .build()));
        // when
        Optional<String> tokenOpt = underTest.login(userName, pin);

        // verify
        assertTrue(tokenOpt.isPresent(), "token was found");
        assertNotNull(tokenOpt.get());
        assertNotEquals(token.toString(), tokenOpt.get(), "token should be new");

        verify(sqliteDao).getAccount(userName, pin);
        verify(sqliteDao).getSession(accountId);

        ArgumentCaptor<Timestamp> argumentCaptor = ArgumentCaptor.forClass(Timestamp.class);
        verify(sqliteDao).persistSession(eq(accountId), any(UUID.class), argumentCaptor.capture());

        Timestamp expiryTimeCap = argumentCaptor.getValue();
        assertTrue(expiryTimeCap.after(Timestamp.from(Instant.now())));
    }

    @Test
    void login_useExistingSession() {
        // given
        String userName = "tester";
        String pin = "1234";
        Integer accountId = 1;
        UUID token = UUID.randomUUID();
        Timestamp expiryTime = Timestamp.from(Instant.now().plus(1, ChronoUnit.MINUTES));

        when(sqliteDao.getAccount(userName, pin))
                .thenReturn(Optional.of(Account.builder()
                        .username(userName)
                        .pin(pin)
                        .id(accountId)
                        .build()));

        when(sqliteDao.getSession(accountId))
                .thenReturn(Optional.of(Session.builder()
                        .token(token)
                        .accountId(accountId)
                        .expires(expiryTime)
                        .build()));
        // when
        Optional<String> tokenOpt = underTest.login(userName, pin);

        // verify
        assertTrue(tokenOpt.isPresent(), "token was found");
        assertEquals(token.toString(), tokenOpt.get(), "tokens match");

        verify(sqliteDao).getAccount(userName, pin);
        verify(sqliteDao).getSession(accountId);
        verify(sqliteDao, never()).persistSession(eq(accountId), any(UUID.class), any(Timestamp.class));
    }

    @Test
    void viewBalance() {
        // given
        Integer accountId = 1;
        UUID token = UUID.randomUUID();
        String tokenStr = token.toString();
        Timestamp expiryTime = Timestamp.from(Instant.now().plus(1, ChronoUnit.MINUTES));

        when(sqliteDao.getSession(tokenStr))
                .thenReturn(Optional.of(Session.builder()
                        .token(token)
                        .accountId(accountId)
                        .expires(expiryTime)
                        .build()));

        when(sqliteDao.getBalance(accountId))
                .thenReturn(Optional.of(Balance.builder()
                        .balance(100L)
                        .accountId(accountId)
                        .build()));

        // when
        long balance = underTest.viewBalance(tokenStr);

        // verify
        assertEquals(100L, balance, "balance matches");

        verify(sqliteDao).getSession(tokenStr);
        verify(sqliteDao).getBalance(accountId);
    }

    @Test
    void viewBalance_invalidToken() {
        // given
        Integer accountId = 1;
        UUID token = UUID.randomUUID();
        String tokenStr = token.toString();

        when(sqliteDao.getSession(tokenStr))
                .thenReturn(Optional.empty());

        when(sqliteDao.getBalance(accountId))
                .thenReturn(Optional.of(Balance.builder()
                        .balance(100L)
                        .accountId(accountId)
                        .build()));

        // when
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            underTest.viewBalance(tokenStr);
        });

        Assertions.assertEquals("invalid token", thrown.getMessage());

        // verify
        verify(sqliteDao).getSession(tokenStr);
        verify(sqliteDao, never()).getBalance(accountId);
    }

    @Test
    void viewBalance_expiredToken() {
        // given
        Integer accountId = 1;
        UUID token = UUID.randomUUID();
        String tokenStr = token.toString();
        Timestamp expiryTime = Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES));

        when(sqliteDao.getSession(tokenStr))
                .thenReturn(Optional.of(Session.builder()
                        .token(token)
                        .accountId(accountId)
                        .expires(expiryTime)
                        .build()));

        when(sqliteDao.getBalance(accountId))
                .thenReturn(Optional.of(Balance.builder()
                        .balance(100L)
                        .accountId(accountId)
                        .build()));

        // when
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            underTest.viewBalance(tokenStr);
        });

        Assertions.assertEquals("invalid token", thrown.getMessage());

        // verify
        verify(sqliteDao).getSession(tokenStr);
        verify(sqliteDao, never()).getBalance(accountId);
    }

    @Test
    void deposit_sunnyday() {
        // given
        Integer accountId = 1;
        UUID token = UUID.randomUUID();
        String tokenStr = token.toString();
        Timestamp expiryTime = Timestamp.from(Instant.now().plus(1, ChronoUnit.MINUTES));

        when(sqliteDao.getSession(tokenStr))
                .thenReturn(Optional.of(Session.builder()
                        .token(token)
                        .accountId(accountId)
                        .expires(expiryTime)
                        .build()));

        when(sqliteDao.getBalance(accountId))
                .thenReturn(Optional.of(Balance.builder()
                        .balance(100L)
                        .accountId(accountId)
                        .build()));

        // when
        boolean success = underTest.deposit(tokenStr, 10L);

        // verify
        assertTrue(success, "deposit success");

        verify(sqliteDao).getSession(tokenStr);
        verify(sqliteDao).getBalance(accountId);

        ArgumentCaptor<Balance> newBalanceCap = ArgumentCaptor.forClass(Balance.class);
        verify(sqliteDao).persistBalance(newBalanceCap.capture());

        Balance balanceRow = newBalanceCap.getValue();
        assertEquals(accountId, balanceRow.getAccountId());
        assertEquals(110L, balanceRow.getBalance());
    }

    @Test
    void deposit_invalidToken() {
        // given
        Integer accountId = 1;
        UUID token = UUID.randomUUID();
        String tokenStr = token.toString();

        when(sqliteDao.getSession(tokenStr))
                .thenReturn(Optional.empty());

        when(sqliteDao.getBalance(accountId))
                .thenReturn(Optional.of(Balance.builder()
                        .balance(100L)
                        .accountId(accountId)
                        .build()));

        // when
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            underTest.deposit(tokenStr, 10L);
        });

        Assertions.assertEquals("invalid token", thrown.getMessage());

        // verify
        verify(sqliteDao).getSession(tokenStr);
        verify(sqliteDao, never()).getBalance(accountId);
        verify(sqliteDao, never()).persistBalance(any(Balance.class));
    }

    @Test
    void deposit_expiredToken() {
        // given
        Integer accountId = 1;
        UUID token = UUID.randomUUID();
        String tokenStr = token.toString();
        Timestamp expiryTime = Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES));

        when(sqliteDao.getSession(tokenStr))
                .thenReturn(Optional.of(Session.builder()
                        .token(token)
                        .accountId(accountId)
                        .expires(expiryTime)
                        .build()));

        when(sqliteDao.getBalance(accountId))
                .thenReturn(Optional.of(Balance.builder()
                        .balance(100L)
                        .accountId(accountId)
                        .build()));

        // when
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            underTest.deposit(tokenStr, 10L);
        });

        Assertions.assertEquals("invalid token", thrown.getMessage());

        // verify
        verify(sqliteDao).getSession(tokenStr);
        verify(sqliteDao, never()).getBalance(accountId);
        verify(sqliteDao, never()).persistBalance(any(Balance.class));
    }

    @Test
    void withdraw_sunnyday() {
        // given
        Integer accountId = 1;
        UUID token = UUID.randomUUID();
        String tokenStr = token.toString();
        Timestamp expiryTime = Timestamp.from(Instant.now().plus(1, ChronoUnit.MINUTES));

        when(sqliteDao.getSession(tokenStr))
                .thenReturn(Optional.of(Session.builder()
                        .token(token)
                        .accountId(accountId)
                        .expires(expiryTime)
                        .build()));

        when(sqliteDao.getBalance(accountId))
                .thenReturn(Optional.of(Balance.builder()
                        .balance(100L)
                        .accountId(accountId)
                        .build()));

        // when
        boolean success = underTest.withdraw(tokenStr, 10L);

        // verify
        assertTrue(success, "withdraw success");

        verify(sqliteDao).getSession(tokenStr);
        verify(sqliteDao).getBalance(accountId);

        ArgumentCaptor<Balance> newBalanceCap = ArgumentCaptor.forClass(Balance.class);
        verify(sqliteDao).persistBalance(newBalanceCap.capture());

        Balance balanceRow = newBalanceCap.getValue();
        assertEquals(accountId, balanceRow.getAccountId());
        assertEquals(90L, balanceRow.getBalance());
    }

    @Test
    void withdraw_invalidToken() {
        // given
        Integer accountId = 1;
        UUID token = UUID.randomUUID();
        String tokenStr = token.toString();

        when(sqliteDao.getSession(tokenStr))
                .thenReturn(Optional.empty());

        when(sqliteDao.getBalance(accountId))
                .thenReturn(Optional.of(Balance.builder()
                        .balance(100L)
                        .accountId(accountId)
                        .build()));

        // when
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            underTest.withdraw(tokenStr, 10L);
        });

        Assertions.assertEquals("invalid token", thrown.getMessage());

        // verify
        verify(sqliteDao).getSession(tokenStr);
        verify(sqliteDao, never()).getBalance(accountId);
        verify(sqliteDao, never()).persistBalance(any(Balance.class));
    }

    @Test
    void withdraw_expiredToken() {
        // given
        Integer accountId = 1;
        UUID token = UUID.randomUUID();
        String tokenStr = token.toString();
        Timestamp expiryTime = Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES));

        when(sqliteDao.getSession(tokenStr))
                .thenReturn(Optional.of(Session.builder()
                        .token(token)
                        .accountId(accountId)
                        .expires(expiryTime)
                        .build()));

        when(sqliteDao.getBalance(accountId))
                .thenReturn(Optional.of(Balance.builder()
                        .balance(100L)
                        .accountId(accountId)
                        .build()));

        // when
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            underTest.withdraw(tokenStr, 10L);
        });

        Assertions.assertEquals("invalid token", thrown.getMessage());

        // verify
        verify(sqliteDao).getSession(tokenStr);
        verify(sqliteDao, never()).getBalance(accountId);
        verify(sqliteDao, never()).persistBalance(any(Balance.class));
    }
}
