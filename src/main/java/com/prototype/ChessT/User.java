package com.prototype.ChessT;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.validator.GenericValidator;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.regex.Pattern;


@RestController
@CrossOrigin("*")
public class User {
    /**
     * Rather ugly, but working regex to validate, provided by users email addresses
     */
    static String regexMailValidationPattern = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:"
            + "(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|"
            + "\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.)"
            + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";

    /**
     * Finds all data about currently logged-in user (except hashed password) for logged-in user
     * @param auth Cookie used to authenticate logged-in users
     * @return JSON Object wth username, mail, first_name, last_name, sex, date_of_birth and fide
     */
    @GetMapping("/api/user")
    public ResponseEntity<String> user(@CookieValue(value = "auth", defaultValue = "") String auth){
        int userId = -1;
        try{
            userId = checkCookie(auth);
        }
        catch (Exception e) {
            return new ResponseEntity<>("No or expired authorization token (CODE 401)", HttpStatus.UNAUTHORIZED);
        }

        try {
            Statement st = ChessTournamentApplication.connection.createStatement();
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

    /**
     * Finds all data about any user (except email and hashed password) for logged-in users only
     * @param auth Cookie used to authenticate logged-in users
     * @param userId Id of a user the data is about
     * @return JSON Object wth username, first_name, last_name, sex, date_of_birth and fide
     */
    @GetMapping("/api/user/account/{userId}")
    public ResponseEntity<String> account(@CookieValue(value = "auth", defaultValue = "xd") String auth,
                                          @PathVariable int userId) {
        try {
            try {
                checkCookie(auth);
            } catch (Exception e) {
                return new ResponseEntity<>("No or expired authorization token (CODE 401)", HttpStatus.UNAUTHORIZED);
            }
            Statement st = ChessTournamentApplication.connection.createStatement();
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

    /**
     * Validates authentication cookie
     * @param auth Cookie used to authenticate logged-in users
     * @return JSON Object with values: valid (boolean), user_id (of logged_in user)
     */
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

    /**
     * Takes username and password, and if correct creates authentication cookie for user
     * @param auth Cookie used to authenticate logged-in users (to check is user isn't already logged-in)
     * @param username username of user to log in
     * @param password password of user to log in
     * @param response used to attach cookie to header of response
     * @return CODE 200 if successfully logged in
     * @see User#addAuthCookie
     */
    @RequestMapping("/api/user/login") // @PostMapping
    public ResponseEntity<String> login(@CookieValue(value = "auth", defaultValue = "") String auth,
                                        @RequestParam(value = "username") String username,
                                        @RequestParam(value = "password") String password,
                                        HttpServletResponse response) {
        try {
            if(!checkFalseCookie(auth)){
                return new ResponseEntity<>("User is already logged in (CODE 409)", HttpStatus.CONFLICT);
            }
            Statement st = ChessTournamentApplication.connection.createStatement();
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

    /**
     * Log out currently loged-in user
     * @param auth Cookie used to authenticate logged-in users (to check is user isn't already logged-out)
     * @return CODE 200 if successfully logged out
     */
    @RequestMapping("/api/user/logout") //@PostMapping
    public ResponseEntity<String> logout(@CookieValue(value = "auth", defaultValue = "xd") String auth) {
        try {
            if (checkFalseCookie(auth)) {
                return new ResponseEntity<>("No user to log out (CODE 409)", HttpStatus.CONFLICT);
            }
            Statement st = ChessTournamentApplication.connection.createStatement();
            String query = String.format("delete from sessions where session_id = '%s'", auth);
            st.execute(query);
            return new ResponseEntity<>("Successfully logged out (CODE 200)", HttpStatus.OK);
        }
        catch (Exception e){
            return new ResponseEntity<>("Internal server error (CODE 500)", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validates data to register user, and if everything is valid registers user in database
     * @param auth Cookie used to authenticate logged-in users (to check is user isn't already logged-in)
     * @param username username of new user
     * @param password password of new user (t will be hashed, then saved to database)
     * @param password2 password for typos elimination
     * @param mail mail of new user
     * @param name first name of new user
     * @param lastname last name of new user
     * @param sex sex of new user
     * @param date birth date of new user
     * @param fide declared by new user, his fide
     * @param response used to attach cookie to header of response
     * @return CODE 200 if successfully logged in
     * @see User#addAuthCookie
     */
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


            Statement st = ChessTournamentApplication.connection.createStatement();
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
                query = String.format("insert into users values ('%d','%s','%s','%s','%s','%s','%s','%s',%d, %d)", id, username, mail, hashPassword(password, username), name, lastname, sex, date, fide, ChessTournamentApplication.kValue(fide, adult));
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

    /**
     * Generates strings with length of 32 for use as authentication token
     */
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

    /**
     * Creates and attaches authentication cookie to header of login or register function
     * @param r response the cookie will be attached to
     * @param userId id of user for whose session the token will be
     * @throws SQLException if no such user
     * @see User#login(String, String, String, HttpServletResponse)
     * @see User#register(String, String, String, String, String, String, String, String, String, int, HttpServletResponse)
     */
    public void addAuthCookie(HttpServletResponse r, int userId) throws SQLException {
        Statement st = ChessTournamentApplication.connection.createStatement();
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

    /**
     * Used widely to verify user session authentication cookie
     * @param auth cookie to validate
     * @return userId of owner of session authentication
     * @throws Exception if auth is older than 30 mins or doesn't exist at all
     */
    public static int checkCookie(String auth) throws Exception {
        Statement st = ChessTournamentApplication.connection.createStatement();
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

    /**
     * Check if no authentication token was attached to header (checks if no one is logged-in)
     * @param auth Cookie to check if valid
     * @return true if no user session authentication valid
     * @throws SQLException if something goes horribly wrong
     * @see User#login(String, String, String, HttpServletResponse)
     * @see User#register(String, String, String, String, String, String, String, String, String, int, HttpServletResponse)
     */
    public static boolean checkFalseCookie(String auth) throws SQLException {
        if(auth.length()<30){
            return true;
        }
        Statement st = ChessTournamentApplication.connection.createStatement();
        String query = String.format("Select user_id from sessions where session_id = '%s' and date > now() - interval '30' minute;", auth);
        ResultSet rs = st.executeQuery(query);
        return !rs.next();
    }

    /**
     * Hashes passwor for safety reasons
     * @param password password to hash
     * @param username used as "salt" during hashing
     * @return hashed password
     * @throws NoSuchAlgorithmException if invalid algorithm for hashing provided by developer
     */
    public String hashPassword(String password, String username) throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA3-256");
        final byte[] hashBytes = digest.digest((password + username).getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hashBytes);
    }

    /**
     * Converts bytes to hexadecimal
     * @param hash bytes to convert
     * @return string, hexadecimal representation of bytes
     */
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

    /**
     * used for regex validation inside user register and tournament create
     * @param regexPattern pattern of regEx
     * @param textToValidate text to validate
     * @return true if valid, false otherwise
     * @see User#register(String, String, String, String, String, String, String, String, String, int, HttpServletResponse)
     * @see Tournament#create(String, String, String, String, String, String, String, int, String)
     */
    public static boolean validate(String regexPattern, String textToValidate){
        return Pattern.compile(regexPattern)
                .matcher(textToValidate)
                .matches();
    }
}
