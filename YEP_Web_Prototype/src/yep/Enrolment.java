package yep;

import java.util.ArrayList;
import java.util.List;

/** Links a participant to an exchange and holds attendance and assessment results. */
public class Enrolment {
    public enum Status { PENDING, APPROVED, WAITLISTED, WITHDRAWN }

    private final Participant participant;
    private final VirtualExchange exchange;
    private Status status = Status.PENDING;
    private int sessionsAttended;
    private final List<Double> quizPercentages = new ArrayList<>();

    Enrolment(Participant p, VirtualExchange e) { this.participant = p; this.exchange = e; }

    public Participant getParticipant() { return participant; }
    public VirtualExchange getExchange() { return exchange; }
    public Status getStatus() { return status; }
    void setStatus(Status s) { status = s; }

    public void recordAttendance(int sessions) { sessionsAttended = Math.max(0, sessions); }
    public int getSessionsAttended() { return sessionsAttended; }

    /** Adds a quiz result; marks must be within 0..max. */
    public void recordQuiz(double marks, double max) {
        if (max <= 0 || marks < 0 || marks > max) throw new IllegalArgumentException("Invalid marks");
        quizPercentages.add(marks / max * 100.0);
    }
    public double averageScore() {
        return quizPercentages.isEmpty() ? 0 : quizPercentages.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
    }
    public double attendanceRate() {
        return exchange.getTotalSessions() == 0 ? 0 : (double) sessionsAttended / exchange.getTotalSessions();
    }
}
