# Dependency Guide

Quick reference for every external API used in the Loopy backend.
Open with `Cmd+P` → type `dependency` → select this file.

Use `Cmd+F` to search for a specific name (e.g. `@Entity`, `SecurityFilterChain`, `Jwts.builder`).

## Convention

When adding new files or using new dependency-provided APIs:

1. **Add a one-liner** at the top of the file (after `package`, before `import`s):
   ```java
   // Dependencies: @Annotation, ClassName, methodName — see DEPENDENCY_GUIDE.md
   ```
2. **Add an entry** to this file under the appropriate section with `### name`, `**From:**`, `**Used in:**`, and a prose explanation.
3. **Skip the one-liner** if the file only uses our own code (e.g. `UserResponse.java`, `Role.java`).
4. **Keep normal comments** (Javadoc, business logic) in the source files — only dependency explanations go here.

---

## Spring Boot (spring-boot-starter)

### `@SpringBootApplication`
**From:** `org.springframework.boot.autoconfigure`
**Used in:** `LoopyApplication.java`

Combines three annotations:
1. `@Configuration` — marks the class as a source of bean definitions
2. `@EnableAutoConfiguration` — auto-configures beans based on classpath dependencies (e.g. sees `spring-boot-starter-web` → sets up DispatcherServlet and embedded Tomcat; sees `spring-boot-starter-data-jpa` → sets up DataSource and EntityManagerFactory)
3. `@ComponentScan` — scans this package and all sub-packages for `@Component`, `@Service`, `@Repository`, `@Controller`, `@Configuration` and registers them as beans

### `SpringApplication.run()`
**From:** `org.springframework.boot`
**Used in:** `LoopyApplication.java`

Bootstraps the app: creates the ApplicationContext, triggers auto-configuration, starts the embedded Tomcat server, and begins listening for HTTP requests.

### `@Configuration`
**From:** `org.springframework.context.annotation`
**Used in:** `AppConfig.java`, `SecurityConfig.java`, `CorsConfig.java`

Marks a class as a source of `@Bean` definitions. Spring calls each `@Bean` method at startup and registers the returned object in the application context, making it available for dependency injection everywhere.

### `@Bean`
**From:** `org.springframework.context.annotation`
**Used in:** `AppConfig.java`, `SecurityConfig.java`, `CorsConfig.java`

Marks a method whose return value should be registered as a Spring-managed bean. The bean is created once at startup and injected wherever it's needed.

### `@Service`
**From:** `org.springframework.stereotype`
**Used in:** `JwtService.java`, `AuthService.java`, `TopicService.java`, `ConceptService.java`, `CardService.java`, `SM2Service.java`, `ReviewService.java`, `StatsService.java`, `EscalationService.java`, `TeachBackService.java`

Specialization of `@Component`. Marks a class as a Spring-managed bean in the service layer. Auto-detected by component scanning and available for injection.

### `@Component`
**From:** `org.springframework.stereotype`
**Used in:** `JwtAuthenticationFilter.java`

Marks a class as a Spring-managed bean. Auto-detected by component scanning. `@Service`, `@Repository`, and `@Controller` are specializations of this.

### `@Value("${property.name}")`
**From:** `org.springframework.beans.factory.annotation`
**Used in:** `JwtService.java`, `AuthService.java`

Injects values from `application.yml` into constructor/field params. `${jwt.secret}` reads the `jwt.secret` property. The syntax `${PROP:default}` provides a fallback value if the property isn't set.

### `@Transactional`
**From:** `org.springframework.transaction.annotation`
**Used in:** `AuthService.java`, `TopicService.java`, `ConceptService.java`, `CardService.java`, `ReviewService.java`, `EscalationService.java`, `TeachBackService.java`, `StatsService.java`

Wraps the method in a database transaction. If the method completes normally, the transaction is committed. If it throws an exception, the transaction is rolled back (all DB changes are undone). Ensures partial operations (e.g. user saved but token failed) don't leave the DB in an inconsistent state.

---

## Spring Web (spring-boot-starter-web)

### `@RestController`
**From:** `org.springframework.web.bind.annotation`
**Used in:** `AuthController.java`, `TopicController.java`, `ConceptController.java`, `CardController.java`, `ReviewController.java`, `StatsController.java`, `TeachBackController.java`

