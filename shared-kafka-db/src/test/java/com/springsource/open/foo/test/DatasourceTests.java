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

package com.springsource.open.foo.test;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class DatasourceTests {

	@Autowired
	private JdbcTemplate simpleJdbcTemplate;

	@Transactional
	@Test
	public void testTemplate() throws Exception {
		simpleJdbcTemplate.execute("delete from T_FOOS");
		int count = simpleJdbcTemplate.queryForObject("select count(*) from T_FOOS", Integer.class);
		assertEquals(0, count);

		simpleJdbcTemplate.update("INSERT into T_FOOS (id,name,foo_date) values (?,?,null)", 0, "foo");
	}

	@Transactional
	@Test
	public void testOffsets() throws Exception {
		simpleJdbcTemplate.execute("delete from T_OFFSETS");
		assertThrows(EmptyResultDataAccessException.class,
				() -> simpleJdbcTemplate.queryForMap("SELECT * FROM T_OFFSETS WHERE ID=0 and TOPIC=?", "async"));
	}
}
