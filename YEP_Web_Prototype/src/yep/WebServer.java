package yep;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Small, framework-free web front end for the YEP classroom prototype. */
public class WebServer {
    private final YepService service = new YepService();
    private final Map<String, User> sessions = new ConcurrentHashMap<>();
    private final Map<Integer, Portfolio> portfolios = new ConcurrentHashMap<>();
    private final Map<Integer, List<String>> completedLessons = new ConcurrentHashMap<>();
    private final Map<Integer, Team> participantTeams = new ConcurrentHashMap<>();
    private final Map<Integer, Certificate> issuedCertificates = new ConcurrentHashMap<>();
    private final Map<String, String> mime = Map.of("html", "text/html; charset=utf-8", "css", "text/css; charset=utf-8", "js", "text/javascript; charset=utf-8");
    private Administrator admin;
    private LevelOneExchange levelOne;
    private LevelTwoExchange levelTwo;

    public static void main(String[] args) throws IOException {
        WebServer app = new WebServer();
        app.seed();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);
        server.createContext("/api/state", app::state);
        server.createContext("/api/login", app::login);
        server.createContext("/api/signup", app::signup);
        server.createContext("/api/logout", app::logout);
        server.createContext("/api/portfolio", app::savePortfolio);
        server.createContext("/api/lesson", app::completeLesson);
        server.createContext("/api/milestone", app::addMilestone);
        server.createContext("/api/enrol", app::enrol);
        server.createContext("/api/verify", app::verify);
        server.createContext("/", app::staticFile);
        server.start();
        System.out.println("YEP prototype is ready at http://localhost:8080");
        System.out.println("Demo login: amina@mail.com / pass1234");
    }

    private void seed() {
        admin = service.add(new Administrator("YEP Administrator", "admin@yep.org", "admin123", "Uganda"));
        Mentor mentorA = service.add(new Mentor("Moses Kiptoo", "moses@yep.org", "mentor1", "Kenya", 3));
        Mentor mentorB = service.add(new Mentor("Lydia Namusoke", "lydia@yep.org", "mentor2", "Uganda", 3));
        levelOne = service.createLevelOne("Digital Skills and AI Cohort", "Digital literacy", 50, 6, 50);
        String[][] people = {
            {"Amina Nakato", "amina@mail.com", "Uganda"}, {"Brian Otieno", "brian@mail.com", "Kenya"},
            {"Chantal Uwase", "chantal@mail.com", "Rwanda"}, {"David Kato", "david@mail.com", "Uganda"},
            {"Esther Achieng", "esther@mail.com", "Kenya"}, {"Fred Mugisha", "fred@mail.com", "Uganda"}
        };
        double[] scores = {82, 74, 91, 45, 68, 77};
        int[] attended = {5, 5, 6, 6, 5, 5};
        List<Participant> participants = new ArrayList<>();
        for (String[] person : people) {
            Participant p = service.registerParticipant(person[0], person[1], "pass1234", person[2], LocalDate.of(2000, 5, 12), true);
            participants.add(p);
            Enrolment e = levelOne.enrol(p);
            levelOne.approve(admin, e);
            e.recordAttendance(attended[participants.size() - 1]);
            e.recordQuiz(scores[participants.size() - 1], 100);
            portfolios.put(p.getUserId(), Portfolio.defaults());
        }
        levelTwo = service.createLevelTwo("Youth Impact Challenge", levelOne, 20, 4, 50);
        levelTwo.selectParticipants(60, 4);
        List<Team> teams = levelTwo.formTeams(2, service.getMentors());
        for (Team t : teams) {
            ImpactProject project = t.createProject(t.getMembers().stream().anyMatch(p -> p.getEmail().equals("amina@mail.com"))
                ? "Community AI learning circles" : "Digital access for local communities", "Digital inclusion");
            ImpactProject.Milestone m1 = project.addMilestone("Community needs mapped");
            ImpactProject.Milestone m2 = project.addMilestone("First learning session planned");
            ImpactProject.Milestone m3 = project.addMilestone("Prototype activities");
            project.updateStatus(m1, ImpactProject.Milestone.Status.DONE);
            project.updateStatus(m2, ImpactProject.Milestone.Status.DONE);
            project.updateStatus(m3, ImpactProject.Milestone.Status.DOING);
            for (Participant member : t.getMembers()) participantTeams.put(member.getUserId(), t);
        }
        Participant amina = participants.get(0);
        completedLessons.put(amina.getUserId(), new ArrayList<>(List.of("digital-basics", "online-safety")));
        try { issuedCertificates.put(amina.getUserId(), service.issueCertificate(levelOne, amina)); } catch (IllegalStateException ignored) { }
        for (int i = 1; i < participants.size(); i++) completedLessons.put(participants.get(i).getUserId(), new ArrayList<>());
    }

    private void staticFile(HttpExchange x) throws IOException {
        String path = x.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        if (path.contains("..") || path.contains("\\")) { text(x, 400, "Bad request", "text/plain; charset=utf-8"); return; }
        Path file = Path.of("web", path.substring(1));
        if (!Files.isRegularFile(file)) { text(x, 404, "Not found", "text/plain; charset=utf-8"); return; }
        String ext = file.getFileName().toString().replaceFirst("^.*\\.", "");
        x.getResponseHeaders().set("Content-Type", mime.getOrDefault(ext, "application/octet-stream"));
        byte[] data = Files.readAllBytes(file);
        x.sendResponseHeaders(200, data.length);
        try (var out = x.getResponseBody()) { out.write(data); }
    }

    private void state(HttpExchange x) throws IOException {
        User user = sessionUser(x);
        if (user == null) { json(x, 401, "{\"error\":\"Please sign in to continue.\"}"); return; }
        json(x, 200, stateJson(user));
    }

    private void login(HttpExchange x) throws IOException {
        Map<String, String> form = form(x);
        User user = service.login(value(form, "email").trim().toLowerCase(), value(form, "password"));
        if (user == null) { json(x, 401, "{\"error\":\"That email and password did not match.\"}"); return; }
        createSession(x, user);
        json(x, 200, stateJson(user));
    }

    private void signup(HttpExchange x) throws IOException {
        Map<String, String> form = form(x);
        try {
            Participant p = service.registerParticipant(value(form, "name"), value(form, "email"), value(form, "password"),
                value(form, "country"), LocalDate.parse(value(form, "dateOfBirth")), Boolean.parseBoolean(value(form, "consent")));
            portfolios.put(p.getUserId(), Portfolio.defaults());
            completedLessons.put(p.getUserId(), new ArrayList<>());
            Enrolment e = levelOne.enrol(p);
            levelOne.approve(admin, e);
            createSession(x, p);
            json(x, 200, stateJson(p));
        } catch (RuntimeException ex) { json(x, 400, errorJson(ex.getMessage())); }
    }

    private void logout(HttpExchange x) throws IOException {
        String token = sessionToken(x);
        if (token != null) sessions.remove(token);
        x.getResponseHeaders().add("Set-Cookie", "YEP_SESSION=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
        json(x, 200, "{\"ok\":true}");
    }

    private void savePortfolio(HttpExchange x) throws IOException {
        User u = requireParticipant(x); if (u == null) return;
        Map<String, String> f = form(x); Portfolio p = portfolios.computeIfAbsent(u.getUserId(), id -> Portfolio.defaults());
        p.headline = value(f, "headline").trim(); p.about = value(f, "about").trim();
        p.visibility = Set.of("Only me", "YEP members", "Public").contains(value(f, "visibility")) ? value(f, "visibility") : "Only me";
        p.skills.clear(); for (String skill : value(f, "skills").split(",")) if (!skill.isBlank() && p.skills.size() < 12) p.skills.add(skill.trim());
        json(x, 200, stateJson(u));
    }

    private void completeLesson(HttpExchange x) throws IOException {
        User u = requireParticipant(x); if (u == null) return;
        String id = value(form(x), "lesson");
        if (!Set.of("digital-basics", "online-safety", "ai-literacy").contains(id)) { json(x, 400, errorJson("Choose a lesson from the list.")); return; }
        completedLessons.computeIfAbsent(u.getUserId(), n -> new ArrayList<>());
        List<String> done = completedLessons.get(u.getUserId()); if (!done.contains(id)) done.add(id);
        json(x, 200, stateJson(u));
    }

    private void addMilestone(HttpExchange x) throws IOException {
        User u = requireParticipant(x); if (u == null) return;
        Team team = participantTeams.get(u.getUserId());
        if (team == null || team.getProject() == null) { json(x, 400, errorJson("A Level Two project is not assigned to this account yet.")); return; }
        String title = value(form(x), "title").trim();
        if (title.isBlank() || title.length() > 70) { json(x, 400, errorJson("Enter a milestone title up to 70 characters.")); return; }
        team.getProject().addMilestone(title);
        json(x, 200, stateJson(u));
    }

    private void enrol(HttpExchange x) throws IOException {
        User u = requireParticipant(x); if (u == null) return;
        Enrolment existing = levelOne.findEnrolment((Participant) u);
        if (existing == null) { existing = levelOne.enrol((Participant) u); levelOne.approve(admin, existing); }
        json(x, 200, stateJson(u));
    }

    private void verify(HttpExchange x) throws IOException {
        String code = value(form(x), "code").trim(); Certificate c = service.verify(code);
        if (c == null) { json(x, 404, "{\"valid\":false,\"message\":\"No certificate matched that code. Check it and try again.\"}"); return; }
        json(x, 200, "{\"valid\":true,\"holder\":" + q(c.getHolder().getFullName()) + ",\"programme\":" + q(c.getExchange().getTitle()) + ",\"issued\":" + q(c.getIssued().toString()) + ",\"code\":" + q(c.getCode()) + "}");
    }

    private String stateJson(User user) {
        if (!(user instanceof Participant p)) return "{\"error\":\"This prototype view is for participants.\"}";
        Portfolio profile = portfolios.computeIfAbsent(p.getUserId(), id -> Portfolio.defaults());
        Enrolment enrolment = levelOne.findEnrolment(p); Team team = participantTeams.get(p.getUserId());
        List<String> lessonDone = completedLessons.computeIfAbsent(p.getUserId(), id -> new ArrayList<>());
        Certificate cert = issuedCertificates.get(p.getUserId());
        StringBuilder b = new StringBuilder("{\"user\":{\"name\":").append(q(p.getFullName())).append(",\"email\":").append(q(p.getEmail())).append(",\"country\":").append(q(p.getCountry())).append("},");
        b.append("\"learning\":{\"title\":").append(q(levelOne.getTitle())).append(",\"attended\":").append(enrolment == null ? 0 : enrolment.getSessionsAttended()).append(",\"total\":").append(levelOne.getTotalSessions()).append(",\"score\":").append(enrolment == null ? 0 : Math.round(enrolment.averageScore())).append(",\"enrolled\":").append(enrolment != null && enrolment.getStatus() == Enrolment.Status.APPROVED).append("},");
        b.append("\"lessonsDone\":["); for(int i=0;i<lessonDone.size();i++){if(i>0)b.append(',');b.append(q(lessonDone.get(i)));} b.append("],");
        b.append("\"team\":");
        if(team==null)b.append("null"); else {
            b.append("{\"name\":").append(q(team.getName())).append(",\"mentor\":").append(q(team.getMentor()==null?"Mentor to be assigned":team.getMentor().getFullName())).append(",\"members\":[");
            for(int i=0;i<team.getMembers().size();i++){Participant member=team.getMembers().get(i);if(i>0)b.append(',');b.append("{\"name\":").append(q(member.getFullName())).append(",\"country\":").append(q(member.getCountry())).append('}');}
            b.append("],\"project\":");
            ImpactProject project=team.getProject(); if(project==null)b.append("null"); else {
                b.append("{\"title\":").append(q(project.getTitle())).append(",\"focus\":").append(q(project.getFocusArea())).append(",\"state\":").append(q(project.getState().name())).append(",\"milestones\":[");
                for(int i=0;i<project.getMilestones().size();i++){var m=project.getMilestones().get(i);if(i>0)b.append(',');b.append("{\"title\":").append(q(m.getTitle())).append(",\"status\":").append(q(m.getStatus().name())).append('}');}
                b.append("]}");
            } b.append("}");
        }
        b.append(",\"certificate\":");if(cert==null)b.append("null");else b.append("{\"code\":").append(q(cert.getCode())).append(",\"issued\":").append(q(cert.getIssued().toString())).append('}');
        b.append(",\"portfolio\":{\"headline\":").append(q(profile.headline)).append(",\"about\":").append(q(profile.about)).append(",\"visibility\":").append(q(profile.visibility)).append(",\"skills\":[");
        for(int i=0;i<profile.skills.size();i++){if(i>0)b.append(',');b.append(q(profile.skills.get(i)));} b.append("]}}"); return b.toString();
    }

    private User requireParticipant(HttpExchange x) throws IOException {
        User u=sessionUser(x); if (!(u instanceof Participant)) { json(x,401,"{\"error\":\"Please sign in as a participant.\"}"); return null; } return u;
    }
    private void createSession(HttpExchange x, User u) { String token=UUID.randomUUID().toString();sessions.put(token,u);x.getResponseHeaders().add("Set-Cookie","YEP_SESSION="+token+"; Path=/; HttpOnly; SameSite=Lax"); }
    private User sessionUser(HttpExchange x) { String token=sessionToken(x); return token==null?null:sessions.get(token); }
    private String sessionToken(HttpExchange x) { String cookie=x.getRequestHeaders().getFirst("Cookie");if(cookie==null)return null;for(String part:cookie.split(";")){String[] kv=part.trim().split("=",2);if(kv.length==2&&kv[0].equals("YEP_SESSION"))return kv[1];}return null; }
    private Map<String,String> form(HttpExchange x) throws IOException { String raw=new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);Map<String,String> map=new HashMap<>();for(String pair:raw.split("&")){if(pair.isBlank())continue;String[] kv=pair.split("=",2);map.put(decode(kv[0]),kv.length>1?decode(kv[1]):"");}return map; }
    private String decode(String s) { return URLDecoder.decode(s,StandardCharsets.UTF_8); }
    private String value(Map<String,String> m,String k) { return m.getOrDefault(k,""); }
    private String q(String s) { if(s==null)return "null";StringBuilder out=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"'->out.append("\\\"");case '\\'->out.append("\\\\");case '\n'->out.append("\\n");case '\r'->out.append("\\r");case '\t'->out.append("\\t");default->{if(c<32)out.append(String.format("\\u%04x",(int)c));else out.append(c);}}}return out.append('"').toString(); }
    private String errorJson(String s) { return "{\"error\":"+q(s==null?"Something went wrong.":s)+"}"; }
    private void json(HttpExchange x,int status,String body) throws IOException { text(x,status,body,"application/json; charset=utf-8"); }
    private void text(HttpExchange x,int status,String body,String type) throws IOException {byte[] bytes=body.getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type",type);x.getResponseHeaders().set("X-Content-Type-Options","nosniff");x.sendResponseHeaders(status,bytes.length);try(var out=x.getResponseBody()){out.write(bytes);}}

    private static final class Portfolio {
        String headline="Community builder | Digital inclusion";
        String about="I care about making useful digital skills easier to access. I enjoy learning with others and building practical ideas for my community.";
        String visibility="YEP members";
        List<String> skills=new ArrayList<>(List.of("Digital literacy","Teamwork","Community research"));
        static Portfolio defaults(){return new Portfolio();}
    }
}
