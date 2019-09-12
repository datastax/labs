# DataStax Graph - System API Usage Examples

## Core Engine

#### Creating a Core Engine Graph

Core Engine graphs are created with `SimpleStrategy` by default:
```
system.graph("test_core").
    ifNotExists().
    create()
```

Or you can use `NetworkTopologyStrategy` by setting the replication explicitly.
Make sure that the `DC_NAME` matches your DC name as listed by `nodetool status`.
```
system.graph("test_core").
    ifNotExists().
    withReplication("{'class': 'NetworkTopologyStrategy', '<DC_NAME>': <REPLICATION_FACTOR>}").
    create()
```

Core Engine graphs are created with `durableWrites` set to `true` by default.
If necessary, user can specify the setting when creating the graph:
```
system.graph("test_core").
    ifNotExists().
    withReplication("{'class': 'NetworkTopologyStrategy', '<DC_NAME>': <REPLICATION_FACTOR>}").
    andDurableWrites(false).
    create()
```
However, it is **NOT recommended** to set `durableWrites` to `false`.

#### Convert an existing non-graph keyspace to a Core Engine Graph
```
system.graph("testExisting").
    fromExistingKeyspace().
    create()
```

#### Dropping/Truncating a Core Engine Graph
`drop()` will drop the underlying keyspace and the tables, whereas `truncate()` will only delete their contents.
```
system.graph("test_core").
    ifExists().
    drop()
```

```
system.graph("test_core").
    ifExists().
    truncate()
```

## Classic Engine
#### Creating a Classic Engine Graph
```
system.graph('testClassic').
    replication("{'class' : 'NetworkTopologyStrategy', '<DC_NAME>' : <RF> }").
    systemReplication("{'class' : 'NetworkTopologyStrategy', '<DC_NAME>' : <RF> }").
    option('graph.schema_mode').
    set('Production').
    engine(Classic).
    create()
```

#### Dropping a Classic Engine Graph
```
system.graph('testClassic').
    ifExists().
    drop()
```

## Retrieving information from a Graph

#### All available Graphs

```
gremlin> system.graphs()
==>test_core
==>testClassic
```

#### Verbose information for all Graphs (Name, Engine, RF)

```
gremlin> system.list()
==>Name: test_core | Engine: Core | Replication: {SearchGraph=1, class=org.apache.cassandra.locator.NetworkTopologyStrategy}
==>Name: testClassic | Engine: Classic | Replication: {SearchGraph=1, class=org.apache.cassandra.locator.NetworkTopologyStrategy}
```

#### Schema definition of a particular Graph

```
gremlin> system.graph("test_core").describe()
==>system.graph('test_core').ifNotExists().withReplication("{'class': 'org.apache.cassandra.locator.NetworkTopologyStrategy', 'SearchGraph': '1'}").andDurableWrites(true).coreEngine().create()
```



# Core Engine Graph - Schema API Usage Examples
All examples are for `Core` graphs.
Remember that once you have created your graph you will need to alias it in the console:
```
:remote config alias g test_core.g
```

## Creating a Vertex/Edge Label 
#### Creating a Vertex Label

The following example creates a vertex label `person`, having a partition key on `name` and `ssn`, using `age` as a clustering column. The underlying table will be named `person`.
```
schema.vertexLabel('person').
    partitionBy('name', Text).
    partitionBy('ssn', Text).
    clusterBy('age', Int).
    property('address', Text).
    property('coffeePerDay', Int).
    create()
```

And here we create a vertex label `software`, using `name` as partition key, `version` and `lang` as clustering columns. `temp` ends up being a regular and `static_property` a static column.
```
schema.vertexLabel('software').
    partitionBy('name', Text).
    clusterBy('version', Int).
    clusterBy('lang', Text).
    property('temp', Text).
    property('static_property', Text, Static).
    create()
```

