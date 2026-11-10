package yep;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/** Swing front end for the prototype. Run with --demo for a console walkthrough. */
public class Main {
    private final YepService svc = new YepService();
    private final JTextArea out = new JTextArea();
    private final Administrator admin = svc.add(new Administrator("Grace Admin", "admin@yep.org", "admin123", "Uganda"));
    private LevelOneExchange l1;
    private LevelTwoExchange l2;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--demo")) { Demo.run(); return; }
        SwingUtilities.invokeLater(() -> new Main().show());
    }

    private void log(String s) { out.append(s + "\n"); out.setCaretPosition(out.getDocument().getLength()); }

    private void show() {
        svc.add(new Mentor("Moses Mentor", "moses@yep.org", "mentor1", "Kenya", 2));
        svc.add(new Mentor("Lydia Mentor", "lydia@yep.org", "mentor2", "Uganda", 2));
        l1 = svc.createLevelOne("Digital Literacy & AI Cohort 1", "Digital literacy", 50, 6, 50);
        JFrame f = new JFrame("YEP - Youth Digital Empowerment Platform (prototype)");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel side = new JPanel(new GridLayout(0, 1, 6, 6));
        side.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(side, "1. Register participant", this::register);
        add(side, "2. Enrol & approve", this::enrol);
        add(side, "3. Record attendance & quiz", this::record);
        add(side, "4. Select Level 2 & form teams", this::teams);
        add(side, "5. Add project milestone", this::milestone);
        add(side, "6. Issue certificate", this::certificate);
        add(side, "7. Verify certificate", this::verify);
        add(side, "List participants", () -> svc.getParticipants().forEach(p -> log(p.toString())));
        out.setEditable(false); out.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        f.add(side, BorderLayout.WEST);
        f.add(new JScrollPane(out), BorderLayout.CENTER);
        f.setSize(900, 520); f.setLocationRelativeTo(null); f.setVisible(true);
        log("Welcome to YEP. Cohort: " + l1.getTitle() + " (6 sessions, pass mark 50%). Admin: admin@yep.org");
    }

    private void add(JPanel p, String label, Runnable r) {
        JButton b = new JButton(label);
        b.addActionListener(e -> { try { r.run(); } catch (RuntimeException ex) { log("ERROR: " + ex.getMessage()); } });
        p.add(b);
    }
    private String ask(String q) { return JOptionPane.showInputDialog(null, q); }
    private Participant pick() {
        String email = ask("Participant email:");
        if (email == null) throw new IllegalStateException("Cancelled");
        User u = svc.login(email, ask("Participant password:"));
        if (!(u instanceof Participant p)) throw new IllegalStateException("Login failed");
        return p;
    }

    private void register() {
        String name = ask("Full name:"), email = ask("Email:"), pw = ask("Password (min 6):"), country = ask("Country:");
        int year = Integer.parseInt(ask("Year of birth (e.g. 2000):"));
        boolean consent = JOptionPane.showConfirmDialog(null, "Do you accept the privacy terms?", "Consent", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        log("Registered: " + svc.registerParticipant(name, email, pw, country, LocalDate.of(year, 1, 1), consent));
    }
    private void enrol() {
        Participant p = pick();
        Enrolment e = l1.enrol(p);
        l1.approve(admin, e);
        log(p.getFullName() + " enrolled: " + e.getStatus());
    }
    private void record() {
        Participant p = pick();
        Enrolment e = l1.findEnrolment(p);
        if (e == null) throw new IllegalStateException("Not enrolled");
        e.recordAttendance(Integer.parseInt(ask("Sessions attended (of 6):")));
        e.recordQuiz(Double.parseDouble(ask("Quiz marks (out of 100):")), 100);
        log(String.format("%s attendance=%.0f%% average=%.0f%%", p.getFullName(), e.attendanceRate() * 100, e.averageScore()));
    }
    private void teams() {
        if (l2 == null) l2 = svc.createLevelTwo("Youth Impact Challenge", l1, 20, 4, 50);
        List<Participant> sel = l2.selectParticipants(60, 12);
        log("Selected for Level Two: " + sel.stream().map(Participant::getFullName).toList());
        for (Team t : l2.formTeams(3, svc.getMentors()))
            log(t.getName() + " " + t.getMembers().stream().map(Participant::getFullName).toList() + " mentor=" + (t.getMentor() == null ? "none" : t.getMentor().getFullName()));
    }
    private void milestone() {
        if (l2 == null || l2.getTeams().isEmpty()) throw new IllegalStateException("Form teams first");
        Team t = l2.getTeams().get(0);
        ImpactProject pr = t.getProject() != null ? t.getProject() : t.createProject("Project of " + t.getName(), "Digital inclusion");
        ImpactProject.Milestone m = pr.addMilestone(ask("Milestone title:"));
        pr.updateStatus(m, ImpactProject.Milestone.Status.DONE);
        log(t.getName() + " project state: " + pr.getState() + " (" + pr.getMilestones().size() + " milestone(s))");
    }
    private void certificate() { log(String.valueOf(svc.issueCertificate(l1, pick()))); }
    private void verify() {
        Certificate c = svc.verify(ask("Certificate code:"));
        log(c == null ? "Certificate NOT found" : "VALID: " + c);
    }
}
