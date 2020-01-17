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

import javax.sql.DataSource;

import org.apache.kafka.clients.admin.NewTopic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.kafka.listener.AfterRollbackProcessor;
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor;
import org.springframework.kafka.transaction.ChainedKafkaTransactionManager;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.util.backoff.FixedBackOff;

@SpringBootApplication
public class ListenerApplication {

	@Bean
	public NewTopic asyncTopic() {
		return new NewTopic("async", 1, (short) 1);
	}

	@Bean
	@Primary
	public ChainedKafkaTransactionManager<Object, Object> chainedKafkaTransactionManager(
			KafkaTransactionManager<String, String> ktm, DataSourceTransactionManager dstm) {

		return new ChainedKafkaTransactionManager<>(ktm, dstm);
	}

	@Bean
	public DataSourceTransactionManager dataSourceTransactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	public AfterRollbackProcessor<Object, Object> arp() {
		// no retries, default logging recoverer, no back off
		DefaultAfterRollbackProcessor<Object, Object> arp =
				new DefaultAfterRollbackProcessor<>(new FixedBackOff(0L, 0L));
		arp.setCommitRecovered(false);
		return arp;
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(ListenerApplication.class, args);
	}

}