#### Creating a Vertex Label with a custom table name
When creating a vertex label, the default table name will be equal to the label name. It is possible to override the default table name by using `.tableName(<name>)` as shown in the below example:
```
schema.vertexLabel('personCustom').
    tableName('personTable').
    partitionBy('name', Text).
    partitionBy('ssn', Text).
    clusterBy('age', Int).
    property('address', Text).
    property('coffeePerDay', Int).
    create()
```

#### Creating a Vertex Label from an existing Table
Assuming we have the following existing non-vertex/edge table:
```
CREATE TABLE test_core.book_table (title varchar, isbn int, pages int, 
PRIMARY KEY ((title), isbn)) 
WITH CLUSTERING ORDER BY (isbn ASC);
```
We can convert it into a vertex label using:
```
schema.vertexLabel('book').
    fromExistingTable('book_table').
    create()
```

#### Creating an Edge Label

The following example creates an edge label `created` that connects `person` to `software`. The underlying table will be named `person__created__software`. This will automatically add required mapping columns from `person` & `software` to the edge table. Since `person` is the `OUT` vertex, all of its primary key columns will be prefixed `person_` in the edge table and all of the primary key columns of `software` will be prefixed with `software_`.

```
schema.edgeLabel('created').
    from('person').to('software').
    property('weight', Double).
    create()
```

Eventually you will end up with the following CQL Schema:
```
CREATE TABLE test_core.person__created__software (
    person_name text,
    person_ssn text,
    person_age int,
    software_name text,
    software_version int,
    software_lang text,
    weight double,
    PRIMARY KEY ((person_name, person_ssn), person_age, software_name, software_version, software_lang)
) WITH CLUSTERING ORDER BY (person_age ASC, software_name ASC, software_version ASC, software_lang ASC) 
    AND EDGE LABEL created 
        FROM person((person_name, person_ssn), person_age) 
        TO software(software_name, software_version, software_lang);
```
#### Creating an Edge label with a custom table name
When creating an edge label, the default table name will be `<fromVLName>__<edgeLabelName>__<toVLName>`. It is possible to override the default table name by using `.tableName(<name>)` as shown in the below example:

```
schema.edgeLabel('createdCustom').
    tableName('createdTable').
    ifNotExists().
    from('person').to('software').
    property('weight', Double).
    create()
```

#### Creating an Edge Label from an existing Table
Assuming we have the following existing non-vertex/edge table:
```
CREATE TABLE writes_table (person_name text, ssn text, age int, book_name text, isbn int, date date, 
    PRIMARY KEY ((person_name), ssn, age, book_name, isbn))
    WITH CLUSTERING ORDER BY (ssn ASC, age ASC, book_name ASC, isbn ASC);
```

We can convert it into an edge label that connects `person` to `book`, using:
```
schema.edgeLabel('writes').
    fromExistingTable('writes_table').
    from('person').
        mappingProperty('person_name').
        mappingProperty('ssn').
        mappingProperty('age').
    to('book').
        mappingProperty('book_name').
        mappingProperty('isbn').
    create()
```
Note that while the column names are not required to match, the data type and the number of mapping properties must match the data type and the number of primary key columns on the vertex label in order.
For example, vertex label `person` has three primary key columns, `name(text)`, `ssn(text)`, `age(int)`, table `writes_table` has five primary key columns,
which three of them `person_name(text)`, `ssn(text)`, `age(int)` are matched in corresponding order in the above definition.

#### Creating an Edge Label with specified mapping columns

Given the vertex labels:
```
schema.vertexLabel('person').
    partitionBy('name', Text).
    partitionBy('age', Int).
    clusterBy('year', Int).
    property('coffeePerDay', Int).
    create()

schema.vertexLabel('software').
    partitionBy('name', Text).
    clusterBy('year', Int).
    clusterBy('license', Text).
    create()
```

You can specify how the mapping columns are named in the edge table:
```
schema.edgeLabel('created').
    from('person').to('software').
    partitionBy(OUT, 'name', 'person_name').
    partitionBy(OUT, 'age', 'person_age').
    partitionBy('creation_date', Text).
    clusterBy(OUT, 'year', 'person_year').
    clusterBy(IN, 'name', 'software_name').
    clusterBy(IN, 'year', 'software_year').
    clusterBy(IN, 'license', 'software_license').
    clusterBy('creation_year', Int, Desc).
    create()
```

