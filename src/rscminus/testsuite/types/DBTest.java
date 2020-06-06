/**
 * rscminus
 *
 * This file is part of rscminus.
 *
 * rscminus is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * rscminus is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with rscminus. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * Authors: see <https://github.com/OrN/rscminus>
 */

package rscminus.testsuite.types;

import rscminus.testsuite.TestManager;

import java.sql.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public abstract class DBTest implements UnitTest {
    private static String connectionURL = "jdbc:mysql://localhost/?user={0}&password={1}";
    private List<Connection> sqlConnections = new ArrayList<>();
    private List<Statement> sqlStatements = new ArrayList<>();

    public boolean init() throws Exception{
        if (TestManager.sqlUsername == null ||
            TestManager.sqlPassword == null) {
            System.out.println("This test requires -sqlu, -sqlp arguments");
            return false;
        }
        return true;
    }

    public void cleanup() {
        for (Statement sqlStatement : sqlStatements)
            try {sqlStatement.close();} catch (Exception ignored) {}
        for (Connection sqlConnection : sqlConnections)
            try {sqlConnection.close();} catch (Exception ignored) {}
    }

    public Statement newStatement(Connection sqlConnection) throws SQLException {
        if (sqlConnection == null) {
            sqlConnection = DriverManager.getConnection(MessageFormat.format(connectionURL, TestManager.sqlUsername, TestManager.sqlPassword));
            sqlConnections.add(sqlConnection);
        }
        Statement sqlStatement = sqlConnection.createStatement();
        sqlStatements.add(sqlStatement);
        return sqlStatement;
    }
}
