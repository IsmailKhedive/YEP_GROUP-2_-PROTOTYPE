package yep;

/** Large-group online learning open to every registered participant. */
public class LevelOneExchange extends VirtualExchange {
    private final String theme;
    public LevelOneExchange(String title, String theme, int capacity, int totalSessions, double passMark) {
        super(title, capacity, totalSessions, passMark);
        this.theme = theme;
    }
    @Override protected boolean mayJoin(Participant p) { return true; }
    @Override public String getLevelName() { return "Level One"; }
    public String getTheme() { return theme; }
}
