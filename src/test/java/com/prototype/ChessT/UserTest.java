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

/**
 * Klasa do testowania klasy User
 */
@SpringBootTest
class UserTest {
    @Autowired
    ChessTournamentApplication cta;
    @Autowired
    Tournament tr;
    @Autowired
    User ur;

    /**
     * Funkcja wykonywana przed rozpoczęciem testów.
     * Nawiązuje połączenie z testową bazą danych.
     */
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

    /**
     * Funkcja wykonywuje się przed każdym testem z osobna.
     * Czyści testową bazę danych i dodaje do niej testowe dane.
     */
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

    /**
     * Funkcja wykonywana po zakończeniu wszystkich testów.
     * Zamyka połączenie z bazą danych.
     */
    @AfterAll
    static void endTest(){
        try {
            ChessTournamentApplication.connection.close();
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
            fail();
        }
    }

    /**
     * Test User.user()
     * Rejestruje nowego użytkownika, następnie weryfikuje zwrócony przez endpoint kod HttpStatus
     */
    @Test
    void user() {
        HttpServletResponse rs = new MockHttpServletResponse();
        ur.register("", "JKowal", "ZAQ!2wsx","ZAQ!2wsx","jankowal@mail.com",
                "Jan", "Kowalski", "Male", "2001-01-01", 2000, rs);

        ResponseEntity<String> responseEntity = ur.user(extractAuth(rs));
        assert(responseEntity.getStatusCode() == HttpStatus.ACCEPTED);
    }

    /**
     * Test User.account()
     * Rejestruje nowego użytkownika, wylogowywuje się, następnie weryfikuje zwrócony przez endpoint kod HttpStatus
     */
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

    /**
     * Test User.validate();
     * Rejestruje nowe użytkownika, następnie sprawdza czy użytkownik jest zalogowany i weryfikuje czy zwraca poprawny HttpStatus.
     * Wylogowywuje, następnie sprawdza czy zwraca HttpStatus dla niezalogowanego użytkownika.
     */
    @Test
    void validate() {
        HttpServletResponse rs = new MockHttpServletResponse();
        ur.register("", "JKowal", "ZAQ!2wsx","ZAQ!2wsx","jankowal@mail.com",
                "Jan", "Kowalski", "Male", "2001-01-01", 2000, rs);

        ResponseEntity<String> responseEntity = ur.validate(extractAuth(rs));
        assert(responseEntity.getStatusCode() == HttpStatus.OK);
        ur.logout(extractAuth(rs));

        responseEntity = ur.validate(extractAuth(rs));
        assert(responseEntity.getStatusCode() == HttpStatus.UNAUTHORIZED);

    }

    /**
     * Test User.login()
     * Rejestruje użytkownika, wylogowywuje a następnie loguje i sprawdza, czy logowanie zwraca poprawny HttpsStatus.
     */
    @Test
    void login() {
        HttpServletResponse rs = new MockHttpServletResponse();
        ur.register("", "JKowal", "ZAQ!2wsx","ZAQ!2wsx","jankowal@mail.com",
                "Jan", "Kowalski", "Male", "2001-01-01", 2000, rs);
        ur.logout(extractAuth(rs));

        ResponseEntity<String> responseEntity = ur.login(extractAuth(rs),"JKowal","ZAQ!2wsx",rs);
        assert(responseEntity.getStatusCode() == HttpStatus.OK);
    }

    /**
     * Test User.logout()
     * Rejestruje użytykownika, a następnie wylogowywuje i spradza, czy wylogowywanie zwraca poprawny HttpStatus.
     * Następnie sprawdza HttpsStatus kod próby wylogowywania, gdy nie ma zalogowania żadnego użytkownika.
     */
    @Test
    void logout() {
        HttpServletResponse rs = new MockHttpServletResponse();
        ur.register("", "JKowal", "ZAQ!2wsx","ZAQ!2wsx","jankowal@mail.com",
                "Jan", "Kowalski", "Male", "2001-01-01", 2000, rs);

        ResponseEntity<String> responseEntity = ur.logout(extractAuth(rs));
        assert(responseEntity.getStatusCode() == HttpStatus.OK);

        responseEntity = ur.logout(extractAuth(rs));
        assert(responseEntity.getStatusCode() == HttpStatus.UNAUTHORIZED);
    }

    /**
     * Test User.register()
     * Rejestruje użytkownika i sprawdza, czy rejestracja zwraca poprawny HttpStatus.
     */
    @Test
    void register() {
        HttpServletResponse rs = new MockHttpServletResponse();
        ResponseEntity<String> responseEntity = ur.register("", "JKowal", "ZAQ!2wsx","ZAQ!2wsx","jankowal@mail.com",
                "Jan", "Kowalski", "Male", "2001-01-01", 2000, rs);
        assert(responseEntity.getStatusCode() == HttpStatus.OK);
    }

    /**
     * Test User.myTournaments()
     * Rejestruje użytkownika, wywołuje myTournaments() i spradza, czy funkcja zwraca poprawny HttpsStatus.
     */
    @Test
    void myTournaments() {
        HttpServletResponse rs = new MockHttpServletResponse();
        ur.register("", "JKowal", "ZAQ!2wsx","ZAQ!2wsx","jankowal@mail.com",
                "Jan", "Kowalski", "Male", "2001-01-01", 2000, rs);

        ResponseEntity<String> responseEntity = ur.myTournaments(extractAuth(rs));
        assert(responseEntity.getStatusCode() == HttpStatus.OK);
    }

    /**
     * Test User.randomString32Char()
     * Wywołuje powyższą funkcję, a następnie sprawdza, czy jej długość jest równa 32.
     */
    @Test
    void randomString32Char() {
        String string = ur.randomString32Char();
        assert(string.length() == 32);
    }

    /**
     * Test User.hashPassword()
     * Wywołuje powyższą funkcje dla hasła z dwóch różnych username.
     * Następnie sprawdza, czy poprawnie jest zashasowane i sprawdza, czy zahashowane te same hasła dla różnych użytkowników są różne.
     */
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

    /**
     * Funkcja pomocnicza, która wyciąga z HttpServletResponse cookie autoryzacyjny.
     * @param rs
     * @return String - auth cookie
     */
    String extractAuth(HttpServletResponse rs){
        String resStr = rs.getHeader("Set-Cookie");
        int temp = resStr.lastIndexOf("auth=")+5;
        return resStr.substring(temp, temp+32);
    }
}