package com.springsource.open.jms;

import javax.sql.DataSource;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

public class FailureSimulator {

	private static final Log logger = LogFactory.getLog(FailureSimulator.class);

	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private BrokerService brokerService;
	
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * Causes the JMS session to fail on commit, as if the middleware has
	 * failed. Can be used to simulate failure of JMS independent of business
	 * processing, causing duplicate messages even if best efforts 1PC is used.
	 * @throws Exception 
	 */
	public void simulateMessageSystemFailure() throws Exception {
		
		logger.debug("ACTIVEMQ_MSGS: "+jdbcTemplate.queryForList("SELECT * FROM ACTIVEMQ_MSGS"));
		// Simulate a message system failure before the main transaction
		// commits...
		brokerService.getTransportConnectors().get(0).stop();
		
	}

	/**
	 * Just throws a {@link DataIntegrityViolationException}.
	 */
	public void simulateBusinessProcessingFailure() {
		throw new DataIntegrityViolationException("Simulated failure.");
	}
	
}
