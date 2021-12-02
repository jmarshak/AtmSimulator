package sandbox.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SessionTest {

    @Test
    void validateSession() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);

        UUID token = UUID.randomUUID();
        Timestamp expiryTime = Timestamp.from(Instant.now().plus(1, ChronoUnit.MINUTES));

        Session session = Session.builder()
                .accountId(1)
                .token(token)
                .expires(expiryTime)
                .build();

        session.setInsertValues(stmt);

        verify(stmt).setInt(1, 1);
        verify(stmt).setString(2, token.toString());
        verify(stmt).setTimestamp(3, expiryTime);
    }

    @Test
    void validateSession_missingAccountId() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);

        UUID token = UUID.randomUUID();
        Timestamp expiryTime = Timestamp.from(Instant.now().plus(1, ChronoUnit.MINUTES));

        Session session = Session.builder()
                .token(token)
                .expires(expiryTime)
                .build();


        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            session.setInsertValues(stmt);
        });

        Assertions.assertEquals("accountId is required", thrown.getMessage());
    }

    @Test
    void validateSession_missingToken() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);

        Timestamp expiryTime = Timestamp.from(Instant.now().plus(1, ChronoUnit.MINUTES));

        Session session = Session.builder()
                .accountId(1)
                .expires(expiryTime)
                .build();


        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            session.setInsertValues(stmt);
        });

        Assertions.assertEquals("token is required", thrown.getMessage());
    }

    @Test
    void validateSession_missingExpires() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);

        Session session = Session.builder()
                .accountId(1)
                .token(UUID.randomUUID())
                .build();


        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            session.setInsertValues(stmt);
        });

        Assertions.assertEquals("expires is required", thrown.getMessage());
    }
    @Test
    void validateSession_expiresInThePast() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);

        UUID token = UUID.randomUUID();
        Timestamp expiryTime = Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES));

        Session session = Session.builder()
                .accountId(1)
                .token(token)
                .expires(expiryTime)
                .build();


        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            session.setInsertValues(stmt);
        });

        Assertions.assertEquals("session has already expired", thrown.getMessage());
    }


}
