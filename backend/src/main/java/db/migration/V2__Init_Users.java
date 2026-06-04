package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class V2__Init_Users extends BaseJavaMigration {

    private static final String INSERT_USER =
            "INSERT IGNORE INTO `app_users` (name, username, encoded_password, email, is_banned, is_deleted) " +
            "VALUES (?, ?, ?, ?, FALSE, FALSE)";

    private static final String INSERT_ROLE =
            "INSERT IGNORE INTO app_users_roles (app_users_id, roles) " +
            "SELECT id, ? FROM `app_users` WHERE username = ?";

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();

        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM `app_users`");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (rs.getLong(1) > 0) return;
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        Object[][] users = {
            {"Usuario",       "user",    "pegog27508@fengnu.com", encoder.encode("pass"),        "USER"},
            {"Administrador", "admin",   "laxari3928@1200b.com",  encoder.encode("adminpass"),   "ADMIN"},
            {"Gerente",       "manager", "manager@gmail.com",     encoder.encode("managerpass"), "MANAGER"},
            {"Conductor",     "driver",  "driver@gmail.com",      encoder.encode("driverpass"),  "DRIVER"},
        };

        for (Object[] u : users) {
            try (PreparedStatement ps = conn.prepareStatement(INSERT_USER)) {
                ps.setString(1, (String) u[0]);
                ps.setString(2, (String) u[1]);
                ps.setString(3, (String) u[3]);
                ps.setString(4, (String) u[2]);
                ps.execute();
            }
            try (PreparedStatement ps = conn.prepareStatement(INSERT_ROLE)) {
                ps.setString(1, (String) u[4]);
                ps.setString(2, (String) u[1]);
                ps.execute();
            }
        }
    }
}
