package yep;

import java.time.LocalDate;

public class Certificate {
    private final String code;
    private final Participant holder;
    private final VirtualExchange exchange;
    private final LocalDate issued = LocalDate.now();

    Certificate(String code, Participant holder, VirtualExchange exchange) { this.code = code; this.holder = holder; this.exchange = exchange; }
    public String getCode() { return code; }
    public Participant getHolder() { return holder; }
    public VirtualExchange getExchange() { return exchange; }
    public LocalDate getIssued() { return issued; }
    @Override public String toString() {
        return "Certificate " + code + ": " + holder.getFullName() + " completed " + exchange.getTitle() + " (" + issued + ")";
    }
}
