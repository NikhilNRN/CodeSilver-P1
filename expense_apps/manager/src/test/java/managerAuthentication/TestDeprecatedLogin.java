package managerAuthentication;

import com.revature.repository.DatabaseConnection;
import com.revature.repository.User;
import com.revature.repository.UserRepository;
import com.revature.service.AuthenticationService;
import io.qameta.allure.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@Epic("Deprecated Manager Authentication")
@Feature("Legacy authentication methods")
@ExtendWith(MockitoExtension.class)
public class TestDeprecatedLogin {

    @Mock
    DatabaseConnection conn;

    @Mock
    UserRepository mockRep;

    @InjectMocks
    @Spy
    AuthenticationService authServ;

    @Mock
    User mockU;

    // ==========================
    // validateAuthentication tests
    // ==========================
    @Test
    @Story("Legacy authentication positive path")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Valid Authorization header with numeric user ID should call repository and return user")
    @Step("Test deprecated authentication positive scenario")
    public void testDeprecatedAuthenticatePositive() {
        String header = "Bearer 12";

        int userId = Integer.parseInt(header.substring("Bearer ".length()));

        when(mockRep.findById(userId)).thenReturn(Optional.of(mockU));

        authServ.validateAuthentication(header);

        verify(mockRep, times(1)).findById(userId);
    }

    @Test
    @Story("Legacy authentication malformed header")
    @Severity(SeverityLevel.NORMAL)
    @Description("Malformed user ID should not call repository and return empty")
    @Step("Test deprecated authentication with malformed ID")
    public void testDeprecatedAuthenticateMalformedID() {
        String header = "Bearer A";

        authServ.validateAuthentication(header);

        verify(mockRep, times(0)).findById(anyInt());
    }

    // ==========================
    // validateManagerAuthenticationLegacy tests
    // ==========================
    @Test
    @Story("Legacy manager authentication positive")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Header is validated and isManager should be called on returned user")
    @Step("Test deprecated manager authentication positive scenario")
    public void testDeprecatedManagerPositive() {
        String header = "Header 12";

        doReturn(Optional.of(mockU)).when(authServ).validateAuthentication(header);

        authServ.validateManagerAuthenticationLegacy(header);

        verify(authServ, times(1)).isManager(mockU);
    }

    @Test
    @Story("Legacy manager authentication negative")
    @Severity(SeverityLevel.NORMAL)
    @Description("Header invalid or user not found should skip isManager check")
    @Step("Test deprecated manager authentication negative scenario")
    public void testDeprecatedManagerNegative() {
        String header = "Header A";

        doReturn(Optional.empty()).when(authServ).validateAuthentication(header);

        authServ.validateManagerAuthenticationLegacy(header);

        verify(authServ, times(0)).isManager(mockU);
    }
}
