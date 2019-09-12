# What's new in DataStax Graph?

## Improved Error Messaging

CTRL-C in gremlin console aborts the current request rather than exits. Use the `:exit` command to exit the console.

```
gremlin> :exit
```

## Improved Error Messaging

DataStax Graph will provide improved and detailed error messages in case a traversal cannot be fulfilled due to missing indexes.
The output will look as shown below and will contain information such as:

* the executed traversal that failed
* details of the step that failed
* the CQL that was executed and failed
* an index suggestion that can be applied in order to fulfill the traversal
* alternative approaches to creating an index


```
gremlin> g.V().hasLabel("a").has("age", 23)
One or more indexes are required to execute the traversal: g.V().hasLabel("a").has("age",(int) 23)
Failed step: __.V().hasLabel("a").has("age",(int) 23)
CQL execution: No table or view could satisfy the query 'SELECT * FROM bla.a WHERE age = ?'
The output of 'schema.indexFor(<your_traversal>).analyze()' suggests the following indexes could be created to allow execution:

schema.vertexLabel('a').materializedView('a_by_age').ifNotExists().partitionBy('age').clusterBy('id', Asc).create()

Alternatively consider using:
g.with('ignore-unindexed') to ignore unindexed traversal. Your results may be incomplete.
g.with('allow-filtering') to allow filtering. This may have performance implications.
```

The main benefit is that it helps users in understanding what went wrong at a particular step and what can be done to resolve it.

## Index suggestion mechanism

DataStax Graph has the ability to figure out what indexes a given traversal requires. This can be done by executing `schema.indexFor(<your_traversal>).analyze()` as shown below:

```
gremlin> schema.indexFor(g.V().has("age", 23)).analyze()
==>Traversal requires that the following indexes are created:
schema.vertexLabel('person').materializedView('person_by_age').ifNotExists().partitionBy('age').clusterBy('firstname', Asc).clusterBy('lastname', Asc).create()
```

The index analyzer will also indicate in case a traversal can be fulfilled by existing indexes:
```
gremlin> schema.indexFor(g.V().has("name", "x")).analyze()
==>Traversal can be satisfied by existing indexes
```

Index suggestions can also be directly applied by executing `schema.indexFor(<your_traversal>).apply()`:

```
gremlin> schema.indexFor(g.V().has("age", 23)).apply()
==>Creating the following indexes:
schema.vertexLabel('person').materializedView('person_by_age').ifNotExists().partitionBy('age').clusterBy('firstname', Asc).clusterBy('lastname', Asc).create()
OK
```

Additional details to the index suggestion mechanism can be found [here](IndexAnalyzer.md).


## Simplified and improved Schema API

DataStax Graph has an improved Schema API that allows the creation/modification of vertex/edge labels and indexes.
The Schema API abstracts away complexity and is also closer to Cassandra terminology when it comes to partitioning data through the usage of `partitionBy(..)` / `clusterBy(..)`.

The following example creates a vertex label `person`, having a partition key on `name` / `ssn` and using `age` as a clustering column. The example also creates a vertex label `software`, using `name` as partition key, `version` and `lang` as clustering columns. `temp` ends up being a regular and `static_property` a static column:
```
schema.vertexLabel('person').
    ifNotExists().
    partitionBy('name', Text).
    partitionBy('ssn', Text).
    clusterBy('age', Int).
    property('address', Text).
    property('coffeePerDay', Int).
    create()
    
schema.vertexLabel('software').
    ifNotExists().
    partitionBy('name', Text).
    clusterBy('version', Int).
    clusterBy('lang', Text).
    property('temp', Text).
    property('static_property', Text, Static).
    create()
```

Given the two vertex labels `person` and `software`, one can create the connection `person-created->software` as shown below:

```
schema.edgeLabel('created').
    ifNotExists().
    from('person').to('software').
    property('weight', Double).
    create()
```

An example of creating a MV and partition it by the `age` property so that graph queries against `age` can be fulfilled is shown below:
```
schema.vertexLabel('person').
    materializedView('by_age').
    partitionBy('age').
    create()
```

Additional Schema API details with usage examples can be found [here](SystemAndSchemaAPI.md).

## Transparent Data Model

DataStax Graph uses a more transparent data model with the following characteristics:

* graph = CQL keyspace
* vertex/edge label = CQL table
* property of the underlying vertex/edge label = CQL column

This means that users can keep their existing data and let a keyspace be treated as a graph in order to perform graph traversals. The data is then explorable through graph and CQL tools.
Additionally, bulk loading graph data can be performed through CQL without having to use a custom tool.


## CQL grammar to specify Graph Metadata on Keyspaces/Tables

DataStax Graph comes with new CQL grammar that allows keyspaces to be treated as graphs and tables to be treated as vertex/edge labels in that graph. This is especially helpful for users that would like to convert their existing data to a graph.

