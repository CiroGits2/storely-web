package com.student.storelyapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
            
    User user = authenticateUser(request.username, request.password);
        
        if (user != null) {
            // Success
            return ResponseEntity.ok(user);
        } else {
            // Failure
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }
    
    private User authenticateUser(String username, String password) {

        final String DB_URL = "jdbc:mysql://" + System.getenv("MYSQLHOST") + ":" + System.getenv("MYSQLPORT") + "/" + System.getenv("MYSQLDATABASE");
        final String USERNAME = System.getenv("MYSQLUSER");
        final String PASSWORD = System.getenv("MYSQLPASSWORD");

        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {

            String sql = "SELECT * FROM Users WHERE username = ? AND masterPassword = ?";

            PreparedStatement pStatement = conn.prepareStatement(sql);

            pStatement.setString(1, username);
            pStatement.setString(2, password);
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
