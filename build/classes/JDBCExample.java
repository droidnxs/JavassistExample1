
import java.sql.*;

public class JDBCExample {

    // JDBC driver name and database URL

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/employees";

    //  Database credentials
    static final String USER = "root";
    static final String PASS = "mysql";

    public static void main(String[] args) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            //STEP 2: Register JDBC driver
            Class.forName("com.mysql.jdbc.Driver");

            //STEP 3: Open a connection
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            //STEP 4: Execute a query
            System.out.println("Creating statement...");
            String sql = "SELECT * FROM employees;";
            stmt = conn.prepareStatement(sql);
			ResultSet rs = null;
			while(true)
			{
				System.out.println("Executing query");
				rs = stmt.executeQuery(sql);
				Thread.sleep(1000);
			}

           
            /*while (rs.next()) {
                //Retrieve by column name
                int empno = rs.getInt("emp_no");
                Date birthdate = rs.getDate("birth_date");
                String fname = rs.getString("first_name");
				String lname = rs.getString("last_name");
				String gender = rs.getString("gender");
				Date hiredate = rs.getDate("hire_date");
            }
            //STEP 6: Clean-up environment
            rs.close();
            stmt.close();
            conn.close();*/
			//throw new NullPointerException();
        } catch (SQLException se) {
            //Handle errors for JDBC
            //se.printStackTrace();
        } catch (Exception e) {			
			//donothing
		}
		finally {
            //finally block used to close resources
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException se2) {
            }// nothing we can do
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException se) {
                //se.printStackTrace();
            }//end finally try
        }//end try
        System.out.println("Goodbye!");
    }//end main
}//end JDBCExample
