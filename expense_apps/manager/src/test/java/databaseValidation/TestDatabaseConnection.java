package databaseValidation;

import com.revature.repository.DatabaseConnection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.crypto.Data;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class TestDatabaseConnection {


    private DriverManager dm;
    private Connection mockConn;


    private DatabaseConnection db;

    @BeforeAll
    public static void allSetup(){
        System.setProperty("databasePath", ":memory:");
    }

    //todo: test constructor
//    @Test
//    void testDatabasePathFromProperties() {
//        String fakeProps = "databasePath=/test/db\n";
//        InputStream input = new ByteArrayInputStream(fakeProps.getBytes());
//
//        DatabaseConnection db = new DatabaseConnection(input);
//
//        assertEquals("/test/db", System.getProperty("databasePath"));
//    }

    /*
        public Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + databasePath;
        return DriverManager.getConnection(url);
    }
     */
    @DisplayName("D01_Egg")
    @Test
    public void testGetConnectionPositive() throws SQLException {
        DatabaseConnection db = new DatabaseConnection();
        Connection conn = db.getConnection();

        assertNotNull(conn);

    }

    @Test
    public void testGetConnectionThrows(){
        //put some random url in and it shouldnt be able to connect to anything
        String url = ":memory:" + "nonsensePath";
        DatabaseConnection db = new DatabaseConnection(url);
        assertThrows(SQLException.class, db::getConnection);


    }

}
