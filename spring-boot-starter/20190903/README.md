# DataStax Java Driver Spring Boot Starter
This Spring Boot Starter can be used for configuring and injecting a Datastax Java Driver Session
into a Spring Boot application. Simply include the jarfile with your application and use Spring's
`@Autowired` annotation (or `@javax.inject.Inject`) to inject the desired Java Driver Beans provided
by this module.

## DataStax Labs Installation
These are the instructions for installing and using the DataStax Java
Driver Spring Boot Labs preview.

For the downloadable tarball version of the Spring Boot Starter
preview, refer to the [DataStax Labs] website and download DataStax
Java Driver Spring Boot Starter. To install from the bundle:

1. Download and extract the bundle
2. Using a console/terminal, change into the extracted location
3. Execute the `install_maven.sh` script in the extracted location
   ```sh
   > cd datastax-java-driver-spring-boot-starter-1.0.0.20190903-LABS
   > ./install_maven.sh
   ```

This will install all of the Spring Boot Starter artifacts into your local Maven repository so that
you can build your own Spring Boot applications with the snippets below. See the README.md in the
demo directory for more on the demo project using this Spring Boot Starter. See the README-tests.md
for more on integration testing with the Spring Boot Starter.

The use of the software described here is subject to the
[DataStax Labs Terms].

## Maven Coordinates
To include this module in a Maven project, add the dependency in your application's POM file:

```xml
  <dependencies>
    <dependency>
      <groupId>com.datastax.oss</groupId>
      <artifactId>java-driver-spring-boot-starter</artifactId>
      <version>1.0.0.20190903-LABS</version>
    </dependency>
  </dependencies>
```

## Injecting Java Driver Session Bean
To inject an instance of a Driver Session into you application, simply use Spring's `@Autowired`
annotation in your Spring components and services:

```java
@Component
public class MyService {

  @Autowired
  private CqlSession cqlSession;

  public ResultSet execute(String query) {
    return cqlSession.execute(query);
  }
}
```
or use the JSR330 `@Inject` annotation:
```java
@Component
public class MyService {

  @Inject
  private CqlSession cqlSession;

  public ResultSet execute(String query) {
    return cqlSession.execute(query);
  }
}
```

## Configuring The Driver
With no additional configuration, Spring will create a driver Session configured with driver
defaults. You can read more about [driver configuration][Driver Config Docs], and more about
[driver configuration defaults][Default Driver Config Reference Docs]. In most cases, you
will want to change some of the default values to match your environment (for example, contact
points, keyspace, local datacenter, etc).

### Override Driver Defaults
Using Spring's [external configuration][Spring Externalized Configuration], it is possible to set
custom values for any driver configuration parameters you may need to override in your application.
For a simple example, if you want to change the default local datacenter, you could use the
following `application.properties` in your application:

```properties
datastax-java-driver.basic.load-balancing-policy.local-datacenter = Cassandra
```
The same override could be made in `application.yml`:
```yaml
datastax-java-driver:
  basic.load-balancing-policy:
    local-datacenter: Cassandra
```
The property keys need to match the driver configuration keys in order to be used. All driver
property overrides must start with `datastax-java-driver`, and use the `.` character as a _path_
separator. Again, these match the property names in the
[driver configuration reference][Default Driver Config Reference Docs]. You only have to provide
values for things you wish to override.

#### Overriding List Values
Overriding List values in `application.properties` requires you to use numbered indexes for the
property name. This is to avoid the ambiguity of the value being a single value with a comma in the
value as opposed to a comma-separated list value. For example, setting contact points would look
like this:

```properties
datastax-java-driver.basic.contact-points[0] = 127.0.0.1:9042
datastax-java-driver.basic.contact-points[1] = 127.0.0.2:9042
```
For YML configuration, it's more straightforward as you can use the standard YML list syntax:
```yaml
datastax-java-driver:
  basic.contact-points:
    - 127.0.0.1:9042
    - 127.0.0.2:9042
```

### Override examples
See [application.properties.example][example properties file override] as an example of a properties
file and [application.yml.example][example yml file override] as an example of a YML file.

### Spring Profiles
You can also make use of [Spring Profiles][Spring Boot Profile Docs] in your application, putting
common configuration properties in `application.properties` (or `application.yml`) and deployment
specific configuration in other profiles.

#### Create Driver Configuration Overrides for "Dev" and "QA"
If you have a `dev` environment with different contact points than your `qa` environment, but both
share the same local datacenter name, you can have this in `application.properties`:

```properties
datastax-java-driver.basic.load-balancing-policy.local-datacenter = Cassandra
```

this in `application-dev.properties`:
```properties
datastax-java-driver.basic.contact-points[0] = 127.0.0.1:9042
datastax-java-driver.basic.contact-points[1] = 127.0.0.2:9042
```

