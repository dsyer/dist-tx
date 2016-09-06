package com.springsource.open.jms;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.datasource.ConnectionProxy;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jms.connection.SynchedLocalTransactionFailedException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.SessionCallback;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * A {@link DataSource} for components that need to participate in a
 * Spring-managed transaction, where there might also be a JMS transaction with
 * a shared resource (the JDBC {@link Connection}). Internally the
 * {@link DataSource} proxies its {@link Connection} instances and masks off
 * calls to transactional and cleanup methods if a local transaction is already
 * underway. It also registers a synchronization with an ongoing transaction to
 * force a commit of a JMS transaction <b>before</b> the local
 * {@link DataSource} transaction.
 * 
 * @author Dave Syer
 * 
 */
public class JmsTransactionAwareDataSourceProxy extends TransactionAwareDataSourceProxy {

	private static final Log logger = LogFactory.getLog(JmsTransactionAwareDataSourceProxy.class);

	private static final String RESOURCE_KEY = JmsTransactionAwareDataSourceProxy.class.getName()
			+ ".JMS_SYNCHRONIZATION";

	private JmsTemplate jmsTemplate;

	/**
	 * Public setter for a {@link JmsTemplate} that can be used internally to
	 * synchronize a JMS {@link Session} with an ongoing transaction. This is
	 * not really a {@link DataSource} concern, and should be factored out into
	 * another utility class (it's only here for a demo to make a point about
	 * how it works as quickly as possible).
	 * 
	 * @param jmsTemplate
	 */
	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	protected Connection getTransactionAwareConnectionProxy(DataSource targetDataSource) {
		// Deal with the JMS Session:
		maybeRegisterSynchronization();
		return (Connection) Proxy.newProxyInstance(ConnectionProxy.class.getClassLoader(),
				new Class[] { ConnectionProxy.class }, new TransactionAwareInvocationHandler(targetDataSource));
	}

	/**
	 * Invocation handler that delegates close calls on JDBC Connections to
	 * DataSourceUtils making it aware of thread-bound transactions.
	 */
	private class TransactionAwareInvocationHandler implements InvocationHandler {

		private final DataSource targetDataSource;

		private Connection target;

		private boolean closed = false;

		public TransactionAwareInvocationHandler(DataSource targetDataSource) {
			this.targetDataSource = targetDataSource;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on ConnectionProxy interface coming in...

			if (method.getName().equals("equals")) {
				// Only considered as equal when proxies are identical.
				return (proxy == args[0] ? Boolean.TRUE : Boolean.FALSE);
			}
			else if (method.getName().equals("hashCode")) {
				// Use hashCode of Connection proxy.
				return new Integer(System.identityHashCode(proxy));
			}
			else if (method.getName().equals("toString")) {
				// Allow for differentiating between the proxy and the raw
				// Connection.
				StringBuffer buf = new StringBuffer("Transaction-aware proxy for target Connection ");
				if (this.target != null) {
					buf.append("[").append(this.target.toString()).append("]");
				}
				else {
					buf.append(" from DataSource [").append(this.targetDataSource).append("]");
				}
			}
			else if (method.getName().equals("close")) {
				// Handle close method: only close if not within a transaction.
				DataSourceUtils.doReleaseConnection(this.target, this.targetDataSource);
				this.closed = true;
				return null;
			}
			else if (method.getName().equals("setAutoCommit")) {
				// Handle setAutoCommit method: only call if not within a
				// transaction.
				if (DataSourceUtils.isConnectionTransactional(this.target, this.targetDataSource)) {
					return null;
				}
			}
			else if (method.getName().equals("commit")) {
				logger.debug("Committing proxied connection.");
				if (DataSourceUtils.isConnectionTransactional(this.target, this.targetDataSource)) {
					logger.debug("Vetoed commit of proxied connection "
							+ "(probably by a non-Spring aware component in a Spring transaction synchronization).");
					return null;
				}
			}
			else if (method.getName().equals("rollback")) {
				logger.debug("Rolling back proxied connection.");
				if (DataSourceUtils.isConnectionTransactional(this.target, this.targetDataSource)) {
					logger.debug("Vetoed rollback of proxied connection "
							+ "(probably by a non-Spring aware component in a Spring transaction synchronization).");
					return null;
				}
			}

			if (this.target == null) {
				if (this.closed) {
					throw new SQLException("Connection handle already closed");
				}
				if (shouldObtainFixedConnection(this.targetDataSource)) {
					this.target = DataSourceUtils.doGetConnection(this.targetDataSource);
				}
			}
			Connection actualTarget = this.target;
			if (actualTarget == null) {
				actualTarget = DataSourceUtils.doGetConnection(this.targetDataSource);
			}

			if (method.getName().equals("getTargetConnection")) {
				// Handle getTargetConnection method: return underlying
				// Connection.
				return actualTarget;
			}

			// Invoke method on target Connection.
			try {
				Object retVal = method.invoke(actualTarget, args);

				// If return value is a Statement, apply transaction timeout.
				// Applies to createStatement, prepareStatement, prepareCall.
				if (retVal instanceof Statement) {
					if (args != null && args.length == 1) {
						logger.debug("Statement prepared for: " + args[0]);
					}
					DataSourceUtils.applyTransactionTimeout((Statement) retVal, this.targetDataSource);
				}

				return retVal;
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
			finally {
				if (actualTarget != this.target) {
					DataSourceUtils.doReleaseConnection(actualTarget, this.targetDataSource);
				}
			}
		}
	}

	/**
	 * If a transaction is already in flight, register a synchronization to
	 * commit the Session before the main transaction commits.
	 */
	private void maybeRegisterSynchronization() {
		if (TransactionSynchronizationManager.hasResource(RESOURCE_KEY)
				|| !TransactionSynchronizationManager.isSynchronizationActive() || jmsTemplate == null) {
			return;
		}

		TransactionSynchronizationManager.bindResource(RESOURCE_KEY, Boolean.TRUE);

		logger.debug("Registering JMS SessionCallback for synchronization with local transaction");
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {

			@Override
			public void beforeCompletion() {
				TransactionSynchronizationManager.unbindResource(RESOURCE_KEY);
			}

			@Override
			public void beforeCommit(boolean readOnly) {

				jmsTemplate.execute(new SessionCallback<Object>() {
					public Object doInJms(final Session session) throws JMSException {
						try {
							logger.debug("Committing JMS Session");
							session.commit();
						}
						catch (JMSException e) {
							throw new SynchedLocalTransactionFailedException(
									"Failed to commit JMS Session in shared resource synchronization.", e);
						}
						return null;
					}
				});

			}

		});

	}

}
