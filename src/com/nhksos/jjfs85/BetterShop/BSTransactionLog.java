/**
 * Programmer: Jacob Scott
 * Program Name: BSLog
 * Description: log of transactions
 * Date: Mar 11, 2011
 */
package com.nhksos.jjfs85.BetterShop;

import com.jascotty2.Shop.TotalTransaction;
import com.jascotty2.Shop.TransactionLog;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class BSTransactionLog extends TransactionLog {

    public BSTransactionLog() {
        //load();
    } // end default constructor

    public final boolean load() {
        transactions.clear();
        totalTransactions.clear();
        logUserTransactions = BetterShop.config.logUserTransactions;
        logTotalTransactions = BetterShop.config.logTotalTransactions;
        transLogTablename = BetterShop.config.transLogTablename;
        recordTablename = BetterShop.config.recordTablename;
        userTansactionLifespan = BetterShop.config.userTansactionLifespan;

        if (BetterShop.config.useMySQL()) {
            // use same connection pricelist is using (pricelist MUST be initialized.. does not check)
            MySQLconnection = BetterShop.pricelist.getMySQLconnection();
            if (MySQLconnection == null) {
                return isLoaded = logTotalTransactions = logUserTransactions = false;
            } else {
                try {
                    if (logUserTransactions) {
                        if (!MySQLconnection.tableExists(transLogTablename)) {
                            logUserTransactions = createTransactionLogTable();
                        } else {
                            tableCheck();
                            try {
                                truncateRecords();
                            } catch (Exception ex) {
                                BetterShop.Log(Level.SEVERE, ex);
                            }
                        }
                    }
                    if (logTotalTransactions) {
                        if (!MySQLconnection.tableExists(recordTablename)) {
                            logTotalTransactions = createTransactionRecordTable();
                        } else {
                            //load into memory
                            //for(Result)
                            ResultSet tb = MySQLconnection.GetTable(recordTablename);
                            for (tb.beforeFirst(); tb.next();) {
                                totalTransactions.add(new TotalTransaction(
                                        tb.getLong("LAST"), tb.getInt("ID"), tb.getInt("SUB"),
                                        tb.getString("NAME"), tb.getLong("SOLD"), tb.getLong("BOUGHT")));
                            }
                        }
                    }
                } catch (SQLException ex) {
                    BetterShop.Log(Level.SEVERE, "Error retrieving table list", ex);
                    return isLoaded = logTotalTransactions = logUserTransactions = false;
                }
            }
        } else {
            MySQLconnection = null;
            flatFile = new File(BSConfig.pluginFolder.getAbsolutePath() + File.separatorChar + BetterShop.config.transLogTablename + ".csv");
            totalsFlatFile = new File(BSConfig.pluginFolder.getAbsolutePath() + File.separatorChar + BetterShop.config.recordTablename + ".csv");
        }
        try {
            updateCache();
        } catch (Exception ex) {
            BetterShop.Log(Level.SEVERE, ex);
        }

        return isLoaded = true;
    }

    public boolean isOpened() {
        return isLoaded && (BetterShop.config.useMySQL()
                ? MySQLconnection != null && MySQLconnection.IsConnected() : flatFile != null);
    }

    public String databaseName() {
        return BetterShop.config.useMySQL()
                ? (MySQLconnection != null ? MySQLconnection.GetDatabaseName() : "null")
                : (flatFile != null ? flatFile.getName() : "null");
    }

    public void tableCheck() {
        if (BetterShop.config.useMySQL()
                && MySQLconnection != null && MySQLconnection.IsConnected()) {
            try {
                //Version 1.6.1.1+  ALTER TABLE BetterShopMarketActivity ADD COLUMN PRICE DECIMAL(11,2);

                if (logUserTransactions
                        && !MySQLconnection.columnExists(transLogTablename, "PRICE")) {
                    MySQLconnection.RunUpdate("ALTER TABLE " + transLogTablename + " ADD COLUMN PRICE DECIMAL(11,2);");
                    BetterShop.Log(transLogTablename + " updated");
                }
            } catch (SQLException ex) {
                BetterShop.Log(Level.SEVERE, "Error while upgrading MySQL Table", ex);
            }
        }
    }
} // end class BSLog

