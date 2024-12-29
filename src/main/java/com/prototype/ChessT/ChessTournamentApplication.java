package com.prototype.ChessT;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

/**
 * Runes spring application, manages database connection and implement helper
 * method and endpoints.
 */
@SpringBootApplication
@RestController
@EnableScheduling
@CrossOrigin("*")
public class ChessTournamentApplication {
  /**
   * Connection with database, used to create statements
   */
  public static Connection connection = null;

  /**
   * Main function called at the start up of a server
   * Runs spring application, establishes connection with database
   * @param args
   */
  public static void main(String[] args) {
    try {
      Class.forName("org.postgresql.Driver");
      connection = establishConnection(0);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
    SpringApplication.run(ChessTournamentApplication.class, args);
  }

  static Connection establishConnection(int mode) throws SQLException {
      if(mode==0){
          // Get credentials from environment variables
          String dbUrl = System.getenv("DATABASE_URL");
          return DriverManager.getConnection(dbUrl);
      }
      return DriverManager.getConnection("jdbc:postgresql://localhost:5432/my_database","postgres","postgres");
  }

  /**
   * Function called every month, on 0:10 of first day of that month
   * Sums all fide changes caused by playing in tournaments that have ended
   * during previous month (the one that ended 10 min earlier), then updates K
   * values for all players
   */
  @Scheduled(cron = "0 10 0 * * ?")
  public void fideUpdate() {
    try {
      Statement st = connection.createStatement();
                        String query = """
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
								""";
			st.execute(query);

    } catch (Exception e) {
        return;
    }
  }

  /**
   * Finds tournaments in database, that match search params and puts result
   * into JSON Array, which is combined with max page number into JSON Object
   * @param mode defaults to -1, describes the state of tournaments user is
   *     looking for
   *            (0 means tournaments waiting to start, 1 tournaments started but
   * not ended, 2 tournaments ended, -1 find tournaments that ended during last
   * week, all tournaments that are ongoing and those that are going to begin
   * during next two months)
   * @param page number of page to show (relevant if mode different from -1, defaults to 1 (first page))
   * @param page_size number of tournaments on page (relevant if mode different from
   *     -1, defaults to 100)
   * @return JSON structured string containing number of possible to see pages
   *     (that are not empty, 0 if mode equals -1)
   * and array named "tournaments" of results
   */

  @GetMapping("/api/homepage")
  public ResponseEntity<String> homepage(
      @RequestParam(value = "mode", defaultValue = "-1") int mode,
      @RequestParam(value = "page", defaultValue = "1") int page,
      @RequestParam(value = "pageSize", defaultValue = "100") int page_size) {
    try {
                        Statement st = connection.createStatement();
                        String query, query2 = "";
                        if (mode > 2 || mode < -1)
                          return new ResponseEntity<>("Invalid mode (CODE 409)",
                                                      HttpStatus.CONFLICT);
                        if (page_size < 1 || page < 1)
                          return new ResponseEntity<>(
                              "Invalid page params (CODE 409)",
                              HttpStatus.CONFLICT);
                        if (mode < 0) {
                          query =
                              // "select tournament_id, name, location,
                              // time_control, start_date, end_date,
                              // tournament_state from tournaments where
                              // (tournament_state = 2 and end_date > now() -
                              // interval '7' day) or tournament_state = 1 or
                              // (tournament_state = 0 and start_date < now() +
                              // interval '2' month and start_date > now() -
                              // interval '7' day);";
                              "select tournament_id, name, location, time_control, start_date, end_date, tournament_state from tournaments;";
                        } else if (mode == 0) {
                          query = String.format(
                              "select tournament_id, name, location, time_control, start_date, end_date, tournament_state from tournaments where tournament_state = %d order by start_date asc limit %d offset %d;",
                              mode, page_size, (page - 1) * page_size);
                          query2 = String.format(
                              "select count(*) from tournaments where tournament_state = %d;",
                              mode);
                        } else {
                          query = String.format(
                              "select tournament_id, name, location, time_control, start_date, end_date, tournament_state from tournaments where tournament_state = %d order by end_date desc limit %d offset %d;",
                              mode, page_size, (page - 1) * page_size);
                          query2 = String.format(
                              "select count(*) from tournaments where tournament_state = %d;",
                              mode);
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
                        // if (result.isEmpty())
                        // 	return new ResponseEntity<>("Data base error
                        // (probably no relevant tournaments found) (CODE 409)",
                        // HttpStatus.CONFLICT); Commented-out ponieważ lepiej
                        // po prostu zwrócić pustą tablicę
                        int pages = 0;
                        if (mode != -1) {
                          rs = st.executeQuery(query2);
                          rs.next();
                          pages = rs.getInt(1);
                        }
                        if (pages % page_size == 0)
                          pages /= page_size;
                        else
                          pages = pages / page_size + 1;
                        JSONObject nRes = new JSONObject();
                        nRes.put("pages", pages);
                        nRes.put("tournaments", result);
                        return new ResponseEntity<>(nRes.toString(),
                                                    HttpStatus.OK);
    } catch (Exception e) {
                        return new ResponseEntity<>(
                            "Internal server error (CODE 500)"+e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

    /**
     * Returns list of tournaments that contain provided text
     * @param name text that must be contained by returned tournament's names
     * @return JSON structured string, array of tournaments that contain provided name
     */
  @GetMapping("/api/search/{name}")
  public ResponseEntity<String> search(
          @PathVariable(value = "name") String name){
      try{
          String query = "select tournament_id, name, location, time_control, start_date, end_date, tournament_state from tournaments where name like ?;";
          PreparedStatement ps = connection.prepareStatement(query);
          ps.setString(1, "%"+name+"%");
          ResultSet rs = ps.executeQuery();
          ResultSetMetaData rsmd = rs.getMetaData();
          JSONArray result = new JSONArray();
          while (rs.next()) {
              JSONObject row = new JSONObject();
              for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                  row.put(rsmd.getColumnLabel(i), rs.getString(i));
              }
              result.put(row);
          }
          return new ResponseEntity<>(result.toString(),HttpStatus.OK);
      }catch (Exception e){
          return new ResponseEntity<>("Internal server error (CODE 500)",HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }

  /**
   * Computes K value used during calculation of fide change after each match
   * @param fide fide of user we want to calculate K for
   * @param adult flag if user is adult, used in computing K
   * @return K value
   */
  public static int kValue(int fide, boolean adult) {
    if (fide > 2400)
        return 10;
    if (!adult && fide < 2300)
        return 40;
    return 20;
  }

    /**
     * Computes Date used in prepared statements
     * @param dateString string with date to be converted to Date object
     * @return Date object ready to be used in prepared statement
     */
  public static Date StringToDate(String dateString) {
      return java.sql.Date.valueOf(dateString);
  }
}