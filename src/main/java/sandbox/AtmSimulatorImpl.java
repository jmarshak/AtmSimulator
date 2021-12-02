package sandbox;

import lombok.extern.slf4j.Slf4j;
import sandbox.model.Account;
import sandbox.model.Balance;
import sandbox.model.Session;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * An implementation of the ATM simulator, using the sqlite db as the storage mechanism
 */
@Slf4j
public class AtmSimulatorImpl implements AtmSimulator, AutoCloseable {

    private final SqliteDao sqliteDao;

    public AtmSimulatorImpl() {
        sqliteDao = new SqliteDao();
        sqliteDao.formatTables();
    }

    public AtmSimulatorImpl(SqliteDao sqliteDao) {
        this.sqliteDao = sqliteDao;
        this.sqliteDao.formatTables();
    }

    @Override
    public void close() throws Exception {
        if (sqliteDao != null) {
            sqliteDao.close();
        }
    }

    @Override
    public Optional<String> login(String username, String pin) {
        Optional<Account> accountOpt = sqliteDao.getAccount(username, pin);
        if (!accountOpt.isPresent()) {
            log.info("not account exists for username={}", username);
            return Optional.empty();
        }

        int accountId = accountOpt.get().getId();
        Optional<Session> sessionOpt = sqliteDao.getSession(accountId);

        boolean isSessionActive = sessionOpt
                .map(Session::getExpires)
                .map(expiryTime -> expiryTime.toInstant().isAfter(Instant.now()))
                .orElse(false);
        if (isSessionActive) {
            log.debug("found active token, for accountId={}", accountId);
            return sessionOpt
                    .map(Session::getToken)
                    .map(UUID::toString);
        }

        log.info("no active session found for accountId={}, creating new one", accountId);
        UUID token = UUID.randomUUID();
        sqliteDao.persistSession(accountId, token, Timestamp.from(Instant.now().plus(10, ChronoUnit.MINUTES)));
        return Optional.of(token.toString());
    }


    @Override
    public long viewBalance(String token) {
        Integer accountId = getActiveAccountIdFromSessionToken(token)
                .orElseThrow(() -> new RuntimeException("invalid token"));

        Optional<Balance> balanceRow = sqliteDao.getBalance(accountId);

        return balanceRow
                .map(Balance::getBalance)
                .orElse(0L);
    }

    @Override
    public boolean deposit(String token, long amount) {
        Integer accountId = getActiveAccountIdFromSessionToken(token)
                .orElseThrow(() -> new RuntimeException("invalid token"));

        Optional<Balance> balanceRow = sqliteDao.getBalance(accountId);

        long currentBalance = balanceRow
                .map(Balance::getBalance)
                .orElse(0L);

        long newBalanceAmount = currentBalance + amount;
        persistBalance(accountId, balanceRow, newBalanceAmount);
        log.info("your new balance is {}", newBalanceAmount);
        return true;
    }

    @Override
    public boolean withdraw(String token, long amount) {
        Integer accountId = getActiveAccountIdFromSessionToken(token)
                .orElseThrow(() -> new RuntimeException("invalid token"));

        Optional<Balance> balanceRow = sqliteDao.getBalance(accountId);

        long currentBalance = balanceRow
                .map(Balance::getBalance)
                .orElse(0L);

        // todo overdraft protection?
        long newBalanceAmount = currentBalance - amount;

        persistBalance(accountId, balanceRow, newBalanceAmount);
        log.info("your new balance is {}", newBalanceAmount);
        return true;
    }

    // try to get an active session, validating the expiration time.
    // returns the accountId for later use, if the token is valid
    private Optional<Integer> getActiveAccountIdFromSessionToken(String token) {
        Optional<Session> sessionOpt = sqliteDao.getSession(token);

        boolean isSessionActive = sessionOpt
                .map(Session::getExpires)
                .map(expiryTime -> expiryTime.toInstant().isAfter(Instant.now()))
                .orElse(false);

        if (!isSessionActive) {
            log.info("session was not found, or was inactive, not processing deposit");
            return Optional.empty();
        }

        return sessionOpt.map(Session::getAccountId);
    }

    // reuse the existing balance row (to keep the ids the same) or make a new one if none was present
    private void persistBalance(Integer accountId, Optional<Balance> existingBalanceRow, long newBalanceAmount) {
        Balance newBalanceRow = existingBalanceRow.map(b -> {
                    b.setBalance(newBalanceAmount);
                    return b;
                })
                .orElseGet(() -> Balance.builder()
                        .balance(newBalanceAmount)
                        .accountId(accountId)
                        .build());

        sqliteDao.persistBalance(newBalanceRow);
    }
}
