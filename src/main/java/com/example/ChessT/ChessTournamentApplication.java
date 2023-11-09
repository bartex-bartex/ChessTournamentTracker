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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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


	@GetMapping("/")
	public ResponseEntity<String> api(){
		return new ResponseEntity<String>(randomString32Char(), HttpStatus.I_AM_A_TEAPOT);
	}

	@GetMapping("/api/user") // sukces polskiej policji
	public ResponseEntity<String> user(@CookieValue(value = "auth") String auth){
		int userId = -1;
		try{
			userId = checkCookie(auth);
		}
		catch (Exception e) {
			return new ResponseEntity<String>("No or expired authorization token (CODE 402)", HttpStatus.UNAUTHORIZED);
		}

		try {
			Statement st = connection.createStatement();
			String query = String.format("select username,mail,first_name,last_name,sex,date_of_birth,fide from users where user_id = %d;", userId);
			ResultSet rs = st.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			JSONObject result = new JSONObject();
			if(rs.next()){
				for (int i=1;i<=rsmd.getColumnCount();i++) {
					result.put(rsmd.getColumnLabel(i),rs.getString(i));
				}
				return new ResponseEntity<String>(result.toString(), HttpStatus.ACCEPTED);
			}
			else {
				return new ResponseEntity<String>("Data base error (probably no relevant user found) (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		catch(SQLException e) {
			return new ResponseEntity<String>("Data base error (probably no relevant user found) (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/api/user/account/{id}")
	public ResponseEntity<String> account(@CookieValue(value = "auth") String auth,
										  @PathVariable(value = "id") int userId) throws SQLException{
		try{
			checkCookie(auth);
		}catch (Exception e){
			return new ResponseEntity<String>("nie zalogowany",HttpStatus.I_AM_A_TEAPOT);
		}
		Statement st = connection.createStatement();
		String query = String.format("select username,first_name,last_name,sex,date_of_birth,fide from users where user_id = '%d';", userId);
		ResultSet rs = st.executeQuery(query);
		ResultSetMetaData rsmd = rs.getMetaData();
		JSONObject result = new JSONObject();
		if(rs.next()){
			for (int i=1;i<=rsmd.getColumnCount();i++) {
				result.put(rsmd.getColumnLabel(i),rs.getString(i));
			}
		}else {
			return new ResponseEntity<String>("409", HttpStatus.I_AM_A_TEAPOT);
		}
		return new ResponseEntity<String>(result.toString(), HttpStatus.ACCEPTED);

	}

	@RequestMapping("/api/user/login")
	public ResponseEntity<String> login(@CookieValue(value = "auth", defaultValue = "xd") String auth,
										@RequestParam(value = "username") String username,
										@RequestParam(value = "password") String password,
										HttpServletResponse response) {
		try {
			if(!checkFalseCookie(auth)){
				return new ResponseEntity<>("User is already logged in (CODE 409)", HttpStatus.CONFLICT);
			}
			Statement st = connection.createStatement();
			String query = String.format("select user_id from users where username = '%s' and encrypted_password = '%s';", username, hashPassword(password, username));
			ResultSet rs = st.executeQuery(query);
			if (!rs.next()) {
				return new ResponseEntity<String>("Username or password incorrect (CODE 404)", HttpStatus.NOT_FOUND);
			}
			addAuthCookie(response, rs.getInt(1));

			return new ResponseEntity<String>("Successfully logged in (CODE 200)", HttpStatus.OK);
		}
		catch(Exception e){
			return new ResponseEntity<String>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping("/api/user/logout")
	public ResponseEntity<String> logout(@CookieValue(value = "auth") String auth) {
		try {
			if (checkFalseCookie(auth)) {
				return new ResponseEntity<>("No user to log out (CODE 409)", HttpStatus.CONFLICT);
			}
			Statement st = connection.createStatement();
			String query = String.format("delete from sessions where session_id = '%s'", auth);
			st.execute(query);
			return new ResponseEntity<String>("Successfully logged out (CODE 200)", HttpStatus.OK);
		}
		catch (Exception e){
			return new ResponseEntity<String>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping("/api/user/register")
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
										   HttpServletResponse response) {
		try {
			if (!checkFalseCookie(auth)) {
				return new ResponseEntity<>("User is already logged in (CODE 409)", HttpStatus.CONFLICT);
			}
			Statement st = connection.createStatement();
			String query = String.format("select count(x) from (select * from users where username = '%s' or mail = '%s') as x", username, mail);
			ResultSet rs = st.executeQuery(query);
			rs.next();
			int rowCount = rs.getInt(1);
			if (rowCount == 0 && password.equals(password2)) {
				query = "select coalesce(max(user_id),0) from users";
				rs = st.executeQuery(query);
				rs.next();
				int id = 1 + rs.getInt(1);
				query = String.format("insert into users (user_id,username,mail,encrypted_password,first_name,last_name,sex,date_of_birth,fide) values ('%d','%s','%s','%s','%s','%s','%s','%s','%s')", id, username, mail, hashPassword(password, username), name, lastname, sex, date, fide);
				st.execute(query);
				addAuthCookie(response, id);
				return new ResponseEntity<String>("Successfully registered (CODE 200)", HttpStatus.OK);
			}
			return new ResponseEntity<String>("User with this username or email already exists (CODE 409)", HttpStatus.CONFLICT);
		}
		catch(Exception e){
			return new ResponseEntity<String>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping("/api/user/create")
	public ResponseEntity<String> create(@CookieValue(value = "auth") String auth,
										   @RequestParam(value = "tournamentName") String name,
										   @RequestParam(value = "location") String location,
										   @RequestParam(value = "organizer") String organizer,
										   @RequestParam(value = "timeControl") String timeControl,
										   @RequestParam(value = "startDate") String startDate,
										   @RequestParam(value = "endDate") String endDate,
										   @RequestParam(value = "rounds") int rounds,
										   @RequestParam(value = "info") String info) {
		int userId = -1;
		try{
			userId = checkCookie(auth);
		}
		catch (Exception e) {
			return new ResponseEntity<String>("No or expired authorization token (CODE 402)", HttpStatus.UNAUTHORIZED);
		}

		try {
			Statement st = connection.createStatement();
			String query = String.format("select count(x) from (select * from tournaments where name = '%s') as x", name);
			ResultSet rs = st.executeQuery(query);
			rs.next();
			int rowCount = rs.getInt(1);
			if (rowCount == 0) {
				query = "select coalesce(max(tournament_id),0) from tournaments";
				rs = st.executeQuery(query);
				rs.next();
				int id = 1 + rs.getInt(1);
				query = String.format("insert into tournaments (tournament_id,name,location,organiser,time_control,start_date,end_date,rounds,info) values ('%d','%s','%s','%s','%s','%s','%s','%d','%s')", id,name,location,organizer,timeControl,startDate,endDate,rounds,info);
				st.execute(query);
				query = String.format("insert into tournament_role (user_id,tournament_id,role) values (%d,%d,'admin')",userId,id);
				st.execute(query);

				return new ResponseEntity<String>("Tournament successfully registered (CODE 200)", HttpStatus.OK);
			}
			return new ResponseEntity<String>("Tournament with that name already exists (CODE 409)", HttpStatus.CONFLICT);
		}
		catch(Exception e){
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

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

	public int checkCookie(String auth) throws Exception {
		Statement st = connection.createStatement();
		String query = String.format("Select user_id from sessions where session_id = '%s' and date > now() - interval '30' minute;", auth);
		ResultSet rs = st.executeQuery(query);
		if(rs.next()){
			int temp = rs.getInt(1);
			query = String.format("Update sessions set date = now() where session_id = '%s' and date > now() - interval '30' minute;", auth);
			st.execute(query);
			return temp;
		}
		throw new Exception("No such active auth token found");
	}

	public boolean checkFalseCookie(String auth) throws SQLException {
		if(auth.length()<30){
			return true;
		}
		Statement st = connection.createStatement();
		String query = String.format("Select user_id from sessions where session_id = '%s' and date > now() - interval '30' minute;", auth);
		ResultSet rs = st.executeQuery(query);
		return !rs.next();
	}

	public String hashPassword(String password, String username) throws NoSuchAlgorithmException {
		final MessageDigest digest = MessageDigest.getInstance("SHA3-256");
		final byte[] hashBytes = digest.digest((password + username.substring(0, 3)).getBytes(StandardCharsets.UTF_8));
		return bytesToHex(hashBytes);
	}

	private static String bytesToHex(byte[] hash) {
		StringBuilder hexString = new StringBuilder(2 * hash.length);
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if(hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}
}