package tn.esprit.scripts;
import tn.esprit.utils.MyDB;
import java.sql.Connection;
import java.sql.ResultSet;

public class DbChecker {
    public static void main(String[] args) {
        try {
            Connection cnx = MyDB.getInstance().getCnx();
            ResultSet rs = cnx.getMetaData().getTables(null, null, "%", new String[] {"TABLE"});
            while (rs.next()) {
                System.out.println("TABLE: " + rs.getString("TABLE_NAME"));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