Which will lead to the below CQL Schema:
```
CREATE TABLE test_core.person__created__software (
    person_name text,
    person_age int,
    creation_date text,
    person_year int,
    software_name text,
    software_year int,
    software_license text,
    creation_year int,
    PRIMARY KEY ((person_name, person_age, creation_date), person_year, software_name, software_year, software_license, creation_year)
) WITH CLUSTERING ORDER BY (person_year ASC, software_name ASC, software_year ASC, software_license ASC, creation_year DESC)
    AND EDGE LABEL created 
    FROM person((person_name, person_age), person_year) 
    TO software(software_name, software_year, software_license);

```

Generally the pattern for specifying the mapping columns is `partitionBy(direction,sourceProperty,targetProperty)` / `clusterBy(direction,sourceProperty,targetProperty[,order])` where:

* `direction` can be only `OUT` for `partitionBy(..)`  and `IN` / `OUT` for `clusterBy(..)`
* `sourceProperty` is the name of the property/column from the source vertex label table
* `targetProperty` is the name of the mapping property/column in the edge label table. If `targetProperty` is not specified, then it will default to `direction.name().toLowerCase() + "_" + sourceProperty`
* `order`, if present, is either `Asc` (the default) or `Desc`

#### Default Edge Table Layout

When creating an edge label, its primary key will be composed and internally ordered by the following categories:

1. Partitioning columns specific to the edge label, in definition order
2. Partition key columns on the `from(..)` vertex label's table, in they order they appear there
3. Clustering columns defined on the `from(..)` vertex label's table, in the order they appear there
4. Clustering columns specific to the edge label, in definition order
5. Primary key columns (both partition key and clustering) defined in the `to(..)` vertex label's table, in the order they appear there

The first two categories become partition key columns in the created edge table.  The last three categories become
clustering columns in the created edge table.

When an edge label is created with one or more `partitionBy(..)` or `clusterBy(..)` clauses, each such clause moves its
associated edge column from its default position to the head of either category one or category four, respectively.
This holds for both mapping columns and for columns that are specific to the edge label itself.

The order in which an edge label's `partitionBy(..)` and `clusterBy(..)` clauses appear is significant.  If multiple
clauses are specified on a single edge label, then their relative ordering is preserved as they move together to their
new category.

For example, here is a variation on the `created` edge label that mostly relies on default ordering:


This leads to the following CQL Schema:
```
CREATE TABLE t.person__created_via_defaults__software (
    creation_date text,
    person_name text,
    person_age int,
    person_year int,
    creation_year int,
    software_name text,
    software_year int,
    software_license text,
    PRIMARY KEY ((creation_date, person_name, person_age), person_year, creation_year, software_name, software_year, software_license)
) WITH CLUSTERING ORDER BY (person_year ASC, creation_year DESC, software_name ASC, software_year ASC, software_license ASC)
    AND EDGE LABEL created_via_defaults
    FROM person((person_name, person_age), person_year)
    TO software(software_name, software_year, software_license);
```

Here's a related example that partially overrides the default clustering column order:

```
schema.edgeLabel('created_cluster_by_license').
    from('person').to('software').
    partitionBy('creation_date', Text).
    clusterBy(IN, 'license', 'software_license').
    clusterBy(OUT, 'year', 'person_year').
    create()
```

This leads to the following CQL schema:
```
CREATE TABLE t.person__created_cluster_by_license__software (
    creation_date text,
    person_name text,
    person_age int,
    software_license text,
    person_year int,
    software_name text,
    software_year int,
    PRIMARY KEY ((creation_date, person_name, person_age), software_license, person_year, software_name, software_year)
) WITH CLUSTERING ORDER BY (software_license ASC, person_year ASC, software_name ASC, software_year ASC)
    AND EDGE LABEL created_cluster_by_license
    FROM person((person_name, person_age), person_year)
    TO software(software_name, software_year, software_license)
```