Combines `@Controller` + `@ResponseBody`. Every method's return value is serialized directly to JSON (via Jackson) and written to the HTTP response body. Without `@ResponseBody`, Spring would try to resolve return values as view/template names.

### `@RequestMapping`
**From:** `org.springframework.web.bind.annotation`
**Used in:** `AuthController.java`, `TopicController.java`, `ConceptController.java`, `CardController.java`, `ReviewController.java`, `StatsController.java`, `TeachBackController.java`

Sets the base URL path for all endpoints in the controller. Every method path is relative to this: `@PostMapping("/register")` → `POST /api/auth/register`.

### `@PostMapping` / `@GetMapping`
**From:** `org.springframework.web.bind.annotation`
**Used in:** `AuthController.java`, `TopicController.java`, `ConceptController.java`, `CardController.java`, `ReviewController.java`, `StatsController.java`, `TeachBackController.java`

Maps HTTP POST/GET requests to a method. Shortcut for `@RequestMapping(method = RequestMethod.POST, path = "...")`.

### `@PutMapping` / `@DeleteMapping`
**From:** `org.springframework.web.bind.annotation`
**Used in:** `TopicController.java`, `ConceptController.java`, `CardController.java`

Maps HTTP PUT/DELETE requests to a method. Used for update and delete operations in CRUD controllers.

### `@PathVariable`
**From:** `org.springframework.web.bind.annotation`
**Used in:** `TopicController.java`, `ConceptController.java`, `CardController.java`, `ReviewController.java`

Extracts a value from a URI template variable (e.g., `/api/topics/{id}`) and binds it to a method parameter.

### `@RequestParam`
**From:** `org.springframework.web.bind.annotation`
**Used in:** `ConceptController.java`, `CardController.java`, `ReviewController.java`, `TeachBackController.java`

Extracts a query parameter from the URL (e.g., `/api/concepts?topicId=...`) and binds it to a method parameter.

### `@RequestBody`
**From:** `org.springframework.web.bind.annotation`
**Used in:** `AuthController.java`, `TopicController.java`, `ConceptController.java`, `CardController.java`, `ReviewController.java`, `StatsController.java`, `TeachBackController.java`

Tells Spring to deserialize the HTTP request body (JSON) into the specified Java object using Jackson. Without it, Spring would try to read parameters from URL query params or form data.

### `ResponseEntity<T>`
**From:** `org.springframework.http`
**Used in:** `AuthController.java`, `GlobalExceptionHandler.java`, `TopicController.java`, `ConceptController.java`, `CardController.java`, `ReviewController.java`, `StatsController.java`

Represents a full HTTP response: status code, headers, and body. Gives explicit control over the response (vs. just returning an object, which always returns 200 OK).

### `@RestControllerAdvice`
**From:** `org.springframework.web.bind.annotation`
**Used in:** `GlobalExceptionHandler.java`

Combines `@ControllerAdvice` + `@ResponseBody`. Makes the class a global exception handler that applies to ALL `@RestController` classes. Any exception thrown inside a controller method is caught here before reaching the client.

### `@ExceptionHandler`
**From:** `org.springframework.web.bind.annotation`
**Used in:** `GlobalExceptionHandler.java`

Marks a method as a handler for a specific exception type. When that exception is thrown anywhere in a controller, Spring routes it to this method instead of returning a default error page. The method's return value becomes the HTTP response.

### `MethodArgumentNotValidException`
**From:** `org.springframework.web.bind`
**Used in:** `GlobalExceptionHandler.java`

Thrown when `@Valid` validation fails on a `@RequestBody` parameter. Contains a `BindingResult` with all field errors. `getBindingResult().getFieldErrors()` returns the list of validation failures, each with the field name and the constraint message.

### `OncePerRequestFilter`
**From:** `org.springframework.web.filter`
**Used in:** `JwtAuthenticationFilter.java`

Abstract base class that guarantees the filter runs exactly once per HTTP request, even if the request is internally forwarded or dispatched multiple times. Override `doFilterInternal()` for the filter logic.

---

