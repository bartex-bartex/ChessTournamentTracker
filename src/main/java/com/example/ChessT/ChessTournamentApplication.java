package com.example.ChessT;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.jooq.impl.DSL;
import org.jooq.tools.json.JSONParser;
import org.jooq.tools.json.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

	/*@GetMapping("/hello")
	public String hello(@RequestParam(value = "name", defaultValue = "World") String name) throws SQLException {
		Statement st = connection.createStatement();
		ResultSet rs = st.executeQuery("select * from users");
		while (rs.next()) {
			temp += rs.getString(1);
		}
		rs.close();
		st.close();

		return String.format("Hello %s!", temp);
	}*/
	//public ResponseEntity<String> user(@RequestParam(value = "id", defaultValue = "1") String id) throws SQLException{
	@GetMapping("/user") // sukces polskiej policji
	public ResponseEntity<String> user(@CookieValue(value = "twoja_stara") String auth) throws SQLException{
		if (auth.equals("nikt_nie_pytal")){
			Statement st = connection.createStatement();
			//String query = String.format("select to_json(array_agg(t)) from (select username,mail,first_name,last_name,sex,date_of_birth,fide from users where user_id = %s or user_id = 6) as t;",id);
			String query = String.format("select username,mail,first_name,last_name,sex,date_of_birth,fide from users where user_id = %s;","1");
			ResultSet rs = st.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			JSONObject result = new JSONObject();
			while(rs.next()){
				for (int i=1;i<=rsmd.getColumnCount();i++) {
					result.put(rsmd.getColumnLabel(i),rs.getString(i));
				}
			}
			return new ResponseEntity<String>(result.toString(), HttpStatus.ACCEPTED);
		}
		return new ResponseEntity<String>("400", HttpStatus.CONFLICT);
	}
	@RequestMapping("/user/register")
	public ResponseEntity<String> register(@RequestParam(value = "username") String username,
						 @RequestParam(value = "password") String password,
						 @RequestParam(value = "passwordAgain") String password2,
						 @RequestParam(value = "mail") String mail,
						 @RequestParam(value = "first_name") String name,
						 @RequestParam(value = "last_name") String lastname,
						 @RequestParam(value = "sex") String sex,
						 @RequestParam(value = "date_of_birth") String date,
						 @RequestParam(value = "fide") String fide,
						 HttpServletResponse response) throws SQLException {
		Statement st = connection.createStatement();
		String query = String.format("select count(x) from (select * from users where username = '%s' or mail = '%s') as x",username,mail);
		ResultSet rs = st.executeQuery(query);
		rs.next();
		int rowCount = Integer.parseInt(rs.getString(1));
		if (rowCount == 0 && password.equals(password2)){
			response.addCookie(new Cookie("twoja_stara","nikt_nie_pytal"));
			query = "select max(user_id) from users";
			rs = st.executeQuery(query);
			rs.next();
			int id = 1 + Integer.parseInt(rs.getString(1));
			query = String.format("insert into users (user_id,username,mail,encrypted_password,first_name,last_name,sex,date_of_birth,fide) values ('%d','%s','%s','%s','%s','%s','%s','%s','%s')",id,username,mail,password,name,lastname,sex,date,fide);
			st.execute(query);
			return new ResponseEntity<String>("200",HttpStatus.ACCEPTED);
		}
		return new ResponseEntity<String>("400",HttpStatus.CONFLICT);
	}
}
