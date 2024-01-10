package com.prototype.ChessT;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
/**
 * Klasa do testowania klasy ChessTournamentApplication
 */
@SpringBootTest
class TournamentTest {
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
    void clearAndInsertData(){
        try {
            ChessTournamentApplication.connection.prepareStatement("""
            delete from fide_changes;
            delete from matches;
            delete from tournament_roles;
            delete from tournaments;
            delete from sessions;
            delete from users;
            insert into users values (1, 'player1', 'player1@mail.com', 'player1password', 'player1name', 'player1lastname', 'player1sex', '2000-01-01', 2000, 20),
            (2, 'player2', 'player2@mail.com', 'player2password', 'player2name', 'player2lastname', 'player2sex', '2001-02-02', 2100, 20),
            (3, 'player3', 'player3@mail.com', 'player3password', 'player3name', 'player3lastname', 'player3sex', '2002-03-03', 2200, 40);
            insert into tournaments values (1, 'tournament1', 'tournament1location', 'tournament1organiser','tournament1timecontrol', now() - interval '2 month', now() - interval '1 month', 2, 'tournament1info', 2),
            (2, 'tournament2', 'tournament2location', 'tournament2organiser','tournament2timecontrol', now() - interval '10 day', now() + interval '2 day', 3, 'tournament2info', 1),
            (3, 'tournament3', 'tournament3location', 'tournament3organiser','tournament3timecontrol', now() + interval '5 day', now() + interval '10 day', 1, 'tournament3info', 0);
            insert into tournament_roles values (1, 1, 'admin', 2000, 20, 0),
            (2, 2, 'admin', 2100, 20, 0),
            (3, 3, 'admin', 2200, 40, 0),
            (1, 2, 'player', 2000, 20, 0),
            (1, 3, 'player', 2000, 20, 0),
            (2, 1, 'player', 2100, 20, 0),
            (2, 3, 'player', 2100, 20, 0),
            (3, 1, 'player', 2200, 40, 0),
            (3, 2, 'player', 2200, 40, 0);
            insert into matches values (1, 2, 3, 1, 0, 1, 1, ''),
            (2, 3, 2, 1, -1, 2, 1, ''),
            (3, 3, 1, 2, 1, 1, 1, ''),
            (4, 1, 3, 2, -1, 2, 1, '');
            insert into fide_changes values (1, 2, 3),
            (1, 3, -3),
            (2, 3, -10),
            (2, 2, 15),
            (3, 1, -10),
            (3, 3, 10),
            (4, 3, 10),
            (4, 1, -10);
            insert into sessions values ('xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx1', 1, now() - interval '1 minute'),
            ('xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx2', 2, now() - interval '1 minute'),
            ('xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx3', 3, now() - interval '1 minute');
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
     * Test Tournament.generateRoundPairings()
     * Wywołujemy powyższą funkcję dla rundy w turnieju, która już się zaczeła,
     * dla rundy, poprzednia nie jest jeszcze wygenerowana oraz
     * dla rundy, która powinna zostać wygenerowana.
     * Sprawdzamy czy HttpsStatus kody są zgodne z dokumentacją i dla poprawnego wywoływania spradzamy,
     * czy poprawna runda się wygenerowała.
     */
    @Test
    void generateRoundPairings() {
        assertEquals(HttpStatus.CONFLICT, tr.generateRoundPairings(2, 2, "xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx2").getStatusCode());
        assertEquals(HttpStatus.CONFLICT, tr.generateRoundPairings(2, 4, "xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx2").getStatusCode());
        assertEquals(HttpStatus.OK, tr.generateRoundPairings(2, 3, "xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx2").getStatusCode());
        String temp = tr.tournamentInfo("", 2).getBody();
        int index = temp.indexOf("rounds_generated\":")+18;
        assertEquals("3", temp.substring(index, index + 1));
    }

    /**
     * Test Tournament.create()
     * Wywołujemy powyższą funkcję dla nazwy turnieju, która już istnieje,
     * dla ujemnej liczby rund oraz dla poprawnego wywołania,
     * a następnie czy HttpStatus kody odpowiednich wywołań się zgadzają.
     */
    @Test
    void create() {
        assertEquals(HttpStatus.CONFLICT, tr.create("xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx2", "tournament1",
                "Cracow", "my organiser", "180+3", "2024-01-31",
                "2024-02-29", 3, "new tournament info").getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, tr.create("xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx2", "new tournament",
                "Cracow", "my organiser", "180+3", "2024-01-31",
                "2024-02-29", -1, "new tournament info").getStatusCode());
        assertEquals(HttpStatus.OK, tr.create("xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx2", "new tournament",
                "Cracow", "my organiser", "180+3", "2024-01-31",
                "2024-02-29", 3, "new tournament info").getStatusCode());
    }
    /**
     * Test Tournament.join()
     * Tworzymy turniej, a następnie próbujemy dołączyć do turnieju który nie istnieje,
     * próbujemy dołączyć do turnieju, w którym już jesteśmy graczami,
     * próbujemy dołączyć do turnieju, w którym jesteśmy adminem oraz
     * próbujemy dołączyć do turnieju, do którego możemy.
     * Następnie sprawdzamy HttpsStatus kody odpowiednich wywołań się zgadzają.
     */
    @Test
    void join() {
        assertEquals(HttpStatus.OK, tr.create("xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx2", "new tournament",
                "Cracow", "my organiser", "180+3", "2024-01-31",
                "2024-02-29", 3, "new tournament info").getStatusCode());
        assertEquals(HttpStatus.CONFLICT, tr.join("xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx1", 5).getStatusCode()); //nie istnieje
        assertEquals(HttpStatus.CONFLICT, tr.join("xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx1", 2).getStatusCode()); //jest graczem
        assertEquals(HttpStatus.CONFLICT, tr.join("xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx3", 3).getStatusCode()); //jest adminem
        assertEquals(HttpStatus.OK, tr.join("xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx1", 4).getStatusCode());
    }

    @Test
    void tournamentInfo() {
        assertEquals(HttpStatus.CONFLICT, tr.tournamentInfo("", 4).getStatusCode());
        assertEquals(587, tr.tournamentInfo("", 1).toString().length());
        assertEquals(583, tr.tournamentInfo("", 2).toString().length());
        assertEquals(519, tr.tournamentInfo("", 3).toString().length());
    }

    @Test
    void getMatch() {
        assertEquals(HttpStatus.CONFLICT, tr.getMatch(5).getStatusCode());
        assertEquals(308, tr.getMatch(1).toString().length());
        System.out.println(tr.getMatch(1).toString());
        assertEquals(308, tr.getMatch(3).toString().length());
    }

    @Test
    void getRound() {
    }

    @Test
    void playerInfo() {
    }

    @Test
    void results() {
    }

    @Test
    void updateMatch() {
    }

    @Test
    void startTournament() {
    }

    @Test
    void endTournament() {
        ResponseEntity<String> responseEntity = tr.endTournament(2,"xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx2");
        assert(responseEntity.getStatusCode() == HttpStatus.OK);

        responseEntity = tr.endTournament(3,"xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx3");
        assert(responseEntity.getStatusCode() == HttpStatus.CONFLICT);
    }

    @Test
    void changeRoundsNo() {
        ResponseEntity<String> responseEntity = tr.changeRoundsNo("xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx3",3,255);
        assert(responseEntity.getStatusCode() == HttpStatus.OK);

        responseEntity = tr.changeRoundsNo("xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdx2",3,255);
        assert(responseEntity.getStatusCode() == HttpStatus.CONFLICT);
    }
}