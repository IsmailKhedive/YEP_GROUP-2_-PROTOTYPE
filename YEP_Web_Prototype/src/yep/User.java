package yep;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Abstract base class for every person who uses YEP (inheritance + abstraction). */
public abstract class User {
    private static int nextId = 1;
    private final int userId;
    private String fullName;
    private final String email;
    private final String salt;
    private String passwordHash;           // encapsulated: never exposed
    private String country;

    protected User(String fullName, String email, String password, String country) {
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Name is required");
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new IllegalArgumentException("Invalid email");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");
        this.userId = nextId++;
        this.fullName = fullName.trim();
        this.email = email.toLowerCase();
        byte[] s = new byte[12];
        new SecureRandom().nextBytes(s);
        this.salt = Base64.getEncoder().encodeToString(s);
        this.passwordHash = hash(password);
        this.country = country;
    }

    private String hash(String pw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest((salt + pw).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public boolean login(String password) { return passwordHash.equals(hash(password)); }

    /** Polymorphic: each subclass reports its own role. */
    public abstract String getRole();

    public int getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getCountry() { return country; }
    public void updateProfile(String name, String country) { this.fullName = name; this.country = country; }

    @Override public String toString() { return getRole() + " #" + userId + " " + fullName + " (" + country + ")"; }
}
