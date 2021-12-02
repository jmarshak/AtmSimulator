package sandbox;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import sandbox.model.Account;
import sandbox.model.Balance;
import sandbox.model.Session;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SqliteDaoTest {
    SqliteDao underTest = new SqliteDao("jdbc:sqlite:atm-test.db");
    private String username;
    private String pin;
    private Integer accountId;
    private UUID token;
    private Timestamp expiryTime;

    @BeforeAll
    void setup() {
        underTest.formatTables();
        username = UUID.randomUUID().toString();
        pin = "1234";
        underTest.createAccount(username, pin);
        Optional<Account> accountOpt = underTest.getAccount(username, pin);
        accountId = accountOpt.get().getId();

        token = UUID.randomUUID();
        expiryTime = Timestamp.from(Instant.now().plus(1, ChronoUnit.MINUTES));
        expiryTime.setNanos(0);
        underTest.persistSession(accountId, token, expiryTime);

        underTest.persistBalance(Balance.builder()
                .accountId(accountId)
                .balance(100L)
                .id(1)
                .build());
    }

    @Test
    void getAccount_sunnyDay() {
        // when
        Optional<Account> accountOpt = underTest.getAccount(username, pin);

        // verify
        assertTrue(accountOpt.isPresent(), "account was found");
        Account account = accountOpt.get();

        assertEquals(username, account.getUsername(), "username matches");
        assertEquals(pin, account.getPin(), "pin matches");
        assertEquals(accountId, account.getId(), "row is expected");
    }

    @Test
    void noAccount() {
        // when
        Optional<Account> accountOpt = underTest.getAccount("unknown", pin);

        // verify
        assertFalse(accountOpt.isPresent(), "account was not found");
    }

    @Test
    void getSessionByToken_sunnyDay() {
        // when
        Optional<Session> sessionOpt = underTest.getSession(token.toString());

        // verify
        assertTrue(sessionOpt.isPresent(), "session was found");
        Session session = sessionOpt.get();

        assertEquals(accountId, session.getAccountId(), "accountId matches");
        assertEquals(token, session.getToken(), "token matches");
        assertEquals(expiryTime, session.getExpires(), "expires matches");
    }

    @Test
    void getSessionByToken_noFound() {
        // when
        Optional<Session> sessionOpt = underTest.getSession(UUID.randomUUID().toString());

        // verify
        assertFalse(sessionOpt.isPresent(), "session was not found");
    }

    @Test
    void getSessionByAccountId_sunnyDay() {
        // when
        Optional<Session> sessionOpt = underTest.getSession(accountId);

        // verify
        assertTrue(sessionOpt.isPresent(), "session was found");
        Session session = sessionOpt.get();

        assertEquals(accountId, session.getAccountId(), "accountId matches");
        assertEquals(token, session.getToken(), "token matches");
        assertEquals(expiryTime, session.getExpires(), "expires matches");
    }

    @Test
    void getSessionByAccountId_noFound() {
        // when
        Optional<Session> sessionOpt = underTest.getSession(-1);

        // verify
        assertFalse(sessionOpt.isPresent(), "session was not found");
    }

    @Test
    void getBalance_sunnyDay() {
        // when
        Optional<Balance> balanceOpt = underTest.getBalance(accountId);

        // verify
        assertTrue(balanceOpt.isPresent(), "balance was found");
        Balance balance = balanceOpt.get();

        assertEquals(accountId, balance.getAccountId(), "accountId matches");
        assertEquals(100, balance.getBalance(), "balance matches");
    }


    @Test
    void getBalance_noFound() {
        // when
        Optional<Balance> balanceOpt = underTest.getBalance(-1);

        // verify
        assertFalse(balanceOpt.isPresent(), "balancee" +
                " was not found");
    }
}