Executing `ALTER KEYSPACE ks WITH <replicationSettings> AND graph_engine = 'Core'` on an existing keyspace will treat that keyspace as a graph in DSE.

By executing `ALTER TABLE ks.tbl WITH VERTEX LABEL <optionalName>` or `ALTER TABLE ks.tbl WITH EDGE LABEL <optionalName> FROM vLabelOne(...) TO vLabelTwo(...)` a CQL table can be represented as a vertex/edge label.

The [Schema API](SystemAndSchemaAPI.md) abstracts away the CQL grammar and easy conversion of existing keyspaces/tables to graphs/element labels. 
For a given keyspace, one can execute `system.graph("test").fromExistingKeyspace().create()`.
One can convert an existing table to a vertex label using `schema.vertexLabel('book').fromExistingTable('book_table').create()`. 

Additional details around the CQL grammar with usage examples can be found [here](CQLGraphExtensions.md). Details and examples around the Schema API can be found [here](SystemAndSchemaAPI.md).

## Improved Profiling output

The new `.profile()` output shows details about the steps of a given traversal and the CQL each step needs to execute. CQL statements are grouped and include duration information. The improved output format helps determining where potential bottlenecks reside.
The output of `g.V().has("id", 1).profile()` might look as shown below:

```
Step                                                               Count  Traversers       Time (ms)    % Dur
=============================================================================================================
__.V().has("id",(int) 1)                                               3           3          20.200    70.69
  CQL statements ordered by overall duration                                                  35.418
    \_1=SELECT * FROM sample.company WHERE id = ? / Duration: 11 ms / Count: 1
    \_2=SELECT * FROM sample.person WHERE id = ? / Duration: 11 ms / Count: 1
    \_3=SELECT * FROM sample.software WHERE id = ? / Duration: 11 ms / Count: 1
HasStep([id.eq(1)])                                                    3           3           6.915    24.20
ReferenceElementStep                                                   3           3           1.461     5.12
                                            >TOTAL                     -           -          28.578        -
```

Appending `.out("works_with")` to the previous traversal in order to get `g.V().hasLabel("person").has("id", 1).out("works_with").profile()` might result in:
```
Step                                                               Count  Traversers       Time (ms)    % Dur
=============================================================================================================
__.V().hasLabel("person".has("id"...                                   1           1           1.860    11.28
  CQL statements ordered by overall duration                                                   0.955
    \_1=SELECT * FROM sample.person WHERE id = ? / Duration: < 1 ms / Count: 1
HasStep([~label.eq(person), id.eq(1)])                                 1           1           0.849     5.15
__.out().hasLabel("works_with")                                        1           1          12.637    76.64
  CQL statements ordered by overall duration                                                   3.993
    \_1=SELECT * FROM sample.person__works_with__software WHERE person_id = ?
        AND person_age = ? / Duration: 2 ms / Count: 1
    \_2=SELECT * FROM sample.software WHERE id = ? AND age = ? / Duration: 1 ms / Cou
        nt: 1
ReferenceElementStep                                                   1           1           1.141     6.92
                                            >TOTAL                     -           -          16.489        -
```

## Full C* Type Alignment

DataStax Graph supports all C* types, including complex types, such as CQL collections and UDTs / Tuples. Below is an example that shows a vertex label with all types:

```
schema.type('address').
    property('address1', Text).
    property('address2', Text).
    property('postCode', Text).
    create()

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

Additional information about supported types can be found in the [Schema API](SystemAndSchemaAPI.md).


## Direct edge lookup may be used

If edges are indexed then they may be queried directly rather than going via a vertex.
```
schema.edgeLabel('created').
    from('person').to('software').
    materializedView('byWeight').
    ifNotExists().
    partitionBy('weight').
    clusterBy(IN, 'name').
    clusterBy(OUT, 'name', Desc).
    create()

g.E().hasLabel('created').has('since', '2002').inV()
```


## Search indexes may be used for edges

DataStax Graph now allows search indexes to be used on edges. In particular this is useful for tokenized edge queries, but 
also any other predicate supported by search.

```
schema.edgeLabel('created').
    from('person').to('software').
    searchIndex().
    ifNotExists().
    by(OUT, 'name').
    by('weight').
    create()

g.V().outE().has('weight', neq(0.8))
``` 

## Development traversal source

DataStax Graph now supports the `dev` traversal source when connecting via Studio or Gremlin Console.
The `dev` traversal source allows queries to be performed without indexing at the cost of performing full scans.

In a typical development workflow for OLTP you should:
1. Create vertex and edge labels.
2. Insert some toy data.
3. Experiment using the `dev` traversal source against your toy graph.
4. Use `schema.indexFor(<your traversal>).apply()`. It will create any missing indexes that you need. You will now be able to execute your traversal against `g` without error.
5. Run `schema.describe()` to get your final schema script with indexes.

Trying to use the `dev` traversal source outside of Studio or Gremlin Console will throw an exception.
