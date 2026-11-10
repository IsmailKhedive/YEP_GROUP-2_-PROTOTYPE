package yep;

import java.time.LocalDate;
import java.util.List;

/** Console walkthrough of the main YEP workflows. */
public class Demo {
    public static void run() {
        YepService svc = new YepService();
        Administrator admin = svc.add(new Administrator("Grace Admin", "admin@yep.org", "admin123", "Uganda"));
        svc.add(new Mentor("Moses Mentor", "moses@yep.org", "mentor1", "Kenya", 2));
        svc.add(new Mentor("Lydia Mentor", "lydia@yep.org", "mentor2", "Uganda", 2));
        System.out.println("== 1. Registration ==");
        String[][] people = {{"Amina Nakato", "amina@mail.com", "Uganda"}, {"Brian Otieno", "brian@mail.com", "Kenya"},
            {"Chantal Uwase", "chantal@mail.com", "Rwanda"}, {"David Kato", "david@mail.com", "Uganda"},
            {"Esther Achieng", "esther@mail.com", "Kenya"}, {"Fred Mugisha", "fred@mail.com", "Uganda"}};
        for (String[] p : people)
            System.out.println("Registered: " + svc.registerParticipant(p[0], p[1], "pass1234", p[2], LocalDate.of(2000, 5, 12), true));
        try { svc.registerParticipant("Too Young", "kid@mail.com", "pass1234", "Uganda", LocalDate.now().minusYears(15), true); }
        catch (IllegalArgumentException ex) { System.out.println("Rejected: " + ex.getMessage()); }
        try { svc.registerParticipant("No Consent", "nc@mail.com", "pass1234", "Uganda", LocalDate.of(1999, 1, 1), false); }
        catch (IllegalArgumentException ex) { System.out.println("Rejected: " + ex.getMessage()); }
        System.out.println("Login ok? " + (svc.login("amina@mail.com", "pass1234") != null) + " | wrong password ok? " + (svc.login("amina@mail.com", "bad") != null));

        System.out.println("\n== 2. Level One enrolment, quizzes, attendance ==");
        LevelOneExchange l1 = svc.createLevelOne("Digital Literacy & AI Cohort 1", "Digital literacy", 5, 6, 50);
        double[] scores = {82, 74, 91, 45, 68, 77};
        int[] attended = {6, 5, 6, 6, 3, 5};
        List<Participant> ps = svc.getParticipants();
        for (int i = 0; i < ps.size(); i++) {
            Enrolment e = l1.enrol(ps.get(i));
            l1.approve(admin, e);
            e.recordAttendance(attended[i]);
            e.recordQuiz(scores[i], 100);
            System.out.printf("%-16s status=%-10s attendance=%3.0f%% avg=%.0f%%%n", ps.get(i).getFullName(), e.getStatus(), e.attendanceRate() * 100, e.averageScore());
        }

        System.out.println("\n== 3. Level Two selection and teams ==");
        LevelTwoExchange l2 = svc.createLevelTwo("Youth Impact Challenge", l1, 4, 4, 50);
        System.out.println("Selected: " + l2.selectParticipants(60, 4).stream().map(Participant::getFullName).toList());
        List<Team> teams = l2.formTeams(2, svc.getMentors());
        for (Team t : teams) {
            ImpactProject pr = t.createProject("Project of " + t.getName(), "Agriculture");
            var m1 = pr.addMilestone("Research"); var m2 = pr.addMilestone("Prototype");
            pr.updateStatus(m1, ImpactProject.Milestone.Status.DONE);
            pr.updateStatus(m2, ImpactProject.Milestone.Status.DONE);
            System.out.println(t.getName() + " members=" + t.getMembers().stream().map(Participant::getFullName).toList()
                + " mentor=" + (t.getMentor() == null ? "none" : t.getMentor().getFullName()) + " project state=" + pr.getState());
        }

        System.out.println("\n== 4. Certificates ==");
        for (Participant p : ps) {
            try { System.out.println(svc.issueCertificate(l1, p)); }
            catch (IllegalStateException ex) { System.out.println("No certificate: " + ex.getMessage()); }
        }
        Certificate first = svc.verify("YEP-" + LocalDate.now().getYear() + "-0001-0000");
        System.out.println("Verify unknown code -> " + first);
    }
}
