package managerAuthentication;

import com.revature.repository.User;
import com.revature.repository.UserRepository;
import com.revature.service.AuthenticationService;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@Epic("Manager Authentication Service")
@Feature("AuthenticationService Layer Tests")
@ExtendWith(MockitoExtension.class)
public class TestLoginServiceLayer {

    private User expectedUser;
    private String uname;
    private String pword;

    @Mock
    private User mockUser;

    @Mock
    private UserRepository mockUserRep;

    @Mock
    private AuthenticationService mockAuth;

    @InjectMocks
    @Spy
    private AuthenticationService testAuth;

    @BeforeEach
    public void setUpUser() {
        uname = "test";
        pword = "pword";
        expectedUser = new User(1, uname, pword, "manager");
    }

    // ============================
    // Positive Manager Authentication
    // ============================
    @Test
    @Story("Authenticate a valid manager")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("C6_01: authenticateManager - positive")
    @Description("Tests that authenticateManager succeeds when valid username and password are provided and user is a manager")
    void test_authenticateManager_positive() {
        doReturn(Optional.of(expectedUser)).when(testAuth).authenticateUser(uname, pword);
        doReturn(true).when(testAuth).isManager(expectedUser);

        testAuth.authenticateManager(uname, pword);

        verify(testAuth, times(1)).authenticateUser(uname, pword);
        verify(testAuth, times(1)).isManager(expectedUser);
    }

    // ============================
    // Negative Manager Authentication
    // ============================
    @Test
    @Story("Fail to authenticate when user not found")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("C6_02: authenticateManager - negative")
    @Description("Tests that authenticateManager short-circuits when the user is not found")
    void test_authenticateManager_negative() {
        doReturn(Optional.empty()).when(testAuth).authenticateUser(uname, pword);

        testAuth.authenticateManager(uname, pword);

        verify(testAuth, times(1)).authenticateUser(uname, pword);
        verify(testAuth, times(0)).isManager(expectedUser);
    }

    // ============================
    // Valid User Authentication
    // ============================
    @Test
    @Story("Authenticate user by username/password")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("C6_03: authenticateUser - valid")
    @Description("Tests that authenticateUser calls UserRepository to fetch user by username")
    void test_userAuth_valid() {
        doReturn(Optional.of(expectedUser)).when(mockUserRep).findByUsername(uname);

        testAuth.authenticateUser(uname, pword);

        verify(mockUserRep, times(1)).findByUsername(uname);
    }

    // ============================
    // JWT Token Creation
    // ============================
    @Test
    @Story("JWT token creation")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("C6_04: createJwtToken - valid")
    @Description("Tests that createJwtToken reads user details and generates token")
    void test_jwtCreation_valid() {
        doReturn(1).when(mockUser).getId();
        doReturn("uname").when(mockUser).getUsername();
        doReturn("user").when(mockUser).getRole();

        testAuth.createJwtToken(mockUser);

        verify(mockUser, times(1)).getId();
        verify(mockUser, times(1)).getUsername();
        verify(mockUser, times(1)).getRole();
    }

    // ============================
    // Check Manager Role
    // ============================
    @Test
    @Story("Check if user is a manager")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("C6_05: isManager - valid")
    @Description("Tests that isManager calls the user's isManager method")
    void test_isManager_valid() {
        doReturn(true).when(mockUser).isManager();

        testAuth.isManager(mockUser);

        verify(mockUser, times(1)).isManager();
    }
}
