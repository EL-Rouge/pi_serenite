package models;

import java.time.LocalDateTime;

public class Consultation {

    private long          id;
    private long          appointmentRequestId; // must be CONFIRMED
    private long          clientId;
    private long          doctorId;
    private String        notes;
    private String        diagnosis;
    private String        prescription;
    private LocalDateTime consultationDate;
    private LocalDateTime creationDate;

    // ─── Constructors ────────────────────────────────────────────

    public Consultation() {}

    public Consultation(long appointmentRequestId, long clientId, long doctorId,
                        String notes, String diagnosis, String prescription,
                        LocalDateTime consultationDate) {
        this.appointmentRequestId = appointmentRequestId;
        this.clientId             = clientId;
        this.doctorId             = doctorId;
        this.notes                = notes;
        this.diagnosis            = diagnosis;
        this.prescription         = prescription;
        this.consultationDate     = consultationDate;
    }

    // ─── Getters & Setters ───────────────────────────────────────

    public long getId()                          { return id; }
    public void setId(long id)                   { this.id = id; }

    public long getAppointmentRequestId()                           { return appointmentRequestId; }
    public void setAppointmentRequestId(long appointmentRequestId)  { this.appointmentRequestId = appointmentRequestId; }

    public long getClientId()                    { return clientId; }
    public void setClientId(long clientId)       { this.clientId = clientId; }

    public long getDoctorId()                    { return doctorId; }
    public void setDoctorId(long doctorId)       { this.doctorId = doctorId; }

    public String getNotes()                     { return notes; }
    public void setNotes(String notes)           { this.notes = notes; }

    public String getDiagnosis()                 { return diagnosis; }
    public void setDiagnosis(String diagnosis)   { this.diagnosis = diagnosis; }

    public String getPrescription()                      { return prescription; }
    public void setPrescription(String prescription)     { this.prescription = prescription; }

    public LocalDateTime getConsultationDate()                       { return consultationDate; }
    public void setConsultationDate(LocalDateTime consultationDate)  { this.consultationDate = consultationDate; }

    public LocalDateTime getCreationDate()                   { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate)  { this.creationDate = creationDate; }
}