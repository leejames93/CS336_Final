import org.sql2o.*;

public class DB {
    static {
        try {
            Class.forName ("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Sql2o sql2o = new Sql2o("jdbc:mysql://election.cmgchntt9krw.us-east-1.rds.amazonaws.com:3306/election", "admin", "driftwood");
}