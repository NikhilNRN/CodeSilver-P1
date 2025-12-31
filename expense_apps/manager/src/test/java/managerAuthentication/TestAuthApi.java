package managerAuthentication;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.lessThan;

public class TestAuthApi {
    static RequestSpecification requestSpec;
    static ResponseSpecification responseSpec;
    @BeforeAll
    static void setUp(){
        RestAssured.baseURI="http://localhost:5001/";
        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("X-Custom-Header", "Test")
                .build();

        responseSpec=new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .expectResponseTime(lessThan(5000L))
                .build();
    }


    @AfterAll
    static void tearDown(){
        RestAssured.reset();
    }


    //With no login recorded, should return false
    @Test
    @DisplayName("Get Authentication Status 200")
    public void getAuthStatus(){
        given()
                .when()
                .get("/api/auth/status")
                .then()
                .spec(responseSpec)
                .body("authenticated", equalTo(false))
                .statusCode(200);
    }



    //LOGIN TESTS

    //valid login

    @Test
    @DisplayName("Valid Manager")
    public void validManagerLogin(){
        String uname = "manager1";
        String pword = "password123";
        String requestBody = String.format("""
            {"username":"%s","password":"%s"}
            """, uname, pword);
        Response response = given()
                .spec(requestSpec)
                .when()
                .body(requestBody)
                .post("/api/auth/login")
                .then()
                .spec(responseSpec)
                .statusCode(200)
                .extract()
                .response();
        String jwtCookie = response.getCookie("jwt");
        given()
                .cookie("jwt", jwtCookie)
                .when()
                .get("/api/auth/status")
                .then()
                .spec(responseSpec)
                .body("authenticated", equalTo(true))
                .statusCode(200);
    }


    //login with user nto a manager
    @DisplayName("Invalid Logins")
    @ParameterizedTest
    @CsvSource({
            "employee1, password123",
            "doesnotexist, none"
    })
    public void invalidManagerLogin(String uname, String pword){
        String requestBody = String.format("""
            {"username":"%s","password":"%s"}
            """, uname, pword);
        given()
                .spec(requestSpec)
                .when()
                .body(requestBody)
                .post("/api/auth/login")
                .then()
                .spec(responseSpec)
                .body("error",equalTo("Invalid credentials or user is not a manager"))
                .statusCode(401);
    }
    @DisplayName("Null Logins")
    @ParameterizedTest
    @CsvSource({
            "{\"password\": null}",
            "{\"username\": \"person\"}",
    })
    public void nullManagerLogin(String requestBody){
        given()
                .spec(requestSpec)
                .when()
                .body(requestBody)
                .post("/api/auth/login")
                .then()
                .spec(responseSpec)
                .body("error",equalTo("Username and password are required"))
                .statusCode(400);
    }

    @DisplayName("Malformed Login Request")
    @ParameterizedTest
    @CsvSource({
            "{\"username\": 145,\"password\": \"hi\"}",
            "{\"username\": \"hi\",\"malware\": \"bad\",\"password\": \"hi\"}"
    })
    public void testLoginBadRequest(String requestBody){
        given()
                .spec(requestSpec)
                .when()
                .body(requestBody)
                .post("/api/auth/login")
                .then()
                .spec(responseSpec)
                .body("error",equalTo("Invalid request format"))
                .statusCode(400);
    }

    //Test logout authorization
    @Test
    @DisplayName("Full Logout Sequence: BUG")
    //BUG: Logging out does not invalidate the jwt token, as it only rmoves it from the client.
    //This is a major security risk, since anyone who can hold on to the cookie can keep reusing it
    //until it expires.
    //Without internally tracked refresh tokens we risk our data being leaked
    public void fullManagerLogoutSequence(){
        //we log in check the authorization logout and check it again
        String uname = "manager1";
        String pword = "password123";
        String requestBody = String.format("""
            {"username":"%s","password":"%s"}
            """, uname, pword);
        Response response = given()
                .spec(requestSpec)
                .when()
                .body(requestBody)
                .post("/api/auth/login")
                .then()
                .spec(responseSpec)
                .statusCode(200)
                .extract()
                .response();
        String jwtCookie = response.getCookie("jwt");
        given()
                .cookie("jwt", jwtCookie)
                .when()
                .get("/api/auth/status")
                .then()
                .spec(responseSpec)
                .body("authenticated", equalTo(true))
                .statusCode(200);
        given()
                .when()
                .post("/api/auth/logout")
                .then()
                .statusCode(200);
        given()
                .cookie("jwt", jwtCookie)
                .when()
                .get("/api/auth/status")
                .then()
                .spec(responseSpec)
                .body("authenticated", equalTo(false))
                .statusCode(200);
    }

}
