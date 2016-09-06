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

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootApplication
public class ListenerApplication {
	
	@Bean
	public Queue sync() {
		return new Queue("queue");
	}

	@Bean
	public Queue async() {
		return new Queue("async");
	}

	@Bean
	public RabbitTemplate jmsTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate jmsTemplate = new RabbitTemplate(connectionFactory);
		jmsTemplate.setReceiveTimeout(200);
		jmsTemplate.setChannelTransacted(true);
		return jmsTemplate;
	}

	@Bean
	public BeanPostProcessor connectionFactoryPostProcessor(
			PlatformTransactionManager transactionManager) {
		return new BeanPostProcessor() {

			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName)
					throws BeansException {
				return bean;
			}

			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName)
					throws BeansException {
				if (bean instanceof SimpleRabbitListenerContainerFactory) {
					((SimpleRabbitListenerContainerFactory) bean)
							.setTransactionManager(transactionManager);
				}
				return bean;
			}
		};
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(ListenerApplication.class, args);
	}

}
