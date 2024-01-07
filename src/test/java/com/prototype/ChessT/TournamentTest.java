package com.prototype.ChessT;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class TournamentTest {
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
            (2, 'tournament2', 'tournament2location', 'tournament2organiser','tournament2timecontrol', now() - interval '10 day', now() + interval '2 day', 2, 'tournament2info', 1),
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
            insert into sessions values ('xdxdxdxdxdxdxdxdxdxdxdxdxdxdxdxd', 1, now() - interval '1 minute');
        """).execute();
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    void firstRoundPairings() {
    }

    @Test
    void generateRoundPairings() {
    }

    @Test
    void create() {
    }

    @Test
    void join() {
    }

    @Test
    void tournamentInfo() {
    }

    @Test
    void getMatch() {
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
    }

    @Test
    void changeRoundsNo() {
        ur.login();
    }
}