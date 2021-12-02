package sandbox;

import lombok.extern.slf4j.Slf4j;
import sandbox.model.Account;
import sandbox.model.Balance;
import sandbox.model.Session;
import sandbox.model.SqliteTable;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
/**
 * Data access layer for retrieving and persisting Account, Session, and Balance information from a sqlite db
 */
public class SqliteDao implements AutoCloseable {

    private final Connection sqliteConnection;

    public SqliteDao() {
        sqliteConnection = initializeDBConnection("jdbc:sqlite:atm.db");
        formatTables();
    }

    public SqliteDao(String connectionUrl) {
        sqliteConnection = initializeDBConnection(connectionUrl);
        formatTables();
    }

    @Override
    public void close() throws SQLException {
        if (sqliteConnection != null) {
            sqliteConnection.close();
        }
    }

    private Connection initializeDBConnection(String connectionUrl) {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection c = DriverManager.getConnection(connectionUrl);
            log.debug("Opened database successfully");
            return c;
        } catch (Exception e) {
            log.error("caught exception connecting to sqlite db", e);
            throw new RuntimeException(e);
        }
    }

    private void executeUpdate(String sql) {
        try (Statement stmt = sqliteConnection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            log.error("caught exception running update statement. sql={}", sql, e);
            throw new RuntimeException(e);
        }
    }


    private void executePreparedStatement(String sql, SqliteTable sqliteTable) {
        try (PreparedStatement stmt = sqliteConnection.prepareStatement(sql)) {
            sqliteTable.setInsertValues(stmt);
            stmt.execute();
        } catch (SQLException e) {
            log.error("caught exception running prepared statement. sql={}", sql, e);
            throw new RuntimeException(e);
        }
    }

    private <T> Optional<T> executePreparedStatement(String sql, SqliteTable sqliteTable, Function<ResultSet, T> resultsMapper) {
        try (PreparedStatement stmt = sqliteConnection.prepareStatement(sql)) {
            sqliteTable.setSelectValues(stmt);
            return Optional.ofNullable(stmt.executeQuery())
                    .map(resultsMapper);
        } catch (Exception e) {
            log.error("caught exception running update statement. sql={}", sql, e);
            return Optional.empty();
        }
    }

    public void formatTables() {
        executeUpdate(Account.accountTableCreation);
        executeUpdate(Session.sessionTableCreation);
        executeUpdate(Balance.balanceTableCreation);
    }

    public void createAccount(String username, String pin) {
        Account newAccount = Account.builder()
                .username(username)
                .pin(pin)
                .build();

        executePreparedStatement(newAccount.getInsertSqlStatement(), newAccount);
    }

    // Account logic
    public Optional<Account> getAccount(String username, String pin) {
        Account account = Account.builder()
                .username(username)
                .pin(pin)
                .build();
        return executePreparedStatement(
                account.getSelectSqlStatement(),
                account,
                Account::mapAccountFromResultSet);
    }

    // Session logic
    public Optional<Session> getSession(Integer accountId) {
        Session session = Session.builder()
                .accountId(accountId)
                .build();

        return executePreparedStatement(
                session.getSelectSqlStatement(),
                session,
                Session::mapAccountFromResultSet);
    }

    public Optional<Session> getSession(String token) {
        Session session = Session.builder()
                .token(UUID.fromString(token))
                .build();

        return executePreparedStatement(
                session.getSelectSqlStatement(),
                session,
                Session::mapAccountFromResultSet);
    }

    public void persistSession(Integer accountId, UUID token, Timestamp expiryTime) {
        Session newSession = Session.builder()
                .accountId(accountId)
                .token(token)
                .expires(expiryTime)
                .build();

        executePreparedStatement(newSession.getInsertSqlStatement(), newSession);
    }

    // Balance logic
    public Optional<Balance> getBalance(Integer accountId) {
        Balance balance = Balance.builder()
                .accountId(accountId)
                .build();

        return executePreparedStatement(
                balance.getSelectSqlStatement(),
                balance,
                Balance::mapAccountFromResultSet);

    }

    public void persistBalance(Balance balance) {
        executePreparedStatement(balance.getInsertSqlStatement(), balance);
    }
}
