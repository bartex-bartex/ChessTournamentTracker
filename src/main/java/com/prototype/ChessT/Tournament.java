package com.prototype.ChessT;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Implements logic for endpoints connected with tournament.
 */
@RestController
@CrossOrigin(origins = {
  "https://chess-tournament-tracker-front-a47b2b11d8a4.herokuapp.com", 
  "https://chess-tournament-tracker.bartoszwarchol.pl",
  "http://localhost:3000" // Or whatever your local frontend URL is
}, allowCredentials = "true", maxAge = 3600)
public class Tournament {

  /**
   * Class Player that contains info about user tournament data
   */
  static class Player {
    /**
     * Constructor for class Player
     * @param userId user unique id
     * @param rank user FIDE rank
     */
    Player(int userId, int rank) {
      this.userId = userId;
      this.rank = rank;
    }

    /**
     * Constructor for class Player
     * @param userId user unique id
     * @param rank user FIDE rank
     * @param bye info of user bye
     * @see Player#bye
     */
    Player(int userId, int rank, int bye) {
      this.userId = userId;
      this.rank = rank;
      this.bye = bye;
      if (bye != 0)
        score += 1;
    }
    /**
     * Bool that indicates if player is already paired with other player.
     * true - he is paired
     * false - he isn't paired
     */
    public boolean played = false;
    /**
     * Information that indicates if player had bye this tournament
     * If bye is equal to 0 then he hasn't received bye
     * If bye is positive it shows round number during he received bye
     */
    public int bye = 0;
    /**
     * User id of current player
     */
    public int userId;
    /**
     * FIDE rank of current player
     */
    public int rank;
    /**
     * Current player score
     */
    public float score = 0;
    /**
     * Number of times current player played as white
     */
    public int playedAsWhite = 0;
    /**
     * Number of times current player played as black
     */
    public int playedAsBlack = 0;
    /**
     * Set that contains player ids that current player already played with
     */
    public Set<Integer> alreadyPlayed = new HashSet<>();
    /**
     * Informs of ratio current player played with different colors of chess
     * pieces If integer is positive he played that many more times as white
     * Else if integer is equal 0 it means he played equal times both colors
     * else if integer is negative it means he player more times as black
     */
    public int playRatio;
    /**
     * More restrictive version of Player.canPlayWithLessRestrictive(Player
     * player) Returns an int that informs us about if two players can play with
     * each other. 1 - current player plays as white 0 - current player plays as
     * black -1 - two players cannot play with each other
     * @param player player, we want to pair current player object with
     * @return int
     * @see Player#canPlayWithLessRestrictive(Player)
     */
    public int canPlayWith(Player player) { // 1 - can play as white, 0 - can
                                            // play as black, -1 - cannot play
      if (played || player.played || alreadyPlayed.contains(player.userId) ||
          (playRatio >= 2 && player.playRatio >= 2) ||
          (playRatio <= -2 && player.playRatio <= -2))
        return -1;
      if (playRatio > player.playRatio)
        return 0;
      if (playRatio == player.playRatio)
        return (rank >= player.rank ? 1 : 0);
      return 1;
    }

    /**
     * Less restrictive version of Player.canPlayWith(Player player)
     * Returns an int that informs us about if two players can play with each
     * other. 1 - current player plays as white 0 - current player plays as
     * black -1 - two players cannot play with each other
     * @param player player, we want to pair current player object with
     * @return int
     * @see Player#canPlayWith(Player)
     */
    public int canPlayWithLessRestrictive(
        Player player) { // 1 - can play as white, 0 - can play as black, -1 -
                         // cannot play
      if (played || player.played) //|| alreadyPlayed.contains(player.userId))
        return -1;
      if (playRatio > player.playRatio)
        return 0;
      if (playRatio == player.playRatio)
        return (rank >= player.rank ? 1 : 0);
      return 1;
    }
  }

  /**
   * Generates round pairings for first round of the tournament
   * @param tournamentId unique tournament id
   * @return boolean true if generated properly false if something is wrong
   */
  public boolean firstRoundPairings(int tournamentId) {
    ArrayList<Player> list = new ArrayList<Player>();
    try {
      Statement st = ChessTournamentApplication.connection.createStatement();
      String query = String.format(
          "select user_id, start_fide from tournament_roles where role = 'player' and tournament_id = %d;",
          tournamentId);
      ResultSet rs = st.executeQuery(query);
      while (rs.next()) {
        list.add(new Player(rs.getInt(1), rs.getInt(2)));
      }
      if (list.isEmpty())
        return false;
      list.sort(Comparator.comparingDouble(player -> player.rank));
      int start = 0;
      if (list.size() % 2 == 1) {
        start = 1;
        query = String.format(
            "update tournament_roles set bye = 1 where tournament_id = %d and user_id = %d;",
            tournamentId, list.get(0).userId);
        st.execute(query);
      }
      for (int i = start; i < list.size(); i += 2) {
        query = String.format(
            "insert into matches (match_id,tournament_id,white_player_id,black_player_id,round,\"table\",score)\n"
                +
                "values ((select 1+coalesce(max(match_id),0) from matches),%d,%d,%d,1,%d,2);",
            tournamentId, list.get(i).userId, list.get(i + 1).userId,
            i / 2 + 1);
        st.execute(query);
      }
      return true;
    } catch (Exception e) {
      System.out.print(e.getMessage());
      return false;
    }
  }