## Spring Security (spring-boot-starter-security)

### `@EnableWebSecurity`
**From:** `org.springframework.security.config.annotation.web.configuration`
**Used in:** `SecurityConfig.java`

Activates Spring Security's web security infrastructure. Registers the `SpringSecurityFilterChain` servlet filter that intercepts ALL incoming HTTP requests and runs them through the security filter chain. Without this, none of the security rules would be applied.

### `SecurityFilterChain`
**From:** `org.springframework.security.web`
**Used in:** `SecurityConfig.java`

The object that defines the ordered list of security filters applied to incoming requests. Each filter handles one concern (CORS → CSRF → session → authentication → authorization). Spring Security can have multiple chains for different URL patterns.

### `HttpSecurity`
**From:** `org.springframework.security.config.annotation.web.builders`
**Used in:** `SecurityConfig.java`

Builder/DSL for configuring the `SecurityFilterChain`. Each method call (`.cors()`, `.csrf()`, `.sessionManagement()`, etc.) configures a specific security filter within the chain. Call `.build()` to produce the final `SecurityFilterChain`.

- `.cors(cors -> cors.configurationSource(...))` — enables the CorsFilter, which reads rules from the `CorsConfigurationSource` and adds `Access-Control-Allow-*` headers to responses
- `.csrf(csrf -> csrf.disable())` — disables CSRF protection. Not needed for stateless JWT auth since we use the `Authorization` header (not cookies)
- `.sessionManagement(session -> session.sessionCreationPolicy(STATELESS))` — tells Spring to never create or use an HTTP session. Each request must self-authenticate via JWT
- `.authorizeHttpRequests()` — defines URL-based access rules. Rules are evaluated in order — the FIRST matching rule wins
- `.addFilterBefore(filter, class)` — inserts a custom filter before a specified built-in filter in the chain

### `SessionCreationPolicy.STATELESS`
**From:** `org.springframework.security.config.http`
**Used in:** `SecurityConfig.java`

Tells Spring Security to never create or use an HTTP session. Each request must be self-authenticated via the JWT token. Without this, Spring creates a session after authentication and uses it for subsequent requests.

### `UserDetailsService`
**From:** `org.springframework.security.core.userdetails`
**Used in:** `AppConfig.java`, `JwtAuthenticationFilter.java`

Functional interface with one method: `loadUserByUsername(String username)`. Spring Security calls this during authentication to look up the user. We implement it as a lambda that queries the DB by email.

### `UserDetails`
**From:** `org.springframework.security.core.userdetails`
**Used in:** `User.java`

Core interface that Spring Security uses to represent an authenticated user. Provides: `getUsername()`, `getPassword()`, `getAuthorities()` (roles/permissions), and account status checks (expired, locked, enabled). Our `User` entity implements this directly.

### `UsernameNotFoundException`
**From:** `org.springframework.security.core.userdetails`
**Used in:** `AppConfig.java`

Thrown when the user isn't found. `DaoAuthenticationProvider` catches this and converts it to a `BadCredentialsException` (so we don't reveal whether the email or password was wrong).

### `PasswordEncoder`
**From:** `org.springframework.security.crypto.password`
**Used in:** `AppConfig.java`, `AuthService.java`

Interface for hashing and verifying passwords. `encode()` hashes a raw password; `matches()` verifies a raw password against a hash.

### `BCryptPasswordEncoder`
**From:** `org.springframework.security.crypto.bcrypt`
**Used in:** `AppConfig.java`

`PasswordEncoder` implementation using the BCrypt algorithm. Each `encode()` call produces a different hash (includes a random salt), but `matches()` can still verify. BCrypt is intentionally slow to make brute-force attacks impractical.

### `AuthenticationProvider`
**From:** `org.springframework.security.authentication`
**Used in:** `AppConfig.java`, `SecurityConfig.java`

Interface that defines how authentication is performed. Spring Security can have multiple providers (LDAP, OAuth, etc.).

### `DaoAuthenticationProvider`
**From:** `org.springframework.security.authentication.dao`
**Used in:** `AppConfig.java`

