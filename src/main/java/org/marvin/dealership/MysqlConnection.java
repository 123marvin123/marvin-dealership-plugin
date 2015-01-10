package org.marvin.dealership;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.*;

/**
 * Created by Marvin on 26.05.2014.
 */
public class MysqlConnection {
    private boolean initialized;
    private Connection connection;
    public boolean initConnection() {
        if(!initialized) {
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
                File fl = new File(DealershipPlugin.getInstance().getDataDir(), "mysql.txt");
                if (fl.exists()) {
                    BufferedReader reader = new BufferedReader(new FileReader(fl));
                    String line;
                    while (reader.ready()) {
                        line = reader.readLine();
                        String[] parts = line.split("[,]");
                        if (line.length() > 4) {
                            if (parts.length == 4)
                                connection = DriverManager.getConnection("jdbc:mysql://" + parts[0] + "/" + parts[1], parts[2], parts[3]);
                            else if (parts.length == 3)
                                connection = DriverManager.getConnection("jdbc:mysql://" + parts[0] + "/" + parts[1], parts[2], null);
                            initialized = true;
                            break;
                        }
                    }
                    reader.close();
                } else {
                    fl.createNewFile();
                    DealershipPlugin.getInstance().getLoggerInstance().info("[Fehler] Die Mysql Datei, wurde so eben erst erstellt!");
                    DealershipPlugin.getInstance().getShoebill().getSampObjectManager().getServer().sendRconCommand("exit");
                    return false;
                }
            } catch (Exception ex) {
                DealershipPlugin.getInstance().getLoggerInstance().info("[Fehler] Verbindung zum MysqlServer konnte nicht hergestellt werden!");
                DealershipPlugin.getInstance().getShoebill().getSampObjectManager().getServer().sendRconCommand("exit");
                return false;
            }
        }
        return true;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void makeDatabase() {
        try {
            Statement stmnt = connection.createStatement();
            if (connection != null && connection.isValid(1000)) {
                stmnt.executeUpdate("CREATE TABLE IF NOT EXISTS playervehicles (Id INTEGER PRIMARY KEY AUTO_INCREMENT, owner CHAR(24), modelid INTEGER, c1 INTEGER, c2 INTEGER," +
                        "spawnX FLOAT, spawnY FLOAT, spawnZ FLOAT, spawnA FLOAT, bought BIGINT, sellerName CHAR(24), price INTEGER, components TEXT(50))");
                stmnt.executeUpdate("CREATE TABLE IF NOT EXISTS vehicleproviders (Id INTEGER PRIMARY KEY AUTO_INCREMENT, owner CHAR(64), pickupLocationX FLOAT, " +
                        "pickupLocationY FLOAT, pickupLocationZ FLOAT, kasse INTEGER, name CHAR(25), testDrives BOOLEAN, testDriveX FLOAT, " +
                        "testDriveY FLOAT, testDriveZ FLOAT, testDriveA FLOAT, testDriveTime INTEGER)");
                stmnt.executeUpdate("CREATE TABLE IF NOT EXISTS messagelog (Id INTEGER PRIMARY KEY AUTO_INCREMENT, providerId INTEGER, buyer CHAR(24), price INTEGER, modelid INTEGER, boughtDate BIGINT)");
                stmnt.executeUpdate("CREATE TABLE IF NOT EXISTS vehicleoffers (Id INTEGER PRIMARY KEY AUTO_INCREMENT, providerId INTEGER, modelid INTEGER, spawnX FLOAT, spawnY FLOAT" +
                        ", spawnZ FLOAT, spawnA FLOAT, price INTEGER)");
                stmnt.executeUpdate("CREATE TABLE IF NOT EXISTS parkingspots (Id INTEGER PRIMARY KEY AUTO_INCREMENT, providerId INTEGER, spawnX FLOAT, spawnY FLOAT, spawnZ FLOAT, spawnA FLOAT)");
                stmnt.executeUpdate("CREATE TABLE IF NOT EXISTS licenses (Id INTEGER PRIMARY KEY AUTO_INCREMENT, providerId INTEGER, modelid INTEGER, price INTEGER, validDays INTEGER, boughtDate BIGINT)");
            } else {
                DealershipPlugin.getInstance().getLoggerInstance().info("Mysql Datenbank konnte nicht erstellt werden.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int executeUpdate(String query) {
        try {
            if (connection != null && connection.isValid(1000)) {
                Statement stmnt = connection.createStatement();
                stmnt.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
                try {
                    ResultSet rs = stmnt.getGeneratedKeys();
                    rs.next();
                    return rs.getInt(1);
                } catch (Exception ignored) { }
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public ResultSet executeQuery(String query) {
        ResultSet rs = null;
        Statement statement;
        try {
            if (connection != null && connection.isValid(1000)) {
                statement = connection.createStatement();
                rs = statement.executeQuery(query);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rs;
    }
}
