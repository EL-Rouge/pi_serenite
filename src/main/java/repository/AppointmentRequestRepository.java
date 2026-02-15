package repository;

import models.AppointmentRequest;
import models.ProposedDate;
import util.DBconnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AppointmentRequestRepository {

    private Connection getConnection() throws SQLException {
        Connection conn = DBconnection.getInstance().getConn();
        if (conn == null) {
            throw new SQLException("Database connection is null.");
        }
        return conn;
    }

    // ─── APPOINTMENT CRUD ───────────────────────────────────────

    public AppointmentRequest save(AppointmentRequest appointment) throws SQLException {
        String sql = "INSERT INTO AppointmentRequest (clientId, doctorId, status, type, creationDate) " +
                "VALUES (?, ?, ?, ?, ?)";

        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, appointment.getClientId());
            pstmt.setLong(2, appointment.getDoctorId());
            pstmt.setString(3, appointment.getStatus());
            pstmt.setString(4, appointment.getType());
            pstmt.setTimestamp(5, Timestamp.valueOf(appointment.getCreationDate()));
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next())
                    appointment.setId(rs.getLong(1));
            }
        }
        return appointment;
    }

    public AppointmentRequest findById(long id) throws SQLException {
        String sql = "SELECT * FROM AppointmentRequest WHERE id = ?";
        Connection conn = getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    AppointmentRequest app = mapRow(rs);
                    app.setProposedDates(findProposedDatesByAppointmentId(app.getId()));
                    return app;
                }
            }
        }
        return null;
    }

    public List<AppointmentRequest> findAll() throws SQLException {
        List<AppointmentRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM AppointmentRequest ORDER BY creationDate DESC";
        Connection conn = getConnection();

        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                AppointmentRequest app = mapRow(rs);
                app.setProposedDates(findProposedDatesByAppointmentId(app.getId()));
                list.add(app);
            }
        }
        return list;
    }

    public List<AppointmentRequest> findByClientId(long clientId) throws SQLException {
        List<AppointmentRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM AppointmentRequest WHERE clientId = ? ORDER BY creationDate DESC";
        Connection conn = getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, clientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AppointmentRequest app = mapRow(rs);
                    app.setProposedDates(findProposedDatesByAppointmentId(app.getId()));
                    list.add(app);
                }
            }
        }
        return list;
    }

    public void update(AppointmentRequest appointment) throws SQLException {
        String sql = "UPDATE AppointmentRequest " +
                "SET clientId=?, doctorId=?, confirmedDate=?, status=?, type=?, creationDate=? " +
                "WHERE id=?";

        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, appointment.getClientId());
            pstmt.setLong(2, appointment.getDoctorId());
            pstmt.setTimestamp(3, appointment.getConfirmedDate() != null
                    ? Timestamp.valueOf(appointment.getConfirmedDate())
                    : null);
            pstmt.setString(4, appointment.getStatus());
            pstmt.setString(5, appointment.getType());
            pstmt.setTimestamp(6, Timestamp.valueOf(appointment.getCreationDate()));
            pstmt.setLong(7, appointment.getId());
            pstmt.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM AppointmentRequest WHERE id = ?";
        Connection conn = getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    // ─── PROPOSED DATES ─────────────────────────────────────────

    public void saveProposedDates(long appointmentId, List<ProposedDate> dates) throws SQLException {
        if (dates == null || dates.isEmpty())
            return;

        String sql = "INSERT INTO ProposedDate (appointmentRequestId, proposedDateTime) VALUES (?, ?)";
        Connection conn = getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (ProposedDate pd : dates) {
                pstmt.setLong(1, appointmentId);
                pstmt.setTimestamp(2, Timestamp.valueOf(pd.getProposedDateTime()));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public List<ProposedDate> findProposedDatesByAppointmentId(long appointmentId) throws SQLException {
        List<ProposedDate> dates = new ArrayList<>();
        String sql = "SELECT * FROM ProposedDate WHERE appointmentRequestId = ?";
        Connection conn = getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, appointmentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ProposedDate pd = new ProposedDate();
                    pd.setId(rs.getLong("id"));
                    pd.setProposedDateTime(rs.getTimestamp("proposedDateTime").toLocalDateTime());
                    dates.add(pd);
                }
            }
        }
        return dates;
    }

    public void deleteProposedDates(long appointmentId) throws SQLException {
        String sql = "DELETE FROM ProposedDate WHERE appointmentRequestId = ?";
        Connection conn = getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, appointmentId);
            pstmt.executeUpdate();
        }
    }

    // ─── MAPPER ─────────────────────────────────────────────────

    private AppointmentRequest mapRow(ResultSet rs) throws SQLException {
        AppointmentRequest a = new AppointmentRequest();
        a.setId(rs.getLong("id"));
        a.setClientId(rs.getLong("clientId"));
        a.setDoctorId(rs.getLong("doctorId"));
        a.setStatus(rs.getString("status"));
        a.setType(rs.getString("type"));
        a.setCreationDate(rs.getTimestamp("creationDate").toLocalDateTime());
        a.setConfirmedDate(rs.getTimestamp("confirmedDate") != null
                ? rs.getTimestamp("confirmedDate").toLocalDateTime()
                : null);
        return a;
    }
}