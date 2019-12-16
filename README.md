# Distributed transactions in Spring with and without XA

## The sample code

This project has updates to the sample code from the [JavaWorld article](http://www.javaworld.com/article/2077963/open-source-tools/distributed-transactions-in-spring--with-and-without-xa.html) on distributed transactions from 2008. It is packaged as a set of Maven projects. All the projects should work as standalones; there are no dependencies between projects and no parent POM for the Maven metadata. If you don't want to use Maven at all, you need to use the dependency information in the `pom.xml` to create a classpath for the build.

All of the samples use Spring to configure the underlying infrastructure (databases and so on), and the configuration is in `src/main/java/**`, with annotations for dependency injection, and using Spring Boot autoconfiguration wherever possible. They also all use embedded database and messaging instances, so you don't need to start any external processes (except for the RabbitMQ example). This is not intended for production use, and I have heard reports of XA problems with several open source RDBMS platforms, including Apache Derby (used in the XA samples without any problems, but this is not an exhaustive test).

Unit tests are used to show the features of each pattern in action. To run them in Eclipse just right click (on a test or on the project) and choose Run As->JUnit Test. All tests should pass. Most use the integration test support from Spring to roll back a transaction automatically, so that the tests can make assertions about the success of the most common failure scenario (full rollback).


### Project `atomikos-db`
This is the XA/JTA example, included for the sake of completeness. It uses Atomikos for the JTA implementation and Apache Derby for the `XADataSource`. The tests show two data sources being updated in the same transaction and then rolling back together.

### Project `best-jms-db`

This is the project showing a Best Efforts 1PC approach to message-driven database updates. The important features of the asynchronous pattern are described in the text above. The main entry point is the unit test `AsynchronousMessageTriggerAndRollbackTests`. There is also a version of the `SynchronousMessageTriggerAndRollbackTests` from the `shared-jms-db` sample showing that the synchronous example also works just fine with this pattern.

### Project `best-rabbit-db`

This is the project showing a Best Efforts 1PC approach using AMQP over RabbitMQ. The features and the tests are the same as for the `best-jms-db` sample.

### Project `best-db-db`

This is the project showing a Best Efforts 1PC approach to linked database updates. The important features are described in the article text. The main entry point is the unit test `MultipleDatasourceTests`.

### Project `shared-jms-db`

This is the project showing a shared resource approach to message-driven database updates. It only works with a very old version of ActiveMQ (5.1) and Spring Boot (1.4). The important features of the configuration are described in the article text. The main entry point is the `SynchronousMessage*Tests`unit test .

The `JmsTransactionAwareDataSourceProxy` that is used to synchronize the JMS `Session` with the Spring transaction is an extension of the Spring `TransactionAwareDataSourceProxy`. It might not be the best way to implement this pattern, but it is the quickest and most direct that works for the purpose of this example.

One other thing that is worth mentioning is the use of ActiveMQ with an embedded broker to keep the test self-contained (the broker URL starts with `vm://`). A distributed system would probably have more than one participant in the messaging, and multiple brokers would be needed, but they should all be embedded and used with `async=false` to make the shared resource pattern work. The embedded brokers in the various processes that comprise a system communicate through network connectors.

It might help if we summarize the ActiveMQ specific features of this pattern, just to be clear what we have done. The main points to note are:

* The embedded broker with `async=false` so that the JMS persistence happens in the same thread as the main transaction.

* The use of the `JDBCPersistenceAdapter` in the broker, where the injected `DataSource` is a special proxy that ensures that transaction boundaries are respected.

* In a distributed system, unlike in the samples, synonyms (or equivalent) would have to be used to link the ActiveMQ data to the business data in the RDBMS platform.

* A distributed system would also have to allow all the embedded brokers to communicate with each other using a network connector. This is standard practice for large messaging installations anyway, but to use the shared resource pattern it is mandatory. See the <a href="http://activemq.apache.org/topologies.html">ActiveMQ topologies documentation</a> for more details.

