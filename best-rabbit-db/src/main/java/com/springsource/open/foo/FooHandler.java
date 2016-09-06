package com.springsource.open.foo;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class FooHandler implements Handler {

	private static final Log log = LogFactory.getLog(FooHandler.class);
	private final JdbcTemplate jdbcTemplate;
	private final AtomicInteger count = new AtomicInteger(0);

	public FooHandler(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@RabbitListener(queues="async")
	public void handle(String msg) {

		log.debug("Received message: [" + msg + "]");

		Date date = new Date();
		jdbcTemplate.update(
				"INSERT INTO T_FOOS (ID, name, foo_date) values (?, ?,?)", count.getAndIncrement(), msg, date);

		log.debug(String
				.format("Inserted foo with name=%s, date=%s", msg, date));
		
	}

	public void resetItemCount() {
		count.set(0);
	}

	public int getItemCount() {
		return count.get();
	}

}
