package managerAuthentication;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.revature.repository.UserRepository;
import com.revature.repository.User;
import com.revature.repository.DatabaseConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


public class TestUserRepo {
    @Mock
    private DatabaseConnection databaseConnection;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);

    @Mock
    private ResultSet resultSet = Mockito.mock(ResultSet.class);

    @InjectMocks
    private UserRepository userRepository;

    @BeforeEach
    void setup() throws SQLException {
        databaseConnection = Mockito.mock(DatabaseConnection.class);
        connection = Mockito.mock(Connection.class);

//        databaseConnection = new DatabaseConnection();
        when(databaseConnection.getConnection()).thenReturn(connection);
        userRepository = new UserRepository(databaseConnection);
    }

    /* =========================
       findById Tests
       ========================= */

    @Test
    void findById_userFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getString("username")).thenReturn("john");
        when(resultSet.getString("password")).thenReturn("pass");
        when(resultSet.getString("role")).thenReturn("USER");

        Optional<User> result = userRepository.findById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getId());
        assertEquals("john", result.get().getUsername());
        assertEquals("pass", result.get().getPassword());
        assertEquals("USER", result.get().getRole());

        verify(preparedStatement).setInt(1, 1);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void findById_userNotFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<User> result = userRepository.findById(99);

        assertTrue(result.isEmpty());
    }

    @Test
    void findById_sqlException_throwsRuntimeException() throws SQLException {
        when(connection.prepareStatement(anyString()))
                .thenThrow(new SQLException("DB error"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userRepository.findById(1)
        );

        assertTrue(exception.getMessage().contains("Error finding user by ID"));
    }

    /* =========================
       findByUsername Tests
       ========================= */

    @Test
    void findByUsername_userFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("id")).thenReturn(2);
        when(resultSet.getString("username")).thenReturn("alice");
        when(resultSet.getString("password")).thenReturn("secret");
        when(resultSet.getString("role")).thenReturn("ADMIN");

        Optional<User> result = userRepository.findByUsername("alice");

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
        assertEquals("ADMIN", result.get().getRole());

        verify(preparedStatement).setString(1, "alice");
        verify(preparedStatement).executeQuery();
    }

    @Test
    void findByUsername_userNotFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<User> result = userRepository.findByUsername("unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByUsername_sqlException_throwsRuntimeException() throws SQLException {
        when(connection.prepareStatement(anyString()))
                .thenThrow(new SQLException("DB error"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userRepository.findByUsername("john")
        );

        assertTrue(exception.getMessage().contains("Error finding user by username"));
    }
}