  /**
   * Generates round pairings for provided round of tournament
   * @param tournamentId unique tournament id
   * @param round round number
   * @param auth authentication cookie
   * @return CODE 200 if generated successfully
   */
  @PutMapping("/api/tournament/generateRoundPairings")
  public ResponseEntity<String>
  generateRoundPairings(@RequestParam(value = "tournamentId") int tournamentId,
                        @RequestParam(value = "round") int round,
                        @CookieValue(value = "auth") String auth) {
    ArrayList<Player> list = new ArrayList<Player>();
    int userId = -1;
    try {
      userId = User.checkCookie(auth);
    } catch (Exception e) {
      return new ResponseEntity<>(
          "No or expired authorization token (CODE 401)",
          HttpStatus.UNAUTHORIZED);
    }

    try {
      Statement st = ChessTournamentApplication.connection.createStatement();
      String query = String.format(
          "select role from tournament_roles where tournament_id = %d and user_id = %d;",
          tournamentId, userId);
      ResultSet rs = st.executeQuery(query);
      if (!rs.next() || !rs.getString(1).equals("admin"))
        return new ResponseEntity<>(
            "User is not an admin of this tournament (CODE 401)",
            HttpStatus.UNAUTHORIZED);

      query = String.format(
          "select rounds, tournament_state from tournaments where tournament_id = %d;",
          tournamentId);
      rs = st.executeQuery(query);
      if (!rs.next() || rs.getInt(1) < round)
        return new ResponseEntity<>(
            "Round exceeds total amount of rounds in the tournament (CODE 409)",
            HttpStatus.CONFLICT);
      if (round <= 1)
        return new ResponseEntity<>("Round is invalid number",
                                    HttpStatus.CONFLICT);

      if (rs.getInt(2) != 1)
        return new ResponseEntity<>(
            "Tournament hasn't started yet or is already finished (CODE 409)",
            HttpStatus.CONFLICT);

      query = String.format(
          "select count(*) from matches where tournament_id = %d and round = %d;",
          tournamentId, round - 1);
      rs = st.executeQuery(query);
      rs.next();
      if (rs.getInt(1) == 0)
        return new ResponseEntity<>(
            "Previous round is not yet generated (CODE 409)",
            HttpStatus.CONFLICT);

      query = String.format(
          "select count(*) from matches where tournament_id = %d and round = %d;",
          tournamentId, round);
      rs = st.executeQuery(query);
      rs.next();
      if (rs.getInt(1) > 0)
        return new ResponseEntity<>(
            "In this round pairing is already generated (CODE 409)",
            HttpStatus.CONFLICT);

      query = String.format("select (select count(*) from matches where tournament_id = %d and round = %d and score in(-1,0,1))/(select count(*) from matches where tournament_id = %d and round = %d) as is_prev_r_gen;",tournamentId,round-1,tournamentId,round-1);
      rs = st.executeQuery(query);
      rs.next();
      if (rs.getInt(1) == 0)
        return new ResponseEntity<>("Not all of scores from previous round were inserted. (CODE 409)",HttpStatus.CONFLICT);

      query = String.format(
          "select user_id, start_fide, bye from tournament_roles where role = 'player' and tournament_id = %d;",
          tournamentId);
      rs = st.executeQuery(query);
      while (rs.next()) {
        list.add(new Player(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
      }
      if (list.isEmpty())
        return new ResponseEntity<>(
            "There are no players in that tournament (CODE 409)",
            HttpStatus.CONFLICT);
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
                for (Player player : list) {
                  if ((player.rank < leastFide.rank && player.bye == 0) ||
                      (player.bye == 0 && leastFide.bye != 0))
                    leastFide = player;
                }
                leastFide.played = true;
                query = String.format(
                    "update tournament_roles set bye = %d where user_id = %d and tournament_id = %d and role = 'player';",
                    round, leastFide.userId, tournamentId);
                st.execute(query);
      }
      int temp, table = 1;
      for (int i = list.size() - 1; i >= 0; i--) {
                if (!list.get(i).played) {
                  for (int j = i - 1; j >= 0; j--) {
                    temp = list.get(i).canPlayWith(list.get(j));
                    if (temp != -1) {
                      list.get(i).played = true;
                      list.get(j).played = true;
                      query = String.format(
                          "insert into matches(match_id, tournament_id, round, white_player_id, "
                              +
                              "black_player_id,\"table\",score) values((select 1 + max(match_id) from matches),%d,%d,%d,%d,%d,2);",
                          tournamentId, round,
                          (temp == 1 ? list.get(i).userId : list.get(j).userId),
                          (temp == 0 ? list.get(i).userId : list.get(j).userId),
                          table);
                      st.execute(query);
                      break;
                    }
                  }
                }
                if (!list.get(i).played) {
                  for (int j = i - 1; j >= 0; j--) {
                    temp = list.get(i).canPlayWithLessRestrictive(list.get(j));
                    if (temp != -1) {
                      list.get(i).played = true;
                      list.get(j).played = true;
                      query = String.format(
                          "insert into matches(match_id, tournament_id, round, white_player_id, "
                              +
                              "black_player_id,\"table\",score) values((select 1 + max(match_id) from matches),%d,%d,%d,%d,%d,2);",
                          tournamentId, round,
                          (temp == 1 ? list.get(i).userId : list.get(j).userId),
                          (temp == 0 ? list.get(i).userId : list.get(j).userId),
                          table);
                      st.execute(query);
                      break;
                    }
                  }
                }
                table++;
                if (!list.get(i).played)
                  return new ResponseEntity<>(
                      "Algorithm didn't managed to pair fairly (CODE 500)",
                      HttpStatus.INTERNAL_SERVER_ERROR);
      }
    } catch (Exception e) {
      return new ResponseEntity<>("Internal server error (CODE 500)",
                                  HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity<>("Successfully paired players (CODE 200)",
                                HttpStatus.OK);
  }

  /**
   * Validates provided data and if valid creates tournament and gives admin role for current user
   * @param auth authentication cookie
   * @param name tournament name
   * @param location tournament location
   * @param organizer tournament organizer
   * @param timeControl time control of tournament
   * @param startDate start date of the tournament
   * @param endDate end date of the tournament
   * @param rounds number of rounds tournament has
   * @param info info about tournament
   * @return CODE 200 if tournament was successfully created
   */
  @PostMapping("/api/tournament/create")
  public ResponseEntity<String>
  create(@CookieValue(value = "auth", defaultValue = "") String auth,
         @RequestParam(value = "tournamentName") String name,
         @RequestParam(value = "location") String location,
         @RequestParam(value = "organizer") String organizer,
         @RequestParam(value = "timeControl") String timeControl,
         @RequestParam(value = "startDate") String startDate,
         @RequestParam(value = "endDate") String endDate,
         @RequestParam(value = "rounds") int rounds,
         @RequestParam(value = "info") String info) {
    int userId = -1;
    try {
      userId = User.checkCookie(auth);
    } catch (Exception e) {
      return new ResponseEntity<>(
          "No or expired authorization token (CODE 401)",
          HttpStatus.UNAUTHORIZED);
    }

    if (!User.validate("^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])$",
                       startDate))
      return new ResponseEntity<>(
          "Wrong start date format, date format should be yyyy-mm-dd (CODE 400)",
          HttpStatus.BAD_REQUEST);

    if (!GenericValidator.isDate(startDate, "yyyy-MM-dd", true)) {
      return new ResponseEntity<>("Date is invalid (CODE 400)",
                                  HttpStatus.BAD_REQUEST);
    }

    if (!User.validate("^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])$",
                       endDate))
      return new ResponseEntity<>(
          "Wrong end date format, date format should be yyyy-mm-dd (CODE 400)",
          HttpStatus.BAD_REQUEST);

    if (!GenericValidator.isDate(endDate, "yyyy-MM-dd", true)) {
      return new ResponseEntity<>("Date is invalid (CODE 400)",
                                  HttpStatus.BAD_REQUEST);
    }

    if (rounds <= 0) {
      return new ResponseEntity<>("Number of rounds (CODE 400)",
                                  HttpStatus.BAD_REQUEST);
    }

    // Sanitize input (probably insufficient)
    name = name.replaceAll("'", "''");
    location = location.replaceAll("'", "''");
    organizer = organizer.replaceAll("'", "''");
    timeControl = timeControl.replaceAll("'", "''");
    info = info.replaceAll("'", "''");

    try {
      Statement st = ChessTournamentApplication.connection.createStatement();
      /*String query = String.format(
          "select count(x) from (select * from tournaments where name = '%s') as x;",
          name);*/
      String query = "select count(x) from (select * from tournaments where name = ?) as x;";
      PreparedStatement preparedStatement = ChessTournamentApplication.connection.prepareStatement(query);
      preparedStatement.setString(1,name);
      ResultSet rs = preparedStatement.executeQuery();
      rs.next();
      int rowCount = rs.getInt(1);
      if (rowCount == 0) {
        query = "select coalesce(max(tournament_id),0) from tournaments";
        rs = st.executeQuery(query);
        rs.next();
        int id = 1 + rs.getInt(1);
        /*query = String.format(
            "insert into tournaments (tournament_id,name,location,organiser,time_control,start_date,end_date,rounds,info) values ('%d','%s','%s','%s','%s','%s','%s','%d','%s')",
            id, name, location, organizer, timeControl, startDate,
            endDate, rounds, info);*/
        String query2 = String.format("insert into tournaments (tournament_id,name,location,organiser,time_control,start_date,end_date,rounds,info) " +
                                               "values ('%d',?,?,?,?,?,?,'%d',?)", id, rounds);
        preparedStatement = ChessTournamentApplication.connection.prepareStatement(query2);
        preparedStatement.setString(1,name);
        preparedStatement.setString(2,location);
        preparedStatement.setString(3,organizer);
        preparedStatement.setString(4,timeControl);
        preparedStatement.setDate(5,ChessTournamentApplication.StringToDate(startDate));
        preparedStatement.setDate(6,ChessTournamentApplication.StringToDate(endDate));
        preparedStatement.setString(7,info);

        preparedStatement.executeUpdate();
        query = String.format(
            "insert into tournament_roles (user_id,tournament_id,role) values (%d,%d,'admin')",
            userId, id);
        st.execute(query);

        JSONObject result = new JSONObject();
        result.put("tournamentId", id);
        return new ResponseEntity<>(result.toString(), HttpStatus.OK);
      }
      return new ResponseEntity<>(
          "Tournament with that name already exists (CODE 409)",
          HttpStatus.CONFLICT);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return new ResponseEntity<>("Internal server error (CODE 500)",
                                  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Allows current user to join provided tournament
   * @param auth authentication cookie
   * @param tournamentId tournament unique id
   * @return CODE 200 if user successfully joined tournament
   */
  @PostMapping("/api/tournament/join/{tournamentId}")
  public ResponseEntity<String>
  join(@CookieValue(value = "auth", defaultValue = "") String auth,
       @PathVariable int tournamentId) {
    int userId = -1;
    try {
      userId = User.checkCookie(auth);
    } catch (Exception e) {
      return new ResponseEntity<>(
          "No or expired authorization token (CODE 401)",
          HttpStatus.UNAUTHORIZED);
    }
    try {
      Statement st = ChessTournamentApplication.connection.createStatement();
      String query = String.format(
          "select tournament_state from tournaments where tournament_id = %d;",
          tournamentId);
      ResultSet rs = st.executeQuery(query);
      if (!rs.next()) {
                return new ResponseEntity<>("Invalid tournament id (CODE 409)",
                                            HttpStatus.CONFLICT);
      }
      if (rs.getInt(1) > 0) {
                return new ResponseEntity<>(
                    "Tournament is already started (CODE 409)",
                    HttpStatus.CONFLICT);
      }
      query = String.format(
          "SELECT count(*) from tournament_roles where tournament_id = %d and user_id = %d;",
          tournamentId, userId);
      rs = st.executeQuery(query);
      rs.next();
      if (rs.getInt(1) >= 1)
                return new ResponseEntity<>(
                    "This user is already assigned to this tournament (CODE 409)",
                    HttpStatus.CONFLICT);
      query = String.format(
          "insert into tournament_roles (user_id, tournament_id, role) values (%d,%d,'player')",
          userId, tournamentId);
      st.execute(query);
      return new ResponseEntity<>(
          "Joined the tournament successfully (CODE 200)", HttpStatus.OK);
    } catch (Exception E) {
      return new ResponseEntity<>("Internal server error (CODE 500)",
                                  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Returns general info of the tournament
   * @param tournamentId unique tournament id
   * @return JSON structured string containing tournament_id, name, location, organiser, time_control, start_date,
   *     end_date, rounds (total number of rounds planned by organiser), info, tournament_state (0 not started yet, 1 - going on, 2 - ended), is_admin(1 - is admin, 0 not an admin),
   *     if tournament_state is 0 (tournament not started) contains array of players participating,
   *        user_id, first_name, last_name, username, fide for each
   *     if tournament_state is greater than 0 (tournament started or ended) contains array of players with data
   *        about their performance, first_name, user_id, start_fide, last_name, change_in_fide, score
   * @see Tournament#results(int)
   */
  @GetMapping("/api/tournament/{tournamentId}")
  public ResponseEntity<String>
  tournamentInfo(@CookieValue(value = "auth", defaultValue = "") String auth,
                 @PathVariable int tournamentId) {
    int userId = -1;
    try {
      userId = User.checkCookie(auth);
    } catch (Exception ignored) {
    }
    try {
      Statement st = ChessTournamentApplication.connection.createStatement();
      String query = String.format("""
              SELECT t.tournament_id, t.name, t.location, t.organiser, t.time_control, t.start_date, t.end_date, t.rounds, t.info, t.tournament_state,
              case when (select role from tournament_roles where tournament_id = %d and user_id = %d) = 'admin' then 1 else 0 end as is_admin
              from tournaments t
              WHERE tournament_id = %d;""", tournamentId, userId, tournamentId);
      ResultSet rs = st.executeQuery(query);
      ResultSetMetaData rsmd = rs.getMetaData();
      JSONObject result = new JSONObject();
      if (!rs.next()) {
        return new ResponseEntity<>(
            "Data base error (probably no relevant tournament found) (CODE 409)",
            HttpStatus.CONFLICT);
      }
      boolean started = false;
      for (int i = 1; i <= rsmd.getColumnCount(); i++) {
        result.put(rsmd.getColumnLabel(i), rs.getString(i));
        if (rsmd.getColumnLabel(i).equals("tournament_state") &&
            rs.getInt(i) > 0)
          started = true;
      }
      if (!started) {
        JSONArray array = new JSONArray();
        query = String.format(
            "SELECT user_id, first_name, last_name, username, fide FROM tournament_roles JOIN users USING(user_id) WHERE tournament_id = %d AND role = 'player';",
            tournamentId);
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

      query = String.format("select max(round) from matches where tournament_id = %d;",tournamentId);
      rs = st.executeQuery(query);
      rs.next();
      result.put("rounds_generated",rs.getInt(1));
      result.put("player_data", playerData);
      return new ResponseEntity<>(result.toString(), HttpStatus.OK);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      for (StackTraceElement ste : e.getStackTrace()) {
        System.out.println(ste + "\n");
      }
      return new ResponseEntity<>("Internal server error (CODE 500)",
                                  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Returns general info about match
   * @param matchId id of match the data will be about
   * @return JSON structured string containing match_id, white_player_id,
   *     black_player_id, score (1 - white player win, 0 - tie,
   * -1 - black player win other values - nodata), table, game_notation.
   */
  @GetMapping("/api/tournament/match/{matchId}")
  public ResponseEntity<String> getMatch(@PathVariable int matchId) {
    try {
      Statement st = ChessTournamentApplication.connection.createStatement();
      String query = String.format(
          "select m.match_id, m.white_player_id, m.black_player_id, m.score, m.round, m.table, coalesce(m.game_notation,'') as game_notation, u1.first_name as white_first_name, u1.last_name as white_last_name, u1.fide as white_fide, u2.first_name as black_first_name, u2.last_name as black_last_name, u2.fide as black_fide from matches m join users u1 on m.white_player_id = u1.user_id join users u2 on m.black_player_id = u2.user_id where m.match_id = %d;",
          matchId);
      ResultSet rs = st.executeQuery(query);
      ResultSetMetaData rsmd = rs.getMetaData();
      JSONObject result = new JSONObject();
      if (rs.next()) {
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                  result.put(rsmd.getColumnLabel(i), rs.getString(i));
                }
                return new ResponseEntity<>(result.toString(), HttpStatus.OK);
      }
      return new ResponseEntity<>(
          "Data base error (probably no relevant match found) (CODE 409)",
          HttpStatus.CONFLICT);

    } catch (Exception e) {
      return new ResponseEntity<>("Internal server error (CODE 500)",
                                  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Returns general info about round of tournament
   * @param tournamentId unique tournament id
   * @param round round number
   * @return JSON structured string containing array of matches, single match
   *     contains match_id, white_player_id, black_player_id,
   *     score, round, table, game_notation, white_first_name, white_last_name,
   *     black_first_name, black_last_name, white_fide, black_fide
   */
  @GetMapping("/api/tournament/round")
  public ResponseEntity<String>
  getRound(@RequestParam(value = "tournamentId") int tournamentId,
           @RequestParam(value = "round") int round) {
    try {
      Statement st = ChessTournamentApplication.connection.createStatement();
      String query = String.format(
              """
                      SELECT m.match_id,
                                    m.white_player_id,
                                    m.black_player_id,
                                    coalesce(m.score, 2) AS score,
                                    m.round,
                                    m.table,
                                    coalesce(m.game_notation, '') AS game_notation,
                                    u1.first_name AS white_first_name,
                                    u1.last_name AS white_last_name,
                                    u1.fide AS white_fide,
                                    u2.first_name AS black_first_name,
                                    u2.last_name AS black_last_name,
                                    u2.fide AS black_fide,
                             
                               (SELECT sum(CASE
                                               WHEN (m2.score = 1
                                                     AND m2.white_player_id = m.white_player_id)
                                                    OR (m2.score = -1
                                                        AND m2.black_player_id = m.white_player_id) THEN 1
                                               WHEN (m2.score = 0
                                                     AND m.white_player_id in (m2.white_player_id, m2.black_player_id)) THEN 0.5
                                               ELSE 0
                                           END)
                                FROM matches m2
                                WHERE tournament_id = %d)+(select (CASE when tr1.bye = 0 then 0 else 1 end) from tournament_roles tr1 where tr1.user_id = m.white_player_id and tr1.tournament_id = m.tournament_id) AS white_score,
                             
                               (SELECT sum(CASE
                                               WHEN (m2.score = 1
                                                     AND m2.white_player_id = m.black_player_id)
                                                    OR (m2.score = -1
                                                        AND m2.black_player_id = m.black_player_id) THEN 1
                                               WHEN (m2.score = 0
                                                     AND m.black_player_id in (m2.white_player_id, m2.black_player_id)) THEN 0.5
                                               ELSE 0
                                           END)
                                FROM matches m2
                                WHERE tournament_id = %d)+(select (CASE when tr2.bye = 0 then 0 else 1 end) from tournament_roles tr2 where tr2.user_id = m.black_player_id and tr2.tournament_id = m.tournament_id) AS black_score
                             FROM matches m
                             JOIN users u1 ON m.white_player_id = u1.user_id
                             JOIN users u2 ON m.black_player_id = u2.user_id
                             WHERE m.tournament_id = %d
                               AND m.round = %d;""",
          tournamentId, tournamentId, tournamentId, round);
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
                return new ResponseEntity<>(
                    "Data base error (probably no relevant matches found) (CODE 409)",
                    HttpStatus.CONFLICT);
      return new ResponseEntity<>(result.toString(), HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>("Internal server error (CODE 500)",
                                  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Retrieves details of a player in a tournament based on the provided
   * tournament ID and user ID.
   * @param tournamentId unique tournament id
   * @param userId unique user id
   * @return JSON structured string containing user_id, username, first_name,
   *     last_name, start_fide, tournament_id, rank_change,
   *     opponents (array of opponents containing tournament_id, match_id, user_id,
   *     first_name, last_name, score, color, round, start_fide, table), bye (0 - no
   *     bye given, other positive integer round which bye was given), sum (score in
   *     whole tournament), avg_fide (average FIDE rating of opponents)
   */
  @GetMapping("/api/tournament/player")
  public ResponseEntity<String>
  playerInfo(@RequestParam(value = "tournamentId") int tournamentId,
             @RequestParam(value = "userId") int userId) {
    try {
      Statement st = ChessTournamentApplication.connection.createStatement();
      String query = String.format(
          "select u.user_id, u.username, u.first_name, u.last_name, tr.start_fide, tr.tournament_id, coalesce(f.change_in_rank,0) as rank_change from users u join tournament_roles tr on u.user_id = tr.user_id left join (select user_id, sum(value) as change_in_rank from fide_changes join matches using(match_id) where tournament_id = %d group by user_id) f on tr.user_id = f.user_id where tr.role = 'player' and tr.tournament_id = %d and u.user_id = %d;",
          tournamentId, tournamentId, userId);
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
            if (rsmd.getColumnLabel(i).equals("score")) {
              temp = rs.getFloat(i);
              if (temp == -1) {
                row.put(rsmd.getColumnLabel(i), "nodata");
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
      return new ResponseEntity<>(
          "No such tournament, user or invalid role assigned to that user (CODE 409)",
          HttpStatus.CONFLICT);

    } catch (Exception e) {
      return new ResponseEntity<>("Internal server error (CODE 500)",
                                  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Returns JSONObject with results of provided tournament
   * @param  tournamentId id of tournament, the data will be about
   * @return JSONArray of players info and their performance in tournament indicated by tournamentId,
   *    first_name, user_id, start_fide, last_name, change_in_fide, score
   * @throws SQLException indicates something went horribly wrong
   */
  public JSONArray results(int tournamentId) throws SQLException {
    Statement st = ChessTournamentApplication.connection.createStatement();
    String query = String.format("""
             select
               uk.first_name,
               uk.user_id,
               tr.start_fide,
               uk.last_name,
               coalesce(change_in_rank, 0) as change_in_fide,
               coalesce(sum(d.score1), 0) + (
                 SELECT
                   CASE WHEN BYE = 0 THEN 0 ELSE 1 END\s
                 FROM
                   tournament_roles tr2
                 where
                   tr2.user_id = uk.user_id
                   AND tournament_id = %d
               ) as score
             from
               (
                 select
                   m.white_player_id as player_id,
                   sum(
                     (
                       CASE WHEN score = 1 THEN 1 WHEN score = 0 THEN 0.5 WHEN score = -1 THEN 0 ELSE null END
                     )
                   ) as score1
                 from
                   matches m
                   join users u on m.black_player_id = u.user_id
                 where
                   tournament_id = %d
                 group by
                   m.white_player_id
                 union all
                 select
                   black_player_id as player_id,
                   sum(
                     (
                       CASE WHEN score = 1 THEN 0 WHEN score = 0 THEN 0.5 WHEN score = -1 THEN 1 ELSE null END
                     )
                   ) as score1
                 from
                   matches m
                   join users u on m.white_player_id = u.user_id\s
                 where
                   tournament_id = %d
                 group by
                   m.black_player_id
               ) as d
               right join users uk on d.player_id = uk.user_id
               right join tournament_roles tr on uk.user_id = tr.user_id
               left join (
                 select
                   user_id,
                   sum(value) as change_in_rank
                 from
                   fide_changes
                   join matches using(match_id)
                 where
                   tournament_id = %d
                 group by
                   user_id
               ) f on f.user_id = d.player_id
             where tr.tournament_id = %d and tr.role = 'player'
             group by
               uk.user_id,
               first_name,
               last_name,
               start_fide,
               change_in_rank;
             
            """,tournamentId,tournamentId,tournamentId,tournamentId,tournamentId);

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

  /*
   * Adds match to selected round of selected tournament. Use at your own risk
   * @param auth authentication cookie
   * @param tournamentId tournament unique id
   * @param wId white player id
   * @param bId black player id
   * @param table table number
   * @param round round number
   * @param score score (1 - white player win, 0 - tie, -1 - black player win,
   *     other values = nodata)
   * @param gameNotation game notation
   * @return CODE 200 if add match successfully added
   * @deprecated

  @PutMapping("/api/tournament/round/addmatch")
  public ResponseEntity<String>
  addMatch(@CookieValue(value = "auth", defaultValue = "") String auth,
           @RequestParam(value = "tournamentId") int tournamentId,
           @RequestParam(value = "whitePlayerId") int wId,
           @RequestParam(value = "blackPlayerId") int bId,
           @RequestParam(value = "table", defaultValue = "-1") int table,
           @RequestParam(value = "round") int round,
           @RequestParam(value = "score", defaultValue = "2") int score,
           @RequestParam(value = "gameNotation",
                         defaultValue = "") String gameNotation) {
    if (score < -1 || score > 2) {
      return new ResponseEntity<>("Invalid score value (CODE 409)",
                                  HttpStatus.CONFLICT);
    }
    int userId = -1;
    try {
      userId = User.checkCookie(auth);
    } catch (Exception e) {
      return new ResponseEntity<String>(
          "No or expired authorization token (CODE 401)",
          HttpStatus.UNAUTHORIZED);
    }
    try {
      Statement st = ChessTournamentApplication.connection.createStatement();
      String query = String.format(
          "select role from tournament_roles where tournament_id = %d and user_id = %d;",
          tournamentId, userId);
      ResultSet rs = st.executeQuery(query);
      if (rs.next() && rs.getString(1).equals("admin")) {
                query = String.format(
                    "select count(*), coalesce(rounds, 0) from tournament_roles join tournaments using(tournament_id) where tournament_id = %d and user_id in (%d,%d) group by rounds;",
                    tournamentId, wId, bId);
                rs = st.executeQuery(query);
                if (!rs.next())
                  return new ResponseEntity<>(
                      "At least one player has not joined this tournament (CODE 409)",
                      HttpStatus.CONFLICT);
                if (rs.getInt(1) < 2 || wId == bId)
                  return new ResponseEntity<>(
                      "One or more player ids are invalid (CODE 409)",
                      HttpStatus.CONFLICT);
                if (round > rs.getInt(2) || round < 1)
                  return new ResponseEntity<>("Invalid round number (CODE 409)",
                                              HttpStatus.CONFLICT);
                query = String.format(
                    "select match_id from matches where tournament_id = %d and white_player_id in (%d,%d) and black_player_id in (%d,%d) and round = %d",
                    tournamentId, wId, bId, wId, bId, round);
                rs = st.executeQuery(query);
                int matchId;
                if (rs.next()) {
                  matchId = rs.getInt(1);
                  int mode = 0;
                  if (table == -1)
                    mode += 1;
                  if (score == 2)
                    mode += 2;
                  if (gameNotation.isEmpty())
                    mode += 4;

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
                        Statement st2 = ChessTournamentApplication.connection.createStatement();
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
    }*/

    /**
     * Updates score and game notation of provided match
     * @param auth authorisation cookie, to check if user is admin of tournament the match was played in
     * @param matchId unique match id
     * @param score score (1 - white player win, 0 - tie, -1 - black player win, 2 - deletes match score and associated fide changes, other values - no change, defaults to 2)
     * @param gameNotation game notation (if empty no change, defaults to epty)
     * @return 200 if match successfully updated
     */
    @PatchMapping("/api/tournament/round/updatematch")
    public ResponseEntity<String> updateMatch(@CookieValue(value = "auth", defaultValue = "") String auth,
                                              @RequestParam(value = "matchId") int matchId,
                                              @RequestParam(value = "score", defaultValue = "2") int score,
                                              @RequestParam(value = "gameNotation", defaultValue = "") String gameNotation
    ){
        if((score > 2 || score < -1) && gameNotation.isEmpty())
            return new ResponseEntity<>("No data tu update (CODE 409)", HttpStatus.CONFLICT);
        int userId = -1;
        try{
            userId = User.checkCookie(auth);
        }
        catch (Exception e) {
            return new ResponseEntity<String>("No or expired authorization token (CODE 401)", HttpStatus.UNAUTHORIZED);
        }
        try {
            Statement st = ChessTournamentApplication.connection.createStatement();
            String query = String.format("select role from tournament_roles join matches using(tournament_id) where match_id = %d and user_id = %d;",matchId,userId);
            ResultSet rs = st.executeQuery(query);
            if (rs.next() && rs.getString(1).equals("admin")){
                // Najpierw czy chcemy edytować score?
                int mode = (score >= -1 && score <= 2 ? 2 : 0); // 00 lub 10
                          // Czy chcemy edytować też gameNotation?
                mode += (!gameNotation.isEmpty() ? 1 : 0); // ?0 lub ?1
                query = switch (mode) {
                    //case 0 -> // 00 = Nie chcemy edytować ani score ani gameNotation
                    //        "";
                    case 1 -> // 01 = Chcemy edytować tylko gameNotation
                            String.format("UPDATE matches SET game_notation = ? WHERE match_id = %d;", matchId);
                    case 2 -> // 10 = Chcemy edytować tylko score
                            String.format("UPDATE matches SET score = %d WHERE match_id = %d;", score, matchId);
                    case 3 -> // 11 = Chcemy edytować i score i gameNotation
                            String.format("UPDATE matches SET score = %d, game_notation = ? WHERE match_id = %d;", score, matchId);
                    default ->
                            throw new IllegalStateException("Unexpected value: " + mode);
                };
                if (score == 2 && mode >= 2)
                  query += String.format("delete from fide_changes where match_id = %d;",matchId);

                if(mode == 2){
                  st.execute(query);
                }else {
                  PreparedStatement preparedStatement = ChessTournamentApplication.connection.prepareStatement(query);
                  preparedStatement.setString(1,gameNotation);
                  preparedStatement.executeUpdate();
                }

                if (score == 2 && mode >= 2) {
                  return new ResponseEntity<>("Match successfully updated (CODE 200)", HttpStatus.OK);
                }
                if(-1 <= score && score <= 1){
                    query = String.format("select count(*) from fide_changes where match_id = %d;", matchId);
                    rs = st.executeQuery(query);
                    rs.next();
                    String query2 = String.format("""
							SELECT m.white_player_id, tr.start_fide, tr.k from matches m
							join tournament_roles tr on m.white_player_id = tr.user_id and m.tournament_id = tr.tournament_id
							where match_id = %d
							union all
							SELECT m.black_player_id, tr.start_fide, tr.k from matches m
							join tournament_roles tr on m.black_player_id = tr.user_id and m.tournament_id = tr.tournament_id
							where match_id = %d;""", matchId, matchId);
                    Statement st2 = ChessTournamentApplication.connection.createStatement();
                    ResultSet rs2 = st2.executeQuery(query2);
                    rs2.next();
                    int wR = rs2.getInt(2), wK = rs2.getInt(3), wId = rs2.getInt(1);
                    rs2.next();
                    int bR = rs2.getInt(2), bK = rs2.getInt(3), bId = rs2.getInt(1);
                    int wF = fideChange(wR, bR, wK, (score+1.f)/2.f);
                    int bF = fideChange(bR, wR, bK, (-score+1.f)/2.f);
                    if(rs.getInt(1)==0){
                        query = String.format("Insert into fide_changes values (%d, %d, %d), (%d, %d, %d);", matchId, wId, wF, matchId, bId, bF);
                    }
                    else{
                        query = String.format("update fide_changes set value = %d where match_id = %d and user_id = %d; update fide_changes set value = %d where match_id = %d and user_id = %d;", wF, matchId, wId, bF, matchId, bId);
                    }
                    st.execute(query);
                    return new ResponseEntity<>("Match successfully updated (CODE 200)",HttpStatus.OK);
                } else if (mode == 1) {
                  return new ResponseEntity<>("Match successfully updated (CODE 200)",HttpStatus.OK);
                }
                return new ResponseEntity<>("No data to update (CODE 409)", HttpStatus.CONFLICT);
            }
            return new ResponseEntity<>("No such tournament or no permissions to add match (CODE 409)",HttpStatus.CONFLICT);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                System.out.println(ste + "\n");
            }
            return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /*
     * Adds match to selected round of selected tournament. Use at your own risk
     * @param auth authentication cookie
     * @param matchId unique match id
     * @return code 200 if match successfully deleted
     * @deprecated
    @DeleteMapping("/api/tournament/round/removematch")
    public ResponseEntity<String> removeMatch(@CookieValue(value = "auth", defaultValue = "") String auth,
                                              @RequestParam(value = "matchId") int matchId
    ){
        int userId = -1;
        try{
            userId = User.checkCookie(auth);
        }
        catch (Exception e) {
            return new ResponseEntity<String>("No or expired authorization token (CODE 401)", HttpStatus.UNAUTHORIZED);
        }
        try {
            Statement st = ChessTournamentApplication.connection.createStatement();
            String query = String.format("select tournament_id from matches where match_id = %d;",matchId);
            ResultSet rs = st.executeQuery(query);
            if (rs.next()) {
                int tournamentId = rs.getInt(1);
                query = String.format("select role from tournament_roles where tournament_id = %d and user_id = %d;", tournamentId, userId);
                rs = st.executeQuery(query);
                if (rs.next() && rs.getString(1).equals("admin")) {
                    query = String.format("delete from fide_changes where match_id = %d; delete from matches where match_id = %d;", matchId, matchId);
                    st.execute(query);
                    return new ResponseEntity<>("Match successfully deleted (CODE 200)", HttpStatus.OK);
                }
                return new ResponseEntity<>("No permissions to remove match (CODE 409)", HttpStatus.CONFLICT);
            }
            return new ResponseEntity<>("No such match found (CODE 409)",HttpStatus.CONFLICT);
        }
        catch (Exception e)
        {
            return new ResponseEntity<>("Internal server error (CODE 500)" + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }*/

    /**
     * Starts the tournament if it hasn't already started
     * @param tournamentId unique tournament id
     * @param auth authentication cookie used to determine if user is admin of the tournament
     * @return CODE 200 if successfully started
     */
    @PatchMapping("/api/tournament/start/{tournamentId}")
    public ResponseEntity<String> startTournament(@PathVariable (value="tournamentId")int tournamentId,
                                                  @CookieValue (value="auth", defaultValue = "") String auth){
        int userId = -1;
        try{
            userId = User.checkCookie(auth);
        }
        catch (Exception e) {
            return new ResponseEntity<>("No or expired authorization token (CODE 401)", HttpStatus.UNAUTHORIZED);
        }
        try{
            Statement st = ChessTournamentApplication.connection.createStatement();
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
                      return new ResponseEntity<>(
                          "This tournament is in progress (CODE 409)",
                          HttpStatus.CONFLICT);
                    case 2:
                      return new ResponseEntity<>(
                          "This tournament has finished (CODE 409)",
                          HttpStatus.CONFLICT);
                    }
                    query = String.format(
                        "select count(*) from tournament_roles where role = 'player' and tournament_id = %d;",
                        tournamentId);
                    rs = st.executeQuery(query);
                    rs.next();
                    int playercount = rs.getInt(1);
                    if (playercount <= 1)
                      return new ResponseEntity<>(
                          "Not enough players to start tournament",
                          HttpStatus.CONFLICT);

                    query = String.format(
                        "select rounds from tournaments where tournament_id = %d;",
                        tournamentId);
                    rs = st.executeQuery(query);
                    if (!rs.next() ||
                        (playercount % 2 == 1 && rs.getInt(1) > playercount))
                      return new ResponseEntity<>(
                          "Number of players is odd and is smaller than number of rounds (CODE 409)",
                          HttpStatus.CONFLICT);

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
                } else {
                        return new ResponseEntity<>(
                            "No such tournament or user is not a member of the tournament (CODE 409)",
                            HttpStatus.CONFLICT);
                }
      }
      catch (Exception e) {
                return new ResponseEntity<>("Internal server error (CODE 500)",
                                            HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
    /**
     * Ends the tournament if it hasn't already ended
     * @param tournamentId unique tournament id
     * @param auth authentication cookie used to determine if user is admin of the tournament
     * @return CODE 200 if successfully started
     */
    @
    PatchMapping(
        "/api/tournament/end/{tournamentId}") public ResponseEntity<String>
    endTournament(@PathVariable(value = "tournamentId") int tournamentId,
                  @CookieValue(value = "auth", defaultValue = "") String auth) {
      int userId = -1;
      try {
                userId = User.checkCookie(auth);
      } catch (Exception e) {
                return new ResponseEntity<>(
                    "No or expired authorization token (CODE 401)",
                    HttpStatus.UNAUTHORIZED);
      }
      try {
                Statement st =
                    ChessTournamentApplication.connection.createStatement();
                String query = String.format(
                    "select role from tournament_roles where tournament_id = %d and user_id = %d;",
                    tournamentId, userId);
                ResultSet rs = st.executeQuery(query);
                if (rs.next()) {
                        if (!rs.getString(1).equals("admin")) {
                    return new ResponseEntity<>(
                        "User is not an admin of this tournament (CODE 401)",
                        HttpStatus.UNAUTHORIZED);
                        } else {
                    query = String.format(
                        "select tournament_state from tournaments where tournament_id = %d;",
                        tournamentId, userId);
                    rs = st.executeQuery(query);
                    rs.next();
                    switch (rs.getInt(1)) {
                    case 0:
                      return new ResponseEntity<>(
                          "This tournament hasn't started (CODE 409)",
                          HttpStatus.CONFLICT);
                    case 2:
                      return new ResponseEntity<>(
                          "This tournament has already finished (CODE 409)",
                          HttpStatus.CONFLICT);
                    }
                    query = String.format("""
							UPDATE tournaments
							SET tournament_state = 2, end_date = now()
							WHERE tournament_id = %d;""", tournamentId);
                    st.execute(query);
                    return new ResponseEntity<>("Tournament has ended! (CODE 200)", HttpStatus.OK);
                        }
                } else {
                        return new ResponseEntity<>(
                            "No such tournament or user is not a member of the tournament (CODE 409)",
                            HttpStatus.CONFLICT);
                }
      } catch (Exception e) {
                return new ResponseEntity<>("Internal server error (CODE 500)",
                                            HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }

  /**
   * Changes number of rounds planned for the tournament (useful if number rounds is to big to start tournament)
   * @param auth Cookie used to determine if user is admin of the tournament
   * @param tournamentId unique id of tournament user wants to edit
   * @param newRounds new number of planned rounds
   * @return CODE 200 if successfully changed round number
   */

  @PutMapping("/api/tournament/change-rounds-no")
  public ResponseEntity<String> changeRoundsNo(@CookieValue(value = "auth", defaultValue = "") String auth,
         @RequestParam(value = "tournamentId") int tournamentId, @RequestParam(value = "newRounds") int newRounds) {
    int userId = -1;
    try{
      userId = User.checkCookie(auth);
    }
    catch (Exception e) {
      return new ResponseEntity<String>("No or expired authorization token (CODE 401)", HttpStatus.UNAUTHORIZED);
    }
    if(newRounds<1)
      return new ResponseEntity<>("Invalid rounds number (CODE 409)", HttpStatus.CONFLICT);
    try {
      Statement st = ChessTournamentApplication.connection.createStatement();
      String query = String.format("select role from tournament_roles where tournament_id = %d and user_id = %d;", tournamentId, userId);
      ResultSet rs = st.executeQuery(query);
      if (rs.next() && rs.getString(1).equals("admin")) {
        query = String.format("Update tournaments set rounds = %d where tournament_id = %d", newRounds, tournamentId);
        st.execute(query);
        return new ResponseEntity<>("Rounds number successfully updated (CODE 200)", HttpStatus.OK);
      }
      return new ResponseEntity<>("No such tournament or no permissions to change rounds number (CODE 409)", HttpStatus.CONFLICT);
    }
    catch (Exception e)
    {
      return new ResponseEntity<>("Internal server error (CODE 500)" + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

    /**
     * Computes fide change based on player ratings, score of the match and K
     * value
     * @param R rating of player
     * @param oR rating of opponent
     * @param K constant K
     * @param S score
     * @return change of ranking
     */
    private static int fideChange(int R, int oR, int K, float S) {
      float p = (float)(1.f / (1 + Math.pow(10.f, (oR - R) / 400.f)));
      return (int)(K * (S - p));
    }
  }
