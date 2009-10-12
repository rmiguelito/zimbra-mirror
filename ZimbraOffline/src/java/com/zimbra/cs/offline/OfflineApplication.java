/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.offline;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.db.Db;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.db.OfflineVersions;
import com.zimbra.cs.store.file.Volume;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.ZimbraApplication;
import com.zimbra.cs.zimlet.ZimletFile;
import com.zimbra.cs.zimlet.ZimletUtil;

public class OfflineApplication extends ZimbraApplication {
    private static String[] sqlScripts = {
        "db", "directory", "wildfire", "versions-init", "default-volumes"
    };
    private static String ZIMBRA_DB_NAME = "zimbra";
        
    @Override
    public String getId() {
        return OfflineLC.zdesktop_app_id.value();
    }

    @Override
    public String getInstallId() {
        return OfflineLC.zdesktop_installation_key.value();
    }

    @Override
    public boolean supports(String className) {
        return false;
    }

    @Override
    public void initialize(boolean forMailboxd) {
        migrateDb();
        deployZimlets();
        
        try {
            if (!forMailboxd)
                return;

            long threshold = OfflineLC.zdesktop_volume_compression_threshold.longValue();
            Volume vol = Volume.getCurrentMessageVolume();
            
            // in offline, we always use the relative path "store" for message volume
            Volume.update(vol.getId(), vol.getType(), vol.getName(),
                "store", vol.getMboxGroupBits(), vol.getMboxBits(),
                vol.getFileGroupBits(), vol.getFileBits(), threshold != 0,
                threshold == 0 ? vol.getCompressionThreshold() : threshold, true);
            Volume.reloadVolumes();
        } catch (ServiceException e) {
            OfflineLog.offline.warn("Unable to update volume compression", e);
        }
    }

    public void initializeZimbraDb(boolean forMailboxd) throws ServiceException {
        if (!forMailboxd)
            return;

        DbPool.Connection conn = DbPool.getConnection();
        try {
            if (!Db.getInstance().databaseExists(conn, ZIMBRA_DB_NAME)) {
                File file = null;
                PreparedStatement stmt = null;
                
                OfflineLog.offline.info("Creating database " + ZIMBRA_DB_NAME);
                for (String name : sqlScripts) {
                    try {
                        file = new File(LC.mailboxd_directory.value() + "/../db/" + name + ".sql");
                        
                        String script;
                        String template = new String(ByteUtil.getContent(file));
                        Map<String, String> vars = new HashMap<String, String>();
                        
                        vars.put("ZIMBRA_HOME", LC.zimbra_home.value() + '/');
                        vars.put("ZIMBRA_INSTALL", LC.zimbra_home.value() + '/');
                        script = StringUtil.fillTemplate(template, vars, StringUtil.atPattern);
                        DbUtil.executeScript(conn, new StringReader(script));
                    } catch (IOException e) {
                        throw ServiceException.FAILURE("unable to read SQL statements from " +
                            file.getPath(), e);
                    } catch (SQLException e) {
                        throw ServiceException.FAILURE("unable to run " +
                            ZIMBRA_DB_NAME + " db script" + file.getPath(), e);
                    } finally {
                        DbPool.closeStatement(stmt);
                    }
                }
                try {
                    stmt = conn.prepareStatement("INSERT INTO config(name, value, description) VALUES ('offline.db.version', '" +
                        OfflineVersions.OFFLINE_DB_VERSION + "', 'offline db schema version')");
                    stmt.executeUpdate();
                    conn.commit();
                } catch (SQLException e) {
                    throw ServiceException.FAILURE("unable to set offline db version", e);
                } finally {
                    DbPool.closeStatement(stmt);
                }
            }
        } finally {
            conn.close();
        }
    }
    
    @Override
    public void shutdown() {
        super.shutdown();
        try {
            DbPool.shutdown();
        } catch (Exception x) {
            OfflineLog.offline.warn("Exception during shutdown", x);
        }
    }
      
    private void migrateDb() {
        try {
            OfflineLog.offline.debug("DB migration check started...");
            new com.zimbra.cs.db.DbOfflineMigration().run();
            OfflineLog.offline.debug("DB migration done");
        } catch (SQLException e) {
            OfflineLog.offline.error("DB migration sql error: " + e.getMessage());
        } catch (Exception e) {
            OfflineLog.offline.error("DB migration error: " + e.getMessage());
        }
    }
    
    private void deployZimlets() {
    	OfflineLog.offline.debug("Deploying new zimlets...");
    	
    	File zimletDir = new File(LC.zimbra_home.value() + "/zimlets");
	if (zimletDir == null || !zimletDir.exists() || !zimletDir.isDirectory()) {
	    OfflineLog.offline.debug("Invalid zimlets directory: " + zimletDir.getPath());
	    return;
	}
	
	boolean hasBackup = true;
	File zimletBackupDir = new File(zimletDir.getPath() + "/backup");
	if (!zimletBackupDir.exists())
	    hasBackup = zimletBackupDir.mkdir();
	
    	String[] zimlets = zimletDir.list();
    	if (zimlets == null) {
	    OfflineLog.offline.debug("No zimlets found at " + zimletDir.getPath());
	    return;
    	}
    	for (int i = 0; i < zimlets.length; i++) {
	    try {
		File zimletFile = new File(zimletDir.getPath() + "/" + zimlets[i]);
		if (zimletFile.isDirectory())
		    continue;
		ZimletUtil.deployZimlet(new ZimletFile(zimletDir, zimlets[i]));
		OfflineLog.offline.debug("Zimlet deployed:  " + zimlets[i]);
		if (hasBackup)
		    zimletFile.renameTo(new File(zimletBackupDir, zimlets[i]));
	    } catch (Exception e) {
		OfflineLog.offline.warn("Fail to deploy zimlet " + zimlets[i] +
		    ": " + e.getMessage());
	    }
    	}
    	OfflineLog.offline.debug("Zimlets deployment done.");
    }
}
