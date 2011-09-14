package com.zimbra.qa.selenium.framework.ui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.zimbra.common.soap.Element;
import com.zimbra.qa.selenium.framework.ui.PerfMetrics.PerfData;
import com.zimbra.qa.selenium.framework.util.HarnessException;
import com.zimbra.qa.selenium.framework.util.ZimbraAdminAccount;
import com.zimbra.qa.selenium.framework.util.ZimbraSeleniumProperties;


/**


mysql> create table apps ( 
 id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
 name VARCHAR(35) 
 );
mysql> insert into apps (name) VALUES ('AJAX');
mysql> insert into apps (name) VALUES ('HTML');
mysql> insert into apps (name) VALUES ('MOBILE');
mysql> insert into apps (name) VALUES ('ADMIN');
mysql> insert into apps (name) VALUES ('DESKTOP');
mysql> insert into apps (name) VALUES ('OCTOPUS');

mysql> create table actions ( 
 id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
 name VARCHAR(35) 
 );
mysql> insert into actions (name) VALUES ('ZmMailApp');

mysql> create table builds ( 
 id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
 build VARCHAR(35) 
 );

mysql> create table milestones ( 
 id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
 milestone VARCHAR(35) 
 );
mysql> insert into milestones (milestone) VALUES ('GunsNRoses');
mysql> insert into milestones (milestone) VALUES ('Helix');
mysql> insert into milestones (milestone) VALUES ('IronMaiden');
mysql> insert into milestones (milestone) VALUES ('JudasPriest');

mysql> create table perf (
 id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created TIMESTAMP(8),
 name VARCHAR(35),
 appid INT,
 buildid INT,
 milestone VARCHAR(35),
 start BIGINT,
 launched BIGINT,
 loaded BIGINT,
 delta BIGINT,
 delta_internal BIGINT,
 description VARCHAR(255)
 );

 **/


public class PerfDatabase {
	protected static Logger logger = LogManager.getLogger(PerfDatabase.class);

	/**
	 * Insert a PerfData values into the database
	 * @param data
	 * @throws HarnessException
	 */
	public static void record(PerfData data) throws HarnessException {

		// Make sure the action exists
		PerfDatabase.getInstance().getActionKey(data.Key.toString());

		// A mapping of column names to values
		HashMap<String, String> table = new HashMap<String, String>();

		table.put("name", "'" + data.Key.toString() +"'");		// VARCHAR ... enclose in single quotes
		table.put("appid", "" + PerfDatabase.getInstance().getAppType());
		table.put("buildid", "" + PerfDatabase.getInstance().getBuildID());
		table.put("milestone", "'" + PerfDatabase.getInstance().getMilestone() +"'"); 		// VARCHAR ... enclose in single quotes

		table.put("start", "" + data.StartStamp);
		table.put("launched", "" + data.LaunchStamp);
		table.put("loaded", "" + data.FinishStamp);

		table.put("delta", "" + "" + (Long.parseLong(data.FinishStamp) - data.StartStamp));

		String delta_internal = "0";
		if ( data.LaunchStamp != null && !data.LaunchStamp.trim().equals("") ) {
			delta_internal = "" + (Long.parseLong(data.FinishStamp) - Long.parseLong(data.LaunchStamp));
		}
		table.put("delta_internal", delta_internal);

		table.put("description", "'" + data.Message +"'");		// VARCHAR ... enclose in single quotes

		// Insert the map into the database
		PerfDatabase.getInstance().insertPerf(table);

	}

	protected void insertPerf(HashMap<String, String> data) throws HarnessException {

		// Convert the keys, values into comma separated strings
		String columns = Arrays.asList(data.keySet().toArray()).toString().replace("[", "").replace("]", "");
		String values = Arrays.asList(data.values().toArray()).toString().replace("[", "").replace("]", "");

		try {

			String command = String.format("INSERT INTO perf (%s) VALUES (%s)", columns, values);
			logger.info("Statement: "+ command);

			Statement statement = DatabaseConnection.getInstance().createStatement();
			int ret = statement.executeUpdate(command);
			logger.info("Statement: ret="+ ret);


		} catch (SQLException e) {
			throw new HarnessException(e);
		}


	}