`AuthenticationProvider` implementation that authenticates by:
1. Calling `userDetailsService.loadUserByUsername()` to load the user from DB
2. Calling `passwordEncoder.matches()` to compare the raw password with the stored hash
3. Checking `UserDetails` status methods (`isEnabled`, `isAccountNonLocked`, etc.)

If all checks pass, it returns an authenticated `Authentication` object.

### `AuthenticationManager`
**From:** `org.springframework.security.authentication`
**Used in:** `AppConfig.java`, `AuthService.java`

Main entry point for authentication. Delegates to the registered `AuthenticationProvider`(s). `authenticate()` takes an `Authentication` object with credentials and returns an authenticated one (or throws).

### `AuthenticationConfiguration`
**From:** `org.springframework.security.config.annotation.authentication.configuration`
**Used in:** `AppConfig.java`

Auto-configured by Spring Boot, holds the default `AuthenticationManager`. We extract it and expose it as a bean so it can be injected into `AuthService`.

### `UsernamePasswordAuthenticationToken`
**From:** `org.springframework.security.authentication`
**Used in:** `AuthService.java`, `JwtAuthenticationFilter.java`

An `Authentication` object that holds credentials. Has two constructors:
- **2-arg** `(principal, credentials)` — creates an **unauthenticated** token. Used in `AuthService` to pass email + password to the `AuthenticationManager`
- **3-arg** `(principal, credentials, authorities)` — creates an **authenticated** token. Used in `JwtAuthenticationFilter` after JWT validation (pass `null` for credentials since the JWT already proved identity)

### `SecurityContextHolder`
**From:** `org.springframework.security.core.context`
**Used in:** `JwtAuthenticationFilter.java`

Thread-local storage for the current user's authentication. `getContext().getAuthentication()` returns the current user (or `null` if not authenticated). Setting the authentication here makes it available to all downstream code: controllers can use `@AuthenticationPrincipal`, and Spring Security's authorization checks can evaluate roles.

### `@AuthenticationPrincipal`
**From:** `org.springframework.security.core.annotation`
**Used in:** `AuthController.java`, `TopicController.java`, `ConceptController.java`, `CardController.java`, `ReviewController.java`, `StatsController.java`, `TeachBackController.java`

Extracts the authenticated user object from the `SecurityContext` (set by `JwtAuthenticationFilter`) and injects it as a method parameter. Without this, you'd need to manually call `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`.

### `WebAuthenticationDetailsSource`
**From:** `org.springframework.security.web.authentication`
**Used in:** `JwtAuthenticationFilter.java`

Extracts request metadata (remote IP, session ID) and attaches it to the auth token. This info is available for logging/auditing downstream.

### `GrantedAuthority` / `SimpleGrantedAuthority`
**From:** `org.springframework.security.core`
**Used in:** `User.java`

`GrantedAuthority` — interface representing a permission/role. `SimpleGrantedAuthority` — basic implementation that wraps a role string. The `ROLE_` prefix is a Spring convention: `hasRole("USER")` internally checks for `ROLE_USER`.

### `BadCredentialsException`
**From:** `org.springframework.security.authentication`
**Used in:** `GlobalExceptionHandler.java`

Thrown by `AuthenticationManager.authenticate()` when the password doesn't match. `DaoAuthenticationProvider` throws this after `passwordEncoder.matches()` returns false.

---

## Spring Web CORS

### `CorsConfiguration`
**From:** `org.springframework.web.cors`
**Used in:** `CorsConfig.java`

Holds CORS rules: which origins, methods, and headers are allowed. Each setter maps to an HTTP response header:
- `setAllowedOrigins()` → `Access-Control-Allow-Origin`
- `setAllowedMethods()` → `Access-Control-Allow-Methods`
- `setAllowedHeaders()` → `Access-Control-Allow-Headers`
- `setAllowCredentials()` → `Access-Control-Allow-Credentials`
- `setMaxAge()` → `Access-Control-Max-Age` (browser caches preflight result for this duration)

### `CorsConfigurationSource`
**From:** `org.springframework.web.cors`
**Used in:** `SecurityConfig.java`, `CorsConfig.java`

Interface that Spring Security's `CorsFilter` calls on every request to get the CORS rules. Returns a `CorsConfiguration` for the matched URL pattern, or null if CORS doesn't apply.

