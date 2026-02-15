package service;

import models.AppointmentRequest;
import models.Consultation;
import repository.ConsultationRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class ConsultationService {

    private final ConsultationRepository repository = new ConsultationRepository();
    private final AppointmentRequestService appointmentService = new AppointmentRequestService();

    // ─── CREATE ─────────────────────────────────────

    public Consultation createConsultation(Consultation consultation) throws SQLException {

        // 1️⃣ Validate appointment exists
        AppointmentRequest appointment =
                appointmentService.getById(consultation.getAppointmentRequestId());

        // 2️⃣ VERY IMPORTANT RULE
        if (!"CONFIRMED".equals(appointment.getStatus())) {
            throw new IllegalStateException(
                    "Consultation can only be created for CONFIRMED appointments.");
        }

        // 3️⃣ Optional: enforce consultation date = confirmed date
        if (appointment.getConfirmedDate() != null &&
                !consultation.getConsultationDate().equals(appointment.getConfirmedDate())) {

            throw new IllegalArgumentException(
                    "Consultation date must match the confirmed appointment date.");
        }

        consultation.setCreationDate(LocalDateTime.now());

        return repository.save(consultation);
    }

    public Consultation getById(long id) throws SQLException {
        Consultation c = repository.findById(id);
        if (c == null)
            throw new IllegalArgumentException("Consultation not found with id: " + id);
        return c;
    }

    public List<Consultation> getAll() throws SQLException {
        return repository.findAll();
    }

    public void updateConsultation(Consultation consultation) throws SQLException {
        repository.update(consultation);
    }

    public void deleteConsultation(long id) throws SQLException {
        repository.delete(id);
    }
}
