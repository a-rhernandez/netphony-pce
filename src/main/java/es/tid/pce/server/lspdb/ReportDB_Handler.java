package es.tid.pce.server.lspdb;

import java.net.Inet4Address;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.pcep.constructs.StateReport;
import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.pcep.objects.LSP;
import es.tid.pce.pcep.objects.OPEN;

public class ReportDB_Handler {
    protected Hashtable<String, ReportDB> moduleList;

    private Logger log;

    private Connection connection;

    private String dbHost = "";
    private boolean dbActive = false;

    protected String handlerId = "";

    public ReportDB_Handler() {
        log = LoggerFactory.getLogger("PCEPParser");
        moduleList = new Hashtable<>();
    }

    public ReportDB_Handler(String handlerId, String dbHost) {
        this();
        this.dbHost = dbHost;
        this.handlerId = handlerId;
        connectToDB("jdbc:postgresql://192.168.159.227:26257/tfs_lsp_mgmt", "tfs", "tfs123");
    }

    private void connectToDB(String url, String user, String password) {
        try {
            connection = DriverManager.getConnection(url, user, password);
            this.setDbActive(true);
            log.info("CockroachDB: Connection established. Handler");
        } catch (SQLException e) {
            e.printStackTrace();
            log.info("CockroachDB: Couldn't establish connection...");
        }
    }

    public void fillFromDB(String handlerId, String dbHost) {
        log.info("CockroachDB: Filling from DB host=" + dbHost + " id=" + handlerId);
        if (connection != null) {
            String query = "SELECT DISTINCT module FROM StateReports WHERE module LIKE ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, handlerId + "_%");
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String modId = rs.getString("module");
                    log.info("CockroachDB: Module found: " + modId);
                    ReportDB_Cockroach rptdb = new ReportDB_Cockroach(modId, dbHost);
                    rptdb.fillFromDB();
                    moduleList.put(modId, rptdb);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            log.info("CockroachDB: Couldn't establish connection to fill from DB");
        }
    }

    public void fillFromDB() {
        if (handlerId.length() == 0 || dbHost.length() == 0 || !dbActive || connection == null) {
            log.info("CockroachDB: Couldn't fill from DB, check your configuration");
            return;
        }
        String query = "SELECT DISTINCT module FROM StateReports WHERE module LIKE ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, handlerId + "_%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String modId = rs.getString("module");
                log.info("CockroachDB: Module found: " + modId);
                ReportDB_Cockroach rptdb = new ReportDB_Cockroach(modId, dbHost);
                rptdb.fillFromDB();
                moduleList.put(modId, rptdb);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void fillFromXML() {
        // TODO: por hacer
    }

    public String getModuleList(String handlerId) {
        return handlerId + "_*_StateReport";
    }

    public String getStateReportDBList(Inet4Address ad) {
        return handlerId + "_" + ad.toString();
    }

    public String getModuleList() {
        return handlerId + "_" + "MODULES";
    }

    public ReportDB getStateReportDB(String key) {
        return moduleList.get(key);
    }

    public void setStateReportDB(String key, ReportDB rptdb) {
        moduleList.put(key, rptdb);
    }

    synchronized public void processReport(PCEPReport pcepReport) {
        log.info("Adding PCEPReport to database, rpts:" + pcepReport.getStateReportList().size());

        for (int i = 0; i < pcepReport.getStateReportList().size(); i++) {
            StateReport stateReport = pcepReport.getStateReportList().get(i);
            
            
            LSP lsp = stateReport.getLsp();
            String lsp1 = stateReport.getLsp().toString();
            String srp = (stateReport.getSrp() != null) ? stateReport.getSrp().toString() : "null";
            String path = stateReport.getPath().toString();
            String associationList = stateReport.getAssociationList().toString();


            if (lsp.getLspId() == 0) {
                log.info("sync lsp received, ignoring..");
                return;
            }
            if (lsp.isRemoveFlag()) {
                log.warn(" Removing LSP: " + lsp1);
                deleteLSP("lspId="+String.valueOf(lsp.getLspId()));
                return;
            }

            Inet4Address address = lsp.getLspIdentifiers_tlv().getTunnelSenderIPAddress();
            String dbId = getStateReportDBList(address);
            ReportDB rptdb = moduleList.get(dbId);
            if (rptdb == null) {
                if (dbActive) {
                    log.info("CockroachDB: Created new CockroachDB rptdb: " + dbId);
                    // rptdb = new ReportDB_Redis(dbId, dbHost);
                } else {
                    log.info("Created new simple rptdb: " + dbId);
                    rptdb = new ReportDB_Simple(dbId);
                }
                //moduleList.put(address.toString(), rptdb);
            }
            
            // if (lsp.isRemoveFlag()) 
            //     rptdb.remove(stateReport);
            // } else {
            //     //log.debug(""+stateReport.toString());
            //     rptdb.add(stateReport);
            // }


            insertLSP(UUID.randomUUID().toString(), srp, lsp1, path, associationList);
        }
    }

    public void deleteLSP(String lspId) {
        String sql = "DELETE FROM public.lsp WHERE lsp LIKE ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + lspId + "%");
            stmt.executeUpdate();
            System.out.println("✅ LSP eliminado correctamente");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Error eliminando el LSP");
        }
    }
     public void insertLSP(String lspUuid, String srp, String lsp, String path, String associationList) {
        String sql = "INSERT INTO public.lsp (lsp_uuid, srp, lsp, path, associationlist) VALUES (?, ?, ?, ?, ?)";
    
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
    
            stmt.setObject(1, java.util.UUID.fromString(lspUuid));  // UUID en formato correcto
            stmt.setString(2, srp);
            stmt.setString(3, lsp);
            stmt.setString(4, path);
            stmt.setString(5, associationList);
    
            stmt.executeUpdate();
            System.out.println("✅ LSP insertado correctamente");
    
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Error insertando el LSP");
        }
    }

    synchronized public void processOpen(OPEN open, Inet4Address address) {
        log.info("PCC database sync");
        ReportDB rptdb = new ReportDB_Simple(address.toString());
    }

    public boolean isDbActive() {
        return dbActive;
    }

    public void setDbActive(boolean dbActive) {
        if (dbActive && (handlerId.length() == 0 || dbHost.length() == 0)) {
            log.info("CockroachDB: Can't set the DB to active, there's no DB host or/and handlerId");
            return;
        }
        this.dbActive = dbActive;
    }

    public int getPCCDatabaseVersion(Inet4Address address) {
        ReportDB rptdb = moduleList.get(getStateReportDBList(address));
        if (rptdb != null)
            return rptdb.getVersion();
        else
            return 0;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(moduleList.size() * 100);
        sb.append("Report DB: ");
        Enumeration<ReportDB> db = moduleList.elements();
        while (db.hasMoreElements()) {
            sb.append(db.nextElement().toString());
        }

        return sb.toString();
    }
    public List<String> obtenerModulosDesdeDB(String handlerId) {
        List<String> modulos = new ArrayList<>();
        String query = "SELECT DISTINCT module FROM StateReports WHERE module LIKE ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, handlerId + "_%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String modId = rs.getString("module");
                modulos.add(modId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    
        return modulos;
    }
    public Connection getConnection() {
        return connection;
    }
}
