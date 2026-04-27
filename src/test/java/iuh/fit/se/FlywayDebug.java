package iuh.fit.se;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class FlywayDebug {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require";
        String user = "postgres.nbazkclfeegsbsuivygo";
        String pass = "MAPproqn123@";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {
             
            System.out.println("Dropping flyway_schema_history and schemas...");
            stmt.execute("DROP TABLE IF EXISTS flyway_schema_history CASCADE;");
            stmt.execute("DROP SCHEMA IF EXISTS identity CASCADE;");
            stmt.execute("DROP SCHEMA IF EXISTS menu CASCADE;");
            stmt.execute("DROP SCHEMA IF EXISTS table_mgmt CASCADE;");
            stmt.execute("DROP SCHEMA IF EXISTS ordering CASCADE;");
            stmt.execute("DROP SCHEMA IF EXISTS shared CASCADE;");
            stmt.execute("DROP SCHEMA IF EXISTS kitchen CASCADE;");
            stmt.execute("DROP SCHEMA IF EXISTS payment CASCADE;");
            stmt.execute("DROP SCHEMA IF EXISTS analytics CASCADE;");
            System.out.println("Cleanup successful.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
