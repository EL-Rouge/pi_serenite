package repository;

import models.Client;
import util.DBconnection;

import java.sql.*;

public class ClientRepository {

    private Connection getConnection() throws SQLException {
        Connection conn = DBconnection.getInstance().getConn();
        if (conn == null) throw new SQLException("Database connection is null.");
        return conn;
    }

    public Client findById(long id) throws SQLException {
        // Adjust table/column names to match your actual DB schema
        String sql = "SELECT * FROM User WHERE id = ? AND role = 'CLIENT'";
        Connection conn = getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    private Client mapRow(ResultSet rs) throws SQLException {
        Client c = new Client();
        c.setId(rs.getLong("id"));
        c.setFullname(rs.getString("fullName"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        // set preferences if column exists
        try { c.setPreferences(rs.getString("preferences")); } catch (SQLException ignored) {}
        return c;
    }
}