### `UrlBasedCorsConfigurationSource`
**From:** `org.springframework.web.cors`
**Used in:** `CorsConfig.java`

Implementation of `CorsConfigurationSource` that maps URL patterns to `CorsConfiguration` objects. `registerCorsConfiguration("/api/**", config)` applies CORS rules only to `/api/**` paths.

---

## Spring Data JPA (spring-boot-starter-data-jpa)

### `@EnableJpaAuditing`
**From:** `org.springframework.data.jpa.repository.config`
**Used in:** `LoopyApplication.java`

Activates the auditing infrastructure so `@CreatedDate` and `@LastModifiedDate` on entity fields are auto-populated with timestamps. Without this, those fields stay null. Works with `@EntityListeners(AuditingEntityListener.class)` on the entity.

### `JpaRepository<Entity, ID>`
**From:** `org.springframework.data.jpa.repository`
**Used in:** `UserRepository.java`, `RefreshTokenRepository.java`, `TopicRepository.java`, `ConceptRepository.java`, `CardRepository.java`, `ReviewLogRepository.java`, `TeachBackRepository.java`

Spring-provided interface that gives full CRUD operations (`save`, `findById`, `findAll`, `delete`, etc.) without writing any SQL or implementation code. Spring Data auto-generates the implementation class at startup. The two type params are `<EntityType, PrimaryKeyType>`.

### Derived Query Methods
**From:** Spring Data JPA (convention)
**Used in:** `UserRepository.java`, `RefreshTokenRepository.java`, `TopicRepository.java`, `ConceptRepository.java`, `CardRepository.java`, `ReviewLogRepository.java`, `TeachBackRepository.java`

Spring parses the method name and auto-generates the SQL query:
- `findByEmail(String email)` → `SELECT * FROM users WHERE email = ?`
- `existsByEmail(String email)` → `SELECT count(*) > 0 FROM users WHERE email = ?`
- `findByToken(String token)` → `SELECT * FROM refresh_tokens WHERE token = ?`

### `@Query`
**From:** `org.springframework.data.jpa.repository`
**Used in:** `RefreshTokenRepository.java`, `CardRepository.java`, `ReviewLogRepository.java`

When a method name can't express the query you need, write JPQL (Java Persistence Query Language) directly. JPQL uses entity/field names (`RefreshToken`, `rt.user.id`), not table/column names. `:userId` is a named parameter bound to the method argument.

### `@Param`
**From:** `org.springframework.data.repository.query`
**Used in:** `CardRepository.java`, `ReviewLogRepository.java`

Binds a method parameter to a named parameter in a `@Query` JPQL string. `@Param("userId") UUID userId` maps to `:userId` in the query.

### `@Modifying`
**From:** `org.springframework.data.jpa.repository`
**Used in:** `RefreshTokenRepository.java`

Required on `@Query` methods that change data (UPDATE, DELETE). Without it, Spring Data assumes all `@Query` methods are SELECTs and throws an exception. Must run inside a `@Transactional` context.

### `@CreatedDate` / `@LastModifiedDate`
**From:** `org.springframework.data.annotation`
**Used in:** `User.java`, `RefreshToken.java`, `Topic.java`, `Concept.java`, `Card.java`

`@CreatedDate` — the `AuditingEntityListener` sets this to the current timestamp when the entity is first persisted (INSERT). `@LastModifiedDate` — set on every persist and update, so it always reflects the last change.

### `AuditingEntityListener`
**From:** `org.springframework.data.jpa.domain.support`
**Used in:** `User.java`, `RefreshToken.java`, `Topic.java`, `Concept.java`, `Card.java`

JPA entity listener that reads `@CreatedDate`/`@LastModifiedDate` fields and sets their values automatically on persist and update events. Activated by `@EnableJpaAuditing`.

---

## JPA / Jakarta Persistence (jakarta.persistence)

### `@Entity`
**From:** `jakarta.persistence`
**Used in:** `User.java`, `RefreshToken.java`, `Topic.java`, `Concept.java`, `Card.java`, `ReviewLog.java`

Marks a class as a JPA entity, meaning Hibernate will manage its lifecycle (persist, merge, remove) and map it to a database table.

