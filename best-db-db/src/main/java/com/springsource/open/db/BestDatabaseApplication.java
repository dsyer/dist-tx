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

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootApplication
public class BestDatabaseApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(BestDatabaseApplication.class, args);
	}

	@Bean
	public ChainedTransactionManager transactionManager(
			@Qualifier("firstDataSource") DataSource firstDataSource,
			@Qualifier("secondDataSource") DataSource secondDataSource) {
		ChainedTransactionManager transactionManager = new ChainedTransactionManager(
				getTransactionManagers(firstDataSource, secondDataSource));
		return transactionManager;
	}

	private List<PlatformTransactionManager> getTransactionManagers(
			DataSource... sources) {
		List<PlatformTransactionManager> list = new ArrayList<>();
		for (DataSource dataSource : sources) {
			DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager(
					dataSource);
			dataSourceTransactionManager
					.setTransactionSynchronizationName("SYNCHRONIZATION_NEVER");
			list.add(dataSourceTransactionManager);
		}
		return list;
	}

	@Bean
	@ConfigurationProperties("first.datasource")
	public DataSource firstDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean
	@ConfigurationProperties("second.datasource")
	public DataSource secondDataSource() {
		return DataSourceBuilder.create().build();
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
