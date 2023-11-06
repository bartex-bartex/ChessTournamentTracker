package com.example.ChessT;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.*;


@SpringBootApplication
@RestController
public class ChessTournamentApplication {
	static Connection connection = null;
	static String temp;
	public static void main(String[] args) {
		SpringApplication.run(ChessTournamentApplication.class, args);

		try {
			Class.forName("org.postgresql.Driver");
			connection=DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres","postgres","root");
			if (connection != null){
				temp = "OK";
			}else {
				temp = "Connection Failed";
			}
		}catch (Exception e){
			temp = e.getMessage();
		}
	}

	@GetMapping("/hello")
	public String hello(@RequestParam(value = "name", defaultValue = "World") String name) throws SQLException {
		Statement st = connection.createStatement();
		ResultSet rs = st.executeQuery("SELECT \"Name\" FROM \"Users\" join \"Matches\" on ((\"UserId\" = \"WhitePlayerId\" and \"Score\" = 1) or (\"UserId\" = \"BlackPlayerId\" and \"Score\" = -1));");
		while (rs.next()) {
			temp += rs.getString(1);
		}
		rs.close();
		st.close();

		return String.format("Hello %s!", temp);
	}
}
