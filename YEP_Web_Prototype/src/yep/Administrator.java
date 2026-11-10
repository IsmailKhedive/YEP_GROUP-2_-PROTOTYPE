package yep;

public class Administrator extends User {
    public Administrator(String name, String email, String password, String country) { super(name, email, password, country); }
    @Override public String getRole() { return "Administrator"; }
}
