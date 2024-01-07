package com.prototype.ChessT;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ChessTournamentApplicationTest {
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

    @AfterAll
    static void endTest(){
        try {
            /*ChessTournamentApplication.connection.prepareStatement("""
            delete from fide_changes;
            delete from matches;
            delete from tournament_roles;
            delete from tournaments;
            delete from sessions;
            delete from users;
        """).execute();*/
            ChessTournamentApplication.connection.close();
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    void fideUpdate() {
        //fail();

    }

    @Test
    void homepage() {
        HttpServletResponse rs = new MockHttpServletResponse();
        ur.register("", "JKowal", "ZAQ!2wsx","ZAQ!2wsx","jankowal@mail.com",
                "Jan", "Kowalski", "Male", "2001-01-01", 2000, rs);

        System.out.println(extractAuth(rs));
    }

    @Test
    void search() {
    }

    @Test
    void kValue() {
    }

    @Test
    void stringToDate() {
    }

    String extractAuth(HttpServletResponse rs){
        String resStr = rs.getHeader("Set-Cookie");
        int temp = resStr.lastIndexOf("auth=")+5;
        return resStr.substring(temp, temp+32);
    }
}