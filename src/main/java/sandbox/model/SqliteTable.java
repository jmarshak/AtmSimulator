package sandbox.model;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * methods common to all sql table classes.
 */
public interface SqliteTable {

    String getInsertSqlStatement();
    void setInsertValues(PreparedStatement stmt) throws SQLException;


    String getSelectSqlStatement();
    void setSelectValues(PreparedStatement stmt) throws SQLException;
}
