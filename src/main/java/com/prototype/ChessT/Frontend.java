package com.prototype.ChessT;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.net.*;

/**
 * This class represents the Frontend controller for the Chess Tournament Tracker application.
 * It handles requests related to serving the frontend content.
 */
@Controller
@CrossOrigin("*")
public class Frontend {
    /**
     * This method serves the frontend content for the Chess Tournament Tracker application.
     * It handles GET requests for any path and forwards the request to the frontend server at localhost:3000.
     * If the path begins with "/api", it returns a 404 NOT_FOUND response.
     * But note that valid /api requests are handled by other controllers.
     * The method retrieves the content from the frontend server and determines the content type based on the file extension.
     * It then adds the appropriate content type header and returns the content as a ResponseEntity with the appropriate status code.
     * If an exception occurs during the process, it returns a 500 INTERNAL_SERVER_ERROR response.
     *
     * @return ResponseEntity<Resource> - The response entity containing the frontend content.
     */
    @GetMapping("/**")
    public ResponseEntity<Resource> serveFrontend() {
        ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequest();
        String url = builder.toUriString();
        String path = url.substring(url.indexOf("://") + 3);
        path = path.substring(path.indexOf("/"));
        
        // Abort if path begins with /api
        if (path.startsWith("/api")) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Forward the request to the frontend server at localhost:3000
        try {
            URL proxyUrl = new URL("http://frontend:3000" + path);
            HttpURLConnection con = (HttpURLConnection) proxyUrl.openConnection();
            con.setRequestMethod("GET");
            con.setInstanceFollowRedirects(false);
            con.connect();
            byte[] content = con.getInputStream().readAllBytes();
            con.disconnect();

            // Detect content type
            String contentType = "text/html";
            if (path.endsWith(".js")) {
                contentType = "text/javascript";
            } else if (path.endsWith(".css")) {
                contentType = "text/css";
            } else if (path.endsWith(".png")) {
                contentType = "image/png";
            } else if (path.endsWith(".jpg")) {
                contentType = "image/jpeg";
            } else if (path.endsWith(".ico")) {
                contentType = "image/x-icon";
            }

            // Add content type header
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", contentType);

            // Return request body
            return ResponseEntity.ok().contentLength(content.length).headers(headers).body(new ByteArrayResource(content));
        } catch(Exception e) {
            System.out.println(e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}