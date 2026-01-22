import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Jdbc_Postgres {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/cabdb";   
        String user = "postgres";                                 
        String password = "p0stgray11";                             

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            // Load the PostgreSQL JDBC driver (optional for newer versions)
            Class.forName("org.postgresql.Driver");                     
            
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to PostgreSQL database!");

           
         stmt = conn.createStatement();
         rs = stmt.executeQuery( "SELECT * FROM employee;" );
         while ( rs.next() ) {
            int id = rs.getInt("emp_id");
            String  name = rs.getString("emp_name");
            //int age  = rs.getInt("age");
            //String  address = rs.getString("address");
            float salary = rs.getFloat("salary");
            System.out.println( "ID = " + id );
            System.out.println( "NAME = " + name );
            //System.out.println( "AGE = " + age );
            //System.out.println( "ADDRESS = " + address );
            System.out.println( "SALARY = " + salary );
            System.out.println();
         }
         rs.close();
         stmt.close();
         conn.close();
      } catch ( Exception e ) {
         System.err.println( e.getClass().getName()+": "+ e.getMessage() );
         System.exit(0);
      }
      System.out.println("Operation done successfully");
   }
}