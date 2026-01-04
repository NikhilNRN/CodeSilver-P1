package managerAuthentication;

import com.revature.repository.User;
import com.revature.repository.UserRepository;
import com.revature.repository.DatabaseConnection;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Epic("Manager Authentication Repository")
@Feature("UserRepository Tests")
@ExtendWith(MockitoExtension.class)
public class TestUserRepo {

    @Mock
    private DatabaseConnection databaseConnection;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private UserRepository userRepository;

    @BeforeEach
    void setup() throws SQLException {
        databaseConnection = Mockito.mock(DatabaseConnection.class);
        connection = Mockito.mock(Connection.class);
        when(databaseConnection.getConnection()).thenReturn(connection);
        userRepository = new UserRepository(databaseConnection);
    }

    // ============================
    // Find by ID - Success
    // ============================
    @Test
    @Story("Find user by ID")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("C6_09: findById - user found")
    @Description("Tests that findById returns a valid user when the ID exists in database")
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

    // ============================
    // Find by ID - Not Found
    // ============================
    @Test
    @Story("Find user by ID")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("C6_10: findById - user not found")
    void findById_userNotFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<User> result = userRepository.findById(99);

        assertTrue(result.isEmpty());
    }

    // ============================
    // Find by ID - SQLException
    // ============================
    @Test
    @Story("Find user by ID")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("C6_11: findById - SQL exception")
    void findById_sqlException_throwsRuntimeException() throws SQLException {
        when(connection.prepareStatement(anyString()))
                .thenThrow(new SQLException("DB error"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userRepository.findById(1)
        );

        assertTrue(exception.getMessage().contains("Error finding user by ID"));
    }

    // ============================
    // Find by Username - Success
    // ============================
    @Test
    @Story("Find user by username")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("C6_12: findByUsername - user found")
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

    // ============================
    // Find by Username - Not Found
    // ============================
    @Test
    @Story("Find user by username")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("C6_13: findByUsername - user not found")
    void findByUsername_userNotFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<User> result = userRepository.findByUsername("unknown");

        assertTrue(result.isEmpty());
    }

    // ============================
    // Find by Username - SQLException
    // ============================
    @Test
    @Story("Find user by username")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("C6_14: findByUsername - SQL exception")
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
