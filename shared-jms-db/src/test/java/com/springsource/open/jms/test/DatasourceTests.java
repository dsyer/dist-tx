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

package com.springsource.open.jms.test;

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DatasourceTests {

	private JdbcTemplate simpleJdbcTemplate;

	@Autowired
	public void setDataSource(@Qualifier("dataSource") DataSource dataSource) {
		this.simpleJdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Transactional @Test
	public void testTemplate() throws Exception {
		int count = JdbcTestUtils.countRowsInTable(simpleJdbcTemplate, "T_FOOS");
		simpleJdbcTemplate.update("INSERT into T_FOOS (id,name,foo_date) values (?,?,null)", count, "spam");
		assertEquals(count + 1, JdbcTestUtils.countRowsInTable(simpleJdbcTemplate, "T_FOOS"));
	}
}
