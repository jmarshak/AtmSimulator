package sandbox.model;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A user session, each row records the token of the session with an expiration time for the session
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Slf4j
public class Session implements SqliteTable {
    private Integer id;
    private Integer accountId;
    private UUID token;
    private Timestamp expires;

    public static String sessionTableCreation =
            "CREATE TABLE IF NOT EXISTS session " +
                    "(id              INTEGER PRIMARY KEY    AUTOINCREMENT," +
                    "account_id       INT      NOT NULL      UNIQUE, " +
                    "token            UUID     NOT NULL      UNIQUE, " +
                    "expires          datetime NOT NULL, " +
                    "FOREIGN KEY(account_id) REFERENCES account(id))";

    public static Session mapAccountFromResultSet(@NonNull ResultSet resultSet) {
        try {
            if (!resultSet.next()) {
                log.debug("no results found");
                return null;
            }
            return Session.builder()
                    .id(resultSet.getInt("id"))
                    .accountId(resultSet.getInt("account_id"))
                    .token(UUID.fromString(resultSet.getString("token")))
                    .expires(resultSet.getTimestamp("expires"))
                    .build();
        } catch (SQLException e) {
            log.error("could not create Account from query results", e);
            return null;
        }
    }

    @Override
    public String getInsertSqlStatement() {
        return "INSERT INTO session (account_id, token, expires) VALUES (?, ?, ?) " +
                "ON CONFLICT(id) DO UPDATE SET token=excluded.token, expires=excluded.expires;";
    }

    @Override
    public void setInsertValues(PreparedStatement stmt) throws SQLException {
        if (Objects.isNull(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        stmt.setInt(1, accountId);

        if (Objects.isNull(token)) {
            throw new IllegalArgumentException("token is required");
        }
        stmt.setString(2, token.toString());

        if (Objects.isNull(expires)) {
            throw new IllegalArgumentException("expires is required");
        }

        if (expires.toInstant().isBefore(Instant.now())) {
            throw new IllegalArgumentException("session has already expired");
        }
        stmt.setTimestamp(3, expires);
    }

    public String getSelectSqlStatement() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM session WHERE ");
        Optional.ofNullable(accountId)
                .ifPresent(a -> sb.append("account_id = ?"));
        Optional.ofNullable(token)
                .ifPresent(a -> sb.append("token = ?"));
        sb.append(" LIMIT 1 ");
        return sb.toString();
    }

    public void setSelectValues(PreparedStatement stmt) throws SQLException {
        int index = 0;
        if (accountId != null) {
            stmt.setInt(++index, accountId);
        }
        if (token != null) {
            stmt.setString(++index, token.toString());
        }

    }
}
