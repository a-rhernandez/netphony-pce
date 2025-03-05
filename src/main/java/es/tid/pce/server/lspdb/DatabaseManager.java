package es.tid.pce.server.lspdb;

import org.slf4j.Logger;

public class DatabaseManager {

    private static final String URL = "jdbc:postgresql://tu-servidor:26257/tu-base-de-datos";
    private static final String USER = "tu-usuario";
    private static final String PASSWORD = "tu-contraseña";
    private Logger log;
}


//     public static Connection getConnection() throws SQLException {
//         return DriverManager.getConnection(URL, USER, PASSWORD);
        
//     }



//     public void insertLSP(String lspUuid, String srp, String lsp, String path, String associationList) {
//     String sql = "INSERT INTO public.lsp (lsp_uuid, srp, lsp, path, association_List) VALUES (?, ?, ?, ?, ?)";

//     try (Connection conn = DatabaseManager.getConnection();
//          PreparedStatement stmt = conn.prepareStatement(sql)) {

//         stmt.setObject(1, java.util.UUID.fromString(lspUuid));  // UUID en formato correcto
//         stmt.setString(2, srp);
//         stmt.setString(3, lsp);
//         stmt.setString(4, path);
//         stmt.setString(5, associationList);

//         stmt.executeUpdate();
//         System.out.println("✅ LSP insertado correctamente");

//     } catch (SQLException e) {
//         e.printStackTrace();
//         System.err.println("❌ Error insertando el LSP");
//     }
// // }
// // synchronized public void processReport(PCEPReport pcepReport) {
    

// //     log.info("Adding PCEPReport to database, rpts: " + pcepReport.getStateReportList().size());

// //     for (int i = 0; i < pcepReport.getStateReportList().size(); i++) {
// //         StateReport stateReport = pcepReport.getStateReportList().get(i);
// //         LSP lsp = stateReport.getLsp();

// //         if (lsp.getLspId() == 0) {
// //             log.info("Sync LSP received, ignoring..");
// //             return;
// //         }

// //         // Extraer los valores necesarios
// //         String lspUuid = java.util.UUID.randomUUID().toString(); // Generar un UUID para la BD
// //         String srp = stateReport.getSrpObject() != null ? stateReport.getSrpObject().toString() : null;
// //         String lspString = lsp.toString();  // Aquí puedes extraer lo que realmente necesites
// //         String path = stateReport.getPath() != null ? stateReport.getPath().toString() : null;
// //         String associationList = lsp.getAssociation() != null ? lsp.getAssociation().toString() : null;

// //         // Insertar en la base de datos
// //         insertLSP(lspUuid, srp, lspString, path, associationList);

// //         log.info("✅ LSP insertado en la base de datos con UUID: " + lspUuid);
// //     }
// // }


// }
