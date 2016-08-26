/*
 * Copyright 2006-2007 the original author or authors.
 *
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
 */

package com.springsource.open.db;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.atomikos.jdbc.SimpleDataSourceBean;

/**
 * A {@link FactoryBean} for an Atomikos pooled {@link DataSource} taking care
 * of lifecycle callbacks through Spring, and allowing an {@link XADataSource}
 * to be injected directly, instead of relying on Atomikos to create one.
 * 
 * @author Dave Syer
 * 
 */
public class AtomikosDataSourceFactoryBean extends AbstractFactoryBean {

	private String uniqueResourceName;
	private XADataSource xaDataSource;
	private boolean exclusiveConnectionMode;
	private int connectionPoolSize;

	public void setUniqueResourceName(String uniqueResourceName) {
		this.uniqueResourceName = uniqueResourceName;
	}

	/**
	 * The {@link XADataSource} to inject into the pool. Atomikos 3.2 does not
	 * expose this as a JavaBean property. Although it breaks the
	 * SimpleDataSourceBean serializability to inject this, it's hard to see why
	 * anyone would want to serialize a DataSource (and as expected the tmlog
	 * doesn't contain any references to a DataSource when I've looked). In
	 * Atomikos 3.4 the restriction is lifted apparently, so we'll assume that
	 * this is OK. Also, Atomikos 3.2 also uses Class.forName (fixed in 3.4), so
	 * users experience problems in come environments (e.g. dm Server) if they
	 * don't use somehting like this {@link FactoryBean}.
	 * 
	 * @param xaDataSource
	 */
	public void setXaDataSource(XADataSource xaDataSource) {
		this.xaDataSource = xaDataSource;
	}

	public void setExclusiveConnectionMode(boolean exclusiveConnectionMode) {
		this.exclusiveConnectionMode = exclusiveConnectionMode;
	}

	public void setConnectionPoolSize(int connectionPoolSize) {
		this.connectionPoolSize = connectionPoolSize;
	}

	/**
	 * Ensure that the data source is cleaned up when the application context is
	 * closed.
	 * 
	 * @see AbstractFactoryBean#destroyInstance(Object)
	 */
	@Override
	protected void destroyInstance(Object instance) throws Exception {
		((SimpleDataSourceBean) instance).close();
	}

	@Override
	protected Object createInstance() throws Exception {
		SimpleDataSourceBean result = new SimpleDataSourceBean();
		result.setXaDataSource(xaDataSource);
		result.setUniqueResourceName(uniqueResourceName);
		result.setExclusiveConnectionMode(exclusiveConnectionMode);
		result.setConnectionPoolSize(connectionPoolSize);
		result.init();
		return result;
	}

	public Class<SimpleDataSourceBean> getObjectType() {
		return SimpleDataSourceBean.class;
	}

	public boolean isSingleton() {
		return true;
	}
}
