package ca.gbc.userservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users") // Ensure this matches your database table name
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique identifier for the user (primary key)

    private String username; // The username of the user (used for login, etc.)
  //  private String password; // The password (hashed, never store plain text passwords)

    private String email; // The user's email address

    private String role; // The role of the user (e.g., "faculty", "student", etc.)

    private String userType; // The type of user (e.g., "student", "faculty", etc.)
}
