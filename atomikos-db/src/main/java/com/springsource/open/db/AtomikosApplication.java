/*
 * Copyright 2012-2015 the original author or authors.
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

import java.io.File;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedXADataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.XADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@SpringBootApplication
public class AtomikosApplication {
	
	private final XADataSourceWrapper wrapper;

	public AtomikosApplication(XADataSourceWrapper wrapper) {
		this.wrapper = wrapper;	
	}

	@PostConstruct
	public void init() throws Exception {
		File directory = new File("derby-home");
		System.setProperty("derby.system.home", directory.getCanonicalPath());
		System.setProperty("derby.storage.fileSyncTransactionLog", "true");
		System.setProperty("derby.storage.pageCacheSize", "100");
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(AtomikosApplication.class, args);
	}

	@Bean
	@ConfigurationProperties("first.datasource")
	public DataSource firstDataSource(@Value("${first.datasource.name}") String name) throws Exception {
		return xaDataSource(name);
	}

	@Bean
	@ConfigurationProperties("second.datasource")
	public DataSource secondDataSource(@Value("${second.datasource.name}") String name) throws Exception {
		return xaDataSource(name);
	}

	private DataSource xaDataSource(String name) throws Exception {
		EmbeddedXADataSource dataSource = new EmbeddedXADataSource();
		dataSource.setCreateDatabase("create");
		dataSource.setDatabaseName(name);
		DataSource wrapped = wrapper.wrapDataSource(dataSource);
		return wrapped;
	}

	@Bean
	public DataSourceInitializer firstDataSourceInitializer(
			@Qualifier("firstDataSource") DataSource dataSource) {
		return initialize(dataSource, "first");
	}

	@Bean
	public DataSourceInitializer secondDataSourceInitializer(
			@Qualifier("secondDataSource") DataSource dataSource) {
		return initialize(dataSource, "second");
	}

	private DataSourceInitializer initialize(DataSource dataSource, String name) {
		DataSourceInitializer initializer = new DataSourceInitializer();
		initializer.setDataSource(dataSource);
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
				new ClassPathResource(name + ".sql"));
		populator.setIgnoreFailedDrops(true);
		initializer.setDatabasePopulator(populator);
		return initializer;
	}
}
