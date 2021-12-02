package sandbox.model;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * represents an account in the atm system, used primarily for identification and uniqueness among
 * the balance and session tables.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Slf4j
public class Account implements SqliteTable {
    private Integer id;
    private String username;
    private String pin;

    public static String accountTableCreation =
            "CREATE TABLE IF NOT EXISTS account " +
                    "(id            INTEGER PRIMARY KEY    AUTOINCREMENT," +
                    "username       TEXT    NOT NULL       UNIQUE, " +
                    "pin            CHAR(4) NOT NULL) ";

    public static Account mapAccountFromResultSet(@NonNull ResultSet resultSet) {
        try {
            if (!resultSet.next()) {
                log.debug("no results found");
                return null;
            }
            return Account.builder()
                    .id(resultSet.getInt("id"))
                    .username(resultSet.getString("username"))
                    .pin(resultSet.getString("pin"))
                    .build();
        } catch (SQLException e) {
            log.error("could not create Account from query results", e);
            return null;
        }
    }

    @Override
    public String getInsertSqlStatement() {
        return "INSERT INTO account (username, pin) VALUES (?, ?)";
    }

    @Override
    public void setInsertValues(PreparedStatement stmt) throws SQLException {
        if (StringUtils.isAllBlank(username)) {
            throw new IllegalArgumentException("username cannot be blank or empty");
        }

        stmt.setString(1, username);

        // validate the pin 4 characters
        Integer pinLength = Optional.ofNullable(pin).map(String::length).orElse(0);
        if (pinLength != 4) {
            throw new IllegalArgumentException("pin must be exactly 4 digits");
        }

        // valid the pin all numerical
        if (!StringUtils.isNumeric(pin)){
            throw new IllegalArgumentException("pin must be numerical");
        }

        stmt.setString(2, pin);
    }

    @Override
    public String getSelectSqlStatement() {
        return "SELECT * FROM account WHERE username = ? AND pin = ? LIMIT 1";
    }

    @Override
    public void setSelectValues(PreparedStatement stmt) throws SQLException {
        stmt.setString(1, username);
        stmt.setString(2, pin);
    }
}
