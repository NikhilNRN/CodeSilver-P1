package managerAuthentication;

import com.revature.repository.User;
import com.revature.api.AuthenticationMiddleware;
import com.revature.service.AuthenticationService;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    public void setupHandler(){
        //create handler to deal with this middleware stuff
        //cant do a mock injection since we need handler type
        handler = new AuthenticationMiddleware(mockAuth).validateManager();
    }

    //C6_06
    @Test
    void test_validateManagerPositive() throws Exception {
        //string doesnt matter, mocked out
        String jwt = "mock-jwt-token";
        when(ctx.cookie("jwt")).thenReturn(jwt);

        when(mockAuth.validateManagerAuthentication(jwt))
                .thenReturn(Optional.of(mockUser));

        handler.handle(ctx);
        verify(ctx).attribute("manager", mockUser);
    }
    //C6_07
    @Test
    void test_validateUserBad() throws Exception {
        //string doesnt matter, mocked out
        String jwt = "funny_token";
        when(ctx.cookie("jwt")).thenReturn(jwt);

        when(mockAuth.validateManagerAuthentication(jwt))
                .thenReturn(Optional.empty());
        //make sure we throw because there user isnt a manager
        assertThrows(UnauthorizedResponse.class, ()->{handler.handle(ctx);});
    }
    //C6_08
    @Test
    void test_validateTokenMissing() {
        //this SHOULD be what happens when you logout, as main
        //calls ctx.removeCookie("jwt")
        //Need to also integration test that removal
        String jwt = "no-token";

        when(ctx.cookie("jwt")).thenReturn(jwt);
        //fail on validation
        when(mockAuth.validateJwtToken(jwt))
                .thenReturn(Optional.empty());

        assertThrows(UnauthorizedResponse.class, ()->{handler.handle(ctx);});
    }


}
