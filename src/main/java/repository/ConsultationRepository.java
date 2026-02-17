package repository;

import models.Consultation;
import util.DBconnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConsultationRepository {

    private Connection getConnection() throws SQLException {
        Connection conn = DBconnection.getInstance().getConn();
        if (conn == null) {
            throw new SQLException("Database connection is null.");
        }
        return conn;
    }

    // ─── CREATE ─────────────────────────────────────

    public Consultation save(Consultation consultation) throws SQLException {
        String sql = """
        INSERT INTO Consultation
        (appointmentRequestId, clientId, doctorId,
         notes, diagnosis, prescription,
         consultationDate, createdAt)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        Connection conn = getConnection();

        try (PreparedStatement pstmt =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, consultation.getAppointmentRequestId());
            pstmt.setLong(2, consultation.getClientId());
            pstmt.setLong(3, consultation.getDoctorId());
            pstmt.setString(4, consultation.getNotes());
            pstmt.setString(5, consultation.getDiagnosis());
            pstmt.setString(6, consultation.getPrescription());
            pstmt.setTimestamp(7, Timestamp.valueOf(consultation.getConsultationDate()));
            pstmt.setTimestamp(8, Timestamp.valueOf(consultation.getCreationDate()));

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    consultation.setId(rs.getLong(1));
                }
            }
        }

        return consultation;
    }

    // ─── READ ─────────────────────────────────────

    public Consultation findById(long id) throws SQLException {
        String sql = "SELECT * FROM Consultation WHERE id = ?";
        Connection conn = getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Consultation> findAll() throws SQLException {
        List<Consultation> list = new ArrayList<>();
        String sql = "SELECT * FROM Consultation ORDER BY consultationDate DESC";

        Connection conn = getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }

        return list;
    }

    public List<Consultation> findByDoctorId(long doctorId) throws SQLException {
        List<Consultation> list = new ArrayList<>();
        String sql = """
            SELECT * FROM Consultation
            WHERE doctorId = ?
            ORDER BY consultationDate DESC
            """;

        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, doctorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    // ─── UPDATE ─────────────────────────────────────

    public void update(Consultation consultation) throws SQLException {
        String sql = """
            UPDATE Consultation SET
            notes = ?, diagnosis = ?, prescription = ?, consultationDate = ?
            WHERE id = ?
            """;

        Connection conn = getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, consultation.getNotes());
            pstmt.setString(2, consultation.getDiagnosis());
            pstmt.setString(3, consultation.getPrescription());
            pstmt.setTimestamp(4, Timestamp.valueOf(consultation.getConsultationDate()));
            pstmt.setLong(5, consultation.getId());
            pstmt.executeUpdate();
        }
    }

    // ─── DELETE ─────────────────────────────────────

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM Consultation WHERE id = ?";
        Connection conn = getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    // ─── MAPPING ─────────────────────────────────────

    private Consultation mapRow(ResultSet rs) throws SQLException {
        Consultation c = new Consultation();
        c.setId(rs.getLong("id"));
        c.setAppointmentRequestId(rs.getLong("appointmentRequestId"));
        c.setClientId(rs.getLong("clientId"));
        c.setDoctorId(rs.getLong("doctorId"));
        c.setNotes(rs.getString("notes"));
        c.setDiagnosis(rs.getString("diagnosis"));
        c.setPrescription(rs.getString("prescription"));
        c.setConsultationDate(rs.getTimestamp("consultationDate").toLocalDateTime());
        c.setCreationDate(rs.getTimestamp("createdAt").toLocalDateTime());
        return c;
    }
}