package com.prototype.ChessT;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import java.sql.SQLException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserTest {
    @Autowired
    ChessTournamentApplication cta;
    @Autowired
    Tournament tr;
    @Autowired
    User ur;

    @BeforeAll
    static void startTest(){
        try {
            ChessTournamentApplication.connection = ChessTournamentApplication.establishConnection(1);
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
            fail();
        }
    }

    @BeforeEach
    void clearData(){
        try {
            ChessTournamentApplication.connection.prepareStatement("""
            delete from fide_changes;
            delete from matches;
            delete from tournament_roles;
            delete from tournaments;
            delete from sessions;
            delete from users;
        """).execute();
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
            fail();
        }
    }

    @AfterAll
    static void endTest(){
        try {
            ChessTournamentApplication.connection.prepareStatement("""
            delete from fide_changes;
            delete from matches;
            delete from tournament_roles;
            delete from tournaments;
            delete from sessions;
            delete from users;
        """).execute();
            ChessTournamentApplication.connection.close();
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    void user() {
        HttpServletResponse rs = new MockHttpServletResponse();
        ur.register("", "JKowal", "ZAQ!2wsx","ZAQ!2wsx","jankowal@mail.com",
                "Jan", "Kowalski", "Male", "2001-01-01", 2000, rs);

        ResponseEntity<String> responseEntity = ur.user(extractAuth(rs));
        assert(responseEntity.getStatusCode() == HttpStatus.ACCEPTED);
    }

    @Test
    void account() {
        HttpServletResponse rs = new MockHttpServletResponse();
        ur.register("", "JKowal", "ZAQ!2wsx","ZAQ!2wsx","jankowal@mail.com",
                "Jan", "Kowalski", "Male", "2001-01-01", 2000, rs);
        ur.logout(extractAuth(rs));

        rs = new MockHttpServletResponse();

        ur.register("", "JJJoker", "ZAQ!2wsx","ZAQ!2wsx","nowymail@mail.com",
                "Jacek", "Srebrnoreki", "Male", "2001-01-01", 2002, rs);

        ResponseEntity<String> responseEntity = ur.account(extractAuth(rs),1);
        assert(responseEntity.getStatusCode() == HttpStatus.OK);
    }

    @Test
    void validate() {
        HttpServletResponse rs = new MockHttpServletResponse();
        ur.register("", "JKowal", "ZAQ!2wsx","ZAQ!2wsx","jankowal@mail.com",
                "Jan", "Kowalski", "Male", "2001-01-01", 2000, rs);

        ResponseEntity<String> responseEntity = ur.validate(extractAuth(rs));
        assert(responseEntity.getStatusCode() == HttpStatus.OK);
    }

    @Test
    void login() {
        HttpServletResponse rs = new MockHttpServletResponse();
        ur.register("", "JKowal", "ZAQ!2wsx","ZAQ!2wsx","jankowal@mail.com",
                "Jan", "Kowalski", "Male", "2001-01-01", 2000, rs);
        ur.logout(extractAuth(rs));

        ResponseEntity<String> responseEntity = ur.login(extractAuth(rs),"JKowal","ZAQ!2wsx",rs);
        assert(responseEntity.getStatusCode() == HttpStatus.OK);
    }

    @Test
    void logout() {
        HttpServletResponse rs = new MockHttpServletResponse();
        ur.register("", "JKowal", "ZAQ!2wsx","ZAQ!2wsx","jankowal@mail.com",
                "Jan", "Kowalski", "Male", "2001-01-01", 2000, rs);

        ResponseEntity<String> responseEntity = ur.logout(extractAuth(rs));
        assert(responseEntity.getStatusCode() == HttpStatus.OK);
    }

    @Test
    void register() {
        HttpServletResponse rs = new MockHttpServletResponse();
        ResponseEntity<String> responseEntity = ur.register("", "JKowal", "ZAQ!2wsx","ZAQ!2wsx","jankowal@mail.com",
                "Jan", "Kowalski", "Male", "2001-01-01", 2000, rs);
        assert(responseEntity.getStatusCode() == HttpStatus.OK);
    }

    @Test
    void myTournaments() {
        HttpServletResponse rs = new MockHttpServletResponse();
        ur.register("", "JKowal", "ZAQ!2wsx","ZAQ!2wsx","jankowal@mail.com",
                "Jan", "Kowalski", "Male", "2001-01-01", 2000, rs);

        ResponseEntity<String> responseEntity = ur.myTournaments(extractAuth(rs));
        assert(responseEntity.getStatusCode() == HttpStatus.OK);
    }

    @Test
    void randomString32Char() {
        String string = ur.randomString32Char();
        assert(string.length() == 32);
    }

    @Test
    void hashPassword() {
        String h1 = null;
        String h2 = null;
        try{h1 = ur.hashPassword("FajneNoweHaslo","user1");}
        catch (Exception e){fail();}

        try{h2 = ur.hashPassword("FajneNoweHaslo","user2");}
        catch (Exception e){fail();}

        assertEquals(h1,"de385791bad16898a9f0f0a50349cac554e4730c1c8bdba70378b97fde6763c5");
        assertNotEquals(h1,h2);
    }

    String extractAuth(HttpServletResponse rs){
        String resStr = rs.getHeader("Set-Cookie");
        int temp = resStr.lastIndexOf("auth=")+5;
        return resStr.substring(temp, temp+32);
    }
}