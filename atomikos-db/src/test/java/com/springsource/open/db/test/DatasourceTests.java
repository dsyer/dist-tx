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

package com.springsource.open.db.test;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import com.springsource.open.db.BaseDatasourceTests;

public class DatasourceTests extends BaseDatasourceTests {

	@Transactional @Test
	public void testMainDataSource() throws Exception {
		getJdbcTemplate().execute("delete from T_FOOS");
		int count = getJdbcTemplate().queryForObject("select count(*) from T_FOOS", Integer.class);
		assertEquals(0, count);

		getJdbcTemplate().update("INSERT into T_FOOS (id,name,foo_date) values (?,?,null)", 0, "foo");
	}

	@Transactional @Test
	public void testOtherDataSource() throws Exception {
		getOtherJdbcTemplate().execute("delete from T_AUDITS");
		int count = getOtherJdbcTemplate().queryForObject("select count(*) from T_AUDITS", Integer.class);
		assertEquals(0, count);

		getOtherJdbcTemplate().update("INSERT into T_AUDITS (id,operation,name,audit_date) values (?,?,?,?)", 0, "INSERT", "foo", new Date());
	}
}
