package yep;

import java.time.LocalDate;
import java.util.*;

/** In-memory service layer. A database repository would replace these collections later. */
public class YepService {
    private final Map<String, User> users = new LinkedHashMap<>();       // key: email
    private final Map<String, Certificate> certificates = new LinkedHashMap<>();
    private final List<VirtualExchange> exchanges = new ArrayList<>();
    private int certCounter = 1;

    public Participant registerParticipant(String name, String email, String pw, String country, LocalDate dob, boolean consent) {
        if (users.containsKey(email.toLowerCase())) throw new IllegalStateException("Email already registered");
        Participant p = new Participant(name, email, pw, country, dob, consent);
        users.put(p.getEmail(), p);
        return p;
    }
    public <T extends User> T add(T user) { users.put(user.getEmail(), user); return user; }

    public User login(String email, String pw) {
        User u = users.get(email.toLowerCase());
        return (u != null && u.login(pw)) ? u : null;
    }

    public LevelOneExchange createLevelOne(String title, String theme, int capacity, int sessions, double passMark) {
        LevelOneExchange e = new LevelOneExchange(title, theme, capacity, sessions, passMark);
        exchanges.add(e);
        return e;
    }
    public LevelTwoExchange createLevelTwo(String title, LevelOneExchange src, int capacity, int sessions, double passMark) {
        LevelTwoExchange e = new LevelTwoExchange(title, src, capacity, sessions, passMark);
        exchanges.add(e);
        return e;
    }

    public Certificate issueCertificate(VirtualExchange ex, Participant p) {
        Enrolment e = ex.findEnrolment(p);
        if (e == null || !ex.isEligibleForCertificate(e))
            throw new IllegalStateException(p.getFullName() + " does not meet the certificate rules (needs approval, 70% attendance and pass mark)");
        for (Certificate c : certificates.values())
            if (c.getHolder() == p && c.getExchange() == ex) throw new IllegalStateException("Certificate already issued");
        String code = String.format("YEP-%d-%04d-%04X", LocalDate.now().getYear(), certCounter++, new Random().nextInt(0xFFFF));
        Certificate c = new Certificate(code, p, ex);
        certificates.put(code, c);
        return c;
    }
    public Certificate verify(String code) { return certificates.get(code.trim().toUpperCase()); }

    public List<User> getUsers() { return List.copyOf(users.values()); }
    public List<Participant> getParticipants() {
        List<Participant> r = new ArrayList<>();
        for (User u : users.values()) if (u instanceof Participant p) r.add(p);
        return r;
    }
    public List<Mentor> getMentors() {
        List<Mentor> r = new ArrayList<>();
        for (User u : users.values()) if (u instanceof Mentor m) r.add(m);
        return r;
    }
    public List<VirtualExchange> getExchanges() { return List.copyOf(exchanges); }
}
