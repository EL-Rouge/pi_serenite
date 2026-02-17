package service;

import models.AppointmentRequest;
import models.Consultation;
import repository.ConsultationRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ConsultationService {

    private final ConsultationRepository    repository         = new ConsultationRepository();
    private final AppointmentRequestService appointmentService = new AppointmentRequestService();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ─── CREATE ─────────────────────────────────────

    public Consultation createConsultation(Consultation consultation) throws SQLException {

        // 1. Appointment must exist
        AppointmentRequest appointment =
                appointmentService.getById(consultation.getAppointmentRequestId());

        // 2. Must be CONFIRMED — reject PENDING and already-CONSULTED
        if ("CONSULTED".equals(appointment.getStatus())) {
            throw new IllegalStateException(
                    "A consultation already exists for this appointment.");
        }
        if (!"CONFIRMED".equals(appointment.getStatus())) {
            throw new IllegalStateException(
                    "Consultation can only be created for CONFIRMED appointments.");
        }

        // 3. Consultation day must match the confirmed date
        if (appointment.getConfirmedDate() != null) {
            LocalDate confirmedDay    = appointment.getConfirmedDate().toLocalDate();
            LocalDate consultationDay = consultation.getConsultationDate().toLocalDate();
            if (!consultationDay.equals(confirmedDay)) {
                throw new IllegalArgumentException(
                        "Consultation date must match the confirmed appointment date ("
                                + confirmedDay.format(DATE_FMT) + ").");
            }
        }

        // 4. Save consultation
        consultation.setCreationDate(LocalDateTime.now());
        Consultation saved = repository.save(consultation);

        // 5. Flip appointment → CONSULTED so it vanishes from the list
        appointmentService.updateStatus(appointment.getId(), "CONSULTED");

        return saved;
    }

    // ─── READ ─────────────────────────────────────

    public Consultation getById(long id) throws SQLException {
        Consultation c = repository.findById(id);
        if (c == null)
            throw new IllegalArgumentException("Consultation not found with id: " + id);
        return c;
    }

    public List<Consultation> getAllConsultations() throws SQLException {
        return repository.findAll();
    }

    public List<Consultation> getConsultationsByDoctorId(long doctorId) throws SQLException {
        return repository.findByDoctorId(doctorId);
    }

    // ─── UPDATE ─────────────────────────────────────

    public void updateConsultation(Consultation consultation) throws SQLException {
        if (consultation.getDiagnosis() == null || consultation.getDiagnosis().isBlank())
            throw new IllegalArgumentException("Diagnosis is required.");
        repository.update(consultation);
    }

    // ─── DELETE ─────────────────────────────────────

    public void deleteConsultation(long id) throws SQLException {
        repository.delete(id);
    }
}