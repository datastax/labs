# DataStax Java Driver Spring Boot Starter Test Module
This test module can be used to assist in writing integration tests for projects that use the
[DataStax Java Driver Spring Boot Starter]. It provides a framework that can spin up a test
instance of Cassandra (using Docker or [CCM]) and automatically configures a Java Driver CqlSession
object that can connect to the test container. It is primarily intended for writing Integration
tests for situations where it is not possible or desirable to maintain a dedicated Cassandra DB.

## Prerequisites
In order to use this test module, you will have to have [Docker] and/or [CCM] installed. You can
install both if you wish, and your can use both in your tests if you have them both installed.

## Maven Coordinates
To use this module in your application's tests, add the following to your Maven pom.xml:

```xml
<dependency>
    <groupId>com.datastax.oss</groupId>
    <artifactId>java-driver-spring-boot-starter-test</artifactId>
    <scope>test</scope>
    <version>1.0.0.20190903-LABS</version>
</dependency>
```

## Test Cassandra Resources
This module provides two types of Cassandra resources that can be automatically instantiated, as
well as automatically configuring the `@Autowired` test Session so that it can successfully connect
to the test Cassandra instance: Docker and [CCM]. For Docker, this project uses the [Testcontainers]
project to manage the container from within Java code, so you only need Docker to be installed and
running. For CCM, this project leverages the Datastax Java Driver test infrastructure project to
manage your Cassandra test instances (see the [CCM] project for details).

To write an integration test using either Docker or CCM, you need to extend one of the provided Base
test classes and annotate your test appropriately.

Container | Cassandra Version | Extend Base Test Class
----------|-------------------|----------------
[Docker][docker package] | Cassandra 2.1 | com.datastax.oss.spring.configuration.docker.Cassandra21DockerRuleTestBase
[Docker][docker package] | Cassandra 2.2 | com.datastax.oss.spring.configuration.docker.Cassandra22DockerRuleTestBase
[Docker][docker package] | Cassandra 3.0 | com.datastax.oss.spring.configuration.docker.Cassandra30DockerRuleTestBase
[Docker][docker package] | Cassandra 3.11 | com.datastax.oss.spring.configuration.docker.Cassandra311DockerRuleTestBase
[CCM][CcmSpringRuleTestBase] | (any version) | com.datastax.oss.spring.configuration.ccm.CcmSpringRuleTestBase

Once you have chosen the base test class to extend, you need to specify a few test annotations.

### Integration Test Annotations
For your Spring Boot Test class, you will need to annotate your test with Spring's `SpringRunner`
class as the JUnit Runner. You will also need to add Springs's `@SpringBootTest` annotation and
specify the following classes:
1. The Datastax Java Driver Spring Boot starter configuration class
1. The [AdminSessionHook] class ([TestKeyspaceCreator]) that will create your application's Keyspace
   _or_ your own implementation (see [Creating the Test Keyspace](#creating-the-test-keyspace))
1. Your own application's Spring Boot main class.

Example test class using Docker to provide Cassandra 3.11:
```java
@RunWith(SpringRunner.class)
@SpringBootTest(
  classes = {TestKeyspaceCreator.class,
      DriverAutoConfiguration.class,
      MyApplication.class}
)
public class MyApplicationTest extends Cassandra311DockerRuleTestBase {
  // tests
}
```
Example test class using CCM:
```java
@RunWith(SpringRunner.class)
@SpringBootTest(
  classes = {TestKeyspaceCreator.class,
      DriverAutoConfiguration.class,
      MyApplication.class}
)
public class MyApplicationTest extends CcmSpringRuleTestBase {
  // tests
}
```
### Creating the Test Keyspace
Since testing with this module is using dynamic Cassandra instances, the keyspace for your
application will not exist in the Cassandra container at startup. This presents a small problem when
Spring tries to create the CqlSession bean in your application code as it will fail to initialize if
the keyspace does not exist. To fix this problem, the Datastax Java Driver Spring Boot Starter has
an [AdminSessionHook] interface that you can implement to execute database setup steps that only
need to happen once during application startup.

This module provides an [AdminSessionHook] implementation, [TestKeyspaceCreator]. If your test class
includes this in the `@SpringBootTest` annotation class list, it will create the keyspace specified
in your Spring environment (`datastax-java-driver.basic.session-keyspace`) as soon as the Cassandra
test instance is spun up. The implementation uses a special _admin session_ that has no keyspace
configuration so that it can be initialized for this purpose.

You can provide your own implementation of [AdminSessionHook] if you wish. If your provided hook is
able to create your application's keyspace, you don't have to include [TestKeyspaceCreator] in your
tests. You can provide multiple hook implementations as well, however you should **pay special
attention to the ordering of the hooks/classes specified**.

## Test Session Configuration
The majority of the Session configuration will likely be inherited from your Spring
`application.yml` in your application's resources. When testing in different environments, some of
the Session settings will need to be overridden to match the environment (i.e. `qa`, `integeration`,
etc). The differences are usually configured in Spring profiles (i.e. `application-qa.yml`,
`application-integration.yml`, etc). However, there are two Driver configuration settings that must
**NOT** be overridden for integration testing using this module: Contact Points and Local
Datacenter.

### Contact Points and Local Datacenter Configuration
In order for the Spring `@Autowired` Driver Session object to be correctly instantiated for testing,
it has to be configured with the correct Contact Points and Local Datacenter for the Cassandra
cluster being tested against. Since this module spins up Cassandra dynamically, these values are set
in the framework by activating the [integration-cassandra] Spring profile which determines them at
runtime. It looks like this:

```yaml
datastax-java-driver:
  basic.contact-points:
    - 127.0.0.1:${cassandra-port}
  basic.load-balancing-policy:
    local-datacenter: ${cassandra-datacenter}
```
The `cassandra-port` and `cassandra-datacenter` values are populated by Spring when the tests are
executed. Again, this is done using Spring profile activation ([integration-cassandra]), so you
**MUST NOT**:
1. Specify `datastax-java-driver.basic.contact-points` in any Spring profiles that are **activated
   during tests** (see NOTE below)
1. Specify `datastax-java-driver.basic.load-balancing-policy.local-datacenter` in any Spring
   profiles that are **activated during tests** (see NOTE below)
1. Create your own `application-integration-cassandra.yml` or
   `application-integration-cassandra.properties` Spring profile.

NOTE: Spring profiles that are activated during tests include any profiles (YAML files or Properties
files) used in Spring's `@ActiveProfiles` annotations, or any profiles that may be activated on the
command line using `-Dspring.profiles.active`.

It is fine (and expected) that you would have these values provided/overridden in your normal
`application.yml`, or any production specific Spring profiles (ex `application-prod.yml`). Regular
production profile values are overridden by test profiles during integration tests. The restrictions
only apply to Spring profiles that are activated specifically for tests.

[DataStax Java Driver Spring Boot Starter]: ../
[Docker]: https://docs.docker.com/install/
[CCM]: https://github.com/riptano/ccm
[Testcontainers]: https://www.testcontainers.org/
[docker package]: src/main/java/com/datastax/oss/spring/configuration/docker
[TestKeyspaceCreator]: src/main/java/com/datastax/oss/spring/configuration/TestKeyspaceCreator.java
[CcmSpringRuleTestBase]: src/main/java/com/datastax/oss/spring/configuration/ccm/CcmSpringRuleTestBase.java
[integration-cassandra]: src/main/resources/application-integration-cassandra.yml
[AdminSessionHook]: ../#admin-session
