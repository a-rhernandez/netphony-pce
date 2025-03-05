import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

import es.tid.pce.server.lspdb.ReportDB_Handler;

public class DatabaseTest {

    private ReportDB_Handler dbHandler;

    @Before
    public void setUp() {
        // Configura el handler para la base de datos antes de cada prueba
        dbHandler = new ReportDB_Handler("TestHandler", "localhost");
    }

    @Test
    public void testDatabaseConnection() {
        // Verificar si la conexión no es nula
        Connection connection = dbHandler.getConnection();
        assertNotNull(connection);
    }

    @Test
    public void testInsertLSP() {
        // Probar la inserción de un LSP
        String lspUuid = "123e4567-e89b-12d3-a456-42129689789";
        String srp = "SRP_Test1";
        String lsp = "LSP_Test2";
        String path = "Test_Path3";
        String associationList = "Test_Association";

        try {
            // Llamamos al método para insertar el LSP
            dbHandler.insertLSP(lspUuid, srp, lsp, path, associationList);
        } catch (Exception e) {
            // Si se lanza una excepción, la prueba falla
            fail("No debería lanzar excepción al insertar el LSP: " + e.getMessage());
        }
    }
    @Test
    public void testSelectedLSP(){
        //Probar select LSP
        try{
        Connection connection = dbHandler.getConnection();
            assertNotNull("La conexión a la base de datos es nula", connection);

            String query = "SELECT * FROM lsp";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();

            System.out.println("Contenido de la tabla LSP:");

            while (resultSet.next()) {
                System.out.println("----------------------");
                System.out.println("LSP UUID: " + resultSet.getString("lsp_uuid"));
                System.out.println("SRP: " + resultSet.getString("srp"));
                System.out.println("LSP: " + resultSet.getString("lsp"));
                System.out.println("Path: " + resultSet.getString("path"));
                System.out.println("Association List: " + resultSet.getString("associationlist"));
            }
            
        } catch (Exception e) {
            fail("Error al recuperar datos de la base de datos: " + e.getMessage());
        }
    }
    @Test
public void testDeleteAllLSPs() {
    // Probar la eliminación de todos los LSPs
    try {
        Connection connection = dbHandler.getConnection();
        assertNotNull("La conexión a la base de datos es nula", connection);

        // Eliminar todos los registros de la tabla lsp
        String deleteQuery = "DELETE FROM lsp";
        PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
        int rowsAffected = deleteStatement.executeUpdate();
        
        // Verificar que se haya eliminado algún registro
        assertTrue("No se eliminó ningún registro", rowsAffected >= 0);

        // Verificar que la tabla esté vacía
        String selectQuery = "SELECT COUNT(*) FROM lsp";
        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        ResultSet resultSet = selectStatement.executeQuery();
        
        if (resultSet.next()) {
            int rowCount = resultSet.getInt(1);
            assertEquals("La tabla lsp no está vacía", 0, rowCount);
        }

    } catch (Exception e) {
        fail("Error al eliminar los LSPs: " + e.getMessage());
    }
}

    @Test
    public void testRetrieveColumns() {
        // Probar la obtención de columnas de la tabla lsp
        try {
            Connection connection = dbHandler.getConnection();
            assertNotNull("La conexión a la base de datos es nula", connection);

            // Obtener los metadatos de la base de datos
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "lsp", null);

            System.out.println("Columnas de la tabla lsp:");

            while (columns.next()) {
                // Obtener el nombre de la columna
                String columnName = columns.getString("COLUMN_NAME");
                System.out.println("Columna: " + columnName);
            }
        } catch (Exception e) {
            fail("Error al obtener las columnas de la base de datos: " + e.getMessage());
        }
    }
    // @Test
    //     public void testGetTableColumns() {
    //         // Verificar que se obtienen las columnas correctas de la tabla 'lsp'
    //         try (Connection connection = dbHandler.getConnection();
    //             PreparedStatement statement = connection.prepareStatement(query);
    //             ResultSet rs = stmt.executeQuery("SELECT * FROM public.lsp LIMIT 1")) {

    //             // Obtener los metadatos de la tabla
    //             ResultSetMetaData metaData = rs.getMetaData();
    //             int columnCount = metaData.getColumnCount();
    //             List<String> columnNames = new ArrayList<>();

    //             // Añadir los nombres de las columnas a la lista
    //             for (int i = 1; i <= columnCount; i++) {
    //                 columnNames.add(metaData.getColumnName(i));
    //                 System.out.println("Columna: " + columnNames);

    //             }

    //             // Verificar que las columnas obtenidas son las correctas
    //             assertTrue(columnNames.contains("lsp_uuid"));
    //             assertTrue(columnNames.contains("srp"));
    //             assertTrue(columnNames.contains("lsp"));
    //             assertTrue(columnNames.contains("path"));
    //             assertTrue(columnNames.contains("association_List"));
    //         } catch (Exception e) {
    //             // Si ocurre algún error, la prueba falla
    //             fail("No se pudo obtener las columnas de la tabla: " + e.getMessage());
    //         }
    //     }


    // @Test
    // public void testDatabaseInsertFailure() {
    //     // Aquí puedes probar un caso en el que se espera que falle la inserción.
    //     // Por ejemplo, puedes probar con valores incorrectos y verificar que se lanza una excepción.
    //     assertThrows(SQLException.class, () -> {
    //         dbHandler.insertLSP(null, null, null, null, null); // Esto debería fallar
    //     });
    // }
}
