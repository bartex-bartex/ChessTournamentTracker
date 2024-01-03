package com.prototype.ChessT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ChessTournamentApplicationTest {
    @Autowired
    ChessTournamentApplication cta;

    @Test
    void fideUpdate() {
        //fail();

    }

    @Test
    void homepage() {

        System.out.println(cta.homepage(-1, 1, 100));

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
}