package service;

import models.Doctor;
import util.DBconnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DoctorService {

    public Doctor getDoctorById(long id) {
        Doctor doctor = new Doctor();
        doctor.setId(id);

        String sql = """
                SELECT
                    u.fullname,
                    u.email,
                    u.phone,
                    d.speciality,
                    d.addressCabine
                FROM user u
                JOIN doctor d ON u.id = d.userId
                WHERE u.id = ?
                """;

        try (Connection conn = DBconnection.getInstance().getConn();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    doctor.setFullname(rs.getString("fullname"));
                    doctor.setEmail(rs.getString("email"));
                    doctor.setPhone(rs.getString("phone"));
                    doctor.setSpeciality(rs.getString("speciality"));
                    doctor.setAddressCabine(rs.getString("addressCabine"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return doctor;
    }
}