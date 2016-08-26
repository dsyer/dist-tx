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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

public class TransactionPropagationTests extends BaseDatasourceTests  {

	// Inject a service that can be proxied so we can apply tx propagation
	@Autowired
	private AuditService auditService;

	@BeforeTransaction
	public void clearData() {
		getJdbcTemplate().update("delete from T_FOOS");
		getOtherJdbcTemplate().update("delete from T_AUDITS");
	}

	@AfterTransaction
	public void checkPostConditions() {

		int count = getJdbcTemplate().queryForObject("select count(*) from T_FOOS", Integer.class);
		// This change was rolled back by the test framework
		assertEquals(0, count);

		count = getOtherJdbcTemplate().queryForObject("select count(*) from T_AUDITS", Integer.class);
		// This one doesn't roll back because of TX propagation
		assertEquals(1, count);

	}

	@Transactional
	@Test
	public void testInsertIntoTwoDataSources() throws Exception {
		int count;
		count = getOtherJdbcTemplate().queryForObject("select count(*) from T_AUDITS", Integer.class);
		assertEquals(0, count);
		count = getJdbcTemplate().queryForObject("select count(*) from T_FOOS", Integer.class);
		assertEquals(0, count);

		getJdbcTemplate().update(
				"INSERT into T_FOOS (id,name,foo_date) values (?,?,null)", 0,
				"foo");
		count = getJdbcTemplate().queryForObject("select count(*) from T_FOOS", Integer.class);
		assertEquals(1, count);

		auditService.update(0, "INSERT", "foo");
		getOtherJdbcTemplate().update("UPDATE T_AUDITS set name=? where name=?",
				"bar", "foo");
		count = getOtherJdbcTemplate().queryForObject("select count(*) from T_AUDITS", Integer.class);
		assertEquals(1, count);

	}
}
