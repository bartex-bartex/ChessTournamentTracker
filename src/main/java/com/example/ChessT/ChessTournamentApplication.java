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
import java.util.Objects;
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
			// Uncomment at the close
			/*
			Statement st = connection.createStatement();
			String query = "delete from sessions;";
			st.execute(query);
			*/
		}catch (Exception e){
			temp = e.getMessage();
		}
	}

	@GetMapping("/api/user")
	public ResponseEntity<String> user(@CookieValue(value = "auth", defaultValue = "xd") String auth){
		int userId = -1;
		try{
			userId = checkCookie(auth);
		}
		catch (Exception e) {
			return new ResponseEntity<>("No or expired authorization token (CODE 402)", HttpStatus.UNAUTHORIZED);
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
				return new ResponseEntity<>(result.toString(), HttpStatus.ACCEPTED);
			}
			else {
				return new ResponseEntity<>("Data base error (probably no relevant user found) (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		catch(SQLException e) {
			return new ResponseEntity<>("Data base error (probably no relevant user found) (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/api/user/account/{userId}")
	public ResponseEntity<String> account(@CookieValue(value = "auth", defaultValue = "xd") String auth,
										  @PathVariable int userId) {
		try {
			try {
				checkCookie(auth);
			} catch (Exception e) {
				return new ResponseEntity<>("No or expired authorization token (CODE 402)", HttpStatus.UNAUTHORIZED);
			}
			Statement st = connection.createStatement();
			String query = String.format("select username,first_name,last_name,sex,date_of_birth,fide from users where user_id = '%d';", userId);
			ResultSet rs = st.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			JSONObject result = new JSONObject();
			if (rs.next()) {
				for (int i = 1; i <= rsmd.getColumnCount(); i++) {
					result.put(rsmd.getColumnLabel(i), rs.getString(i));
				}
				return new ResponseEntity<>(result.toString(), HttpStatus.OK);
			}
			return new ResponseEntity<>("Data base error (probably no relevant user found) (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		catch(Exception e){
			return new ResponseEntity<>("Data base error (probably no relevant user found) (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@GetMapping("/api/validate-session")
	public ResponseEntity<String> validate(@CookieValue(value = "auth", defaultValue = "xd") String auth){
		int userId = -1;
		try{
			userId = checkCookie(auth);
		}
		catch (Exception e) {
			return new ResponseEntity<>("No or expired authorization token (CODE 402)", HttpStatus.UNAUTHORIZED);
		}
		JSONObject result = new JSONObject();
		result.put("valid",true);
		result.put("user_id",userId);
		return new ResponseEntity<>(result.toString(),HttpStatus.OK);
	}

	@RequestMapping("/api/user/login")
	public ResponseEntity<String> login(@CookieValue(value = "auth", defaultValue = "") String auth,
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
				return new ResponseEntity<>("Username or password incorrect (CODE 404)", HttpStatus.NOT_FOUND);
			}
			addAuthCookie(response, rs.getInt(1));

			return new ResponseEntity<>("Successfully logged in (CODE 200)", HttpStatus.OK);
		}
		catch(Exception e){
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping("/api/user/logout")
	public ResponseEntity<String> logout(@CookieValue(value = "auth", defaultValue = "xd") String auth) {
		try {
			if (checkFalseCookie(auth)) {
				return new ResponseEntity<>("No user to log out (CODE 409)", HttpStatus.CONFLICT);
			}
			Statement st = connection.createStatement();
			String query = String.format("delete from sessions where session_id = '%s'", auth);
			st.execute(query);
			return new ResponseEntity<>("Successfully logged out (CODE 200)", HttpStatus.OK);
		}
		catch (Exception e){
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping("/api/user/register")
	public ResponseEntity<String> register(@CookieValue(value = "auth",defaultValue = "") String auth,
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
				query = String.format("insert into users values ('%d','%s','%s','%s','%s','%s','%s','%s','%s')", id, username, mail, hashPassword(password, username), name, lastname, sex, date, fide);
				st.execute(query);
				addAuthCookie(response, id);
				return new ResponseEntity<>("Successfully registered (CODE 200)", HttpStatus.OK);
			}
			return new ResponseEntity<>("User with this username or email already exists (CODE 409)", HttpStatus.CONFLICT);
		}
		catch(Exception e){
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping("/api/tournament/create")
	public ResponseEntity<String> create(@CookieValue(value = "auth", defaultValue = "xd") String auth,
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
			return new ResponseEntity<>("No or expired authorization token (CODE 402)", HttpStatus.UNAUTHORIZED);
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
				query = String.format("insert into tournament_roles (user_id,tournament_id,role) values (%d,%d,'admin')",userId,id);
				st.execute(query);

				return new ResponseEntity<>("Tournament successfully registered (CODE 200)", HttpStatus.OK);
			}
			return new ResponseEntity<>("Tournament with that name already exists (CODE 409)", HttpStatus.CONFLICT);
		}
		catch(Exception e){
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@RequestMapping("/api/tournament/join/{tournamentId}")
	public ResponseEntity<String> join(@CookieValue(value = "auth", defaultValue = "xd") String auth,
										@PathVariable int tournamentId){
		int userId = -1;
		try{
			userId = checkCookie(auth);
		}
		catch (Exception e) {
			return new ResponseEntity<>("No or expired authorization token (CODE 402)", HttpStatus.UNAUTHORIZED);
		}
		try {
			Statement st = connection.createStatement();
			String query = String.format("select tournament_state from tournaments where tournament_id = %d;",tournamentId);
			ResultSet rs = st.executeQuery(query);
			if(!rs.next()){
				return new ResponseEntity<>("Ivalid tournament id (CODE 409)", HttpStatus.CONFLICT);
			}
			if(rs.getInt(1) > 0){
				return new ResponseEntity<>("Tournament is already started (CODE 409)", HttpStatus.CONFLICT);
			}
			query = String.format("SELECT count(*) from tournament_roles where tournament_id = %d and user_id = %d;",tournamentId,userId);
			rs = st.executeQuery(query);
			rs.next();
			if (rs.getInt(1) >= 1)
				return new ResponseEntity<>("This user is already assigned to this tournament (CODE 409)",HttpStatus.CONFLICT);
			query = String.format("insert into tournament_roles (user_id, tournament_id, role) values (%d,%d,'player')",userId,tournamentId);
			st.execute(query);
			return new ResponseEntity<>("Joined to the tournament successfully (CODE 200)",HttpStatus.OK);
		}catch (Exception E){
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/api/tournament/{tournamentId}")
	public ResponseEntity<String> tournamentInfo(@PathVariable int tournamentId){
		try {
			Statement st = connection.createStatement();
			String query = String.format("SELECT * from tournaments where tournament_id = %d;",tournamentId);
			ResultSet rs = st.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			JSONObject result = new JSONObject();
			if (!rs.next()) {
				return new ResponseEntity<>("Data base error (probably no relevant tournament found) (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
			}
			boolean started = false;
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				result.put(rsmd.getColumnLabel(i), rs.getString(i));
				if(rsmd.getColumnLabel(i).equals("tournament_state") && rs.getInt(i) > 0)
					started = true;
			}
			if(!started){
				JSONArray array = new JSONArray();
				query = String.format("SELECT user_id, first_name, last_name, username, fide from tournament_roles join users using(user_id) where tournament_id = %d and role = 'player';",tournamentId);
				rs = st.executeQuery(query);
				rsmd = rs.getMetaData();
				while (rs.next()) {
					JSONObject row = new JSONObject();
					for (int i = 1; i <= rsmd.getColumnCount(); i++) {
						row.put(rsmd.getColumnLabel(i), rs.getString(i));
					}
					array.put(row);
				}
				result.put("players", array);
				return new ResponseEntity<>(result.toString(), HttpStatus.OK);
			}
			JSONArray rankingC = new JSONArray();
			query = String.format("SELECT u.user_id, first_name, last_name, username, sum(fc.value) as fide_change, coalesce(start_fide, u.fide) as start_fide, (select sum(case when white_player_id = u.user_id then (CASE WHEN score = 1 THEN 1 WHEN score = 0 THEN 0.5 WHEN score = -1 THEN 0 ELSE 0 END) else (CASE WHEN score = 1 THEN 0 WHEN score = 0 THEN 0.5 WHEN score = -1 THEN 1 ELSE 0 END) end) as score from matches m where tournament_id = 1 and (white_player_id = u.user_id or black_player_id =u.user_id)) as score from fide_changes fc join matches m using(match_id) join users u using(user_id) join tournament_roles tr on tr.tournament_id = m.tournament_id and u.user_id = tr.user_id where m.tournament_id = %d group by u.user_id, first_name, last_name, username, start_fide;",tournamentId);
			rs = st.executeQuery(query);
			rsmd = rs.getMetaData();
			while (rs.next()) {
				JSONObject row = new JSONObject();
				for (int i = 1; i <= rsmd.getColumnCount(); i++) {
					row.put(rsmd.getColumnLabel(i), rs.getString(i));
				}
				rankingC.put(row);
			}
			result.put("player_data", rankingC);
			return new ResponseEntity<>(result.toString(), HttpStatus.OK);
		}catch (Exception e){
			return new ResponseEntity<>("Internal server error (CODE 500)" + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/api/tournament/match/{matchId}")
	public ResponseEntity<String> getMatch(@PathVariable int matchId){
		try{
			Statement st = connection.createStatement();
			String query = String.format("select m.match_id, m.white_player_id, m.black_player_id, m.score, m.round, m.table, coalesce(m.game_notation,'') as game_notation, u1.first_name as white_first_name, u1.last_name as white_last_name, u1.fide as white_fide, u2.first_name as black_first_name, u2.last_name as black_last_name, u2.fide as black_fide from matches m join users u1 on m.white_player_id = u1.user_id join users u2 on m.black_player_id = u2.user_id where m.match_id = %d;",matchId);
			ResultSet rs = st.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			JSONObject result = new JSONObject();
			if (rs.next()) {
				for (int i = 1; i <= rsmd.getColumnCount(); i++) {
					result.put(rsmd.getColumnLabel(i), rs.getString(i));
				}
				return new ResponseEntity<>(result.toString(), HttpStatus.OK);
			}
			return new ResponseEntity<>("Data base error (probably no relevant match found) (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);

		}catch (Exception e){
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@GetMapping("/api/tournament/round")
	public ResponseEntity<String> getRound(@RequestParam (value = "tournamentId") int tournamentId,
										   @RequestParam (value = "round") int round){
		try {
			Statement st = connection.createStatement();
			String query = String.format("select m.match_id, m.white_player_id, m.black_player_id, coalesce(m.score,'') as score, m.round, m.table, coalesce(m.game_notation,'') as game_notation, u1.first_name as white_first_name, u1.last_name as white_last_name, u1.fide as white_fide, u2.first_name as black_first_name, u2.last_name as black_last_name, u2.fide as black_fide from matches m join users u1 on m.white_player_id = u1.user_id join users u2 on m.black_player_id = u2.user_id where m.tournament_id = %d and m.round = %d;",tournamentId,round);
			ResultSet rs = st.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			JSONArray result = new JSONArray();
			while (rs.next()) {
				JSONObject row = new JSONObject();
				for (int i = 1; i <= rsmd.getColumnCount(); i++) {
					row.put(rsmd.getColumnLabel(i), rs.getString(i));
				}
				result.put(row);
			}
			if (result.isEmpty())
				return new ResponseEntity<>("Data base error (probably no relevant matches found) (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
			return new ResponseEntity<>(result.toString(), HttpStatus.OK);
		}
		catch (Exception e){
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping("/api/tournament/player")
	public ResponseEntity<String> playerInfo(@RequestParam(value = "tournamentId") int tournamentId,
											 @RequestParam(value = "userId") int userId){ //not done yet
		try {
			Statement st = connection.createStatement();
			String query = String.format("select u.user_id, u.username, u.first_name, u.last_name, tr.start_role, tr.tournament_id, coalesce(f.change_in_rank,0) as rank_change from users u join tournament_roles tr on u.user_id = tr.user_id left join (select user_id, sum(value) as change_in_rank from fide_changes join matches using(match_id) where tournament_id = %d group by user_id) f on tr.user_id = f.user_id where tr.role = 'player' and tr.tournament_id = %d and u.user_id = %d;",tournamentId,tournamentId,userId);
			ResultSet rs = st.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			JSONObject result = new JSONObject();
			JSONArray opponents = new JSONArray();

			if (rs.next()) {
				for (int i = 1; i <= rsmd.getColumnCount(); i++) {
					result.put(rsmd.getColumnLabel(i), rs.getString(i));
				}
				query = String.format("""
						select tournament_id, match_id, u.user_id as opponent_id, u.first_name, u.last_name,
						(CASE
						WHEN score = 1 THEN 1
						WHEN score = 0 THEN 0.5
						WHEN score = -1 THEN 0
						ELSE -1 END) as score,
						m.round, u.fide, m.table from matches m join users u on m.black_player_id = u.user_id
						where tournament_id = %d and white_player_id = %d
						union
						select tournament_id, match_id, u.user_id as opponent_id, u.first_name, u.last_name,
						(CASE
						WHEN score = 1 THEN 0
						WHEN score = 0 THEN 0.5
						WHEN score = -1 THEN 1
						ELSE -1 END) as score,
						m.round, u.fide, m.table from matches m join users u on m.white_player_id = u.user_id
						where tournament_id = %d and black_player_id = %d;
						""",tournamentId,userId,tournamentId,userId);
				rs = st.executeQuery(query);
				rsmd = rs.getMetaData();
				int avg = 0, j=0;
				float score = 0;
				float temp;
				while (rs.next()) {
					JSONObject row = new JSONObject();
					for (int i = 1; i <= rsmd.getColumnCount(); i++) {
						if (rsmd.getColumnLabel(i).equals("fide"))
							avg += rs.getInt(i);
						if (rsmd.getColumnLabel(i).equals("score")){
							temp = rs.getFloat(i);
							if (temp == -1) {
								row.put(rsmd.getColumnLabel(i),"nodata");
								continue;
							}
							score += temp;
						}
						row.put(rsmd.getColumnLabel(i), rs.getString(i));
 					}
					j++;
					opponents.put(row);
				}
				result.put("opponents",opponents);
				result.put("SUM",String.valueOf(score));
				result.put("AVG_FIDE",String.valueOf(avg/j));
				return new ResponseEntity<>(result.toString(), HttpStatus.OK);
			}
			return new ResponseEntity<>("Data base error (probably no relevant player in that tournament found) (CODE 409)", HttpStatus.CONFLICT);

		} catch (Exception e) {
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping("/api/tournament/round/addmatch") // "/api/{tournament_id}/{round}/addmatch" ???
	public ResponseEntity<String> addMatch(@CookieValue(value = "auth", defaultValue = "xd") String auth,
										   @RequestParam(value = "tournament_id") int tournamentId,
										   @RequestParam(value = "white_player_id") int wId,
										   @RequestParam(value = "black_player_id") int bId,
										   @RequestParam(value = "table", defaultValue = "-1") int table,
										   @RequestParam(value = "round") int round,
										   @RequestParam(value = "score", defaultValue = "2") int score,
										   @RequestParam(value = "game_notation", defaultValue = "") String gameNotation
										   ){
		int userId = -1;
		try{
			userId = checkCookie(auth);
		}
		catch (Exception e) {
			return new ResponseEntity<String>("No or expired authorization token (CODE 402)", HttpStatus.UNAUTHORIZED);
		}
		try {
			Statement st = connection.createStatement();
			String query = String.format("select role from tournament_roles where tournament_id = %d and user_id = %d;",tournamentId,userId);
			ResultSet rs = st.executeQuery(query);
			if (rs.next() && rs.getString(1).equals("admin")){
				query = String.format("select count(*) from tournament_roles where tournament_id = %d and user_id in (%d,%d);",tournamentId,wId,bId);
				rs = st.executeQuery(query);
				rs.next();
				if (rs.getInt(1) < 2)
					return new ResponseEntity<>("One or more player ids are invalid (CODE 409)",HttpStatus.CONFLICT);
				query = String.format("select match_id from matches where tournament_id = %d and white_player_id = %d and black_player_id = %d and round = %d",tournamentId,wId,bId,round);
				rs = st.executeQuery(query);
				int matchId;
				if (rs.next()){
					matchId = rs.getInt(1);
					if(table==-1)
						query = String.format("update matches set score = %d, game_notation = '%s' where match_id = %d;",score,gameNotation,matchId);
					else
						query = String.format("update matches set score = %d, \"table\" = %d, game_notation = '%s' where match_id = %d;",score,table,gameNotation,matchId);
					st.execute(query);
					return new ResponseEntity<>("Match successfully updated (CODE 200)",HttpStatus.OK);
				}
				else {
					query = "select coalesce(max(match_id),0) from matches";
					rs = st.executeQuery(query);
					rs.next();
					matchId = 1 + rs.getInt(1);
					query = String.format("insert into matches values (%d,%d,%d,%d,%d,%d,%d,'%s')",matchId,wId,bId,tournamentId,score,round,table,gameNotation);
					st.execute(query);
					return new ResponseEntity<>("Match successfully added (CODE 200)",HttpStatus.OK);
				}

			}
			return new ResponseEntity<>("No permissions to add match (CODE 403)",HttpStatus.FORBIDDEN);
		}
		catch (Exception e)
		{
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping("/api/tournament/start/{tournamentId}")
	public ResponseEntity<String> startTournament(@PathVariable (value="tournamentId")int tournamentId,
												  @CookieValue (value="auth", defaultValue = "xd") String auth){
		int userId = -1;
		try{
			userId = checkCookie(auth);
		}
		catch (Exception e) {
			return new ResponseEntity<>("No or expired authorization token (CODE 402)", HttpStatus.UNAUTHORIZED);
		}
		try{
			Statement st = connection.createStatement();
			String query = String.format("select role from tournament_roles where tournament_id = %d and user_id = %d;",tournamentId,userId);
			ResultSet rs = st.executeQuery(query);
			if (rs.next()){
				if (!rs.getString(1).equals("admin")){
					return new ResponseEntity<>("You are not admin of this tournament (CODE 402)",HttpStatus.UNAUTHORIZED);
				}else{
					query = String.format("select tournament_state from tournaments where tournament_id = %d;",tournamentId,userId);
					rs = st.executeQuery(query);
					rs.next();
					switch(rs.getInt(1)) {
						case 1:
							return new ResponseEntity<>("This tournament is in progress (CODE 409)", HttpStatus.CONFLICT);
						case 2:
							return new ResponseEntity<>("This tournament has finished (CODE 409)", HttpStatus.CONFLICT);
					}
					query = String.format("""
							UPDATE tournament_roles
							SET start_fide = (SELECT fide from users u2 where u2.user_id = tournament_roles.user_id)
							where tournament_id = %d and role = 'player';
							UPDATE tournaments
							SET tournament_state = 1, start_date=now()
							WHERE tournament_id = %d;""", tournamentId, tournamentId);
					st.execute(query);
					return new ResponseEntity<>("Tournament began! (CODE 200)", HttpStatus.OK);
				}
			}else{
				return new ResponseEntity<>("Tournament doesn't exist or you are not member of the tournament (CODE 409)",HttpStatus.CONFLICT);
			}
		}catch (Exception e){
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@RequestMapping("/api/tournament/end/{tournamentId}")
	public ResponseEntity<String> endTournament(@PathVariable (value="tournamentId")int tournamentId,
												  @CookieValue (value="auth", defaultValue = "xd") String auth){
		int userId = -1;
		try{
			userId = checkCookie(auth);
		}
		catch (Exception e) {
			return new ResponseEntity<>("No or expired authorization token (CODE 402)", HttpStatus.UNAUTHORIZED);
		}
		try{
			Statement st = connection.createStatement();
			String query = String.format("select role from tournament_roles where tournament_id = %d and user_id = %d;",tournamentId,userId);
			ResultSet rs = st.executeQuery(query);
			if (rs.next()){
				if (!rs.getString(1).equals("admin")){
					return new ResponseEntity<>("You are not admin of this tournament (CODE 402)",HttpStatus.UNAUTHORIZED);
				}else{
					query = String.format("select tournament_state from tournaments where tournament_id = %d;",tournamentId,userId);
					rs = st.executeQuery(query);
					rs.next();
					switch(rs.getInt(1)) {
						case 0:
							return new ResponseEntity<>("This tournament hasn't started (CODE 409)", HttpStatus.CONFLICT);
						case 2:
							return new ResponseEntity<>("This tournament has already finished (CODE 409)", HttpStatus.CONFLICT);
					}
					query = String.format("""
							UPDATE tournaments
							SET tournament_state = 2, end_date = now()
							WHERE tournament_id = 3;""", tournamentId);
					st.execute(query);
					return new ResponseEntity<>("Tournament has ended! (CODE 200)", HttpStatus.OK);
				}
			}else{
				return new ResponseEntity<>("Tournament doesn't exist or you are not member of the tournament (CODE 409)",HttpStatus.CONFLICT);
			}
		}catch (Exception e){
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@GetMapping("/api/homepage")
	public ResponseEntity<String> homepage(){
		try {
			Statement st = connection.createStatement();
			String query = "select tournament_id, name,location,time_control,start_date from tournaments where start_date>now() and start_date<now() + interval '3' month limit 100;";
			ResultSet rs = st.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			JSONArray result = new JSONArray();
			while (rs.next()) {
				JSONObject row = new JSONObject();
				for (int i = 1; i <= rsmd.getColumnCount(); i++) {
					row.put(rsmd.getColumnLabel(i), rs.getString(i));
				}
				result.put(row);
			}
			if (result.isEmpty())
				return new ResponseEntity<>("Data base error (probably no relevant tournaments found) (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
			return new ResponseEntity<>(result.toString(), HttpStatus.OK);

		}catch (Exception e){
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	public String randomString32Char() {
		int leftLimit = 48; // numeral '0'
		int rightLimit = 122; // letter 'z'
		int targetStringLength = 32;
		Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
				.filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
				.limit(targetStringLength)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();
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
		c.setPath("/api/");
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
		final byte[] hashBytes = digest.digest((password + username).getBytes(StandardCharsets.UTF_8));
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