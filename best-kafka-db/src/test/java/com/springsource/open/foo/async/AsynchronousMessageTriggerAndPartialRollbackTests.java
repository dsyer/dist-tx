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

package com.springsource.open.foo.async;

import org.junit.jupiter.api.Test;

import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.Assert.assertEquals;

public class AsynchronousMessageTriggerAndPartialRollbackTests extends AbstractAsynchronousMessageTriggerTests {

	@Test
	public void testPartialFailureWithDuplicateMessage() {
		kafkaTemplate.executeInTransaction(t -> t.send("async", "foo"));
		kafkaTemplate.executeInTransaction(t -> t.send("async", "bar.fail.partial"));
	}

	@Override
	protected void checkPostConditions() throws Exception {

		// Both committed
		assertEquals(2, JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_FOOS"));
		// One message rolled back and returned to queue
		assertEquals(1, consumerOffset());

	}

}