If the edge label `create()` statement had not mentioned the `person_year` mapping column, then it would have taken its
default position in the primary key as the first clustering column.

## Dropping a Vertex/Edge Label

#### Dropping a Vertex Label
This will drop the table of the vertex label `software` if it exists and associated edge tables. So e.g. if there is `person->created->software` / `software->generated->software`, then there will be **3** delete statements (one for the table behind `software` and two for the associated edge tables).
```
schema.vertexLabel('software').
    ifExists().
    drop()
```

Note that any connected edge labels will also be dropped. For instance `person-created->software` would be dropped, but
 `person-created->building` would remain.


#### Dropping an Edge Label
This will drop the table of the edge label `created` if it exists. Please note that this will drop **all** connections where this particular label is being used. So e.g. if there is a `person->created->software` and a `software->created->software` connection, then both tables will be dropped.
```
schema.edgeLabel('created').
    ifExists().
    drop()
```

## Dropping a Vertex/Edge Label's Metadata

#### Dropping a Vertex Label's Metadata
This will drop the vertex label `software` but keep the underlying table and also remove the label from any associated edge tables. So e.g. if there is `person->created->software` / `software->generated->software`, then there will be **3** alter statements (one for the table behind `software` and two for the associated edge tables).
```
schema.vertexLabel('software').
    dropMetadata()
```

#### Dropping an Edge Label's Metadata
This will drop the edge label `created` but keep the underlying table. Please note that this will drop **all** connections where this particular label is being used. So e.g. if there is a `person->created->software` and a `software->created->software` connection, then the edge labels of both will be removed.
```
schema.edgeLabel('created').
    dropMetadata()
```

## Dropping all Metadata

This will drop all the vertex/edge labels but keep the underlying tables. 

```
schema.dropMetadata()

```

## Adding / Dropping Properties

#### Adding properties to a Vertex Label
```
schema.vertexLabel('person').
    addProperty('one', Int).
    addProperty('two', Int).
    alter()
```

#### Adding properties to an Edge Label
You can add properties to every single connection of the same edge label.

```
schema.edgeLabel('created').
    addProperty('one', Int).
    addProperty('two', Int).
    alter()
```
You can add properties to the specific connection (`person->created->software`) by using `from`/`to`.
```
schema.edgeLabel('created').
    from('person').to('software').
    addProperty('three', Int).
    addProperty('four', Int).
    alter()
```
#### Dropping properties from a Vertex Label
```
schema.vertexLabel('person').
    dropProperty('one').
    dropProperty('two').
    alter()
```

#### Dropping properties from an Edge Label
This will drop the specified properties from every single edge table.
```
schema.edgeLabel('created').
    dropProperty('one').
    dropProperty('two').
    alter()
```
You can drop properties from the specific connection (`person->created->software`) by using `from`/`to`.
```
schema.edgeLabel('created').
    from('person').to('software').
    dropProperty('three').
    dropProperty('four').
    alter()
```

## Creating Indexes automatically
Use `schema.indexFor(<traversal>).analyze()` to show the indexes required to execute the `<traversal>`:
```
schema.indexFor(g.V().has('age', 30).out().in()).analyze()
```
```
Traversal requires that the following indexes are created:
schema.vertexLabel('person').materializedView('person_by_age').ifNotExists().partitionBy('age').clusterBy('name', Asc).clusterBy('year', Asc).create()
schema.edgeLabel('created').from('person').to('software').materializedView('person__created__software_by_software_license_software_name_software_year').ifNotExists().partitionBy(IN, 'license').partitionBy(IN, 'name').partitionBy(IN, 'year').clusterBy(OUT, 'name', Asc).clusterBy(OUT, 'age', Asc).clusterBy(OUT, 'year', Asc).create()
```

