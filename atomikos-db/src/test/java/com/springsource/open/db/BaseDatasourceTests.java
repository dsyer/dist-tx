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

import java.io.File;

import javax.sql.DataSource;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/META-INF/spring/data-source-context.xml")
public abstract class BaseDatasourceTests {

	private JdbcTemplate jdbcTemplate;
	private JdbcTemplate otherJdbcTemplate;
	
	protected JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	protected JdbcTemplate getOtherJdbcTemplate() {
		return otherJdbcTemplate;
	}

	@BeforeClass
	@AfterClass
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
	public void setDataSources(@Qualifier("dataSource") DataSource dataSource,
			@Qualifier("otherDataSource") DataSource otherDataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.otherJdbcTemplate = new JdbcTemplate(otherDataSource);
	}

}
