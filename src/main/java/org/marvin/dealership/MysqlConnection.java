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
                            connection = DriverManager.getConnection("jdbc:mysql://" + parts[0] + "/" + parts[1], parts[2], parts[3]);
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
                        "spawnX FLOAT, spawnY FLOAT, spawnZ FLOAT, spawnA FLOAT, bought BIGINT, sellerName CHAR(24), price INTEGER)");
                stmnt.executeUpdate("CREATE TABLE IF NOT EXISTS vehicleproviders (Id INTEGER PRIMARY KEY AUTO_INCREMENT, owner CHAR(24), pickupLocationX FLOAT, " +
                        "pickupLocationY FLOAT, pickupLocationZ FLOAT, cameraLocationX FLOAT, cameraLocationY FLOAT, cameraLocationZ FLOAT, cameraLookAtX FLOAT, " +
                        "cameraLookAtY FLOAT, cameraLookAtZ FLOAT, offers TEXT(500), messageLog Text(700), kasse INTEGER, parkingList TEXT(500))");
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
                return stmnt.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
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
