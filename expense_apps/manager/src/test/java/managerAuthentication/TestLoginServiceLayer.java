package managerAuthentication;

import com.revature.repository.User;
import com.revature.repository.UserRepository;
import com.revature.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestLoginServiceLayer {
    @BeforeEach
    public void setup(){

    }
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
    public void setUpUser(){
        uname = "test";
        pword = "pword";
        expectedUser = new User(1, uname, pword, "manager");
    }
    //test authenticate manager
    //C6_01
    @DisplayName("Test Case C6_01: Description")
    @Test
    public void test_authenticateManager_positive(){
        //create mock user and return that is correct
        //calls authentication method authenticate user, need to test that separately
        //
        //Instead of running authenticate user, we override it to return our expected dummy user
        doReturn(Optional.of(expectedUser)).when(testAuth).authenticateUser(uname, pword);
        //mock isManager and always return true for this one
        doReturn(true).when(testAuth).isManager(expectedUser);

        testAuth.authenticateManager(uname, pword);

        verify(testAuth, times(1)).authenticateUser(uname, pword);
        verify(testAuth, times(1)).isManager(expectedUser);
    }
    //C6_02
    @Test
    public void test_authenticateManager_negative(){

        //no user found
        doReturn(Optional.empty()).when(testAuth).authenticateUser(uname, pword);

        testAuth.authenticateManager(uname, pword);
        verify(testAuth, times(1)).authenticateUser(uname, pword);
        //isManager should never be called due to short circuit on isPresent on empty Optional
        verify(testAuth, times(0)).isManager(expectedUser);
    }

    //C6_03
    @Test
    public void test_userAuth_valid(){
        doReturn(Optional.of(expectedUser)).when(mockUserRep).findByUsername(uname);

        testAuth.authenticateUser(uname, pword);

        verify(mockUserRep, times(1)).findByUsername(uname);
    }
    //C6_04
    @Test
    public void test_jwtCreation_valid(){
        doReturn(1).when(mockUser).getId();
        doReturn("uname").when(mockUser).getUsername();
        doReturn("user").when(mockUser).getRole();

        testAuth.createJwtToken(mockUser);
        verify(mockUser, times(1)).getId();
        verify(mockUser, times(1)).getUsername();
        verify(mockUser, times(1)).getRole();

    }

    //C6_05
    @Test
    public void test_isManager_valid(){
        //user isManager is called through auth isManager
        doReturn(true).when(mockUser).isManager();

        testAuth.isManager(mockUser);
        verify(mockUser, times(1)).isManager();
    }
}
