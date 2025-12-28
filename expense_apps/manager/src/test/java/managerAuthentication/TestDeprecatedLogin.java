package managerAuthentication;

import com.revature.repository.DatabaseConnection;
import com.revature.repository.User;
import com.revature.repository.UserRepository;
import com.revature.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class TestDeprecatedLogin {

    /*
        @Deprecated
    public Optional<User> validateManagerAuthenticationLegacy(String authorizationHeader) {
        Optional<User> userOpt = validateAuthentication(authorizationHeader);

        if (userOpt.isPresent() && isManager(userOpt.get())) {
            return userOpt;
        }

        return Optional.empty();
    }


        @Deprecated
    public Optional<User> validateAuthentication(String authorizationHeader) {
        // Check if Authorization header is present and properly formatted
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }

        try {
            String userIdStr = authorizationHeader.substring("Bearer ".length());
            int userId = Integer.parseInt(userIdStr);
            return userRepository.findById(userId);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
     */
    @Mock
    DatabaseConnection conn;
    @Mock
    UserRepository mockRep;
    @InjectMocks
    @Spy
    AuthenticationService authServ;

    @Mock
    User mockU;

    //C6_15
    @Test
    public void testDeprecatedAuthenticatePositive() {
        String header = "Bearer 12";
        String userIdStr = header.substring("Bearer ".length());
        int userId = Integer.parseInt(userIdStr);
        System.out.println(userId);

        when(mockRep.findById(12)).thenReturn(Optional.ofNullable(mockU));

        authServ.validateAuthentication(header);

        verify(mockRep, times(1)).findById(12);


    }

    //C6_16
    @Test
    public void testDeprecatedAuthenticateMalformedID() {
        String header = "Bearer A";
        authServ.validateAuthentication(header);

        verify(mockRep, times(0)).findById(12);

    }
    //C6_17
    @Test
    public void testDeprecatedManagerPositive() {
        String header= "Header 12";

        doReturn(Optional.ofNullable(mockU)).when(authServ).validateAuthentication(header);
        authServ.validateManagerAuthenticationLegacy(header);
        verify(authServ, times(1)).isManager(mockU);

    }
    //C6_18
    @Test
    public void testDeprecatedManagerNegative() {
        String header= "Header A";
        doReturn(Optional.ofNullable(null)).when(authServ).validateAuthentication(header);
        authServ.validateManagerAuthenticationLegacy(header);
        verify(authServ, times(0)).isManager(mockU);
    }


}
