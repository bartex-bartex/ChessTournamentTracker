package com.example.ChessT;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.*;
import org.json.*;
import java.util.Set;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.commons.validator.GenericValidator;

@SpringBootApplication
@RestController
@EnableScheduling
@CrossOrigin("*")
public class ChessTournamentApplication {
	static Connection connection = null;
	static String temp;
	static String regexMailValidationPattern = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:"
			+ "(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|"
			+ "\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.)"
			+ "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
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
			// Uncomment (Open) at the close
			/*
			Statement st = connection.createStatement();
			String query = "delete from sessions;";
			st.execute(query);
			*/
		}catch (Exception e){
			temp = e.getMessage();
		}
	}
	class Player{
		Player(int userId, int rank){
			this.userId = userId;
			this.rank = rank;
		}
		Player(int userId, int rank, int bye){
			this.userId = userId;
			this.rank = rank;
			this.bye = bye;
			if (bye != 0)
				score += 1;
		}
		public boolean played = false;
		public int bye = 0;
		public int userId;
		public int rank;
		public float score = 0;
		public int playedAsWhite = 0;
		public int playedAsBlack = 0;
		public Set<Integer> alreadyPlayed = new HashSet<>();
		public int playRatio;
		public int canPlayWith(Player player) { // 1 - can play as white, 0 - can play as black, -1 - cannot play

			if (played || player.played || alreadyPlayed.contains(player.userId) || (playRatio >= 2 && player.playRatio >= 2) || (playRatio <= -2 && player.playRatio <= -2))
				return -1;
			if (playRatio > player.playRatio)
				return 0;
			if (playRatio == player.playRatio)
				return (rank >= player.rank? 1:0);
			return 1;
		}

		public int canPlayWithLessRestricted(Player player) { // 1 - can play as white, 0 - can play as black, -1 - cannot play
			if (played || player.played) //|| alreadyPlayed.contains(player.userId))
				return -1;
			if (playRatio > player.playRatio)
				return 0;
			if (playRatio == player.playRatio)
				return (rank >= player.rank? 1:0);
			return 1;
		}
	}
	public boolean firstRoundPairings(int tournamentId){
		ArrayList<Player> list = new ArrayList<Player>();
		try{
			Statement st = connection.createStatement();
			String query = String.format("select user_id, start_fide from tournament_roles where role = 'player' and tournament_id = %d;",tournamentId);
			ResultSet rs = st.executeQuery(query);
			while(rs.next()){
				list.add(new Player(rs.getInt(1),rs.getInt(2)));
			}
			if (list.isEmpty())
				return false;
			list.sort(Comparator.comparingDouble(player -> player.rank));
			int start = 0;
			if (list.size() % 2 == 1){
				start = 1;
				query = String.format("update tournament_roles set bye = 1 where tournament_id = %d and user_id = %d;",tournamentId,list.get(0).userId);
				st.execute(query);
			}
			for (int i=start;i<list.size();i+=2){
				query = String.format("insert into matches (match_id,tournament_id,white_player_id,black_player_id,round)\n" +
						"values ((select 1+max(match_id) from matches),%d,%d,%d,1);",tournamentId,list.get(i).userId,list.get(i+1).userId);
				st.execute(query);
			}
			return true;
		}catch(Exception e){
			return false;
		}
	}
	@RequestMapping("/api/tournament/generateRoundPairings/{tournamentId}/{round}")
	public ResponseEntity<String> generateRoundPairings(@PathVariable(value = "tournamentId") int tournamentId,
														@PathVariable(value = "round") int round){
		//walidacja czy w runda < max ilosc rund w turnieju
		//walidacja czy juz nie ma jakiegos meczu w tej rundzie czyli czy nie jest juz wygenerowany
		//walidacja czy jestes adminem turnieju bo nie robie tego jeszcze xd

		ArrayList<Player> list = new ArrayList<Player>();
		try{
			Statement st = connection.createStatement();
			/*String query = String.format();
			ResultSet rs = st.executeQuery(query);*/


			String query = String.format("select user_id, start_fide, bye from tournament_roles where role = 'player' and tournament_id = %d;",tournamentId);
			ResultSet rs = st.executeQuery(query);
			while(rs.next()){
				list.add(new Player(rs.getInt(1),rs.getInt(2),rs.getInt(3)));
			}
			if (list.isEmpty())
				return new ResponseEntity<>("Nie ma graczy",HttpStatus.I_AM_A_TEAPOT);
            for (Player player : list) {
                query = String.format("""
                        select 'bialy' as kolor,
                        sum((CASE
                        WHEN score = 1 THEN 1
                        WHEN score = 0 THEN 0.5
                        WHEN score = -1 THEN 0
                        ELSE null END)) as score,
                        count(*) as ile_meczy
                        from matches m join users u on m.black_player_id = u.user_id
                        where tournament_id = %d and white_player_id = %d
                        union
                        select 'czarny' as kolor,
                        sum((CASE
                        WHEN score = 1 THEN 0
                        WHEN score = 0 THEN 0.5
                        WHEN score = -1 THEN 1
                        ELSE null END)) as score,
                        count(*) as ile_meczy
                        from matches m join users u on m.white_player_id = u.user_id
                        where tournament_id = %d and black_player_id = %d;
                        """, tournamentId, player.userId, tournamentId, player.userId);
                rs = st.executeQuery(query);
                rs.next();
                player.playedAsWhite = rs.getInt(3);
                player.score = rs.getFloat(2);
                rs.next();
                player.playedAsBlack = rs.getInt(3);
                player.score += rs.getFloat(2);
                player.playRatio = player.playedAsWhite - player.playedAsBlack;

				query = String.format("""
						select user_id
						from matches m join users u on m.black_player_id = u.user_id
						where tournament_id = %d and white_player_id = %d
						union
						select user_id
						from matches m join users u on m.white_player_id = u.user_id
						where tournament_id = %d and black_player_id = %d;
						""",tournamentId,player.userId,tournamentId,player.userId);
				rs = st.executeQuery(query);
				while(rs.next())
					player.alreadyPlayed.add(rs.getInt(1));
            }
			list.sort(Comparator.comparingDouble(player -> player.score));
			if (list.size() % 2 == 1) {
				Player leastFide = list.get(0);
				for (Player player : list){
					if ((player.rank < leastFide.rank && player.bye == 0) || (player.bye == 0 && leastFide.bye != 0))
						leastFide = player;
				}
				leastFide.played = true;
				query = String.format("update tournament_roles set bye = %d where user_id = %d and tournament_id = %d and role = 'player';", round, leastFide.userId, tournamentId);
				st.execute(query);

			}
			int temp;
			for (int i=list.size()-1;i>=0;i--){
				for (int j=i-1;j>=0;j--){
					temp = list.get(i).canPlayWith(list.get(j));
					if (temp != -1) {
						list.get(i).played = true;
						list.get(j).played = true;
						query = String.format("insert into matches(match_id, tournament_id, round, white_player_id, " +
								"black_player_id) values((select 1 + max(match_id) from matches),%d,%d,%d,%d);",
								tournamentId,round,(temp == 1 ? list.get(i).userId:list.get(j).userId),(temp == 0 ? list.get(i).userId:list.get(j).userId));
						st.execute(query);
						break;
					}
				}
				if (!list.get(i).played){
					for (int j=i-1;j>=0;j--){
						temp = list.get(i).canPlayWithLessRestricted(list.get(j));
						if (temp != -1) {
							list.get(i).played = true;
							list.get(j).played = true;
							query = String.format("insert into matches(match_id, tournament_id, round, white_player_id, " +
											"black_player_id) values((select 1 + max(match_id) from matches),%d,%d,%d,%d);",
									tournamentId,round,(temp == 1 ? list.get(i).userId:list.get(j).userId),(temp == 0 ? list.get(i).userId:list.get(j).userId));
							st.execute(query);
							break;
						}
					}
				}
				if (!list.get(i).played)
					return new ResponseEntity<>("Nie udalo sie kogos w zaden sposob spairowac xd",HttpStatus.I_AM_A_TEAPOT);
			}
		}
		catch(Exception e){
			return new ResponseEntity<>("powodzenia w szukaniu bledu",HttpStatus.I_AM_A_TEAPOT);
		}
		return new ResponseEntity<>("powinno byc g",HttpStatus.I_AM_A_TEAPOT);
	}


	@Scheduled(cron = "0 10 0 * * ?")
	public void fideUpdate(){
		try{
			Statement st = connection.createStatement();
			String query = String.format("""
					UPDATE users
					SET fide = fide+coalesce((SELECT sum(fc.value) from fide_changes fc
					join matches m using(match_id) join tournaments t using(tournament_id)
					where tournament_state = 2 and extract('MONTH' from end_date) = extract('MONTH' from now() - interval '1' month)
					and extract('month' from end_date) = extract('month' from now() - interval '1' month) and fc.user_id = users.user_id), 0);
					UPDATE users
					set k = (case
							when fide > 2400 or k=10 then 10
							when fide > 2300 or k=20 then 20
							when date_of_birth < now() - interval '18' year then 20
							else 40 end);
								""");
			st.execute(query);

		}catch (Exception e){
			return;
		}
	}

	@GetMapping("/api/user")
	public ResponseEntity<String> user(@CookieValue(value = "auth", defaultValue = "xd") String auth){
		int userId = -1;
		try{
			userId = checkCookie(auth);
		}
		catch (Exception e) {
			return new ResponseEntity<>("No or expired authorization token (CODE 401)", HttpStatus.UNAUTHORIZED);
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
				return new ResponseEntity<>("No or expired authorization token (CODE 401)", HttpStatus.UNAUTHORIZED);
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
			return new ResponseEntity<>("No or expired authorization token (CODE 401)", HttpStatus.UNAUTHORIZED);
		}
		JSONObject result = new JSONObject();
		result.put("valid",true);
		result.put("user_id",userId);
		return new ResponseEntity<>(result.toString(),HttpStatus.OK);
	}

	@PostMapping("/api/user/login") // @PostMapping
	public ResponseEntity<String> login(@CookieValue(value = "auth", defaultValue = "") String auth,
										//@RequestBody (value = )
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

	@RequestMapping("/api/user/logout") //@DeleteMapping
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



	@RequestMapping("/api/user/register") //@PostMapping
	public ResponseEntity<String> register(@CookieValue(value = "auth",defaultValue = "") String auth,
										   @RequestParam(value = "username") String username,
										   @RequestParam(value = "password") String password,
										   @RequestParam(value = "passwordAgain") String password2,
										   @RequestParam(value = "mail") String mail,
										   @RequestParam(value = "first_name") String name,
										   @RequestParam(value = "last_name") String lastname,
										   @RequestParam(value = "sex") String sex,
										   @RequestParam(value = "date_of_birth") String date,
										   @RequestParam(value = "fide") int fide,
										   HttpServletResponse response) {
		try {
			if (!checkFalseCookie(auth))
				return new ResponseEntity<>("User is already logged in (CODE 409)", HttpStatus.CONFLICT);
			//Minimum eight characters, at least one uppercase letter, one lowercase letter, one number and one special character
			if(!validate("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",password))
				return new ResponseEntity<>("Password is invalid (CODE 400)", HttpStatus.BAD_REQUEST);

			if(!password.equals(password2))
				return new ResponseEntity<>("Passwords are not equal (CODE 400)", HttpStatus.CONFLICT);

			if(!validate(regexMailValidationPattern,mail))
				return new ResponseEntity<>("Mail is invalid (CODE 400)",HttpStatus.BAD_REQUEST);

			if(!validate("^[A-Za-z]+(?:[' -][A-Za-z]+)*$",name))
				return new ResponseEntity<>("First name is invalid (CODE 400)",HttpStatus.BAD_REQUEST);

			if(!validate("^[A-Za-z]+(?:[' -][A-Za-z]+)*$",lastname))
				return new ResponseEntity<>("Last is invalid (CODE 400)",HttpStatus.BAD_REQUEST);

			if(!validate("^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])$",date))
				return new ResponseEntity<>("Wrong date format, date format should be yyyy-mm-dd (CODE 400)",HttpStatus.BAD_REQUEST);

			if(!GenericValidator.isDate(date,"yyyy-MM-dd",true)){
				return new ResponseEntity<>("Date is invalid (CODE 400)", HttpStatus.BAD_REQUEST);
			}


			if(fide < 0)
				return new ResponseEntity<>("Fide is invalid (CODE 400)",HttpStatus.BAD_REQUEST);


			Statement st = connection.createStatement();
			String query = String.format("select count(x) from (select * from users where username = '%s' or mail = '%s') as x", username, mail);
			ResultSet rs = st.executeQuery(query);
			rs.next();
			int rowCount = rs.getInt(1);
			if (rowCount == 0) {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-d");
				LocalDate localDate = LocalDate.parse(date, formatter);
				boolean adult = Period.between(LocalDate.parse(date, formatter),LocalDate.now()).getYears()>=18;
				query = "select coalesce(max(user_id),0) from users";
				rs = st.executeQuery(query);
				rs.next();
				int id = 1 + rs.getInt(1);
				query = String.format("insert into users values ('%d','%s','%s','%s','%s','%s','%s','%s',%d, %d)", id, username, mail, hashPassword(password, username), name, lastname, sex, date, fide, kValue(fide, adult));
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

	@RequestMapping("/api/tournament/create") //@PostMapping
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
			return new ResponseEntity<>("No or expired authorization token (CODE 401)", HttpStatus.UNAUTHORIZED);
		}

		if(!validate("^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])$",startDate))
			return new ResponseEntity<>("Wrong start date format, date format should be yyyy-mm-dd (CODE 400)",HttpStatus.BAD_REQUEST);

		if(!GenericValidator.isDate(startDate,"yyyy-MM-dd",true)){
			return new ResponseEntity<>("Date is invalid (CODE 400)", HttpStatus.BAD_REQUEST);
		}

		if(!validate("^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])$",endDate))
			return new ResponseEntity<>("Wrong end date format, date format should be yyyy-mm-dd (CODE 400)",HttpStatus.BAD_REQUEST);

		if(!GenericValidator.isDate(endDate,"yyyy-MM-dd",true)){
			return new ResponseEntity<>("Date is invalid (CODE 400)", HttpStatus.BAD_REQUEST);
		}

		if(rounds <= 0){
			return new ResponseEntity<>("Number of rounds (CODE 400)", HttpStatus.BAD_REQUEST);
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

	@RequestMapping("/api/tournament/join/{tournamentId}") //@PostMapping
	public ResponseEntity<String> join(@CookieValue(value = "auth", defaultValue = "xd") String auth,
										@PathVariable int tournamentId){
		int userId = -1;
		try{
			userId = checkCookie(auth);
		}
		catch (Exception e) {
			return new ResponseEntity<>("No or expired authorization token (CODE 401)", HttpStatus.UNAUTHORIZED);
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
				return new ResponseEntity<>("Data base error (probably no relevant tournament found) (CODE 409)", HttpStatus.CONFLICT);
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
			JSONArray playerData = results(tournamentId);
			if (playerData.isEmpty())
				return new ResponseEntity<>("No users in this tournament (CODE 409)",HttpStatus.CONFLICT);
			result.put("player_data", playerData);
			return new ResponseEntity<>(result.toString(), HttpStatus.OK);
		}catch (Exception e){
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
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
			return new ResponseEntity<>("Data base error (probably no relevant match found) (CODE 409)", HttpStatus.CONFLICT);

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
				return new ResponseEntity<>("Data base error (probably no relevant matches found) (CODE 409)", HttpStatus.CONFLICT);
			return new ResponseEntity<>(result.toString(), HttpStatus.OK);
		}
		catch (Exception e){
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping("/api/tournament/player")
	public ResponseEntity<String> playerInfo(@RequestParam(value = "tournamentId") int tournamentId,
											 @RequestParam(value = "userId") int userId){
		try {
			Statement st = connection.createStatement();
				String query = String.format("select u.user_id, u.username, u.first_name, u.last_name, tr.start_fide, tr.tournament_id, coalesce(f.change_in_rank,0) as rank_change from users u join tournament_roles tr on u.user_id = tr.user_id left join (select user_id, sum(value) as change_in_rank from fide_changes join matches using(match_id) where tournament_id = %d group by user_id) f on tr.user_id = f.user_id where tr.role = 'player' and tr.tournament_id = %d and u.user_id = %d;",tournamentId,tournamentId,userId);
			ResultSet rs = st.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			JSONObject result = new JSONObject();
			JSONArray opponents = new JSONArray();

			if (rs.next()) {
				for (int i = 1; i <= rsmd.getColumnCount(); i++) {
					result.put(rsmd.getColumnLabel(i), rs.getString(i));
				}
				query = String.format("""
						select m.tournament_id, match_id, u.user_id as opponent_id, u.first_name, u.last_name,
						(CASE
						WHEN score = 1 THEN 1
						WHEN score = 0 THEN 0.5
						WHEN score = -1 THEN 0
						ELSE -1 END) as score,'white' as color,
						m.round, tr.start_fide, m.table
						from matches m join users u on m.black_player_id = u.user_id
						join tournament_roles tr on u.user_id = tr.user_id and tr.tournament_id = m.tournament_id
						where m.tournament_id = %d and white_player_id = %d
						union
						select m.tournament_id, match_id, u.user_id as opponent_id, u.first_name, u.last_name,
						(CASE
						WHEN score = 1 THEN 0
						WHEN score = 0 THEN 0.5
						WHEN score = -1 THEN 1
						ELSE -1 END) as score, 'black' as color,
						m.round, tr.start_fide, m.table
						from matches m join users u on m.white_player_id = u.user_id
						join tournament_roles tr on u.user_id = tr.user_id and tr.tournament_id = m.tournament_id
						where m.tournament_id = %d and black_player_id = %d;
						""",tournamentId,userId,tournamentId,userId);
				rs = st.executeQuery(query);
				rsmd = rs.getMetaData();
				int avg = 0, j=0;
				float score = 0;
				float temp;
				while (rs.next()) {
					JSONObject row = new JSONObject();
					for (int i = 1; i <= rsmd.getColumnCount(); i++) {
						if (rsmd.getColumnLabel(i).equals("start_fide"))
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
				query = String.format("select bye from tournament_roles where user_id = %d and tournament_id = %d;",userId,tournamentId);
				rs = st.executeQuery(query);
				rs.next();
				int bye = rs.getInt(1);
				result.put("bye",bye);

				result.put("opponents",opponents);
				result.put("sum",String.valueOf(score + (bye != 0 ? 1:0)));
				result.put("avg_fide",String.valueOf(avg/j));

				return new ResponseEntity<>(result.toString(), HttpStatus.OK);
			}
			return new ResponseEntity<>("No such tournament, user or invalid role assigned to that user (CODE 409)", HttpStatus.CONFLICT);

		} catch (Exception e) {
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public JSONArray results(@PathVariable(value = "tournamentId") int tournamentId) throws SQLException {
		Statement st = connection.createStatement();
		String query = String.format("""
					select player_id,first_name,last_name, tr.start_fide, coalesce(change_in_rank,0) as change_in_fide, sum(score1) +\s
					(SELECT CASE WHEN BYE = 0 THEN 0 ELSE 1
					END FROM tournament_roles where user_id = player_id) as score
					from (select m.white_player_id as player_id,
					sum((CASE
					WHEN score = 1 THEN 1
					WHEN score = 0 THEN 0.5
					WHEN score = -1 THEN 0
					ELSE null END)) as score1
					from matches m join users u on m.black_player_id = u.user_id
					where tournament_id = %d
					group by m.white_player_id
					union
					select black_player_id as player_id,
					sum((CASE
					WHEN score = 1 THEN 0
					WHEN score = 0 THEN 0.5
					WHEN score = -1 THEN 1
					ELSE null END)) as score1
					from matches m join users u on m.white_player_id = u.user_id
					where tournament_id = %d
					group by m.black_player_id) join users uk on player_id = uk.user_id
					join tournament_roles tr on player_id = tr.user_id and tournament_id = %d
					left join (select user_id, sum(value) as change_in_rank from fide_changes join matches using(match_id) where tournament_id = %d group by user_id) f
					on f.user_id = player_id
					group by player_id, first_name,last_name,start_fide,change_in_rank;
					""",tournamentId,tournamentId,tournamentId,tournamentId);
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
		return result;
	}

	@RequestMapping("/api/tournament/round/addmatch") // @PutMapping
	public ResponseEntity<String> addMatch(@CookieValue(value = "auth", defaultValue = "xd") String auth,
										   @RequestParam(value = "tournamentId") int tournamentId,
										   @RequestParam(value = "whitePlayerId") int wId,
										   @RequestParam(value = "blackPlayerId") int bId,
										   @RequestParam(value = "table", defaultValue = "-1") int table,
										   @RequestParam(value = "round") int round,
										   @RequestParam(value = "score", defaultValue = "2") int score,
										   @RequestParam(value = "gameNotation", defaultValue = "") String gameNotation
										   ){
		if(score <-1 || score >2){
			return new ResponseEntity<>("Invalid score value (CODE 409)", HttpStatus.CONFLICT);
		}
		int userId = -1;
		try{
			userId = checkCookie(auth);
		}
		catch (Exception e) {
			return new ResponseEntity<String>("No or expired authorization token (CODE 401)", HttpStatus.UNAUTHORIZED);
		}
		try {
			Statement st = connection.createStatement();
			String query = String.format("select role from tournament_roles where tournament_id = %d and user_id = %d;",tournamentId,userId);
			ResultSet rs = st.executeQuery(query);
			if (rs.next() && rs.getString(1).equals("admin")){
				query = String.format("select count(*), coalesce(rounds, 0) from tournament_roles join tournaments using(tournament_id) where tournament_id = %d and user_id in (%d,%d) group by rounds;",tournamentId,wId,bId);
				rs = st.executeQuery(query);
				if(!rs.next())
					return new ResponseEntity<>("At least one player has not joined this tournament (CODE 409)", HttpStatus.CONFLICT);
				if (rs.getInt(1) < 2 || wId==bId)
					return new ResponseEntity<>("One or more player ids are invalid (CODE 409)",HttpStatus.CONFLICT);
				if (round > rs.getInt(2) || round<1)
					return new ResponseEntity<>("Invalid round number (CODE 409)",HttpStatus.CONFLICT);
				query = String.format("select match_id from matches where tournament_id = %d and white_player_id in (%d,%d) and black_player_id in (%d,%d) and round = %d",tournamentId,wId,bId, wId, bId,round);
				rs = st.executeQuery(query);
				int matchId;
				if (rs.next()){
					matchId = rs.getInt(1);
					int mode = 0;
					if(table == -1) mode+=1;
					if(score == 2) mode+=2;
					if(gameNotation.isEmpty()) mode+=4;

                    query = switch (mode) {
                        case 1 ->
                                String.format("update matches set score = %d, game_notation = '%s', white_player_id = %d, black_player_id=%d where match_id = %d;", score, gameNotation, wId, bId, matchId);
                        case 2 ->
                                String.format("update matches set \"table\" = %d, game_notation = '%s', white_player_id = %d, black_player_id=%d where match_id = %d;", table, gameNotation, wId, bId, matchId);
                        case 3 ->
                                String.format("update matches set game_notation = '%s', white_player_id = %d, black_player_id=%d where match_id = %d;", gameNotation, wId, bId, matchId);
                        case 4 ->
                                String.format("update matches set score = %d, \"table\" = %d, white_player_id = %d, black_player_id=%d where match_id = %d;", score, table, wId, bId, matchId);
                        case 5 ->
                                String.format("update matches set score = %d, white_player_id = %d, black_player_id=%d where match_id = %d;", score, wId, bId, matchId);
                        case 6 ->
                                String.format("update matches set \"table\" = %d, white_player_id = %d, black_player_id=%d where match_id = %d;", table, wId, bId, matchId);
                        case 7 ->
                                String.format("update matches set white_player_id = %d, black_player_id=%d where match_id = %d;", wId, bId, matchId);
                        default ->
								String.format("update matches set score = %d, \"table\" = %d, game_notation = '%s', white_player_id = %d, black_player_id=%d where match_id = %d;", score, table, gameNotation, wId, bId, matchId);
                    };
					st.execute(query);
					if(score != 2){
						query = String.format("select count(*) from fide_changes where match_id = %d;", matchId);
						rs = st.executeQuery(query);
						rs.next();
						String query2 = String.format("""
								SELECT m.white_player_id, tr.start_fide, tr.k from matches m
								join tournament_roles tr on m.white_player_id = tr.user_id and m.tournament_id = tr.tournament_id
								where match_id = %d
								union
								SELECT m.black_player_id, tr.start_fide, tr.k from matches m
								join tournament_roles tr on m.black_player_id = tr.user_id and m.tournament_id = tr.tournament_id
								where match_id = %d;""", matchId, matchId);
						Statement st2 = connection.createStatement();
						ResultSet rs2 = st2.executeQuery(query2);
						rs2.next();
						int wR = rs2.getInt(2), wK = rs2.getInt(3);
						rs2.next();
						int bR = rs2.getInt(2), bK = rs2.getInt(3);
						int wF = fideChange(wR, bR, wK, (score+1.f)/2.f);
						int bF = fideChange(bR, wR, bK, (-score+1.f)/2.f);
						if(rs.getInt(1)==0){
							query = String.format("Insert into fide_changes values (%d, %d, %d), (%d, %d, %d);", matchId, wId, wF, matchId, bId, bF);
						}
						else{
							query = String.format("update fide_changes set value = %d where match_id = %d and user_id = %d; update fide_changes set value = %d where match_id = %d and user_id = %d;", wF, matchId, wId, bF, matchId, bId);
						}
						st.execute(query);
					}
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
			return new ResponseEntity<>("No such tournament or no permissions to add match (CODE 409)",HttpStatus.CONFLICT);
		}
		catch (Exception e) {
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping("/api/tournament/round/removematch") // @DeleteMapping
	public ResponseEntity<String> removeMatch(@CookieValue(value = "auth", defaultValue = "") String auth,
										   @RequestParam(value = "matchId") int matchId
	){
		int userId = -1;
		try{
			userId = checkCookie(auth);
		}
		catch (Exception e) {
			return new ResponseEntity<String>("No or expired authorization token (CODE 401)", HttpStatus.UNAUTHORIZED);
		}
		try {
			Statement st = connection.createStatement();
			String query = String.format("select tournament_id from matches where match_id = %d;",matchId);
			ResultSet rs = st.executeQuery(query);
			if (rs.next()) {
				int tournamentId = rs.getInt(1);
				query = String.format("select role from tournament_roles where tournament_id = %d and user_id = %d;", tournamentId, userId);
				rs = st.executeQuery(query);
				if (rs.next() && rs.getString(1).equals("admin")) {
					query = String.format("delete from fide_changes where match_id = %d; delete from matches where match_id = %d;", matchId, matchId);
					st.execute(query);
					return new ResponseEntity<>("Match sucessfully deleted (CODE 200)", HttpStatus.OK);
				}
				return new ResponseEntity<>("No permissions to remove match (CODE 409)", HttpStatus.CONFLICT);
			}
			return new ResponseEntity<>("No such match found (CODE 409)",HttpStatus.CONFLICT);
		}
		catch (Exception e)
		{
			return new ResponseEntity<>("Internal server error (CODE 500)" + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping("/api/tournament/start/{tournamentId}") //@PutMapping
	public ResponseEntity<String> startTournament(@PathVariable (value="tournamentId")int tournamentId,
												  @CookieValue (value="auth", defaultValue = "xd") String auth){
		//sprawdzanie czy ilosc graczy jest wieksza rowna od ilosci rund lub czy jest parzysta ilosc graczy
		int userId = -1;
		try{
			userId = checkCookie(auth);
		}
		catch (Exception e) {
			return new ResponseEntity<>("No or expired authorization token (CODE 401)", HttpStatus.UNAUTHORIZED);
		}
		try{
			Statement st = connection.createStatement();
			String query = String.format("select role from tournament_roles where tournament_id = %d and user_id = %d;",tournamentId,userId);
			ResultSet rs = st.executeQuery(query);
			if (rs.next()){
				if (!rs.getString(1).equals("admin")){
					return new ResponseEntity<>("User is not an admin of this tournament (CODE 401)",HttpStatus.UNAUTHORIZED);
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
							SET start_fide = (SELECT fide from users u2 where u2.user_id = tournament_roles.user_id),
							k = (SELECT u2.k from users u2 where u2.user_id = tournament_roles.user_id)
							where tournament_id = %d and role = 'player';
							UPDATE tournaments
							SET tournament_state = 1, start_date=now()
							WHERE tournament_id = %d;""", tournamentId, tournamentId);
					st.execute(query);
					if(firstRoundPairings(tournamentId))
						return new ResponseEntity<>("Tournament successfully began (CODE 200)", HttpStatus.OK);
					return new ResponseEntity<>("Tournament began, but there aren't any players in the tournament or cos wyjebalo sie na glupi ryj (CODE ???)", HttpStatus.OK);
				}
			}else{
				return new ResponseEntity<>("No such tournament or user is not a member of the tournament (CODE 409)",HttpStatus.CONFLICT);
			}
		}catch (Exception e){
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@RequestMapping("/api/tournament/end/{tournamentId}") //@PutMapping
	public ResponseEntity<String> endTournament(@PathVariable (value="tournamentId")int tournamentId,
												  @CookieValue (value="auth", defaultValue = "xd") String auth){
		int userId = -1;
		try{
			userId = checkCookie(auth);
		}
		catch (Exception e) {
			return new ResponseEntity<>("No or expired authorization token (CODE 401)", HttpStatus.UNAUTHORIZED);
		}
		try{
			Statement st = connection.createStatement();
			String query = String.format("select role from tournament_roles where tournament_id = %d and user_id = %d;",tournamentId,userId);
			ResultSet rs = st.executeQuery(query);
			if (rs.next()){
				if (!rs.getString(1).equals("admin")){
					return new ResponseEntity<>("User is not an admin of this tournament (CODE 401)",HttpStatus.UNAUTHORIZED);
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
							WHERE tournament_id = %d;""", tournamentId);
					st.execute(query);
					return new ResponseEntity<>("Tournament has ended! (CODE 200)", HttpStatus.OK);
				}
			}else{
				return new ResponseEntity<>("No such tournament or user is not a member of the tournament (CODE 409)",HttpStatus.CONFLICT);
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
				return new ResponseEntity<>("Data base error (probably no relevant tournaments found) (CODE 409)", HttpStatus.CONFLICT);
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
	public static boolean validate(String regexPattern, String textToValidate){
		return Pattern.compile(regexPattern)
				.matcher(textToValidate)
				.matches();
	}

	private static int kValue(int fide, boolean adult){
		if(fide > 2400)
			return 10;
		if(!adult && fide < 2300)
			return 40;
		return 20;
	}

	private static int fideChange(int R, int oR, int K, float S){
		float p = (float) (1.f/(1+Math.pow(10.f, (oR-R)/400.f)));
		return (int) (K*(S - p));
	}
}