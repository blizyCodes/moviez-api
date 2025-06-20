# Building a Movie Reservation System with Spring Boot (Gradle)

This guide provides a comprehensive walkthrough for building a backend system for a movie reservation service using Spring Boot, Spring Security with JWT, and PostgreSQL. This project focuses on complex data modeling, business logic for seat reservations, and role-based access control (Admin vs. User).

---

## 1. Project Setup with Spring Initializr

First, we'll generate the Spring Boot project structure using the Spring Initializr in VS Code.

1.  **Open the Command Palette** (`Ctrl+Shift+P` or `Cmd+Shift+P`).
2.  Type `Spring Initializr` and select **Spring Initializr: Create a Gradle Project...**.
3.  Follow the prompts:
    - **Specify Spring Boot version:** Choose the latest stable version (e.g., 3.x.x).
    - **Specify project language:** `Java`.
    - **Input Group Id:** `com.example`.
    - **Input Artifact Id:** `movie-reservation-api`.
    - **Specify Java version:** `17` or newer.
    - **Search for dependencies:** Select the following:
      - `Spring Web`
      - `Spring Data JPA`
      - `PostgreSQL Driver`
      - `Spring Security`
      - `Lombok`
      - `Validation`
4.  Select a folder to generate the project into and open it in VS Code.

### Add JWT Dependencies

Open your `build.gradle` file and add the following lines inside the `dependencies { ... }` block:

```groovy
dependencies {
	// ... other dependencies
	implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
	runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
	runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'
}
```

Save the file. Gradle will automatically sync and download the new dependencies.

---

## 2. Configure Application Properties

Open `src/main/resources/application.properties`. Configure the connection to your PostgreSQL database and set a secret key for signing JWTs.

**Note:** Ensure you have created a database named `movie_db` in PostgreSQL. Replace `your_postgres_username` and `your_postgres_password` with your credentials.

```properties
# PostgreSQL Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/movie_db
spring.datasource.username=your_postgres_username
spring.datasource.password=your_postgres_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# JWT Secret Key - USE A STRONG, RANDOMLY GENERATED KEY IN PRODUCTION
jwt.secret=a-very-strong-and-secure-secret-key-for-movie-reservation-project

# App specific properties
app.total-seats-per-showtime=100 # Example: 100 seats per hall
```

---

## 3. Create Data Models (Entities)

This project requires a more complex data model. Create a new package `com.example.movie-reservation-api.model`.

### Role Entity

We need roles for our users (`ROLE_USER`, `ROLE_ADMIN`).

`Role.java`:

````java
package com.example.movie-reservation-api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ERole name;

    public Role(ERole name) {
        this.name = name;
    }
}
```ERole.java` (Enum):
```java
package com.example.movie-reservation-api.model;

public enum ERole {
    ROLE_USER,
    ROLE_ADMIN
}
````

### User Entity

`User.java`:

```java
package com.example.movie-reservation-api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    private String password;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
               joinColumns = @JoinColumn(name = "user_id"),
               inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
}
```

### Movie Entity

`Movie.java`:

```java
package com.example.movie-reservation-api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "movies")
@Data
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    private String posterImageUrl;

    @Column(nullable = false)
    private String genre;

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Showtime> showtimes;
}
```

### Showtime Entity

`Showtime.java`:

```java
package com.example.movie-reservation-api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "showtimes")
@Data
public class Showtime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    @JsonIgnore
    private Movie movie;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @OneToMany(mappedBy = "showtime", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reservation> reservations;
}
```

### Reservation Entity

`Reservation.java`:

```java
package com.example.movie-reservation-api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reservations")
@Data
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reservation_seats", joinColumns = @JoinColumn(name = "reservation_id"))
    @Column(name = "seat_number")
    private List<Integer> reservedSeats;

    private LocalDateTime bookingTime;
}
```

---

## 4. Create Repositories

Create a `repository` package and add a repository interface for each entity.

- `RoleRepository.java`
- `UserRepository.java`
- `MovieRepository.java`
- `ShowtimeRepository.java`
- `ReservationRepository.java`

Example: `UserRepository.java`

```java
package com.example.movie-reservation-api.repository;

import com.example.movie-reservation-api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
}
```

Example: `ShowtimeRepository.java`

```java
package com.example.movie-reservation-api.repository;

import com.example.movie-reservation-api.model.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    // Find showtimes for a specific date range
    List<Showtime> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}
```

Create the other repositories similarly, extending `JpaRepository`.

---

## 5. Implement Security (JWT & Role-Based)

The security setup is similar to the previous project but now includes roles. Create a `security` package.

### JWT and UserDetails Logic

- `JwtTokenProvider.java`: This class remains largely the same.
- `CustomUserDetails.java`: This will represent the authenticated user principal.
- `CustomUserDetailsService.java`: This service loads user data for Spring Security.

`CustomUserDetails.java`:

````java
// In security package
public class CustomUserDetails implements UserDetails {
    private Long id;
    private String email;
    @JsonIgnore
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    // Constructor, getters, and implementations of UserDetails methods...
}
```CustomUserDetailsService.java`:
```java
// In security package
@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(), user.getPassword(), authorities);
    }
}
````

### Security Configuration

`SecurityConfig.java`: Here we define endpoint permissions based on roles.

