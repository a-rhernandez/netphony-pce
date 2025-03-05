package es.tid.pce.server.lspdb;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;

import es.tid.pce.pcep.PCEPProtocolViolationException;
import es.tid.pce.pcep.constructs.StateReport;


public class ReportDB_Cockroach extends ReportDB_Simple {

    public static final String DEF_MODULE = "DEFAULT";
    private Connection connection;
    private boolean dbActive = true;

    public ReportDB_Cockroach() {
        super(ReportDB_Cockroach.DEF_MODULE);
        connectToDB("jdbc:postgresql://192.168.159.227:26257/defaultdb", "tfs", "tfs123");
    }

    public ReportDB_Cockroach(String moduleId, String dbHost) {
        super(moduleId);
        connectToDB("jdbc:postgresql://192.168.159.227:26257/defaultdb", "tfs", "tfs123");
    }

    public ReportDB_Cockroach(String moduleId, String url, String user, String password) {
        super(moduleId);
        connectToDB(url, user, password);
    }

    private void connectToDB(String url, String user, String password) {
        try {
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("CockroachDB: Connection established. CockroachDb");
        } catch (SQLException e) {
            e.printStackTrace();
            dbActive = false;
        }
    }

    public void fillFromDB() {
        if (!dbActive) {
            System.out.println("CockroachDB: Couldn't establish connection...");
            return;
        }
        System.out.println("CockroachDB: Filling from DB");
        String query = "SELECT id, data FROM StateReports WHERE module = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, moduleId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String jsonData = rs.getString("data");
                Gson gson = new Gson();
                byte[] rptBytes = gson.fromJson(jsonData, byte[].class);
                try {
                    StateReport rpt = new StateReport(rptBytes, 0);
                    StateReportList.put(id, rpt);
                    System.out.println("CockroachDB: Loaded StateReport with Id " + id);
                } catch (PCEPProtocolViolationException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void add(StateReport rpt) {
        super.add(rpt);
        if (dbActive) {
            int key = getKey(rpt);
            try {
                rpt.encode();
                Gson gson = new Gson();
                String jsonData = gson.toJson(rpt.getBytes());
                String query = "INSERT INTO StateReports (id, module, data) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET data = EXCLUDED.data";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setInt(1, key);
                    stmt.setString(2, moduleId);
                    stmt.setString(3, jsonData);
                    stmt.executeUpdate();
                    System.out.println("CockroachDB: Added/Updated StateReport with key " + key);
                }
            } catch (PCEPProtocolViolationException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public StateReport remove(int rptId) {
        if (dbActive) {
            String query = "DELETE FROM StateReports WHERE id = ? AND module = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, rptId);
                stmt.setString(2, moduleId);
                stmt.executeUpdate();
                System.out.println("CockroachDB: Removed StateReport with id " + rptId);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return super.remove(rptId);
    }

    public void clearStateReports() {
        super.clearReports();
        if (dbActive) {
            String query = "DELETE FROM StateReports WHERE module = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, moduleId);
                stmt.executeUpdate();
                System.out.println("CockroachDB: Cleared all StateReports for module " + moduleId);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void update(StateReport rpt) {
        add(rpt);
    }

    public String getStateReportListKey() {
        return moduleId + "_StateReport";
    }

    public boolean isDbActive() {
        return dbActive;
    }

    public void setDbActive(boolean dbActive) {
        this.dbActive = dbActive;
    }
}