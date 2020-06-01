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

package testsuite.types;

import testsuite.manager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.MessageFormat;

public abstract class dbTest implements unitTest{
    protected static String connectionURL = "jdbc:mysql://localhost/?user={0}&password={1}";
    protected Connection sqlConnection = null;
    protected Statement sqlStatement = null;
    protected ResultSet sqlResultSet = null;
    public boolean init() throws Exception{
        if (manager.sqlUsername == null ||
            manager.sqlPassword == null) {
            System.out.println("This test requires -sqlu, -sqlp arguments");
            return false;
        }
        sqlConnection = DriverManager.getConnection(
                MessageFormat.format(connectionURL, manager.sqlUsername, manager.sqlPassword));

        if (sqlConnection == null || sqlConnection.isClosed()) {
            System.out.println("Could not connect to database.");
            return false;
        }

        return true;
    }
    public void cleanup() {
        try {sqlResultSet.close();} catch (Exception ignored) {}
        try {sqlStatement.close();} catch (Exception ignored) {}
        try {sqlConnection.close();} catch (Exception ignored) {}
    }
}
