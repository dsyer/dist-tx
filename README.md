# Distributed transactions in Spring with and without XA

## The sample code

The sample code is packaged as a set of Eclipse / Maven projects. All the projects should work as standalones; there are no dependencies between projects and no parent POM for the Maven metadata. I used m2e to provide the Maven support in Eclipse, so if you have that you are set. If you don't want to use m2e, and you still want to use Eclipse, you can just use your favorite Maven support to re-create the Eclipse metadata. Remember to add the Spring project nature back if you do that. If you don't want to use Maven at all, you need to use the dependency information in the `pom.xml to create a classpath for the build.

All of the samples use Spring to configure the underlying infrastructure (databases and so on), and the configuration is in `src/main/resources/META-INF/spring/*.xml`, with annotations for dependency injection. They also all use embedded database and messaging instances, so you don't need to start any external processes. This is not intended for production use, and I have heard reports of XA problems with several open source RDBMS platforms, including Apache Derby (used in the XA samples without any problems, but this is not an exhaustive test).

Unit tests are used to show the features of each pattern in action. To run them in Eclipse just right click (on a test or on the project) and choose Run As->JUnit Test. All tests should pass. Most use the integration test support from Spring to roll back a transaction automatically, so that the tests can make assertions about the success of the most common failure scenario (full rollback).


### Project `atomikos-db`
This is the XA/JTA example, included for the sake of completeness. It uses Atomikos for the JTA implementation and Apache Derby for the `XADataSource`. The tests show two data sources being updated in the same transaction and then rolling back together.

In the sample code, some `XADataSource` instances are configured like this:

```
<bean id="dataSource" class="com.springsource.open.db.AtomikosDataSourceFactoryBean">
    <property name="uniqueResourceName" value="data-source"/>
    <property name="xaDataSource">
      <bean class="test.jdbc.datasource.DerbyDataSourceFactoryBean">
        <property name="databaseName" value="derbydb" />
      </bean>
    </property>
</bean>
```

The `AtomikosDataSourceFactoryBean` is a simple factory bean that we provide for the sample just to make it easy to configure and handle the life cycle of an Atomikos data source. The `DerbyDataSourceFactoryBean` is provided also for test purposes as a factory for the `XADataSource` provided by Apache Derby. (Setup details for Oracle, MySQL, DB2, and other RDBMSs are in the Atomikos documentation). The main point here is that we are using a connection pool provided by the JTA vendor (Atomikos) and a special `XADataSource` provided by the database vendor (Apache Derby).

The transaction manager is configured like this:

```
<bean id="transactionManager"
    class="org.springframework.transaction.jta.JtaTransactionManager">
    <property name="transactionManager">
      <bean class="com.atomikos.icatch.jta.UserTransactionManager"
        init-method="init" destroy-method="close">
        <property name="forceShutdown" value="true"/>
        <property name="transactionTimeout" value="600"/>
      </bean>
    </property>
    <property name="userTransaction">
      <bean class="com.atomikos.icatch.jta.UserTransactionImp" />
    </property>
  </bean>
</pre>
```

### Project `shared-jms-db`

This is the project showing a shared resource approach to message-driven database updates. The important features of the configuration are described in the article text. The main entry point is the `SynchronousMessage*Tests`unit test .

The `JmsTransactionAwareDataSourceProxy` that is used to synchronize the JMS `Session` with the Spring transaction is an extension of the Spring `TransactionAwareDataSourceProxy`. It might not be the best way to implement this pattern, but it is the quickest and most direct that works for the purpose of this example.

One other thing that is worth mentioning is the use of ActiveMQ with an embedded broker to keep the test self-contained (the broker URL starts with `vm://`). A distributed system would probably have more than one participant in the messaging, and multiple brokers would be needed, but they should all be embedded and used with `async=false` to make the shared resource pattern work. The embedded brokers in the various processes that comprise a system communicate through network connectors.

It might help if we summarize the ActiveMQ specific features of this pattern, just to be clear what we have done. The main points to note are:

* The embedded broker with `async=false` so that the JMS persistence happens in the same thread as the main transaction.

* The use of the `JDBCPersistenceAdapter` in the broker, where the injected `DataSource` is a special proxy that ensures that transaction boundaries are respected.

* In a distributed system, unlike in the samples, synonyms (or equivalent) would have to be used to link the ActiveMQ data to the business data in the RDBMS platform.

* A distributed system would also have to allow all the embedded brokers to communicate with each other using a network connector. This is standard practice for large messaging installations anyway, but to use the shared resource pattern it is mandatory. See the <a href="http://activemq.apache.org/topologies.html">ActiveMQ topologies documentation</a> for more details.

### Project `best-jms-db`

This is the project showing a Best Efforts 1PC approach to message-driven database updates. The important features of the asynchronous pattern are described in the text above. The main entry point is the unit test `AsynchronousMessageTriggerAndRollbackTests`. There is also a version of the `SynchronousMessageTriggerAndRollbackTests` from the `shared-jms-db` sample showing that the synchronous example also works just fine with this pattern.

### Project `best-db-db`

This is the project showing a Best Efforts 1PC approach to linked database updates. The important features are described in the article text. The main entry point is the unit test MultipleDatasourceTests.