Use `schema.indexFor(<traversal>).apply()` to create the required indexes for the `<traversal>`.
```
schema.indexFor(g.V().has('age', 30).out().in()).apply()
```
```
Creating the following indexes:
schema.vertexLabel('person').materializedView('person_by_age').ifNotExists().partitionBy('age').clusterBy('name', Asc).clusterBy('year', Asc).create()
schema.edgeLabel('created').from('person').to('software').materializedView('person__created__software_by_software_license_software_name_software_year').ifNotExists().partitionBy(IN, 'license').partitionBy(IN, 'name').partitionBy(IN, 'year').clusterBy(OUT, 'name', Asc).clusterBy(OUT, 'age', Asc).clusterBy(OUT, 'year', Asc).create()
OK
```

More details around the workings of automatic index creation can be found [here](IndexAnalyzer.md).


## Creating Indexes manually
Users also have the option to create all indexes manually as shown in the following examples.

#### Materialized Views

This example creates a new MV and partitions it by the `coffeePerDay` property so that `g.V().has('coffeePerDay', 4)` can be fulfilled. 
```
schema.vertexLabel('person').
    materializedView('by_coffee').
    partitionBy('coffeePerDay').
    create()
```

For edge indexes you can reference the primary key columns from the incident vertices by using `IN` and `OUT` parameters. 
In this example the entire view is partitioned by `weight` followed by clustering columns `IN` name of `software` and `OUT` name of `person` in `Desc` order.
```
schema.edgeLabel('created').
    from('person').to('software').
    materializedView('byWeight').
    ifNotExists().
    partitionBy('weight').
    clusterBy(IN, 'name').
    clusterBy(OUT, 'name', Desc).
    create()
```

Syntactic sugar exists for creating an edge materialized view index supporting traversals against the edge's natural direction.  To use it, insert a `inverse()` call after `ifNotExists()` and before any `partitionBy`/`clusterBy` calls.  The partition key of the resulting materialized view will be composed of any partition key columns on to-vertex combined with any supplemental partition key columns added to the edge table when the edge label was defined.  Here's an example:

```
schema.edgeLabel('created').
    from('person').to('software').
    materializedView('person_software_inv').
    ifNotExists().
    inverse().
    create()
```

This creates the following materialized view:

```
CREATE MATERIALIZED VIEW t.person_software_inv AS
    SELECT *
    FROM t.person__created__software
    WHERE creation_date IS NOT NULL AND software_name IS NOT NULL AND software_year IS NOT NULL AND software_license IS NOT NULL AND creation_year IS NOT NULL AND person_name IS NOT NULL AND person_age IS NOT NULL AND person_year IS NOT NULL
    PRIMARY KEY ((creation_date, software_name), software_year, software_license, creation_year, person_name, person_age, person_year)
    WITH CLUSTERING ORDER BY (software_year ASC, software_license ASC, creation_year DESC, person_name ASC, person_age ASC, person_year ASC)
```

The `inverse()` shortcut follows the same column ordering rules described under the Default Edge Table Layout section, except for reversing the sense of the from-vertex and to-vertex.

#### Secondary Indexes

This will create a secondary index on the property/column `coffeePerDay`.
```
schema.vertexLabel('person').
    secondaryIndex('by_coffee').
    ifNotExists().
    by('coffeePerDay').
    create()
```

