package test.jdbc.datasource;

import java.io.File;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.derby.jdbc.EmbeddedXADataSource;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class DerbyDataSourceFactoryBean extends AbstractFactoryBean {

	private String dataDirectory = "derby-home";
	private String databaseName = "derbydb";;

	public void setDataDirectory(String dataDirectory) {
		this.dataDirectory = dataDirectory;
	}
	
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	@Override
	protected void destroyInstance(Object instance) throws Exception {
		EmbeddedXADataSource dataSource = (EmbeddedXADataSource)instance;
		dataSource.setShutdownDatabase("shutdown");
		dataSource.getConnection().close();
	}

	protected Object createInstance() throws Exception {
		File directory = new File(dataDirectory);
		System.setProperty("derby.system.home", directory.getCanonicalPath());
		System.setProperty("derby.storage.fileSyncTransactionLog", "true");
		System.setProperty("derby.storage.pageCacheSize", "100");

		final EmbeddedXADataSource ds = new EmbeddedXADataSource();
		ds.setDatabaseName(databaseName);
		ds.setCreateDatabase("create");

		return ds;
	}

	public Class<ExtendedDataSource> getObjectType() {
		return ExtendedDataSource.class;
	}
	
	public interface ExtendedDataSource extends DataSource, XADataSource {
		
	}

}
