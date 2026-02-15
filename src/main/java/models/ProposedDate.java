package models;

import java.time.LocalDateTime;

public class ProposedDate {
    private long id;
    private long appointmentRequestId;
    private LocalDateTime proposedDateTime;

    // Constructors
    public ProposedDate() {}

    public ProposedDate(long appointmentRequestId, LocalDateTime proposedDateTime) {
        this.appointmentRequestId = appointmentRequestId;
        this.proposedDateTime = proposedDateTime;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getAppointmentRequestId() { return appointmentRequestId; }
    public void setAppointmentRequestId(long appointmentRequestId) { this.appointmentRequestId = appointmentRequestId; }
    public LocalDateTime getProposedDateTime() { return proposedDateTime; }
    public void setProposedDateTime(LocalDateTime proposedDateTime) { this.proposedDateTime = proposedDateTime; }
}