The below example will create a column `map` of type `frozen(mapOf(Int, Text))` and index it via secondary index. Using `indexFull()` in this example will index the entire map. Available collection indexing options are `indexKeys()` / `indexValues()` / `indexEntries()` / `indexFull()`. See [here](https://docs.datastax.com/en/dse/6.0/cql/cql/cql_using/useIndexColl.html) for additional details about indexing collections.
```
schema.edgeLabel('person').
    addProperty('map', frozen(mapOf(Int, Text))).
    alter()

schema.edgeLabel('person').
    secondaryIndex('by_map').
    by('map').indexFull().
    create()
```

#### Search Indexes
This will index properties/column `name` and `license`.
```
schema.vertexLabel('software').
    searchIndex().
    by('name').
    by('license').
    create()
```

If columns `name` and `license` are indexed, then we can add `year` using:
```
schema.vertexLabel('software').
    searchIndex().
    by('year').
    create()
```

This will index property/column `weight` on the edge label `created`
```
schema.edgeLabel('created').
    from('person').to('software').
    searchIndex().
    ifNotExists().
    by('weight').
    create()
```

##### Indexing Types
When indexing a text column, you can choose to index it using `asString()` / `asText()`.

```
schema.vertexLabel('software').
    addProperty('stringProperty', Text).
    addProperty('textProperty', Text).
    alter()
    
schema.vertexLabel('software').
    searchIndex().
    ifNotExists().
    by('stringProperty').asString().
    by('textProperty').asText().
    create()
```

* `asString()`:  will be using a **non-tokenized** (`StrField`) field
* `asText()`: will be using a **tokenized** (`TextField`) field
* if no indexing type is specified, it will be using a `StrField` and a `TextField` copy field, which means that all textual predicates (token, tokenPrefix, tokenRegex, eq, neq, regex, prefix) will be usable.

Additional details about indexing types and when to use which one can be found in the DSE [docs](https://docs.datastax.com/en/dse/6.0/dse-dev/datastax_enterprise/graph/using/useSearchIndexes.html).

##### Dropping Indexed properties

You can drop a property from the search index using the below syntax:

```
schema.vertexLabel('software').
    searchIndex().
    dropProperty('textProperty').
    alter()
```

The same applies for edge label search indexes:

```
schema.edgeLabel('created').
    from('person').to('software').
    searchIndex().
    dropProperty('weight').
    alter()
```


## Dropping Indexes manually
Indexes can also be dropped manually as shown in the following examples.

#### Dropping a Materialized View
```
schema.vertexLabel('person').
    materializedView('by_coffee').
    ifExists().
    drop()
```

```
schema.edgeLabel('created').
    from('person').to('software').
    materializedView('by_r1b').
    ifExists().
    drop()
```

#### Dropping a Secondary Index
```
schema.vertexLabel('person').
    secondaryIndex('non_existing').
    ifExists().
    drop()
```

```
schema.edgeLabel('created').
    from('person').to('software').
    secondaryIndex('by_r2c').
    ifExists().
    drop()
```

#### Dropping a Search Index
```
schema.vertexLabel('person').
    searchIndex().
    drop()
```

```
schema.edgeLabel('created').
    from('person').to('software').
    searchIndex().
    drop()
```

### Waiting for indexing to finish before querying data

The Schema API provides an optional `.waitForIndex(<optionalTimeoutInSeconds>)` method that can be used during index creation
and allows to wait the specified timeout until a created index is built.

The default timeout value is **10s**. Below are some examples for each index type:

```
schema.vertexLabel('person').
    materializedView('by_coffee').
    partitionBy('coffeePerDay').
    waitForIndex().
    create()
```

```
schema.vertexLabel('person').
    secondaryIndex('by_coffee').
    ifNotExists().
    by('coffeePerDay').
    waitForIndex(5).
    create()
```

```
schema.vertexLabel('software').
    searchIndex().
    by('name').
    by('license').
    waitForIndex(30).
    create()
```

## Using Complex Types

#### List / `frozen` List
```
schema.vertexLabel('complexList').
    partitionBy('name', Text).
    property('major', listOf(Int)).
    property('majorfrozen', frozen(listOf(Int))).
    create()
```

#### Set / `frozen` Set
```
schema.vertexLabel('complexSet').
    partitionBy('name', Text).
    property('major', setOf(Int)).
    property('majorfrozen', frozen(setOf(Int))).
    create()
```

#### Map / `frozen` Map
```
schema.vertexLabel('complexMap').
    partitionBy('name', Text).
    property('versioncodename', mapOf(Int, Varchar)).
    property('versioncodenamefrozen', frozen(mapOf(Int, Varchar))).
    create()
```

#### Tuple
```
schema.vertexLabel('complexTuple').
    partitionBy('name', Text).
    property('versions1', tupleOf(Int, Varchar, Timestamp)).
    create()
```

Using a collection as an element in a `Tuple` requires the collection to be `frozen`.
```
schema.vertexLabel('complexTupleNested').
    partitionBy('name', Text).
    property('versions1', tupleOf(Varchar, frozen(listOf(Int)))).
    property('versions2', tupleOf(Varchar, frozen(setOf(Int)))).
    property('versions3', tupleOf(Varchar, frozen(mapOf(Varchar, Int)))).
    create()
```

Creating a tuple instance via `as`
```
[2, '3', Instant.now()] as Tuple
```

#### Nested Collections

Using a nested collection requires it to be `frozen`.

```
schema.vertexLabel('complexCollectionNested').
    partitionBy('name', Text).
    property('majorminor1', listOf(frozen(setOf(Int)))).
    property('majorminor2', setOf(frozen(listOf(Int)))).
    property('majorminor3', mapOf(Varchar, frozen(listOf(Int)))).
    create()
```

#### User defined types

Creating a user defined type via schema:
```
schema.type('address').
    property('address1', Text).
    property('address2', Text).
    property('postCode', Text).
    create()
```

Using a nested user defined type via `typeOf`:
```
schema.type('contactDetails').
    property('address', frozen(typeOf('address'))).
    property('telephone', listOf(Text)).
    create()
```

Dropping a user defined type:
```
schema.type('contactDetails').drop()
```

Using a user defined type in a label:
```
schema.vertexLabel('contact').
    partitionBy('name', Text).
    property('address', typeOf('address')).
    create()
```

For each UDT in the keyspace a class will be created that allows the user to use `as` syntax for creation. In this case
`address` is a synthetic type.
Creating and initializing a user defined type via `as`:
```
[address1: 'add1', address2: 'add2', postCode: 'pc'] as address
```

If there are nested complex types the conversion for nested elements will happen automatically. For instance if 
`address` has a column of type `nestedUdt` there is no need to specify the nested type:

```
[
 address1:'add1',
 address2:'add2', 
 postCode:'pc', 
 nestedUdt:[nested1:'n1', nested2:'n2']
] as address

//Is the same as
[
 address1:'add1',
 address2:'add2', 
 postCode:'pc', 
 nestedUdt:[nested1:'n1', nested2:'n2'] as nestedUdt
] as address
```

## An example that uses all available types

```
schema.type('address').
    property('address1', Text).
    property('address2', Text).
    property('postCode', Text).
    create()
```

```
schema.vertexLabel('allTypes').
    partitionBy('id', Int).
    property('ascii', Ascii).
    property('bigint', Bigint).
    property('blob', Blob).
    property('boolean', Boolean).
    property('date', Date).
    property('decimal', Decimal).
    property('double', Double).
    property('duration', Duration).
    property('float', Float).
    property('inet', Inet).
    property('int', Int).
    property('linestring', LineString).
    property('point', Point).
    property('polygon', Polygon).
    property('smallint', Smallint).
    property('text', Text).
    property('time', Time).
    property('timestamp', Timestamp).
    property('timeuuid', Timeuuid).
    property('tinyint', Tinyint).
    property('uuid', Uuid).
    property('varchar', Varchar).
    property('varint', Varint).
    property('list', listOf(Int)).
    property('set', setOf(Int)).
    property('map', mapOf(Int, Int)).
    property('tuple', tupleOf(Int, Int, Int)).
    property('udt', typeOf('address')).
    create()
```


`Counter` is a special type and cannot be used with other column types, otherwise you will see the error `Cannot mix counter and non counter columns in the same table`. So here's a separate `Counter` example:
```
schema.vertexLabel('counterExample').
    partitionBy('name', Text).
    property('counter', Counter).
    create()
```


Here's an overview of how the column types map to their java types:

| **Column Type** | **Java Type** |
|:-------------- |:---------------|
|Ascii|String.class|
|Bigint|Long.class|
|Blob|ByteBuffer.class|
|Boolean|Boolean.class|
|Counter|Long.class|
|Date|LocalDate.class|
|Decimal|BigDecimal.class|
|Double|Double.class|
|Duration|com.datastax.driver.core.Duration.class|
|Float|Float.class|
|Inet|InetAddress.class|
|Int|Integer.class|
|linestring|com.datastax.driver.dse.geometry.LineString.class|
|point|com.datastax.driver.dse.geometry.Point.class|
|polygon|com.datastax.driver.dse.geometry.Polygon.class|
|Smallint|Short.class|
|Text|String.class|
|Time|LocalTime.class|
|Timestamp|Instant.class|
|Timeuuid|UUID.class|
|Tinyint|Byte.class|
|Uuid|UUID.class|
|Varchar|String.class|
|Varint|BigInteger.class|
|listOf(..)|List.class|
|setOf(..)|Set.class|
|mapOf(..)|Map.class|
|tupleOf(..)|com.datastax.driver.core.TupleValue.class|
|typeOf(..)|com.datastax.driver.core.UDTValue.class|


An example of how to insert data for all types is shown below.

```
g.addV('allTypes').
    property('id', 232).
    property('ascii', 'ascii').
    property('bigint', 23L).
    property('blob', [3, 4] as ByteBuffer).
    property('boolean', true).
    property('date', '2007-12-03' as LocalDate).
    property('decimal', 2.3).
    property('double', 2.3d).
    property('duration', 'PT10H' as Duration).
    property('float', 2.3f).
    property('inet', '192.168.1.2' as InetAddress).
    property('inet', '2001:4860:4860::8888' as InetAddress).
    property('int', 23).
    property('linestring', [1, 1, 2, 2, 3, 3] as LineString)).
    property('linestring', 'LINESTRING (30 10, 10 30, 40 40)' as LineString)).
    property('point', [1.1, 2.2] as Point).
    property('point', 'POINT (30 10)' as Point).
    property('polygon', [0.9, 0.9, 1.1, 0.9, 1.1, 1.1, 0.9, 1.1] as Polygon).
    property('polygon', 'POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))' as Polygon).
    property('smallint', 23 as short).
    property('text', 'some text').
    property('time', '10:15:30' as LocalTime).
    property('timestamp', '2007-12-03T10:15:30.00Z' as Instant).
    property('timeuuid', UUIDs.timeBased()).
    property('tinyint', 38 as byte).
    property('uuid', 'bb6d7af7-f674-4de7-8b4c-c0fdcdaa5cca' as UUID).
    property('varchar', 'some text').
    property('varint', 1).
    property('list', [1, 2, 3]).
    property('set', [1, 2, 3] as Set).
    property('map', [k1:'v1', k2:v2]).
    property('tuple', [1, 2, 3] as Tuple).
    property('udt', [address1:'add1', address2:'add2', postCode:'pc'] as address)
```


## Dropping Schema

This will drop all the indexes, edge labels, vertex labels and user defined types in the schema.

```
schema.drop()
```

## Describe Schema

This will describe all the UDTs, vertex labels, edge labels and indexes in the schema.

```
schema.describe()
```

## Describe a Vertex Label

A particular vertex label's schema definition can be described with:

```
schema.vertexLabel('person').describe()
```

## Describe all Vertex Labels

It is possible to retrieve the schema definition of all vertex labels by running:

```
schema.vertexLabels().describe()
```

## Describe an Edge Label

A particular edge label's schema definition can be described with:

```
schema.edgeLabel('created').from('person').to('software').describe()
```
e.g. `person->created->software`

A particular edge label's schema definitions can be described with:

```
schema.edgeLabel('created').describe()
```
e.g. `person->created->software`, `software->created->software`


## Describe all Edge Labels

It is possible to retrieve the schema definition of all edge labels by running:

```
schema.edgeLabels().describe()
```

## Describe a User Defined Type

A particular user defined type definition can be described with:

```
schema.type('address').describe()
```

## Describe all User Defined Types

It is possible to retrieve the definition of all user defined types by running:

```
schema.types().describe()
```
