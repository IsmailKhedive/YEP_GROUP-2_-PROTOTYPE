package yep;

import java.util.ArrayList;
import java.util.List;

public class Mentor extends User {
    private final int maxTeams;
    private final List<Team> teams = new ArrayList<>();

    public Mentor(String name, String email, String password, String country, int maxTeams) {
        super(name, email, password, country);
        this.maxTeams = maxTeams;
    }
    @Override public String getRole() { return "Mentor"; }
    public boolean isAvailable() { return teams.size() < maxTeams; }
    public boolean acceptTeam(Team t) {
        if (!isAvailable()) return false;
        teams.add(t);
        return true;
    }
    public List<Team> getTeams() { return List.copyOf(teams); }
}