### `@Table`
**From:** `jakarta.persistence`
**Used in:** `User.java`, `RefreshToken.java`, `Topic.java`, `Concept.java`, `Card.java`, `ReviewLog.java`

Specifies the DB table name. Without this, JPA defaults to the class name. `@Table(name = "users")` avoids using "User", which is a reserved keyword in PostgreSQL.

### `@Id`
**From:** `jakarta.persistence`
**Used in:** `User.java`, `RefreshToken.java`, `Topic.java`, `Concept.java`, `Card.java`, `ReviewLog.java`

Marks a field as the primary key of the entity.

### `@GeneratedValue`
**From:** `jakarta.persistence`
**Used in:** `User.java`, `RefreshToken.java`, `Topic.java`, `Concept.java`, `Card.java`, `ReviewLog.java`

Tells Hibernate to auto-generate the value on insert. `GenerationType.UUID` makes Hibernate generate a random UUID before persisting.

### `@Column`
**From:** `jakarta.persistence`
**Used in:** `User.java`, `RefreshToken.java`, `Topic.java`, `Concept.java`, `Card.java`, `ReviewLog.java`

Maps a field to a DB column. Options:
- `nullable = false` — adds a NOT NULL constraint
- `unique = true` — adds a UNIQUE constraint
- `name = "password_hash"` — maps to a differently-named column (without it, JPA uses the lowercased field name)
- `updatable = false` — prevents Hibernate from including this column in UPDATE statements

### `@Enumerated(EnumType.STRING)`
**From:** `jakarta.persistence`
**Used in:** `User.java`, `Concept.java`, `Card.java`

Stores the enum as its name string (`"USER"`, `"ADMIN"`) instead of its ordinal index (0, 1). STRING is safer because reordering or adding enum values won't corrupt existing data. JPA defaults to ORDINAL without this.

### `@ManyToOne`
**From:** `jakarta.persistence`
**Used in:** `RefreshToken.java`, `Topic.java`, `Concept.java`, `Card.java`, `ReviewLog.java`

Defines a many-to-one relationship: many RefreshTokens belong to one User. `FetchType.LAZY` means the User is NOT loaded from DB until you call `getUser()`, avoiding unnecessary JOINs. Default is `FetchType.EAGER` which always JOINs.

### `@JoinColumn`
**From:** `jakarta.persistence`
**Used in:** `RefreshToken.java`, `Topic.java`, `Concept.java`, `Card.java`, `ReviewLog.java`

Specifies the foreign key column in the owning table that references the related table. `@JoinColumn(name = "user_id")` maps to `user_id UUID REFERENCES users(id)`.

### `@EntityListeners`
**From:** `jakarta.persistence`
**Used in:** `User.java`, `RefreshToken.java`, `Topic.java`, `Concept.java`, `Card.java`

Registers a listener that intercepts entity lifecycle events (pre-persist, pre-update). Used with `AuditingEntityListener.class` to auto-set `@CreatedDate`/`@LastModifiedDate`.

---

## Jakarta Validation (jakarta.validation)

### `@Valid`
**From:** `jakarta.validation`
**Used in:** `AuthController.java`, `TopicController.java`, `ConceptController.java`, `CardController.java`, `ReviewController.java`, `StatsController.java`, `TeachBackController.java`

Triggers validation on the request body using the constraint annotations on the DTO fields. If validation fails, Spring throws `MethodArgumentNotValidException` before the method body runs.

### `@NotBlank`
**From:** `jakarta.validation.constraints`
**Used in:** `RegisterRequest.java`, `LoginRequest.java`, `RefreshRequest.java`, `CreateTopicRequest.java`, `UpdateTopicRequest.java`, `CreateConceptRequest.java`, `UpdateConceptRequest.java`, `CreateCardRequest.java`, `UpdateCardRequest.java`, `SubmitTeachBackRequest.java`

Rejects null, empty `""`, and whitespace-only `"   "` strings.

### `@Email`
**From:** `jakarta.validation.constraints`
**Used in:** `RegisterRequest.java`, `LoginRequest.java`

Validates the string matches email format (contains `@` and domain).

