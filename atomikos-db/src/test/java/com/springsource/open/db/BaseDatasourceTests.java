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

import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

@SpringBootTest
public abstract class BaseDatasourceTests {

	@Autowired
	private PlatformTransactionManager transactionManager;

	private JdbcTemplate jdbcTemplate;
	private JdbcTemplate otherJdbcTemplate;

	protected JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	protected JdbcTemplate getOtherJdbcTemplate() {
		return otherJdbcTemplate;
	}

	@BeforeEach
	public void check() {
		assertTrue("Wrong transactionManager: " + transactionManager,
				transactionManager instanceof JtaTransactionManager);
	}

	@BeforeAll
	@AfterAll
	public static void clearLog() {
		// Ensure that Atomikos logging directory exists
		File dir = new File("atomikos");
		if (!dir.exists()) {
			dir.mkdir();
		}
		// ...and delete any stale locks (this would be a sign of a crash)
		File tmlog = new File("atomikos/tmlog.lck");
		if (tmlog.exists()) {
			tmlog.delete();
		}
	}

	@Autowired
	public void setDataSources(@Qualifier("firstDataSource") DataSource dataSource,
			@Qualifier("secondDataSource") DataSource otherDataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.otherJdbcTemplate = new JdbcTemplate(otherDataSource);
	}

}