	/**
	 * Get the ID corresponding to the app from the perf DB
	 * @return
	 * @throws HarnessException
	 */
	protected int getAppType() throws HarnessException {
		getAppTypeTable();

		String type = ZimbraSeleniumProperties.getAppType().toString().toLowerCase();
		if (!appTable.containsKey(type))
			throw new HarnessException("Unknown app type: "+ type +".  Maybe INSERT INTO apps ('name') VALUES ('"+ type +"') ?");

		return (appTable.get(type));
	}

	protected static HashMap<String, Integer> appTable = null;
	protected void getAppTypeTable() throws HarnessException {
		if ( appTable == null ) {
			appTable = new HashMap<String, Integer>();

			try {

				String query = "SELECT id, name FROM apps";

				Statement statement = DatabaseConnection.getInstance().createStatement();
				ResultSet rs = statement.executeQuery(query);
				while (rs.next()) {

					Integer id = rs.getInt("id");
					String name = rs.getString("name").toLowerCase();

					logger.info("getAppTypeTable(): id="+ id +" name="+ name);

					appTable.put(name, id);

				}

			} catch (SQLException e) {
				throw new HarnessException(e);
			}


		}
	}

	/**
	 * Get the ID corresponding to the Build from the perf DB
	 * @return
	 * @throws HarnessException
	 */
	protected int getBuildID() throws HarnessException {
		getBuildTable();

		String build = getVersionString();
		if (!buildTable.containsKey(build)) {
			insertBuild(build);
		}

		return (buildTable.get(build));
	}

	protected static HashMap<String, Integer> buildTable = null;
	protected void getBuildTable() throws HarnessException {
		if ( buildTable == null ) {
			buildTable = new HashMap<String, Integer>();

			try {

				String query = "SELECT id, build FROM builds";

				Statement statement = DatabaseConnection.getInstance().createStatement();
				ResultSet rs = statement.executeQuery(query);
				while (rs.next()) {

					Integer id = rs.getInt("id");
					String build = rs.getString("build");

					logger.info("getBuildTable(): id="+ id +" build="+ build);

					buildTable.put(build, id);

				}

			} catch (SQLException e) {
				throw new HarnessException(e);
			}


		}
	}
	protected void insertBuild(String build) throws HarnessException {

		if ( buildTable.containsKey(build) )
			throw new HarnessException("buildTable already contains "+ build);

		try {

			String command = String.format("INSERT INTO builds (build) VALUES ('%s')", build);
			logger.info("Statement: "+ command);

			Statement statement = DatabaseConnection.getInstance().createStatement();
			int ret = statement.executeUpdate(command);
			logger.info("Statement: ret="+ ret);


		} catch (SQLException e) {
			throw new HarnessException(e);
		}

		// Reset the action table to pick up the new ID
		buildTable = null;
		getBuildTable();

	}


	/**
	 * Get the ID corresponding to the key from the perf DB
	 * @return
	 * @throws HarnessException
	 */
	protected int getActionKey(String action) throws HarnessException {
		getActionTable();

		if (!actionTable.containsKey(action)) {
			insertAction(action);
		}

		return (actionTable.get(action));
	}

	protected static HashMap<String, Integer> actionTable = null;
	protected void getActionTable() throws HarnessException {
		if ( actionTable == null ) {
			actionTable = new HashMap<String, Integer>();

			try {

				String query = "SELECT id, name FROM actions";

				Statement statement = DatabaseConnection.getInstance().createStatement();
				ResultSet rs = statement.executeQuery(query);
				while (rs.next()) {

					Integer id = rs.getInt("id");
					String name = rs.getString("name");

					logger.info("getActionTable(): id="+ id +" name="+ name);

					actionTable.put(name, id);

				}

			} catch (SQLException e) {
				throw new HarnessException(e);
			}


		}
	}
	protected void insertAction(String action) throws HarnessException {

		if ( actionTable.containsKey(action) )
			throw new HarnessException("actionTable already contains "+ action);

		try {

			String command = String.format("INSERT INTO actions (name) VALUES ('%s')", action);
			logger.info("Statement: "+ command);

			Statement statement = DatabaseConnection.getInstance().createStatement();
			int ret = statement.executeUpdate(command);
			logger.info("Statement: ret="+ ret);


		} catch (SQLException e) {
			throw new HarnessException(e);
		}

		// Reset the action table to pick up the new ID
		actionTable = null;
		getActionTable();

	}