```java
package com.example.movie-reservation-api.security;

// imports...

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // ... Injected beans (JwtRequestFilter, etc.)

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll() // Login/Register
                .requestMatchers("/api/movies/**", "/api/showtimes/**").permitAll() // Publicly viewable movies/showtimes
                .requestMatchers("/api/reservations/**").hasRole("USER") // Users can make reservations
                .requestMatchers("/api/admin/**").hasRole("ADMIN") // Admin-only endpoints
                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // ... Other beans (PasswordEncoder, AuthenticationManager)
}
```

---

## 6. Data Seeding for Initial Roles and Admin User

We need to ensure the `roles` table is populated and create an initial admin user on startup.

Create `DataSeeder.java` in the main package.

```java
package com.example.movie-reservation-api;

import com.example.movie-reservation-api.model.*;
import com.example.movie-reservation-api.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {
    // ... Inject repositories and PasswordEncoder

    @Override
    public void run(String... args) throws Exception {
        // Seed Roles
        if (roleRepository.findByName(ERole.ROLE_USER).isEmpty()) {
            roleRepository.save(new Role(ERole.ROLE_USER));
        }
        if (roleRepository.findByName(ERole.ROLE_ADMIN).isEmpty()) {
            roleRepository.save(new Role(ERole.ROLE_ADMIN));
        }

        // Seed Admin User
        if (!userRepository.existsByEmail("admin@movie.com")) {
            User admin = new User();
            admin.setName("Admin User");
            admin.setEmail("admin@movie.com");
            admin.setPassword(passwordEncoder.encode("adminpassword"));
            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                          .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
        }
    }
}
```

---

## 7. Implement Services and Controllers

Create `dto`, `service`, and `controller` packages.

### Auth Flow (Register/Login)

- **DTOs**: `SignupRequest`, `LoginRequest`, `JwtResponse`.
- **`AuthService`**: Handles user registration, assigning the `ROLE_USER` by default.
- **`AuthController`**: Exposes `/api/auth/signup` and `/api/auth/signin`.

### Admin Functionality

- **`AdminController`** (`/api/admin`):
  - Add/Update/Delete Movies
  - Add/Update/Delete Showtimes
  - View all reservations and revenue reports.
- **`MovieService`**, **`ShowtimeService`**: Contain the business logic for these operations.

### User Functionality

- **`MovieController`** (`/api/movies`):
  - Get all movies.
  - Get a single movie by ID.
- **`ShowtimeController`** (`/api/showtimes`):
  - Get showtimes for a specific movie on a specific date.
- **`ReservationController`** (`/api/reservations`):
  - Create a reservation.
  - View own reservations.
  - Cancel an upcoming reservation.
- **`ReservationService`**: The core logic resides here.

#### Handling Seat Reservations (Concurrency)

To avoid two users booking the same seat simultaneously, we need to handle concurrency. A simple and effective method is using pessimistic locking.

In your `ReservationService`:

```java
@Service
public class ReservationService {

    @Autowired
    private ShowtimeRepository showtimeRepository;
    // ... other repositories

    @Transactional
    public Reservation createReservation(Long showtimeId, List<Integer> seatNumbers, User user) {
        // Lock the showtime row in the database for this transaction
        Showtime showtime = showtimeRepository.findById(showtimeId)
            .orElseThrow(() -> new RuntimeException("Showtime not found"));
        // This is a simplified example. A more robust way is to use a dedicated lock query.
        // For high concurrency, consider using @Lock(LockModeType.PESSIMISTIC_WRITE) on a repository method.

        // 1. Get all currently reserved seats for this showtime
        List<Integer> allReservedSeats = showtime.getReservations().stream()
                .flatMap(r -> r.getReservedSeats().stream())
                .collect(Collectors.toList());

        // 2. Check if any of the requested seats are already taken
        boolean seatsAlreadyTaken = seatNumbers.stream().anyMatch(allReservedSeats::contains);
        if (seatsAlreadyTaken) {
            throw new RuntimeException("One or more selected seats are already reserved.");
        }

        // 3. Check if booking exceeds total seats
        // ...

        // 4. If all checks pass, create and save the reservation
        Reservation reservation = new Reservation();
        // ... set properties
        return reservationRepository.save(reservation);
    }
}
```

---

## 8. Running and Testing the API

1. **Database Setup:** Ensure PostgreSQL is running and you have created the `movie_db` database.
2. **Run the App:** Run `MovieReservationApiApplication.java`. The `DataSeeder` will populate initial data.
3. **Test with an API Client (e.g., Postman):**
   - **Login as Admin:**
     - `POST` `/api/auth/signin` with admin credentials (`admin@movie.com`, `adminpassword`).
     - Copy the JWT token. Use it in the `Authorization: Bearer <token>` header for all subsequent admin requests.
   - **Admin: Add a Movie:**
     - `POST` `/api/admin/movies`
     - Body: `{"title": "Inception", "description": "...", "genre": "Sci-Fi"}`
   - **Admin: Add a Showtime:**
     - `POST` `/api/admin/showtimes`
     - Body: `{"movieId": 1, "startTime": "2025-06-21T19:00:00", "endTime": "2025-06-21T21:30:00"}`
   - **Register as a User:**
     - `POST` `/api/auth/signup` with a new user's details.
   - **Login as User:**
     - `POST` `/api/auth/signin` with the new user's credentials.
     - Copy the new JWT token for user actions.
   - **User: View Movies:**
     - `GET` `/api/movies`
   - **User: Reserve Seats:**
     - `POST` `/api/reservations`
     - Header: `Authorization: Bearer <user_token>`
     - Body: `{"showtimeId": 1, "seatNumbers": [25, 26]}`

This structure provides a robust foundation for your movie reservation system. You can build out the DTOs, services, and controller methods following this design.
