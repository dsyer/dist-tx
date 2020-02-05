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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;

@SpringBootApplication
public class ListenerApplication {

	@Bean
	public NewTopic asyncTopic() {
		return new NewTopic("async", 1, (short) 1);
	}

	@Bean
	public ConsumerAwareRebalanceListener listenerContanerCustomizer(ConsumerConfiguration config) {
		return new ConsumerAwareRebalanceListener() {
			@Override
			public void onPartitionsAssigned(org.apache.kafka.clients.consumer.Consumer<?, ?> consumer,
					Collection<TopicPartition> partitions) {
				for (TopicPartition partition : partitions) {
					long offset = config.getOffset(partition.topic(), partition.partition()) + 1;
					System.err.println("Seeking: " + partition + " to offset=" + offset + " from position="
							+ consumer.position(partition));
					consumer.seek(partition, offset);
				}
			}
		};
	}

	@Bean
	ConsumerConfiguration config(DataSource dataSource) {
		return new ConsumerConfiguration(dataSource);
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(ListenerApplication.class, args);
	}

}

class ConsumerConfiguration {
	private Map<String, Long> cache = new HashMap<>();
	private final JdbcTemplate jdbcTemplate;

	public ConsumerConfiguration(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public long getOffset(String topic, int partition) {
		init(topic);
		return this.cache.get(topic);
	}

	private void init(String topic) {
		Long initialized = this.cache.get(topic);
		if (initialized != null) {
			return;
		}
		try {
			Map<String, Object> offset = jdbcTemplate.queryForMap("SELECT * FROM T_OFFSETS WHERE ID=0 and TOPIC=?",
					topic);
			this.cache.put(topic, (Long) offset.get("offset"));
		} catch (EmptyResultDataAccessException e) {
			Long offset = -1L;
			jdbcTemplate.update("INSERT into T_OFFSETS (ID, topic, part, offset) values (?, ?, ?, ?)", 0, topic, 0,
					offset);
			this.cache.put(topic, offset);
		}
	}

}