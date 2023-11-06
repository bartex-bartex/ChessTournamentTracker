package com.example.ChessT;

import org.jooq.impl.DSL;
import org.jooq.tools.json.JSONParser;
import org.jooq.tools.json.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import org.json.*;
import org.jooq.*;

import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


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
		ResultSet rs = st.executeQuery("select * from users");
		while (rs.next()) {
			temp += rs.getString(1);
		}
		rs.close();
		st.close();

		return String.format("Hello %s!", temp);
	}
	@GetMapping("/user")
	public String user(@RequestParam(value = "id", defaultValue = "1") String id) throws SQLException{
		Statement st = connection.createStatement();

		//select username,mail,first_name,last_name,sex,date_of_birth,fide from users natural join where kod = x;
		ResultSet rs = st.executeQuery(String.format("select to_json(array_agg(t)) from (select username,mail,first_name,last_name,sex,date_of_birth,fide from users where user_id = %s) as t;",id));
		String username = null;
		while (rs.next()) {
		username = rs.getString(1);
		}
		return String.format("Username: %s",username);
	}
	@GetMapping("/user2")
	public JSONObject user2(@RequestParam(value = "id", defaultValue = "1") String id) throws SQLException, ParseException {
		Statement st = connection.createStatement();
		String query = String.format("select to_json(array_agg(t)) from (select username,mail,first_name,last_name,sex,date_of_birth,fide from users where user_id = %s) as t;",id);
		ResultSet rs = st.executeQuery(query);
		JSONParser parser = new JSONParser();
		JSONObject result = (JSONObject) parser.parse(rs.getString(1));
		return result;
	}
	@RequestMapping("/user/register")
	public void register(@RequestParam(value = "username") String username,
						 @RequestParam(value = "password") String password,
						 @RequestParam(value = "passwordAgain") String password2,
						 @RequestParam(value = "mail") String mail,
						 @RequestParam(value = "first_name") String name,
						 @RequestParam(value = "last_name") String lastname,
						 @RequestParam(value = "sex") String sex,
						 @RequestParam(value = "date_of_birth") String date,
						 @RequestParam(value = "fide") String fide) throws SQLException {
		Statement st = connection.createStatement();
		String query = String.format("select count(x) from (select * from users where username = '%s' or mail = '%s') as x",username,mail);
		ResultSet rs = st.executeQuery(query);
		rs.next();
		int rowCount = Integer.parseInt(rs.getString(1));
		if (rowCount == 0){
			query = "select max(user_id) from users";
			rs = st.executeQuery(query);
			rs.next();
			int id = 1 + Integer.parseInt(rs.getString(1));
			query = String.format("insert into users (user_id,username,mail,encrypted_password,first_name,last_name,sex,date_of_birth,fide) values ('%d','%s','%s','%s','%s','%s','%s','%s','%s')",id,username,mail,password,name,lastname,sex,date,fide);
			st.execute(query);
		}
	}
}
