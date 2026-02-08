package com.student.storelyapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
public class MainController {
    //*  Encryption  *//
    private String manageEncryption(String input, String key, int mode) throws Exception {
        byte[] keyBytes = new byte[16];
        byte[] originalKeyBytes = key.getBytes("UTF-8");
        System.arraycopy(originalKeyBytes, 0, keyBytes, 0, Math.min(originalKeyBytes.length, 16));
    
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(mode, secretKey);

        if (mode == Cipher.ENCRYPT_MODE) {
            return Base64.getEncoder().encodeToString(cipher.doFinal(input.getBytes("UTF-8")));
        } else {
            return new String(cipher.doFinal(Base64.getDecoder().decode(input)));
        }
    }

    final String DB_URL = "jdbc:mysql://" + System.getenv("MYSQLHOST") + ":" + System.getenv("MYSQLPORT") + "/" + System.getenv("MYSQLDATABASE");
    final String USERNAME = System.getenv("MYSQLUSER");
    final String PASSWORD = System.getenv("MYSQLPASSWORD");

    @GetMapping("/passwords")
    public ResponseEntity<List<Password>> getPasswords(@RequestParam int userId, @RequestParam String masterPassword) throws SQLException {
        List<Password> list = new ArrayList<>();
        String sql = "SELECT * FROM passwords WHERE user_id = ?";

        try(Connection con = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Password password = new Password();
                password.number = rs.getInt("Number");
                password.website = rs.getString("Website"); 
                password.username = rs.getString("Username");
                String encPass = rs.getString("Password");

                password.encPassword = manageEncryption(encPass, masterPassword, Cipher.DECRYPT_MODE);
                
                list.add(password);
            }
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping("/contacts")
    public ResponseEntity<List<Contact>> getContacts(@RequestParam int userId) throws SQLException {
        List<Contact> conList = new ArrayList<>();
        String sql = "SELECT * FROM contacts WHERE user_id = ?";

        try(Connection con = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Contact contact = new Contact();
                contact.idNumber = rs.getInt("Number");
                contact.contactName = rs.getString("Name");
                contact.contactEmail = rs.getString("Email");
                contact.contactNumber = rs.getString("phoneNumber");
                conList.add(contact);
            }
            return ResponseEntity.ok(conList);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


// ******************* PASSWORD METHODS **************** //


    @PostMapping("/addPasswords{id}")
    public ResponseEntity<?> addNewPassword(@RequestBody PasswordRequest request, @RequestParam int userId) {
        
        if (request.masterPassword == null || request.masterPassword.isEmpty()) {
            return ResponseEntity.status(400).body("Master Password is required for encryption.");
        }
        
        Password password = addPassword(request.website, request.username, request.password, userId, request.masterPassword);

        if (password != null) {
            // Success
            return ResponseEntity.ok(password);
        } else {
            // Failure
            return ResponseEntity.status(500).body("Failed to encrypt or save password.");
        }
    }

    private Password addPassword(String website, String username, String commonPassword, int user_id, String masterPassword) {
            
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {

            String sql = "INSERT INTO passwords (Website, Username, Password, user_id) VALUES (?, ?, ?, ?)";

            String encValue;
            try {
                encValue = manageEncryption(commonPassword, masterPassword, Cipher.ENCRYPT_MODE);
            } catch (Exception e) {
                System.err.println("Encryption failed: " + e.getMessage());
                return null;
            }
            
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, website);
            ps.setString(2, username);
            ps.setString(3, encValue);
            ps.setInt(4, user_id);
                
            int addedRows = ps.executeUpdate();

             if (addedRows > 0) {
                Password password = new Password();
                password.website = website;
                password.username = username;
                password.encPassword = commonPassword;
                password.user_id = user_id;
                return password;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @PutMapping("/editPasswords/{id}")
    public ResponseEntity<?> editPasswords(@PathVariable int id, @RequestBody PasswordRequest request) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {

            String encValue;
            try {
                encValue = manageEncryption(request.password, request.masterPassword, Cipher.ENCRYPT_MODE);
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Encryption failed: " + e.getMessage());
            }

            String sql = "UPDATE passwords SET Website = ?, Username = ?, Password = ? WHERE Number = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, request.website);
            ps.setString(2, request.username);
            ps.setString(3, encValue);
            ps.setInt(4, id);
            ps.executeUpdate();
            return ResponseEntity.ok().build();
        } catch (SQLException e) { return ResponseEntity.status(500).body(e.getMessage()); }
    }

    @DeleteMapping("/deletePasswords/{id}")
    public ResponseEntity<Void> deletePasswordApi(@PathVariable int id) {
        try {
            // Calling your original method
            deletePassword(id); 
            return ResponseEntity.ok().build();
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    private void deletePassword(int id) throws SQLException {
        String sql = "DELETE FROM passwords WHERE Number = ?";
    
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }


// ******************* CONTACT METHODS **************** //

    @PostMapping("/addContacts{id}")
    public ResponseEntity<?> addNewContact(@RequestBody ContactRequest request, @RequestParam int userId) {

        Contact contact = addContact(request.name, request.email, request.phone, userId);

        if (contact != null) {
            // Success
            return ResponseEntity.ok(contact);
        } else {
            // Failure
            return null;
        }
    }

    private Contact addContact(String contactName, String contactEmail, String contactNumber, int user_id) {
            
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {

            String sql = "INSERT INTO contacts (Name, Email, phoneNumber, user_id) VALUES (?, ?, ?, ?)";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, contactName);
            ps.setString(2, contactEmail);
            ps.setString(3, contactNumber);
            ps.setInt(4, user_id);
                
            int addedRows = ps.executeUpdate();

             if (addedRows > 0) {
                Contact contact = new Contact();
                contact.contactName = contactName;
                contact.contactEmail = contactEmail;
                contact.contactNumber = contactNumber;
                contact.user_id = user_id;
                return contact;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @PutMapping("/editContacts/{id}")
    public ResponseEntity<?> editContacts(@PathVariable int id, @RequestBody ContactRequest request) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            String sql = "UPDATE contacts SET Name = ?, Email = ?, phoneNumber = ? WHERE Number = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, request.name);
            ps.setString(2, request.email);
            ps.setString(3, request.phone);
            ps.setInt(4, id);
            ps.executeUpdate();
            return ResponseEntity.ok().build();
        } catch (SQLException e) { return ResponseEntity.status(500).body(e.getMessage()); }
    }

    @DeleteMapping("/deleteContacts/{id}")
    public ResponseEntity<Void> deleteConatctsApi(@PathVariable int id) {
        try {
            // Calling your original method
            deleteContact(id); 
            return ResponseEntity.ok().build();
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    private void deleteContact(int id) throws SQLException {
        String sql = "DELETE FROM contacts WHERE Number = ?";
    
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    } 

    public static class PasswordRequest {
        public String website;
        public String username;
        public String password;
        public String masterPassword;
    } 

    public static class ContactRequest {
        public String name;
        public String email;
        public String phone;
    }
}
