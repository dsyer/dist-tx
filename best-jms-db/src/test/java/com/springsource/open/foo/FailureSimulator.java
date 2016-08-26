package com.springsource.open.foo;

import static org.junit.Assert.assertTrue;

import javax.jms.JMSException;
import javax.jms.Session;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jms.connection.SessionProxy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.SessionCallback;

@Aspect
public class FailureSimulator {

	private JmsTemplate jmsTemplate;
	
	@Autowired
	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	/**
	 * Causes the JMS session to fail on commit, as if the middleware has
	 * failed. Can be used to simulate failure of JMS independent of business
	 * processing, causing duplicate messages even if best efforts 1PC is used.
	 */
	public void simulateMessageSystemFailure() {

		// Simulate a message system failure before the main transaction
		// commits...
		jmsTemplate.execute(new SessionCallback() {
			public Object doInJms(Session session) throws JMSException {
				try {
					assertTrue("Not a SessionProxy - wrong spring version?",
							session instanceof SessionProxy);
					((SessionProxy) session).getTargetSession().rollback();
				} catch (JMSException e) {
					throw e;
				} catch (Exception e) {
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
			} else {
				simulateBusinessProcessingFailure();
			}
		}		
	}

}
