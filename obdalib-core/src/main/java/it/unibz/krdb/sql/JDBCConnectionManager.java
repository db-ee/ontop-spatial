package it.unibz.krdb.sql;

/*
 * #%L
 * ontop-obdalib-core
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import it.unibz.krdb.obda.model.OBDADataSource;
import it.unibz.krdb.obda.model.OBDAException;
import it.unibz.krdb.obda.model.impl.RDBMSourceParameterConstants;
import it.unibz.krdb.sql.api.RelationJSQL;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDBCConnectionManager {

	public static final String JDBC_AUTOCOMMIT = "autocommit";
	public static final String JDBC_FETCHSIZE = "fetchsize";
	public static final String JDBC_RESULTSETTYPE = "resultsettype";
	public static final String JDBC_RESULTSETCONCUR = "resultsetconcur";
	
	private static JDBCConnectionManager instance = null;

	private HashMap<String, Object> properties = null;
	private HashMap<OBDADataSource, Connection> connectionPool = null;

	private static Logger log = LoggerFactory.getLogger(JDBCConnectionManager.class);

	/**
	 * Private constructor.
	 */
	private JDBCConnectionManager() {
		connectionPool = new HashMap<>();

		properties = new HashMap<>();
		properties.put(JDBC_AUTOCOMMIT, false);
		properties.put(JDBC_FETCHSIZE, 100);
		properties.put(JDBC_RESULTSETCONCUR, ResultSet.CONCUR_READ_ONLY);
		properties.put(JDBC_RESULTSETTYPE, ResultSet.TYPE_FORWARD_ONLY);
	}

	/**
	 * Returns a single connection manager.
	 */
	public static JDBCConnectionManager getJDBCConnectionManager() {
		if (instance == null) {
			instance = new JDBCConnectionManager();
		}
		return instance;
	}

	/**
	 * Constructs a new database connection object from a data source and
	 * retrieves the object.
	 * 
	 * @param dataSource
	 *            The data source object.
	 * @return The connection object.
	 * @throws SQLException
	 */
	public Connection createConnection(OBDADataSource dataSource) throws SQLException {

		if (connectionPool.get(dataSource) != null && !connectionPool.get(dataSource).isClosed())
			return connectionPool.get(dataSource);

		String url = dataSource.getParameter(RDBMSourceParameterConstants.DATABASE_URL);
		String username = dataSource.getParameter(RDBMSourceParameterConstants.DATABASE_USERNAME);
		String password = dataSource.getParameter(RDBMSourceParameterConstants.DATABASE_PASSWORD);
		String driver = dataSource.getParameter(RDBMSourceParameterConstants.DATABASE_DRIVER);

		if (driver == null || driver.trim().equals(""))
			throw new SQLException("Invalid driver");

		Connection conn = DriverManager.getConnection(url, username, password);
		connectionPool.put(dataSource, conn);
		return conn;
	}
	
	/**
	 * Retrieves the connection object from the connection pool. If the
	 * connection doesnt exist or is dead, it will attempt to create a new
	 * connection.
	 * 
	 * @param sourceId
	 *            The connection ID (usually the same as the data source URI).
	 */
	public Connection getConnection(OBDADataSource source) throws SQLException {
		boolean alive = isConnectionAlive(source);
		if (!alive) {
			createConnection(source);
		}
		Connection conn = connectionPool.get(source);
		return conn;
	}

	/**
	 * Removes a connection object form the pool. The system will put the
	 * connection back to the pool if an exception occurs.
	 * 
	 * @param sourceId
	 *            The connection ID that wants to be removed.
	 * @return Returns true if the removal is successful, or false otherwise.
	 */
	public boolean closeConnection(OBDADataSource source) throws OBDAException, SQLException {
		boolean bStatus = true; // the status flag.
		Connection existing = connectionPool.get(source);
		if (existing == null)
			throw new OBDAException("There is connection for such source");
		if (existing.isClosed()) {
			connectionPool.remove(source);
			throw new OBDAException("Connection is already close");
		}
		try {
			connectionPool.remove(source);
			existing.close();
		} catch (SQLException e) {
			log.error(e.getMessage());
		}
		return bStatus;
	}

	/**
	 * Checks whether the connection is still alive.
	 * 
	 * @param sourceId
	 *            The connection ID (usually the same as the data source URI).
	 * @return Returns true if the connection exists and is still open.
	 * 
	 * @throws SQLException
	 */
	public boolean isConnectionAlive(OBDADataSource sourceId) throws SQLException {
		Connection conn = connectionPool.get(sourceId);
		if (conn == null || conn.isClosed()) {
			return false;
		}
		return !conn.isClosed();
	}

	
	/**
	 * Removes all the connections in the connection pool.
	 * 
	 * @throws SQLException
	 */
	public void dispose() throws SQLException {
		Set<OBDADataSource> keys = connectionPool.keySet();
		for (OBDADataSource sourceId : keys) {
			try {
				closeConnection(sourceId);
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
	}
	
		
	/**
	 * Retrieves the database metadata (table schema and database constraints) 
	 * 
	 * This method either uses the given list of tables or 
	 *    if it is null then it retrives all the complete list of tables from 
	 *    the connection metadata
	 * 
	 * @return The database metadata object.
	 */

	public static DBMetadata getMetaData(Connection conn, List<RelationJSQL> tables) throws SQLException {
		final DatabaseMetaData md = conn.getMetaData();
		List<RelationDefinition> tableList;
		
		if (md.getDatabaseProductName().contains("Oracle")) {
			if (tables == null || tables.isEmpty())
				tableList = getTableListOracle(getOracleDefaultOwner(conn), tables);
			else
				tableList = getTableListOracle(getOracleDefaultOwner(conn), conn);
			
			return getMetadata(md, tableList, OracleTypeFixer);
		} 
		else if (md.getDatabaseProductName().contains("DB2")) {
			if (tables == null || tables.isEmpty()) {
				tableList = getTableListDB2(conn);
				return getMetadata(md, tableList, DefaultTypeFixer);
			}
			else {
				tableList = getTableListUpperCase(tables);
				return getMetadata(md, tableList, MySQLTypeFixer); // why MySQLTypeFixer?
			}
		}  
		else if (md.getDatabaseProductName().contains("H2")) {
			if (tables == null || tables.isEmpty()) 
				tableList = getTableListDefault(md);
			else 
				tableList = getTableListUpperCase(tables);
			
			return getMetadata(md, tableList, MySQLTypeFixer);
		}
		else if (md.getDatabaseProductName().contains("PostgreSQL")) {
			// Postgres treats unquoted identifiers as lowercase
			if (tables == null || tables.isEmpty()) 
				tableList = getTableListDefault(md);
			else 
				tableList = getTableListLowerCase(tables);
			
			return getMetadata(md, tableList, MySQLTypeFixer);
		} 
		else if (md.getDatabaseProductName().contains("SQL Server")) { // MS SQL Server
			// UNIQUE CONSTRAINTS WERE MISSING in the special procedure
			if (tables == null || tables.isEmpty()) 
				tableList = getTableListSQLServer(conn);
			else
				tableList = getTableListDefault(tables);
				
			return  getMetadata(md, tableList, DefaultTypeFixer);
 		} 
		else {
			// For other database engines, i.e. MySQL
			if (tables == null || tables.isEmpty()) 
				tableList = getTableListDefault(md);
			else
				tableList = getTableListDefault(tables);
			
			return getMetadata(md, tableList, MySQLTypeFixer);
		}
	}
	
	/**
	 * Retrieve the table and view list for most database engines, e.g., MySQL and PostgreSQL
	 */
	private static List<RelationDefinition> getTableListDefault(DatabaseMetaData md) throws SQLException {
		List<RelationDefinition> tables = new LinkedList<>();
		try (ResultSet rsTables = md.getTables(null, null, null, new String[] { "TABLE", "VIEW" })) {	
			while (rsTables.next()) {
				final String tblCatalog = rsTables.getString("TABLE_CAT");
				final String tblName = rsTables.getString("TABLE_NAME");
				final String tblSchema = rsTables.getString("TABLE_SCHEM");

				tables.add(new TableDefinition(tblCatalog, tblSchema, tblName, tblName));
				// new FullyQualifiedDD(null, null, tblName, fki.td));
				// was null for catalog and null for schema (for FKs only)
			}
		} 
		return tables;
	}

	/**
	 * Retrieve metadata for most of the database engine, e.g., MySQL and PostgreSQL
	 */
	private static List<RelationDefinition> getTableListDefault(List<RelationJSQL> tables) throws SQLException {

		List<RelationDefinition> fks = new LinkedList<>();
		for (RelationJSQL table : tables) {
			// tableGivenName is exactly the name the user provided, 
			// including schema prefix if that was provided, otherwise without.
			String tableGivenName = table.getGivenName();

			String tblName = table.getTableName(); 
			String tableSchema = table.getSchema();

			fks.add(new TableDefinition(null, tableSchema, tblName, tableGivenName));
		}
		return fks;
	}

	private static List<RelationDefinition> getTableListLowerCase(List<RelationJSQL> tables) throws SQLException {

		List<RelationDefinition> fks = new LinkedList<>();
		for (RelationJSQL table : tables) {
			// tableGivenName is exactly the name the user provided, 
			// including schema prefix if that was provided, otherwise without.
			String tableGivenName = table.getGivenName();
			
			String tblName = table.getTableName(); 
			if(!table.isTableQuoted())
				tblName = tblName.toLowerCase();
			
			String tableSchema = table.getSchema();
			if(tableSchema != null && !table.isSchemaQuoted())
				tableSchema = tableSchema.toLowerCase();
			
			fks.add(new TableDefinition(null, tableSchema, tblName, tableGivenName));
		}
		return fks;
	}
	private static List<RelationDefinition> getTableListUpperCase(List<RelationJSQL> tables) throws SQLException {

		List<RelationDefinition> fks = new LinkedList<>();
		for (RelationJSQL table : tables) {
			// tableGivenName is exactly the name the user provided, 
			// including schema prefix if that was provided, otherwise without.
			String tableGivenName = table.getGivenName();

			String tblName = table.getTableName(); 
			if(!table.isTableQuoted())
				tblName = tblName.toUpperCase();
			
			String tableSchema = table.getSchema();
			if(tableSchema != null && !table.isSchemaQuoted())
				tableSchema = tableSchema.toUpperCase();
			
			fks.add(new TableDefinition(null, tableSchema, tblName, tableGivenName));
		}
		return fks;
	}
	
	

	private static final String tableSelectQuerySQLServer = "SELECT TABLE_CATALOG, TABLE_SCHEMA, TABLE_NAME " +
			"FROM INFORMATION_SCHEMA.TABLES " +
			"WHERE TABLE_TYPE='BASE TABLE' OR TABLE_TYPE='VIEW'";
	
	/**
	 * Retrieve metadata for SQL Server database engine
	 */
	private static List<RelationDefinition> getTableListSQLServer(Connection conn) throws SQLException {
		List<RelationDefinition> tables = new LinkedList<>();
		try (Statement stmt = conn.createStatement()) {
			// Obtain the relational objects (i.e., tables and views) */
			try (ResultSet resultSet = stmt.executeQuery(tableSelectQuerySQLServer)) {
				while (resultSet.next()) {
					final String tblCatalog = resultSet.getString("TABLE_CATALOG");
					final String tblSchema = resultSet.getString("TABLE_SCHEMA");
					final String tblName = resultSet.getString("TABLE_NAME");
					tables.add(new TableDefinition(tblCatalog, tblSchema, tblName, tblName));
				}
			}
		} 
		return tables;
	}

	
	private static final String tableSelectQueryDB2 = "SELECT TABSCHEMA, TABNAME " +
			"FROM SYSCAT.TABLES " +
			"WHERE OWNERTYPE='U' " +
			"	AND (TYPE='T' OR TYPE='V') " +
			"	AND TBSPACEID IN (SELECT TBSPACEID FROM SYSCAT.TABLESPACES WHERE TBSPACE LIKE 'USERSPACE%')";
	
	/**
	 * Retrieve metadata for DB2 database engine
	 */
	private static List<RelationDefinition> getTableListDB2(Connection conn) throws SQLException {

		List<RelationDefinition> tables = new LinkedList<>();
		try (Statement stmt = conn.createStatement()) {
			// Obtain the relational objects (i.e., tables and views) */
			try (ResultSet resultSet = stmt.executeQuery(tableSelectQueryDB2)) {
				while (resultSet.next()) {
					final String tblSchema = resultSet.getString("TABSCHEMA");
					final String tblName = resultSet.getString("TABNAME");
					tables.add(new TableDefinition(null, tblSchema, tblName, tblName));
				}
			}
		} 
		return tables; 
	}

	
	private static final String tableSelectQueryOracle = "SELECT object_name FROM ( " +
			"SELECT table_name as object_name FROM user_tables WHERE " +
			"NOT table_name LIKE 'MVIEW$_%' AND " +
			"NOT table_name LIKE 'LOGMNR_%' AND " +
			"NOT table_name LIKE 'AQ$_%' AND " +
			"NOT table_name LIKE 'DEF$_%' AND " +
			"NOT table_name LIKE 'REPCAT$_%' AND " +
			"NOT table_name LIKE 'LOGSTDBY$%' AND " +
			"NOT table_name LIKE 'OL$%' " +
			"UNION " +
			"SELECT view_name as object_name FROM user_views WHERE " +
			"NOT view_name LIKE 'MVIEW_%' AND " +
			"NOT view_name LIKE 'LOGMNR_%' AND " +
			"NOT view_name LIKE 'AQ$_%')";
	
	/**
	 * Retrieve metadata for Oracle database engine
	 */
	private static List<RelationDefinition> getTableListOracle(String defaultTableOwner, Connection conn) throws SQLException {
		
		List<RelationDefinition> fks = new LinkedList<>();
		try (Statement stmt = conn.createStatement()) {
			// Obtain the relational objects (i.e., tables and views) */
			try (ResultSet resultSet = stmt.executeQuery(tableSelectQueryOracle)) {
				while (resultSet.next()) {
					final String tblName = resultSet.getString("object_name");
					fks.add(new TableDefinition(null, defaultTableOwner, tblName, tblName));
				}
			}
		}
		return fks; 
	}
	
	/**
	 * Retrieve metadata for Oracle database engine
	 */
	private static  List<RelationDefinition> getTableListOracle(String defaultTableOwner, List<RelationJSQL> tables) throws SQLException {
		
		List<RelationDefinition> fks = new LinkedList<>();
		
		// The tables contains all tables which occur in the sql source queries
		// Note that different spellings (casing, quotation marks, optional schema prefix) 
		// may lead to the same table occurring several times 
		for (RelationJSQL table : tables) {
			// givenTableName is exactly the name the user provided, 
			// including schema prefix if that was provided, otherwise without.
			String tableGivenName = table.getGivenName();
			
			// If there is a schema prefix, this must be the tableOwner argument to the 
			// jdbc methods below. Otherwise, we use the logged in user. I guess null would
			// also have worked in the latter case.
			String tableOwner;
			if (table.getSchema() != null) {
				tableOwner = table.getSchema();
				if (!table.isSchemaQuoted())
					tableOwner = tableOwner.toUpperCase();
			}
			else
				tableOwner = defaultTableOwner.toUpperCase();

			String tblName = table.getTableName();
			if (!table.isTableQuoted())
				tblName = tblName.toUpperCase();
			
			fks.add(new TableDefinition(null, tableOwner, tblName, tableGivenName));
		}
		return fks;
	}
	
	
	
	
	private static String getOracleDefaultOwner(Connection conn) throws SQLException {
		// Obtain the table owner (i.e., schema name) 
		String loggedUser = "SYSTEM"; // by default
		try (Statement stmt = conn.createStatement()) {
			try (ResultSet resultSet = stmt.executeQuery("SELECT user FROM dual")) {
				if (resultSet.next()) {
					loggedUser = resultSet.getString("user");
				}
			}
		}
		return loggedUser;
	}
		
	
	private interface DatatypeNormalizer {
		int getCorrectedDatatype(int dataType, String typeName);
	}
	
	private static DatatypeNormalizer DefaultTypeFixer = new DatatypeNormalizer() {
		@Override
		public int getCorrectedDatatype(int dataType, String typeName) {					
			return dataType;
		}};
		
	private static DatatypeNormalizer MySQLTypeFixer = new DatatypeNormalizer() {
		@Override
		public int getCorrectedDatatype(int dataType, String typeName) {					
			// Fix for MySQL YEAR
			if (dataType == 91 && typeName.equals("YEAR")) 
				return -10000;
			return dataType;
		}};	
			
	private static DatatypeNormalizer OracleTypeFixer = new DatatypeNormalizer() {
		@Override
		public int getCorrectedDatatype(int dataType, String typeName) {					
			
			//TODO 
			// Oracle bug here - wrong automatic typing - Date vs DATETIME - driver ojdbc16-11.2.0.3
			// Oracle returns 93 for DATE SQL types, but this corresponds to 
			// TIMESTAMP. This causes a wrong typing to xsd:dateTime and later
			// parsing errors. To avoid this bug manually type the column in the
			// mapping. This may be a problem of the driver, try with other version
			// I tried oracle thin driver ojdbc16-11.2.0.3
			
			if (dataType == 93 && typeName.equals("DATE")) 
				dataType = 91;
			return dataType;
		}};
	
	private static DBMetadata getMetadata(DatabaseMetaData md, List<RelationDefinition> tables, DatatypeNormalizer dt) throws SQLException {
		DBMetadata metadata = new DBMetadata(md.getDriverName(), md.getDriverVersion(), md.getDatabaseProductName());
		
		for (RelationDefinition table : tables) {
			getTableColumns(md, table, dt);
			getPrimaryKey(md, table);
			getUniqueAttributes(md, table);
			metadata.add(table);
		}	
		for (RelationDefinition table : tables) 
			getForeignKeys(md, table, metadata);
		return metadata;
	}
	
	
	private static void getTableColumns(DatabaseMetaData md, RelationDefinition table, DatatypeNormalizer dt) throws SQLException {
		// needed for checking uniqueness of lower-case versions of columns names
		//  (only in getOtherMetadata)
		//Set<String> tableColumns = new HashSet<>();
		
		try (ResultSet rsColumns = md.getColumns(table.getCatalog(), table.getSchema(), table.getTableName(), null)) {
			//if (rsColumns == null) 
			//	return;			
			while (rsColumns.next()) {
				final String columnName = rsColumns.getString("COLUMN_NAME");
				// columnNoNulls, columnNullable, columnNullableUnknown 
				final boolean isNullable = rsColumns.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
				
				final String typeName = rsColumns.getString("TYPE_NAME");
				final int dataType = dt.getCorrectedDatatype(rsColumns.getInt("DATA_TYPE"), typeName);
				
				table.addAttribute(columnName, dataType, typeName, isNullable);
				// Check if the columns are unique regardless their letter cases
				//if (!tableColumns.add(columnName.toLowerCase())) {
				//	throw new RuntimeException("The system cannot process duplicate table columns!");
				//}
			}
		}
	}
	
	/**
	 * Prints column names of a given table.
     *
	 */
	private static void displayColumnNames(DatabaseMetaData dbMetadata, 
			Connection connection, ResultSet rsColumns, 
			String tableSchema, String tableName) throws SQLException {

		log.debug("=============== COLUMN METADATA ========================");
		
		if (dbMetadata.getDatabaseProductName().contains("DB2")) {
			 // Alternative solution for DB2 to print column names
		     // Queries directly the system table SysCat.Columns
			try (Statement st = connection.createStatement()) {
		        String sqlQuery = String.format("SELECT colname, typename \n FROM SysCat.Columns \n" + 
		        								"WHERE tabname = '%s' AND tabschema = '%s'", tableName, tableSchema);
		        st.execute(sqlQuery);
		        ResultSet results = st.getResultSet();
		        while (results.next()) {
		            log.debug("Column={} Type={}", results.getString("colname"), results.getString("typename"));
		        }
			}
		}
		else {
			 // Generic procedure based on JDBC
			ResultSetMetaData columnMetadata = rsColumns.getMetaData();
			int count = columnMetadata.getColumnCount();
			for (int j = 0; j < count; j++) {
			    String columnName = columnMetadata.getColumnName(j + 1);
			    String value = rsColumns.getString(columnName);
			    log.debug("Column={} Type={}", columnName, value);
			}
						
		}
	}
	
	
	

	/** 
	 * Retrieves the primary key for the table 
	 * 
	 */
	private static void getPrimaryKey(DatabaseMetaData md, RelationDefinition table) throws SQLException {
		UniqueConstraint.Builder pk = UniqueConstraint.builder("PK_" + table.getName(), table);
		try (ResultSet rsPrimaryKeys = md.getPrimaryKeys(table.getCatalog(), table.getSchema(), table.getName())) {
			while (rsPrimaryKeys.next()) {
				String colName = rsPrimaryKeys.getString("COLUMN_NAME");
				String pkName = rsPrimaryKeys.getString("PK_NAME");
				if (pkName != null) 
					pk.add(table.getAttribute(colName));
			}
		} 
		table.setPrimaryKey(pk.build());
	}
	
	/**
	 * Retrieves the unique attributes(s) 
	 * @param md
	 * @return
	 * @throws SQLException 
	 */
	private static void getUniqueAttributes(DatabaseMetaData md, RelationDefinition table) throws SQLException {

		Set<String> uniqueSet  = new HashSet<>();

		// extracting unique 
		try (ResultSet rsUnique= md.getIndexInfo(table.getCatalog(), table.getSchema(), table.getTableName(), true, true)) {
			while (rsUnique.next()) {
				String colName = rsUnique.getString("COLUMN_NAME");
				String nonUnique = rsUnique.getString("NON_UNIQUE");
				
				if (colName!= null){
                // MySQL: false
                // Postgres: f
				// DB2 : 0
					boolean unique =  nonUnique.toLowerCase().startsWith("f") || nonUnique.toLowerCase().startsWith("0") ;
					if (unique /*&& !(pk.contains(colName))*/ ) { // !!! ROMAN
						uniqueSet.add(colName);
					}
				}
			}
		}
		
		// Adding keys and Unique	
	}
	
	/** 
	 * Retrieves the foreign keys for the table 
	 * 
	 */
	private static void getForeignKeys(DatabaseMetaData md, RelationDefinition table, DBMetadata metadata) throws SQLException {
		
		try (ResultSet rsForeignKeys = md.getImportedKeys(table.getCatalog(), table.getSchema(), table.getTableName())) {
			ForeignKeyConstraint.Builder builder = null;
			String currentName = "";
			while (rsForeignKeys.next()) {
				String pkTableName = rsForeignKeys.getString("PKTABLE_NAME");
				RelationDefinition ref = metadata.getDefinition(pkTableName);
				String name = rsForeignKeys.getString("FK_NAME");
				if (!currentName.equals(name)) {
					if (builder != null) 
						table.addForeignKeyConstraint(builder.build());
					
					builder = new ForeignKeyConstraint.Builder(name, table, ref);
					currentName = name;
				}
				String colName = rsForeignKeys.getString("FKCOLUMN_NAME");
				String pkColumnName = rsForeignKeys.getString("PKCOLUMN_NAME");
				if (ref != null)
					builder.add(table.getAttribute(colName), ref.getAttribute(pkColumnName));
				else {
					System.err.println("Cannot find table: " + pkTableName + " for " + name);
					builder = null;
				}
			}
			if (builder != null)
				table.addForeignKeyConstraint(builder.build());
		} 
	}
}
