package yep;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Team-based Impact Project exchange; only participants selected from Level One may join. */
public class LevelTwoExchange extends VirtualExchange {
    private final LevelOneExchange source;
    private final List<Participant> selected = new ArrayList<>();
    private final List<Team> teams = new ArrayList<>();

    public LevelTwoExchange(String title, LevelOneExchange source, int capacity, int totalSessions, double passMark) {
        super(title, capacity, totalSessions, passMark);
        this.source = source;
    }
    @Override protected boolean mayJoin(Participant p) { return selected.contains(p); }
    @Override public String getLevelName() { return "Level Two"; }

    /** Picks the best completed Level One participants (score and attendance rules). */
    public List<Participant> selectParticipants(double minScore, int places) {
        selected.clear();
        source.getEnrolments().stream()
            .filter(e -> e.getStatus() == Enrolment.Status.APPROVED)
            .filter(e -> e.averageScore() >= minScore && e.attendanceRate() >= 0.7)
            .sorted(Comparator.comparingDouble(Enrolment::averageScore).reversed())
            .limit(places)
            .forEach(e -> selected.add(e.getParticipant()));
        for (Participant p : selected) enrol(p);
        return List.copyOf(selected);
    }

    /** Forms teams of the given size and assigns an available mentor to each. */
    public List<Team> formTeams(int teamSize, List<Mentor> mentors) {
        teams.clear();
        List<Participant> pool = new ArrayList<>(selected);
        // spread countries: sort by country then deal participants round-robin into teams
        pool.sort(Comparator.comparing(Participant::getCountry));
        int n = Math.max(1, (int) Math.ceil(pool.size() / (double) teamSize));
        for (int i = 0; i < n; i++) teams.add(new Team("Team " + (i + 1)));
        for (int i = 0; i < pool.size(); i++) teams.get(i % n).addMember(pool.get(i));
        for (Team t : teams) {
            Mentor m = mentors.stream().filter(Mentor::isAvailable).findFirst().orElse(null);
            if (m != null) t.assignMentor(m);
        }
        return List.copyOf(teams);
    }
    public List<Team> getTeams() { return List.copyOf(teams); }
}
