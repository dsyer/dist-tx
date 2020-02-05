package com.springsource.open.foo;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FooHandler implements Handler {

	private static final Log log = LogFactory.getLog(FooHandler.class);

	private final JdbcTemplate jdbcTemplate;

	private final AtomicInteger count = new AtomicInteger(0);

	public FooHandler(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	@KafkaListener(id = "group", topics = "async", autoStartup = "false")
	@Transactional
	public void handle(@Payload String msg, @Header(KafkaHeaders.OFFSET) long offset) {

		log.debug("Received message: [" + msg + "]");

		Date date = new Date();
		jdbcTemplate.update("INSERT INTO T_FOOS (ID, name, foo_date) values (?, ?,?)", count.getAndIncrement(), msg,
				date);

		int updated = jdbcTemplate.update("UPDATE T_OFFSETS set topic=?, part=0, offset=? where ID=0", "async",
				offset);
		if (updated < 1) {
			jdbcTemplate.update("INSERT into T_OFFSETS (ID, topic, part, offset) values (?,?,?,?)", 0, "async", 0,
					offset);
		}

		log.debug(String.format("Inserted foo with name=%s, date=%s, offset=%d", msg, date, offset));

	}

	@Override
	public void resetItemCount() {
		count.set(0);
	}

	@Override
	public int getItemCount() {
		return count.get();
	}

}