	protected String getMilestone() throws HarnessException {

		if ( getVersionString().startsWith("6") ) {
			return ("GunsNRoses");
		}
		if ( getVersionString().startsWith("7") ) {
			return ("Helix");
		}
		if ( getVersionString().startsWith("8") ) {
			return ("IronMaiden");
		}
		if ( getVersionString().startsWith("9") ) {
			return ("JudasPriest");
		}

		throw new HarnessException("Unable to determine Milestone from version string: "+ getVersionString());

	}

	protected String versionString = null;
	protected String getVersionString() throws HarnessException {
		if ( versionString == null ) {
			ZimbraAdminAccount.GlobalAdmin().soapSend("<GetVersionInfoRequest xmlns='urn:zimbraAdmin'/>");
			Element GetVersionInfo = ZimbraAdminAccount.GlobalAdmin().soapSelectNode("//admin:GetVersionInfoResponse//admin:info", 1);
			versionString = String.format("%s.%s.%s",
					GetVersionInfo.getAttribute("majorversion", "X"),
					GetVersionInfo.getAttribute("minorversion", "X"),
					GetVersionInfo.getAttribute("microversion", "X"));
		}
		return (versionString);
	}

	public static PerfDatabase getInstance() {
		if (Instance == null) {
			synchronized(PerfDatabase.class) {
				if ( Instance == null) {
					Instance = new PerfDatabase();
				}
			}
		}
		return (Instance);
	}


	protected volatile static PerfDatabase Instance;

	protected PerfDatabase() {	
		logger.info("New "+ this.getClass().getCanonicalName());			
	}

	protected static class DatabaseConnection {
		protected static Logger logger = LogManager.getLogger(DatabaseConnection.class);


		public static final String dbDefaultURL = "jdbc:mysql://10.20.140.198/perf";
		public static final String dbDefaultUsername = "perf";
		public static final String dbDefaultUserpass = "perf";


		public Connection getConnection() throws SQLException {

			if ( conn == null ) {

				conn = DriverManager.getConnection(dbURL, dbUsername, dbUserpass);
				logger.info("Database connection established: "+ dbURL);

			}

			return (conn);
		}

		public Statement createStatement() throws SQLException {

			return (getConnection().createStatement());

		}

		public static DatabaseConnection getInstance() throws HarnessException {
			if (Instance == null) {
				synchronized(DatabaseConnection.class) {
					if ( Instance == null) {
						Instance = new DatabaseConnection();
					}
				}
			}
			return (Instance);
		}


		protected volatile static DatabaseConnection Instance;
		protected Connection conn = null;
		protected String dbURL = null;
		protected String dbUsername = null;
		protected String dbUserpass = null;

		protected DatabaseConnection() throws HarnessException {	
			logger.info("New "+ this.getClass().getCanonicalName());			

			try {

				Class.forName("com.mysql.jdbc.Driver");
				
				dbURL = ZimbraSeleniumProperties.getStringProperty("performance.metrics.db.url", dbDefaultURL);
				dbUsername = ZimbraSeleniumProperties.getStringProperty("performance.metrics.db.username", dbDefaultUsername);
				dbUserpass = ZimbraSeleniumProperties.getStringProperty("performance.metrics.db.userpass", dbDefaultUserpass);

			} catch (ClassNotFoundException e) {
				throw new HarnessException(e);
			}


		}


	}


}