### `@Size(min, max)`
**From:** `jakarta.validation.constraints`
**Used in:** `RegisterRequest.java`, `CreateTopicRequest.java`, `UpdateTopicRequest.java`, `CreateConceptRequest.java`, `UpdateConceptRequest.java`

Validates string length is within bounds. Violations throw `MethodArgumentNotValidException` (caught by `GlobalExceptionHandler`).

### `@NotNull`
**From:** `jakarta.validation.constraints`
**Used in:** `CreateConceptRequest.java`, `CreateCardRequest.java`, `SubmitReviewRequest.java`, `SubmitTeachBackRequest.java`

Rejects null values. Unlike `@NotBlank`, doesn't check for empty or whitespace — use for non-string types (UUIDs, integers).

### `@Min` / `@Max`
**From:** `jakarta.validation.constraints`
**Used in:** `SubmitReviewRequest.java`, `SubmitTeachBackRequest.java`

Validates a numeric value is within bounds. `@Min(0) @Max(5)` constrains SM-2 ratings to the 0–5 range.

---

## JJWT (io.jsonwebtoken)

### `Jwts.builder()`
**From:** `io.jsonwebtoken`
**Used in:** `JwtService.java`

Fluent builder for constructing a JWT token. A JWT has three parts: header (algorithm), payload (claims), and signature.
- `.subject()` — sets the "sub" claim: who this token is about
- `.claims()` — adds custom key-value claims to the payload
- `.issuedAt()` — sets the "iat" claim: when the token was created
- `.expiration()` — sets the "exp" claim: when the token expires
- `.signWith(key)` — signs with HMAC-SHA256, producing the signature that prevents tampering
- `.compact()` — serializes to final string: `base64(header).base64(payload).base64(signature)`

### `Jwts.parser()`
**From:** `io.jsonwebtoken`
**Used in:** `JwtService.java`

Creates a JWT parser that verifies and decodes tokens:
- `.verifyWith(key)` — sets the key to verify the signature against
- `.parseSignedClaims(token)` — decodes the token, verifies the signature matches, checks expiration, and returns the payload. Throws exceptions if the token is tampered with, expired, or malformed.

### `Claims`
**From:** `io.jsonwebtoken`
**Used in:** `JwtService.java`

Represents the JWT payload: a map of key-value pairs (`sub`, `iat`, `exp`, and custom claims like `roles`). Extends `java.util.Map`.

### `Keys.hmacShaKeyFor()`
**From:** `io.jsonwebtoken.security`
**Used in:** `JwtService.java`

Creates a `SecretKey` from raw bytes for HMAC-SHA signing. The key must be at least 256 bits (32 bytes) for HMAC-SHA256.

### `SecretKey`
**From:** `javax.crypto`
**Used in:** `JwtService.java`

Represents a symmetric cryptographic key. Used for both signing (creating) and verifying JWTs. The same key must be used for both operations.

---

## Jakarta Servlet API

### `HttpServletRequest` / `HttpServletResponse`
**From:** `jakarta.servlet.http`
**Used in:** `JwtAuthenticationFilter.java`

Represent the incoming HTTP request and outgoing HTTP response. Provide access to headers, parameters, body, etc.

### `FilterChain`
**From:** `jakarta.servlet`
**Used in:** `JwtAuthenticationFilter.java`

Represents the remaining filters in the chain. Calling `filterChain.doFilter()` passes the request to the next filter. If you don't call it, the request stops here and never reaches the controller.

---

## Java Records (Java 17)

### `record`
**From:** Java language feature
**Used in:** `RegisterRequest.java`, `LoginRequest.java`, `RefreshRequest.java`, `TokenResponse.java`, `UserResponse.java`, `CreateTopicRequest.java`, `UpdateTopicRequest.java`, `TopicResponse.java`, `CreateConceptRequest.java`, `UpdateConceptRequest.java`, `ConceptResponse.java`, `CreateCardRequest.java`, `UpdateCardRequest.java`, `CardResponse.java`, `SubmitReviewRequest.java`, `ReviewResponse.java`, `SM2Result.java`

Compact class that auto-generates constructor, getters, `equals()`, `hashCode()`, and `toString()`. Fields are final and immutable. Ideal for DTOs.
