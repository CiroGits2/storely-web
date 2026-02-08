package com.student.storelyapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {
     //*  Encryption  *//
    private BCryptPasswordEncoder enc = new BCryptPasswordEncoder(12);

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
            
    User user = authenticateUser(request.username);
        
        if (user != null) {
            boolean match = enc.matches(request.password, user.masterPassword);
            if (match == true) {
                // Success
                return ResponseEntity.ok(user);
            }
        } else {
            // Failure
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        return null;
    }
    
    private User authenticateUser(String username) {

        final String DB_URL = "jdbc:mysql://" + System.getenv("MYSQLHOST") + ":" + System.getenv("MYSQLPORT") + "/" + System.getenv("MYSQLDATABASE");
        final String USERNAME = System.getenv("MYSQLUSER");
        final String PASSWORD = System.getenv("MYSQLPASSWORD");

        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {

            String sql = "SELECT id, username, masterPassword FROM Users WHERE username = ?";

            PreparedStatement pStatement = conn.prepareStatement(sql);

            pStatement.setString(1, username);
            ResultSet rs = pStatement.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.id = rs.getInt("id");
                user.username = rs.getString("username");
                user.masterPassword = rs.getString("masterPassword");
                return user;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }
    
    public static class LoginRequest {
        public String username;
        public String password;
    }  
}
