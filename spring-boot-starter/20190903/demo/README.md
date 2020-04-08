# DataStax Java Driver Spring Demo
This is a demo project that is using DataStax Java Driver Spring Boot Starter together with DataStax
[ObjectMapper]. It uses Spring Starter by importing the dependency:

```xml
<dependency>
    <groupId>com.datastax.oss</groupId>
    <artifactId>java-driver-spring-boot-starter</artifactId>
    <version>1.0.0.20190903-LABS</version>
</dependency>
```
Thanks to the starter mechanism, the `CqlSession` is automatically created. It can be injected by using Spring `@Autowired`:

```java
@Autowired CqlSession cqlSession
```

## Prerequisites
This demo application has one prerequisite: running Cassandra on localhost.
If you wish to connect to a Cassandra instance that is remote, or is not bound to localhost, please alter the
application.yml cassandra configuration:

```yaml
datastax-java-driver:
  basic.contact-points:
    - 127.0.0.1:9042
  basic.session-keyspace: test
  basic.load-balancing-policy:
    local-datacenter: datacenter1
```

**IMPORTANT: Pay special attention to the Driver configuration here, in particular to the contact points
and the local datacenter name. Ensure that your Cassandra instance is accessible at the contact points
you provide, and that the local datacenter name matches.**

## How to start the Demo App

To start the Demo Application run the [maven] command:

`mvn spring-boot:run`

Remember that you need to have running Cassandra instance on your localhost (see [Prerequisites](#Prerequisites) section)

## Object Mapper
The application is using DataStax Object Mapper to map product entity from cql to java objects.
To make it work this dependency is needed:
```xml
<dependency>
    <groupId>com.datastax.oss</groupId>
    <artifactId>java-driver-mapper-processor</artifactId>
    <version>${datastax.java.driver.version}</version>
</dependency>
```

Object Mapper is mapping Product table: `product(id int PRIMARY KEY, description text)` to Product Entity:

```java
@Entity
public class Product {

  @PartitionKey private Integer id;
  private String description;

  public Product() {}

  public Product(Integer id, String description) {
    this.id = id;
    this.description = description;
  }
  // getters and setters omitted
}
```

The generated `ProductDao` is constructed and injected into Spring DI container by `MapperConfiguration` class.

## AdminSessionHook Mechanism
This application is using the [AdminSessionHook] mechanism to provide an automatic way of creating
the`test` keyspace and the `product` table that are used by the `ProductDao` interface.

The `KeyspaceTableInitHook` class is performing keyspace and table initialization inside the
`executeOnAdminSession` callback.

This logic is executed on the special admin CQL session (a session without keyspace explicitly set)
and is guaranteed to be executed _before_ the main `cqlSession` bean is initialized (see
[AdminSessionHook documentation][AdminSessionHook]).

## REST API
The REST API is exposed by the `ProductController`.
You can send requests to exercise the API using curl with the following commands:

```bash
# create an entity
curl -d '{"id":1, "description":"i was created by curl"}' -H "Content-Type: application/json" -X POST http://localhost:8080/product/

# retrieve
curl -X GET http://localhost:8080/product/1

# delete
curl -X DELETE http://localhost:8080/product/1

# retrieve - NOT_FOUND
curl -X GET http://localhost:8080/product/1
```

Please note that `ProductController` is exposing `Product` entity by mapping it to `ProductDto` - it is important to not expose
Entity by the REST API directly if you want to evolve `Product` entity class independently from your API.

## Testing
The DataStax Spring Boot starter provides an [integration test module] that ships with embedded Cassandra that could be used in integration tests out of the box.
To use it you need to add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.datastax.oss</groupId>
    <artifactId>java-driver-spring-boot-starter-test</artifactId>
    <version>${datastax.java.driver.spring.version}</version>
    <scope>test</scope>
</dependency>
```

The starter provides its own [AdminSessionHook] that is able to create the keyspace specified as the
`datastax-java-driver.basic.session-keyspace` setting in your `application.yml` file
(see spring documentation about inheriting settings from properties files).

To see the usage of this automatic hook in an integration test that exercises the REST API see the [ProductControllerIT]
(note that the `classes` attribute with explicit `TestKeyspaceCreator.class` was added):

```java
@RunWith(SpringRunner.class)
@SpringBootTest(
  classes = {TestKeyspaceCreator.class, DriverAutoConfiguration.class, MainApplication.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class ProductControllerIT extends CcmSpringRuleTestBase {
```

If you have your own hook that creates test keyspace you can use it instead (for example `KeyspaceTableInitHook`).

## Profiles in Tests
If you want to provide your own profile for the purpose of integration testing you can use `@ActiveProfiles("integration")`:

```java
@ActiveProfiles("integration")
public class ProductServiceIT extends CcmSpringRuleTestBase {
```

NOTE: Any settings defined in `application-integration.yml` will override the same settings in your `application.yml`
(see [ProductServiceIT]`#should_load_settings_from_integration_profile` test).

## Packages in the demo project:
- api: Contains the DTO (Data Transfer Object) classes used by the API
- controller: Contains the controller that are exposing DTOs via REST API.
- mapper: All classes needed for Java Driver Object Mapper. It contains `@Mapper`, `@Dao`, and `@Entity`.
- persistence:  Provides a `ProductDao` bean that is injected into the Spring DI Container.
                Contains an implementation of `AdminSessionHook` with keyspace and table initialization.
- service: Contains ProductService that performs the mapping from Entity to DTO.
           It has also the `@ConfigurationProperties` `@Component` that demonstrates loading custom setting from `application.yml`.

[ObjectMapper]: https://docs.datastax.com/en/developer/java-driver/4.1/manual/mapper/
[ProductControllerIT]: src/test/java/com/datastax/oss/spring/demo/controller/ProductControllerIT.java
[ProductServiceIT]: src/test/java/com/datastax/oss/spring/demo/service/ProductServiceIT.java
[AdminSessionHook]: ../README.md#admin-session
[integration test module]: ../test-infra
[maven]: https://maven.apache.org/
