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

package com.springsource.open.foo;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.connection.TransactionAwareConnectionFactoryProxy;
import org.springframework.jms.core.JmsTemplate;

@SpringBootApplication
public class ListenerApplication {

	@Bean
	public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
		JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		jmsTemplate.setReceiveTimeout(200);
		jmsTemplate.setSessionTransacted(true);
		return jmsTemplate;
	}

	@Bean
	public ConnectionFactory connectionFactory() {
		ActiveMQConnectionFactory target = new ActiveMQConnectionFactory(
				"vm://localhost?broker.persistent=false&jms.prefetchPolicy.queuePrefetch=0");
		TransactionAwareConnectionFactoryProxy proxy = new TransactionAwareConnectionFactoryProxy(
				target);
		proxy.setSynchedLocalTransactionAllowed(true);
		return proxy;
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(ListenerApplication.class, args);
	}

}
