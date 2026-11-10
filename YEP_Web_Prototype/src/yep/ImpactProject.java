package yep;

import java.util.ArrayList;
import java.util.List;

public class ImpactProject {
    public enum State { IN_PROGRESS, READY_FOR_REVIEW }
    public static class Milestone {
        public enum Status { TODO, DOING, DONE }
        private final String title;
        private Status status = Status.TODO;
        Milestone(String title) { this.title = title; }
        public String getTitle() { return title; }
        public Status getStatus() { return status; }
    }

    private final String title;
    private final String focusArea;
    private State state = State.IN_PROGRESS;
    private final List<Milestone> milestones = new ArrayList<>();

    ImpactProject(String title, String focusArea) { this.title = title; this.focusArea = focusArea; }

    public Milestone addMilestone(String title) { Milestone m = new Milestone(title); milestones.add(m); return m; }

    public void updateStatus(Milestone m, Milestone.Status s) {
        m.status = s;
        if (!milestones.isEmpty() && milestones.stream().allMatch(x -> x.status == Milestone.Status.DONE)) state = State.READY_FOR_REVIEW;
        else state = State.IN_PROGRESS;
    }
    public String getTitle() { return title; }
    public String getFocusArea() { return focusArea; }
    public State getState() { return state; }
    public List<Milestone> getMilestones() { return List.copyOf(milestones); }
}
