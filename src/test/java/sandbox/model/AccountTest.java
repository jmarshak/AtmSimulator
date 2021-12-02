package sandbox.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AccountTest {

    @Test
    void validateAccount() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);

        Account account = Account.builder()
                .username("tester")
                .pin("1234")
                .build();

        account.setInsertValues(stmt);

        verify(stmt).setString(1, "tester");
        verify(stmt).setString(2, "1234");
    }

    @Test
    void validateAccount_pinAllZeros() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);

        Account account = Account.builder()
                .username("tester")
                .pin("0000")
                .build();

        account.setInsertValues(stmt);

        verify(stmt).setString(1, "tester");
        verify(stmt).setString(2, "0000");
    }

    @Test
    void validateAccount_pinToShort() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);

        Account account = Account.builder()
                .username("tester")
                .pin("12")
                .build();


        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            account.setInsertValues(stmt);
        });

        Assertions.assertEquals("pin must be exactly 4 digits", thrown.getMessage());
    }

    @Test
    void validateAccount_pinNotNumerical() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);

        Account account = Account.builder()
                .username("tester")
                .pin("pins")
                .build();


        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            account.setInsertValues(stmt);
        });

        Assertions.assertEquals("pin must be numerical", thrown.getMessage());
    }

    @Test
    void validateAccount_usernameBlank() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);

        Account account = Account.builder()
                .username("  ")
                .pin("0000")
                .build();


        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            account.setInsertValues(stmt);
        });

        Assertions.assertEquals("username cannot be blank or empty", thrown.getMessage());
    }

}
