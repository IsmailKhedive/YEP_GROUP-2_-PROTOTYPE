package yep;

import java.util.ArrayList;
import java.util.List;

/** Abstract exchange; Level One and Level Two supply their own enrolment rules (polymorphism). */
public abstract class VirtualExchange {
    private static int nextId = 1;
    private final int exchangeId = nextId++;
    private final String title;
    private final int capacity;
    private final int totalSessions;
    private final double passMark;
    protected final List<Enrolment> enrolments = new ArrayList<>();

    protected VirtualExchange(String title, int capacity, int totalSessions, double passMark) {
        this.title = title; this.capacity = capacity; this.totalSessions = totalSessions; this.passMark = passMark;
    }

    /** Each level decides who may join. */
    protected abstract boolean mayJoin(Participant p);
    public abstract String getLevelName();

    public Enrolment enrol(Participant p) {
        if (findEnrolment(p) != null) throw new IllegalStateException(p.getFullName() + " is already enrolled");
        if (!mayJoin(p)) throw new IllegalStateException(p.getFullName() + " is not eligible for " + getLevelName());
        Enrolment e = new Enrolment(p, this);
        if (countActive() >= capacity) e.setStatus(Enrolment.Status.WAITLISTED);
        enrolments.add(e);
        return e;
    }

    public void approve(Administrator admin, Enrolment e) {
        if (admin == null) throw new SecurityException("Only an administrator can approve enrolments");
        if (e.getStatus() == Enrolment.Status.PENDING) e.setStatus(Enrolment.Status.APPROVED);
    }

    private int countActive() {
        return (int) enrolments.stream().filter(e -> e.getStatus() == Enrolment.Status.PENDING || e.getStatus() == Enrolment.Status.APPROVED).count();
    }

    public Enrolment findEnrolment(Participant p) {
        return enrolments.stream().filter(e -> e.getParticipant() == p).findFirst().orElse(null);
    }

    public boolean isEligibleForCertificate(Enrolment e) {
        return e.getStatus() == Enrolment.Status.APPROVED && e.attendanceRate() >= 0.7 && e.averageScore() >= passMark;
    }

    public int getExchangeId() { return exchangeId; }
    public String getTitle() { return title; }
    public int getCapacity() { return capacity; }
    public int getTotalSessions() { return totalSessions; }
    public double getPassMark() { return passMark; }
    public List<Enrolment> getEnrolments() { return List.copyOf(enrolments); }
}
