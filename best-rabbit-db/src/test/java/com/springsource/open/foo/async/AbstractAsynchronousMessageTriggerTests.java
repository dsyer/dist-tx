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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import com.springsource.open.foo.Handler;

@RunWith(SpringRunner.class)
@SpringBootTest
public abstract class AbstractAsynchronousMessageTriggerTests implements
		ApplicationContextAware {

	@Autowired
	protected RabbitTemplate jmsTemplate;

	@Autowired
	private Handler handler;

	protected JdbcTemplate jdbcTemplate;

	private Lifecycle lifecycle;

	@Autowired
	public void setDataSource(@Qualifier("dataSource") DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.lifecycle = (Lifecycle) applicationContext;
	}

	@Before
	public void clearData() {
		// Start the listeners...
		lifecycle.start();
		getMessages(); // drain queue
		handler.resetItemCount();
		jdbcTemplate.update("delete from T_FOOS");
	}

	@After
	public void waitForMessages() throws Exception {

		int count = 0;
		while (handler.getItemCount() < 2 && (count++) < 30) {
			Thread.sleep(100);
		}
		// Stop the listeners...
		lifecycle.stop();
		// Give it time to finish up...
		Thread.sleep(2000);
		assertTrue("Wrong item count: " + handler.getItemCount(), handler.getItemCount() >= 2);

		checkPostConditions();

	}

	protected abstract void checkPostConditions();

	protected List<String> getMessages() {
		String next = "";
		List<String> msgs = new ArrayList<String>();
		while (next != null) {
			next = (String) jmsTemplate.receiveAndConvert("async");
			if (next != null)
				msgs.add(next);
		}
		return msgs;
	}

}
