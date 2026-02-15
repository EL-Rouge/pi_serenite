package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AppointmentRequest {
    private long id;
    private long clientId;
    private long doctorId;
    private LocalDateTime confirmedDate;
    private String status;
    private String type;
    private LocalDateTime creationDate;
    private List<ProposedDate> proposedDates = new ArrayList<>();
    private Doctor doctor; // ← ADD THIS

    // Constructors
    public AppointmentRequest() {}

    public AppointmentRequest(long clientId, long doctorId, String status, String type, LocalDateTime creationDate) {
        this.clientId = clientId;
        this.doctorId = doctorId;
        this.status = status;
        this.type = type;
        this.creationDate = creationDate;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getClientId() { return clientId; }
    public void setClientId(long clientId) { this.clientId = clientId; }
    public long getDoctorId() { return doctorId; }
    public void setDoctorId(long doctorId) { this.doctorId = doctorId; }
    public LocalDateTime getConfirmedDate() { return confirmedDate; }
    public void setConfirmedDate(LocalDateTime confirmedDate) { this.confirmedDate = confirmedDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }
    public List<ProposedDate> getProposedDates() { return proposedDates; }
    public void setProposedDates(List<ProposedDate> proposedDates) { this.proposedDates = proposedDates; }
    public void addProposedDate(ProposedDate proposedDate) { this.proposedDates.add(proposedDate); }
    public Doctor getDoctor() { return doctor; }       // ← ADD THIS
    public void setDoctor(Doctor doctor) { this.doctor = doctor; } // ← ADD THIS
}