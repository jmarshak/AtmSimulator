package sandbox.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BalanceTest {

    @Test
    void validateBalance() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);

        Balance balance = Balance.builder()
                .accountId(1)
                .balance(1234L)
                .build();

        balance.setInsertValues(stmt);

        verify(stmt).setInt(1, 1);
        verify(stmt).setLong(2, 1234L);
    }

    @Test
    void validateBalance_missingAccountId() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);

        Balance balance = Balance.builder()
                .balance(1234L)
                .build();


        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            balance.setInsertValues(stmt);
        });

        Assertions.assertEquals("accountId is required", thrown.getMessage());
    }

    @Test
    void validateBalance_missingBalanceField() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);

        Balance balance = Balance.builder()
                .accountId(1)
                .build();


        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            balance.setInsertValues(stmt);
        });

        Assertions.assertEquals("balance field is required", thrown.getMessage());
    }

}
