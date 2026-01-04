package databaseValidation;

import com.revature.repository.DatabaseConnection;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@Epic("Manager App")
@Feature("Database Connection")
@Story("Validate Database Connection")
public class TestDatabaseConnection {

    @BeforeAll
    @Step("Set system property for in-memory database")
    public static void allSetup() {
        System.setProperty("databasePath", ":memory:");
        Allure.step("System property 'databasePath' set to :memory:");
    }

    @DisplayName("D01 - Positive Connection Test")
    @Test
    @Severity(SeverityLevel.CRITICAL)
    public void testGetConnectionPositive() throws SQLException {
        Allure.step("Creating DatabaseConnection instance", () -> {
            DatabaseConnection db = new DatabaseConnection();

            Allure.step("Getting connection from DatabaseConnection", () -> {
                Connection conn = db.getConnection();
                assertNotNull(conn, "Connection should not be null");
            });
        });
    }

    @DisplayName("D02 - Connection Throws Exception Test")
    @Test
    @Severity(SeverityLevel.NORMAL)
    public void testGetConnectionThrows() {
        Allure.step("Creating DatabaseConnection with invalid path", () -> {
            String url = ":memory:" + "nonsensePath";
            DatabaseConnection db = new DatabaseConnection(url);

            Allure.step("Verifying that getConnection throws SQLException", () -> {
                assertThrows(SQLException.class, db::getConnection);
            });
        });
    }
}
