package sandbox.model;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Keep track of balances for all accounts.
 * Does not track individual transactions, it was not a requirement of the challenge
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Slf4j
public class Balance implements SqliteTable {
    private Integer id;
    private Integer accountId;
    private Long balance;

    public static String balanceTableCreation = "CREATE TABLE IF NOT EXISTS balance " +
            "(id              INTEGER PRIMARY KEY    AUTOINCREMENT, " +
            "account_id       INT     NOT NULL       UNIQUE, " + // must be unique to enforce one balance row per account
            "balance          LONG    NOT NULL, " +
            "FOREIGN KEY(account_id) REFERENCES account(id))";

    public static Balance mapAccountFromResultSet(@NonNull ResultSet resultSet) {
        try {
            if (!resultSet.next()) {
                log.debug("no results found");
                return null;
            }
            return Balance.builder()
                    .id(resultSet.getInt("id"))
                    .accountId(resultSet.getInt("account_id"))
                    .balance(resultSet.getLong("balance"))
                    .build();
        } catch (SQLException e) {
            log.error("could not create Account from query results", e);
            return null;
        }
    }

    @Override
    public String getInsertSqlStatement() {
        return "INSERT INTO balance (account_id, balance) VALUES (?, ?)" +
                " ON CONFLICT(account_id) DO UPDATE SET balance=excluded.balance;";
    }

    @Override
    public void setInsertValues(PreparedStatement stmt) throws SQLException {
        if (Objects.isNull(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        stmt.setInt(1, accountId);

        if (Objects.isNull(balance)) {
            throw new IllegalArgumentException("balance field is required");
        }
        stmt.setLong(2, balance);
    }

    public String getSelectSqlStatement() {
        return "SELECT * FROM balance WHERE account_id = ? LIMIT 1";
    }

    public void setSelectValues(PreparedStatement stmt) throws SQLException {
        stmt.setInt(1, accountId);
    }
}
