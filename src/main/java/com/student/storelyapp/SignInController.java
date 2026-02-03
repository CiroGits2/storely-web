package com.student.storelyapp;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class SignInController {

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody SigninRequest request) {
        
        User user = addUser(request.username, request.password);

        if (user != null) {
            // Success
            return ResponseEntity.ok(user);
        } else {
            // Failure
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    private User addUser(String username, String masterPassword) {

        final String DB_URL = "jdbc:mysql://" + System.getenv("MYSQLHOST") + ":" + System.getenv("MYSQLPORT") + "/" + System.getenv("MYSQLDATABASE");
        final String USERNAME = System.getenv("MYSQLUSER");
        final String PASSWORD = System.getenv("MYSQLPASSWORD");

        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {

            //Inset row into table
            String sql = "INSERT INTO users (username, masterPassword)" + 
                "VALUES (?, ?)";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, masterPassword); 

            int addedRows = ps.executeUpdate();

            if (addedRows > 0) {
                User user = new User();
                user.username = username;
                user.masterPassword = masterPassword;
                return user;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }
    
    public static class SigninRequest {
        public String username;
        public String password;
    }
}
