package com.prototype.ChessT;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.validator.GenericValidator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@SpringBootApplication
@RestController
@EnableScheduling
@CrossOrigin("*")
public class ChessTournamentApplication {
	public static Connection connection = null;
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

	@GetMapping("/api/homepage")
	public ResponseEntity<String> homepage(@RequestParam(value = "mode", defaultValue = "-1") int mode,
										   @RequestParam(value = "page", defaultValue = "1") int page,
										   @RequestParam(value = "pageSize", defaultValue = "100") int page_size){
		try {
			Statement st = connection.createStatement();
			String query, query2 = "";
			if(mode > 2 || mode < -1)
				return new ResponseEntity<>("Invalid mode (CODE 409)", HttpStatus.CONFLICT);
			if(page_size < 1 || page < 1)
				return new ResponseEntity<>("Invalid page params (CODE 409)", HttpStatus.CONFLICT);
			if(mode < 0){
				query = "select tournament_id, name, location, time_control, start_date, end_date, tournament_state from tournaments where (tournament_state = 2 and end_date > now() - interval '7' day) or tournament_state = 1 or (tournament_state = 0 and start_date < now() + interval '2' month and start_date > now() - interval '7' day);";

			}
			else if(mode == 0){
				query = String.format("select tournament_id, name, location, time_control, start_date, end_date, tournament_state from tournaments where tournament_state = %d order by start_date asc limit %d offset %d;", mode, page_size, (page - 1) * page_size);
				query2 = String.format("select count(*) from tournaments where tournament_state = %d;", mode);
			}
			else{
				query = String.format("select tournament_id, name, location, time_control, start_date, end_date, tournament_state from tournaments where tournament_state = %d order by end_date desc limit %d offset %d;", mode, page_size, (page - 1) * page_size);
				query2 = String.format("select count(*) from tournaments where tournament_state = %d;", mode);
			}

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
			int pages = 0;
			if(mode != -1){
				rs = st.executeQuery(query2);
				rs.next();
				pages = rs.getInt(1);
			}
			if(pages % page_size == 0)
				pages/=page_size;
			else
				pages = pages/page_size + 1;
			JSONObject nRes = new JSONObject();
			nRes.put("pages", pages);
			nRes.put("tournaments", result);
			return new ResponseEntity<>(nRes.toString(), HttpStatus.OK);
		}catch (Exception e){
			return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public static int kValue(int fide, boolean adult){
		if(fide > 2400)
			return 10;
		if(!adult && fide < 2300)
			return 40;
		return 20;
	}
}