package com.springsource.open.foo;

import org.apache.kafka.clients.producer.Producer;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaOperations.ProducerCallback;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FailureSimulator {

	private KafkaTemplate<Object, String> kafkaTemplate;

	@Autowired
	public void setJmsTemplate(KafkaTemplate<Object, String> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	/**
	 * Causes Kafka to fail on commit, as if the middleware has failed.
	 */
	public void simulateMessageSystemFailure() {

		// Simulate a message system failure before the main transaction
		// commits...
		kafkaTemplate.execute(new ProducerCallback<Object, String, Void>() {

			@Override
			public Void doInKafka(Producer<Object, String> producer) {
				try {
					producer.abortTransaction();
				}
				catch (Exception e) {
					// swallow it
					e.printStackTrace();
				}
				return null;
			}
		});

	}

	/**
	 * Just throws a {@link DataIntegrityViolationException}.
	 */
	public void simulateBusinessProcessingFailure() {
		throw new DataIntegrityViolationException("Simulated failure.");
	}

	@AfterReturning("execution(* *..*Handler+.handle(String)) && args(msg)")
	public void maybeFail(String msg) {
		if (msg.contains("fail")) {
			if (msg.contains("partial")) {
				simulateMessageSystemFailure();
			}
			else {
				simulateBusinessProcessingFailure();
			}
		}
	}

}
