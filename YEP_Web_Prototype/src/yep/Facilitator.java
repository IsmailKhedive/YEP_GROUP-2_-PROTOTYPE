package yep;

public class Facilitator extends User {
    public Facilitator(String name, String email, String password, String country) { super(name, email, password, country); }
    @Override public String getRole() { return "Facilitator"; }
}
