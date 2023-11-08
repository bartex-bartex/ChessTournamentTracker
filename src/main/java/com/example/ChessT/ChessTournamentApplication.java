package com.example.ChessT;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import netscape.javascript.JSObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.json.*;

import javax.swing.plaf.nimbus.State;
import java.sql.*;
import java.util.List;
import java.util.Random;
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
	@GetMapping("/api")
	public ResponseEntity<String> api(){
		return new ResponseEntity<String>(randomString32Char(), HttpStatus.I_AM_A_TEAPOT);
	}

	@GetMapping("/user") // sukces polskiej policji
	public ResponseEntity<String> user(@CookieValue(value = "auth") String auth) throws SQLException{
		int userId = -1;
		try{
			userId = checkCookie(auth);
		}
		catch (Exception e){
			return new ResponseEntity<String>("400", HttpStatus.CONFLICT);
		}
		Statement st = connection.createStatement();
		String query = String.format("select username,mail,first_name,last_name,sex,date_of_birth,fide from users where user_id = %d;", userId);
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

	@RequestMapping("/user/login") // sukces polskiej policji
	public ResponseEntity<String> login(@CookieValue(value = "auth", defaultValue = "xd") String auth,
										@RequestParam(value = "username") String username,
										@RequestParam(value = "password") String password,
									   HttpServletResponse response) throws SQLException{
		if(!checkFalseCookie(auth)){
			return new ResponseEntity<>("410", HttpStatus.IM_USED);
		}
		Statement st = connection.createStatement();
		String query = String.format("select user_id from users where username = '%s' and encrypted_password = '%s';",username,password);
		ResultSet rs = st.executeQuery(query);
		if (!rs.next()){
			return new ResponseEntity<String>("400", HttpStatus.I_AM_A_TEAPOT);
		}
		addAuthCookie(response, rs.getInt(1));

		return new ResponseEntity<String>("200", HttpStatus.ACCEPTED);

	}

	@RequestMapping("/user/logout")
	public ResponseEntity<String> logout(@CookieValue(value = "auth") String auth) throws SQLException {
		if(checkFalseCookie(auth)){
			return new ResponseEntity<>("400", HttpStatus.CONFLICT);
		}
		Statement st = connection.createStatement();
		String query = String.format("delete from sessions where session_id = '%s'",auth);
		st.execute(query);
		return new ResponseEntity<String>("200", HttpStatus.ACCEPTED);
	}

	@RequestMapping("/user/register")
	public ResponseEntity<String> register(@CookieValue(value = "auth", defaultValue = "xd") String auth,
			 			 @RequestParam(value = "username") String username,
						 @RequestParam(value = "password") String password,
						 @RequestParam(value = "passwordAgain") String password2,
						 @RequestParam(value = "mail") String mail,
						 @RequestParam(value = "first_name") String name,
						 @RequestParam(value = "last_name") String lastname,
						 @RequestParam(value = "sex") String sex,
						 @RequestParam(value = "date_of_birth") String date,
						 @RequestParam(value = "fide") String fide,
						 HttpServletResponse response) throws SQLException {
		if(!checkFalseCookie(auth)){
			return new ResponseEntity<>("410", HttpStatus.IM_USED);
		}
		Statement st = connection.createStatement();
		String query = String.format("select count(x) from (select * from users where username = '%s' or mail = '%s') as x",username,mail);
		ResultSet rs = st.executeQuery(query);
		rs.next();
		int rowCount = rs.getInt(1);
		if (rowCount == 0 && password.equals(password2)){
			query = "select max(user_id) from users";
			rs = st.executeQuery(query);
			rs.next();
			int id = 1 + rs.getInt(1);
			query = String.format("insert into users (user_id,username,mail,encrypted_password,first_name,last_name,sex,date_of_birth,fide) values ('%d','%s','%s','%s','%s','%s','%s','%s','%s')",id,username,mail,password,name,lastname,sex,date,fide);
			st.execute(query);
			addAuthCookie(response, id);
			return new ResponseEntity<String>("200",HttpStatus.ACCEPTED);
		}
		return new ResponseEntity<String>("400",HttpStatus.I_AM_A_TEAPOT);
	}


	public String randomString32Char() {
		int leftLimit = 48; // numeral '0'
		int rightLimit = 122; // letter 'z'
		int targetStringLength = 32;
		Random random = new Random();

		String generatedString = random.ints(leftLimit, rightLimit + 1)
				.filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
				.limit(targetStringLength)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();

		return generatedString;
	}

	public void addAuthCookie(HttpServletResponse r, int userId) throws SQLException {
		Statement st = connection.createStatement();
		String query = String.format("delete from sessions where user_id = %d and date < now() - interval '30' minute;",userId);
		st.execute(query);
		boolean b = false;
		String newAuth;
		do{
			newAuth = randomString32Char();
			query = String.format("Select count(*) from sessions where session_id = '%s'", newAuth);
			ResultSet rs = st.executeQuery(query);
			rs.next();
			b = (rs.getInt(1) != 0);
		}while(b);
		query = String.format("insert into sessions values ('%s',%d,now())",newAuth,userId);
		st.execute(query);

		Cookie c = new Cookie("auth", newAuth);
		c.setSecure(true);
		r.addCookie(c);
	}

	public int checkCookie(String auth) throws SQLException, Exception {
		Statement st = connection.createStatement();
		String query = String.format("Select user_id from sessions where session_id = '%s' and date > now() - interval '30' minute", auth);
		ResultSet rs = st.executeQuery(query);
		if(rs.next()){
			return rs.getInt(1);
		}
		throw new Exception("No such active auth token found");
	}

	public boolean checkFalseCookie(String auth) throws SQLException {
		if(auth.length()<30){
			return true;
		}
		Statement st = connection.createStatement();
		String query = String.format("Select user_id from sessions where session_id = '%s' and date > now() - interval '30' minute", auth);
		ResultSet rs = st.executeQuery(query);
		return !rs.next();
	}
}
