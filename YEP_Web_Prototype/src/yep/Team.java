package yep;

import java.util.ArrayList;
import java.util.List;

public class Team {
    private final String name;
    private final List<Participant> members = new ArrayList<>();
    private Mentor mentor;
    private ImpactProject project;

    public Team(String name) { this.name = name; }
    public boolean addMember(Participant p) { return !members.contains(p) && members.add(p); }
    public void assignMentor(Mentor m) { if (m.acceptTeam(this)) this.mentor = m; }
    public ImpactProject createProject(String title, String focusArea) { project = new ImpactProject(title, focusArea); return project; }

    public String getName() { return name; }
    public List<Participant> getMembers() { return List.copyOf(members); }
    public Mentor getMentor() { return mentor; }
    public ImpactProject getProject() { return project; }
}
