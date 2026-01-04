package managerAuthentication;

import com.revature.repository.User;
import com.revature.api.AuthenticationMiddleware;
import com.revature.service.AuthenticationService;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Epic("Manager API Authentication")
@Feature("Middleware validation for manager access")
@ExtendWith(MockitoExtension.class)
public class TestLoginAPILayer {

    @Mock
    private User mockUser;

    @Mock
    private Context ctx;

    private Handler handler;

    @Mock
    private AuthenticationService mockAuth;

    @BeforeEach
    public void setupHandler() {
        handler = new AuthenticationMiddleware(mockAuth).validateManager();
    }

    // ============================
    // Positive Path: Valid Manager
    // ============================
    @Test
    @Story("Valid manager JWT allows access")
    @Severity(SeverityLevel.CRITICAL)
    @Description("If the JWT cookie is valid and corresponds to a manager, the middleware should attach the user to the context")
    @Step("Test middleware validates manager JWT successfully")
    void test_validateManagerPositive() throws Exception {
        String jwt = "mock-jwt-token";

        when(ctx.cookie("jwt")).thenReturn(jwt);
        when(mockAuth.validateManagerAuthentication(jwt))
                .thenReturn(Optional.of(mockUser));

        handler.handle(ctx);

        verify(ctx).attribute("manager", mockUser);
    }

    // ============================
    // Negative Path: User Not Manager
    // ============================
    @Test
    @Story("Invalid manager JWT should throw Unauthorized")
    @Severity(SeverityLevel.NORMAL)
    @Description("If the JWT is valid but user is not a manager, an UnauthorizedResponse should be thrown")
    @Step("Test middleware rejects non-manager JWT")
    void test_validateUserBad() throws Exception {
        String jwt = "funny_token";

        when(ctx.cookie("jwt")).thenReturn(jwt);
        when(mockAuth.validateManagerAuthentication(jwt))
                .thenReturn(Optional.empty());

        assertThrows(UnauthorizedResponse.class, () -> handler.handle(ctx));
    }

    // ============================
    // Negative Path: Token Missing or Invalid
    // ============================
    @Test
    @Story("Missing or invalid JWT should throw Unauthorized")
    @Severity(SeverityLevel.NORMAL)
    @Description("If the JWT is missing or invalid (e.g., after logout), the middleware should throw UnauthorizedResponse")
    @Step("Test middleware rejects missing or invalid JWT")
    void test_validateTokenMissing() {
        String jwt = "no-token";

        when(ctx.cookie("jwt")).thenReturn(jwt);
        when(mockAuth.validateJwtToken(jwt))
                .thenReturn(Optional.empty());

        assertThrows(UnauthorizedResponse.class, () -> handler.handle(ctx));
    }
}
