package yep;

import java.time.LocalDate;
import java.time.Period;

public class Participant extends User {
    private final LocalDate dateOfBirth;
    private boolean lowBandwidthMode = true;

    public Participant(String name, String email, String password, String country, LocalDate dob, boolean consent) {
        super(name, email, password, country);
        if (!consent) throw new IllegalArgumentException("Consent to the privacy terms is required");
        int age = Period.between(dob, LocalDate.now()).getYears();
        if (age < 18 || age > 35) throw new IllegalArgumentException("Participants must be aged 18-35 (age " + age + ")");
        this.dateOfBirth = dob;
    }

    @Override public String getRole() { return "Participant"; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public boolean isLowBandwidthMode() { return lowBandwidthMode; }
    public void setLowBandwidthMode(boolean b) { lowBandwidthMode = b; }
}
