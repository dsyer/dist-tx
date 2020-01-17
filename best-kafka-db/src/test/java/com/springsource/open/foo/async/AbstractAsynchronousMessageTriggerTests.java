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

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.springsource.open.foo.Handler;

import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest("spring.kafka.consumer.group-id=group")
public abstract class AbstractAsynchronousMessageTriggerTests {

	private AdminClient client;

	@Autowired
	protected KafkaTemplate<Object, String> kafkaTemplate;

	@Autowired
	private Handler handler;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@BeforeEach
	public void clearData(@Autowired KafkaAdmin admin, @Autowired KafkaListenerEndpointRegistry registry,
			@Autowired DefaultKafkaProducerFactory<?, ?> pf)
					throws InterruptedException, ExecutionException, TimeoutException {

		pf.reset(); // close the producer(s) because his metadata will be invalidated when we delete the topic
		this.client = AdminClient.create(admin.getConfig());
		this.client.deleteTopics(Collections.singletonList("async")).all().get(10, TimeUnit.SECONDS);
		int n = 0;
		while (n++ < 100) {
			try {
				this.client
						.createTopics(
								Collections.singletonList(TopicBuilder.name("async").partitions(1).replicas(1).build()))
						.all().get(10, TimeUnit.SECONDS);
				break;
			}
			catch (ExecutionException e) {
				// race with async topic deletion
				Thread.sleep(100);
			}
		}

		// Start the listeners...
		handler.resetItemCount();
		registry.getListenerContainer("group").start();
		jdbcTemplate.update("delete from T_FOOS");
	}

	@AfterEach
	public void waitForMessages(@Autowired KafkaListenerEndpointRegistry registry) throws Exception {

		int count = 0;
		while (handler.getItemCount() < 2 && (count++) < 30) {
			Thread.sleep(100);
		}
		// Stop the listeners...
		registry.getListenerContainer("group").stop();
		// Give it time to finish up...
		Thread.sleep(2000);
		assertThat(handler.getItemCount()).isGreaterThanOrEqualTo(2);

		checkPostConditions();
		this.client.close();
	}

	protected abstract void checkPostConditions() throws Exception;

	protected long consumerOffset() throws InterruptedException, ExecutionException, TimeoutException {
		return this.client.listConsumerGroupOffsets("group").partitionsToOffsetAndMetadata().get(10, TimeUnit.SECONDS)
				.values().iterator().next().offset();
	}

}