and this in `application-qa.properties`:
```properties
datastax-java-driver.basic.contact-points[0] = 10.0.1.1:9042
datastax-java-driver.basic.contact-points[1] = 10.0.1.2:9042
```

When running your application, the contact points used by the driver would be determined by which
profile is active. Also, remember that if properties exist in `application.properties` as well as
an active Spring profile, the profile values will take precedence. See
[Spring's documentation][Spring Externalized Configuration] for precedence order.

#### Spring Profile Caveats and Driver Execution Profiles
1. If you enable multiple Spring profiles, and more than one profile defines a property, the last
one activated wins. Using the above example, if you run your application like this:
   ```sh
   java -Dspring.profiles.active=dev,qa myApplication.jar
   ```
   The Driver Session bean will use the contact points in `application-qa.properties`.
   ```sh
   java -Dspring.profiles.active=qa,dev myApplication.jar
   ```
   If you change the order of the active profiles, the Driver Session bean will use the contact
   points in `application-dev.properties`.

1. Spring profiles are not the same as, and do not map to,
[Driver Execution Profiles][Driver Execution Profile Docs]. Driver Execution Profiles are fully
supported and can be defined in any Spring properties files. Any driver configuration that you want
to set in a driver execution profile will just need `profiles` and the profile name as _paths_ in
the property name. For example, if you want a **slow** profile that increases request timeouts from
2 seconds (the default) to 30 seconds, put this in `application.properties`:
   ```properties
   datastax-java-driver.profiles.slow.basic.request.timeout = 30 seconds
   ```
   The general format for driver execution profile configuration of property **someProp** is:
   ```properties
   datastax-java-driver.profiles.myProfile.someProp = <profile specific value>
   ```
   where **myProfile** is your execution profile name and **someProp** is whatever driver property
   you want to change within the execution profile.

## Admin Session
Sometimes it is necessary to setup your Cassandra database before your application starts performing
its normal operations. An example of this would be creating the keyspace that your application will
use if it doesn't already exist. If you try to create a `CqlSession` that has a configured keyspace,
and the keyspace doesn't exist, the session will fail to be instantiated. To handle this, there is
an _admin session_ that is created with the same Spring configuration that your application will
use, but without a keyspace configured. This will allow the _admin session_ to connect where the
normal application session would fail.

The _admin session_ performs any defined admin operations that are provided as `AdminSessionHook`
implementations. The API for the Hook simply has one method:

```java
void executeOnAdminSession(CqlSession cqlSession)
```

As long as your implementations of this interface are picked up via Spring's Component scan, they
will be added to the `DriverAutoConfiguration` and invoked as soon as the _admin session_ is
created. An example of setting up your application's keyspace using this hook would be:

```java
@Component
public class KeyspaceCreator implements AdminSessionHook {

  // Get the application keyspace from Spring config
  @Value("${datastax-java-driver.basic.session-keyspace}")
  private String keysapce;

  @Override
  public void executeOnAdminSession(CqlSession cqlSession) {
    // create the keyspace when the hook is invoked
    cqlSession.execute(
        String.format(
            "CREATE KEYSPACE IF NOT EXISTS %s "
                + "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}",
            keysapce));
  }
}
```

It is not possible to inject the _admin session_ directly into your application or tests.
Interaction with the _admin session_ can only be done through the `AdminSessionHook` interface.

When implementing `AdminSessionHook` keep in mind the following guidelines:

1. Your implementation should perform the necessary operations synchronously. If you really need
to dispatch operations asynchronously, make sure that all of them are properly terminated when
the call to the `executeOnAdminSession` method returns, because the admin session is closed 
immediately after all the hooks are executed.
2. Your implementation should not throw; if a `RuntimeException` is thrown from 
`executeOnAdminSession`, that exception will be caught and logged at `ERROR` level along with its
stack trace, then the execution flow will skip to the next hook.

[Driver Config Docs]: https://docs.datastax.com/en/developer/java-driver/4.1/manual/core/configuration/
[Default Driver Config Reference Docs]: https://docs.datastax.com/en/developer/java-driver/4.1/manual/core/configuration/reference/
[Spring Externalized Configuration]: https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html
[Spring Boot Profile Docs]: https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-profile-specific-properties
[Driver Execution Profile Docs]: https://docs.datastax.com/en/developer/java-driver/4.1/manual/core/configuration/#execution-profiles
[example properties file override]: starter/src/main/resources/application.properties.example
[example yml file override]: starter/src/main/resources/application.yml.example
[DataStax Labs]: https://downloads.datastax.com/#labs
[DataStax Labs Terms]: https://www.datastax.com/terms/datastax-labs-terms
