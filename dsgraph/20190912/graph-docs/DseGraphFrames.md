# DataStax Graph Frames
This guide is broken down into three main sections, a [DataStax Graph Frames API](#datastax-graph-frames-api) section describing fundamental methods for managing data, a [Best Practices Guide for Loading Data](#best-practices-guide-for-loading-data) section highlighting recommended practices, and a general [How-to](#how-to) section showing examples of common operations.

The DseGraphFrame package provides a Spark API for bulk operations and analytics on DataStax Graph. It is inspired by Databricks’ GraphFrame library and supports a subset of Apache TinkerPop™ Gremlin graph traversal language. It supports reading of DataStax Graph data into a GraphFrame and writing GraphFrames from any format supported by Spark into DataStax Graph.
For a review of our initial offering and more introductory examples see our [Introducing DataStax Graph Frames](https://www.datastax.com/dev/blog/dse-graph-frame) blog post.

### DataStax Graph Frames API
The following table shows the key methods available for managing data with DataStax Graph Frames.

|Method|Result|
|----------|:-------------|
|gf()|GraphFrame object for graph frame API usage|
|V()|DseGraphTraversal\[Vertex\] object used to start a TinkerPop vertex traversal|
|E()|DseGraphTraversal\[Edge\] object used to start a TinkerPop edge traversal|
|io()|TP IOStep to export or inport graph from external source|
|[deleteVertices()](#deletevertices), [deleteEdges()](#deleteedges)|Delete vertices and edges|
|[deleteVertexProperties()](#deletevertexproperties), [deleteEdgeProperties()](#deleteedgeproperties)|Delete property values (does not change schema)|
|[updateVertices()](#updatevertices), [updateEdges()](#updateedges)|Update or insert properties, vertices, and edges|

The examples shown in this section build upon the vertex and edge examples shown in [SystemAndSchemaApi docs](SystemAndSchemaAPI.md).
As a reminder, here are the schemas for these elements.
```groovy
gremlin> schema.vertexLabel('person').
    ifNotExists().
    partitionBy('name', Text).
    partitionBy('ssn', Text).
    clusterBy('age', Int).
    property('address', Text).
    property('coffeePerDay', Int).
    create()

gremlin> schema.vertexLabel('software').
    ifNotExists().
    partitionBy('name', Text).
    clusterBy('version', Int).
    clusterBy('lang', Text).
    property('temp', Text).
    property('static_property', Text, Static).
    create()
    
gremlin> schema.edgeLabel('created').
    ifNotExists().
    from('person').to('software').
    property('weight', Double).
    create()
```

#### deleteVertices
Vertices for a given label can easily be deleted using `deleteVertices` with the vertex label to delete.
```scala
def deleteVertices(label: String): Unit 
```

###### Example
```scala
scala> g.V().show(false)
+------------------------------+--------+-----+-------+------+-----------+----+---------------+-----+------------------+------------+
|id                            |~label  |name |version|lang  |ssn        |age |static_property|temp |address           |coffeePerDay|
+------------------------------+--------+-----+-------+------+-----------+----+---------------+-----+------------------+------------+
|software:timer:2.0:groovy#00  |software|timer|2.0    |groovy|null       |null|beta           |100  |null              |null        |
|software:chat:1.0:scala#20    |software|chat |1.0    |scala |null       |null|united states  |mambo|null              |null        |
|person:elmo:123-45-6789:4#33  |person  |elmo |null   |null  |123-45-6789|4   |null           |null |123 sesame street|1000        |
|person:rocco:111-11-1111:21#11|person  |rocco|null   |null  |111-11-1111|21  |null           |null |123 sesame street|100         |
+------------------------------+--------+-----+-------+------+-----------+----+---------------+-----+------------------+------------+

scala> g.deleteVertices("software")

scala> g.V().show(false)
+------------------------------+------+-----+-------+----+-----------+---+---------------+----+------------------+------------+
|id                            |~label|name |version|lang|ssn        |age|static_property|temp|address           |coffeePerDay|
+------------------------------+------+-----+-------+----+-----------+---+---------------+----+------------------+------------+
|person:elmo:123-45-6789:4#33  |person|elmo |null   |null|123-45-6789|4  |null           |null|123 sesame street|1000        |
|person:rocco:111-11-1111:21#11|person|rocco|null   |null|111-11-1111|21 |null           |null|123 sesame street|100         |
+------------------------------+------+-----+-------+----+-----------+---+---------------+----+------------------+------------+
```


#### deleteEdges
This process is very similar to [updateEdges](#updateedges), but with an extra step between 3 and 4 that purges all the DataFrame columns except the newly add primary property key column names that were added in step 2.
Another key difference with step 4 is that we use `df.rdd.deleteFromCassandra` for carrying out the delete operation.

```scala
def deleteEdges(df: DataFrame, cache: Boolean = true): Unit
```
###### Example
Continuing from the previous example, deleting the `created` edge between `person` and `software` vertex is accomplished like this
```scala
scala> g.E.show(false)
+------------------------------+--------------------------+-------+------+
|src                           |dst                       |~label |weight|
+------------------------------+--------------------------+-------+------+
|person:rocco:111-11-1111:21#11|software:chat:1.0:scala#20|created|null  |
+------------------------------+--------------------------+-------+------+


scala> g.deleteEdges(edgeTarget)

scala> g.E.show(false)
+---+---+------+------+
|src|dst|~label|weight|
+---+---+------+------+
+---+---+------+------+
```

Where the `edgeTarget` is defined like this.
```scala
scala> edgeTarget.show(false)
+------------------------------+--------------------------+-------+
|src                           |dst                       |~label |
+------------------------------+--------------------------+-------+
|person:rocco:111-11-1111:21#11|software:chat:1.0:scala#20|created|
+------------------------------+--------------------------+-------+
```

#### deleteVertexProperties
Deleting vertex properties can be done by providing a data frame with IDs of the vertices to target, a list of properties to delete, and optional parameters for isolating vertex labels and caching.
The API looks like this
```scala
def deleteVertexProperties(df: DataFrame, properties: Seq[String], labels: Seq[String] = Seq.empty, cache: Boolean = true): Unit
```
###### Example
Say we want to delete the `coffeePerDay` property of the `person` vertex with name "rocco"
```scala
scala> g.V().show(false)
+------------------------------+------+-----+-------+----+-----------+---+---------------+----+------------------+------------+
|id                            |~label|name |version|lang|ssn        |age|static_property|temp|address           |coffeePerDay|
+------------------------------+------+-----+-------+----+-----------+---+---------------+----+------------------+------------+
|person:rocco:111-11-1111:21#11|person|rocco|null   |null|111-11-1111|21 |null           |null|123 sesame street|100         |
+------------------------------+------+-----+-------+----+-----------+---+---------------+----+------------------+------------+
``` 

We could delete this vertex property like so
```scala
scala> g.deleteVertexProperties(target, Seq("coffeePerDay"))

scala> g.V().show(false)
+------------------------------+------+-----+-------+----+-----------+---+---------------+----+------------------+------------+
|id                            |~label|name |version|lang|ssn        |age|static_property|temp|address           |coffeePerDay|
+------------------------------+------+-----+-------+----+-----------+---+---------------+----+------------------+------------+
|person:rocco:111-11-1111:21#11|person|rocco|null   |null|111-11-1111|21 |null           |null|123 sesame street|null        |
+------------------------------+------+-----+-------+----+-----------+---+---------------+----+------------------+------------+
```

Where the supplied data frame could take any of the following forms
```scala
scala> target.show(false)
+------------------------------+------+-----+-----------+---+
|id                            |~label|name |ssn        |age|
+------------------------------+------+-----+-----------+---+
|person:rocco:111-11-1111:21#11|person|rocco|111-11-1111|21 |
+------------------------------+------+-----+-----------+---+
```

```scala
scala> target.drop("id").show(false)
+------+-----+-----------+---+
|~label|name |ssn        |age|
+------+-----+-----------+---+
|person|rocco|111-11-1111|21 |
+------+-----+-----------+---+
```

```scala
scala> target.drop("~label").show(false)
+------------------------------+-----+-----------+---+
|id                            |name |ssn        |age|
+------------------------------+-----+-----------+---+
|person:rocco:111-11-1111:21#11|rocco|111-11-1111|21 |
+------------------------------+-----+-----------+---+
```

#### deleteEdgeProperties
The process is very similar to [updateEdges](#updateedges), but with an extra step between 3 and 4 that adds the supplied list of properties to the DataFrame, these are the properties to be deleted.
Another key difference with step 4 is that we omit the `.options(WriteConf.IgnoreNullsParam.sqlOption(true))` option which allows us to overwrite existing values with null properties.
```scala
def deleteEdgeProperties(df: DataFrame, properties: String*): Unit
```

###### Example
Given the following edge
```scala
scala> g.E().show(false)
+------------------------------+--------------------------+-------+------+
|src                           |dst                       |~label |weight|
+------------------------------+--------------------------+-------+------+
|person:rocco:111-11-1111:21#11|software:chat:1.0:scala#20|created|100.0 |
+------------------------------+--------------------------+-------+------+
```

With the following data frame targeting this edge
```scala
scala> edgeTarget.show(false)
+------------------------------+--------------------------+-------+
|src                           |dst                       |~label |
+------------------------------+--------------------------+-------+
|person:rocco:111-11-1111:21#11|software:chat:1.0:scala#20|created|
+------------------------------+--------------------------+-------+
```

Deleting the edge properties is as simple as this
```scala
scala> g.deleteEdgeProperties(edgeTarget, "weight")

scala> g.E().show(false)
+------------------------------+--------------------------+-------+------+
|src                           |dst                       |~label |weight|
+------------------------------+--------------------------+-------+------+
|person:rocco:111-11-1111:21#11|software:chat:1.0:scala#20|created|null  |
+------------------------------+--------------------------+-------+------+
```

#### updateVertices
The `updateVertices` method comes in two flavors, updating multiple vertex labels or updating a single vertex label (new API).

##### Updating multiple vertex labels
The API for updating multiple vertex labels looks like this
```scala
def updateVertices(df: DataFrame, labels: Seq[String] = Seq.empty, cache: Boolean = true): Unit
```
Where the `df` is the data frame with vertex ID and columns to be updated.
The `labels` is used to group vertices within the same ID format, empty (means all)
The `cache` param indicates whether to cache the data frame before processing, it is set to true by default for consistence update and performance.

Here is a simple example updating the person vertex table shown in the [SystemAndSchemaApi docs](SystemAndSchemaAPI.md)
As a reminder, the schema for the person and software vertex looks like this
```groovy
gremlin> schema.vertexLabel('person')
    .ifNotExists()
    .partitionBy('name', Text)
    .partitionBy('ssn', Text)
    .clusterBy('age', Int)
    .property('address', Text)
    .property('coffeePerDay', Int)
    .create()

gremlin> schema.vertexLabel('software')
    .ifNotExists()
    .partitionBy('name', Text)
    .clusterBy('version', Int)
    .clusterBy('lang', Text)
    .property('temp', Text)
    .property('static_property', Text, Static)
    .create()
```
Remember, DataStax Graph Frames represents a Graph as two virtual tables: a Vertex DataFrame and an Edge DataFrame.
So in the running example involving a `person` and `software` vertex, we will see a single Vertex DataFrame ecapsulating data for both.
```scala
scala> g.V().printSchema
root
 |-- id: string (nullable = true)
 |-- ~label: string (nullable = false)
 |-- name: string (nullable = false)
 |-- version: string (nullable = true)
 |-- lang: string (nullable = true)
 |-- ssn: string (nullable = true)
 |-- age: integer (nullable = true)
 |-- static_property: string (nullable = true)
 |-- temp: string (nullable = true)
 |-- address: string (nullable = true)
 |-- coffeePerDay: integer (nullable = true)
```

Now suppose we want to update or insert vertex data for both `person` and `software` vertices, we would need to construct a data frame that looks like this.
```scala
scala> multiVertexLabelDF.show
+-----+--------+-----------+----+-----------------+------------+-------+-----+---------------+-----+
| name|  ~label|        ssn| age|          address|coffeePerDay|version| lang|static_property| temp|
+-----+--------+-----------+----+-----------------+------------+-------+-----+---------------+-----+
|rocco|  person|222-22-2222|  20|3975 Freedom Blvd|           2|   null| null|           null| null|
| chat|software|       null|null|             null|        null|    1.0|scala|  united states|mambo|
+-----+--------+-----------+----+-----------------+------------+-------+-----+---------------+-----+

```
Note that we have a `~label` column and the necessary ID columns for both `person` (i.e. name, ssn, age) and `software` (i.e. name, version, lang) vertices.
Once the data frame is constructed properly, updating the vertices is straight forward.
```scala
scala> g.updateVertices(multiVertexLabelDF)
```

##### Updating a single vertex label (new API)
The API for updating a single vertex label is bit simpler. In this case the API looks like this.
```scala
def updateVertices(vertexLabel: String, df: DataFrame): Unit
```
Where the `vertexLabel` specifies the single vertex label of intertest, and the data frame consists of the vertex IDs and 
any additional columns targeted for updates. In the `person` vertex example the data frame would look like this
```scala
scala> personDF.show
+----+-----------+---+------------------+------------+
|name|        ssn|age|           address|coffeePerDay|
+----+-----------+---+------------------+------------+
|elmo|123-45-6789|  4| 123 sesame street|        1000|
+----+-----------+---+------------------+------------+
```
Once again updating is simple.
```scala
scala> g.updateVertices("person", personDF)
```

#### updateEdges
Similar to vertex updates, `updateEdges` also comes in two flavors, updating multiple or single edge labels.

##### Updating multiple edge labels 
The process for updating edges and ultimately **mapping column names** follow this process:

1. `updateEdges()` takes a DataFrame and using the `src`, `dst`, and `~label` columns builds a list of `EdgeLabel` objects.
2. For each edge label, prepare and filter edge IDs using `prepareAndFilterEdgeIds`. This takes a data frame that has a schema with `src`, `dst`, and `~label` columns and replaces `src` with the edge label's out-vertex primary property key column names. It does the same for `dst`, replacing this with the edge label's in-vertex primary property key column names. So in the end, the DataFrame is transformed with a schema with `~label`, out-vertex column names, and in-vertex column names in that order.
3. The `~label` column is then dropped on the returned DataFrame.
4. Then updates the DSE table using the `df.write.cassandraFormat...` path.

The API for the multi-edge update method looks like this.
```scala
def updateEdges(df: DataFrame, cache: Boolean = true): Unit
```

Where the `df` data frame contains the edge IDs and columns to be updated. When `cache` is true (default), the data frame is explicitly cached before processing, and uncached upon completion.

###### Example
 
Let's imagine we have 2 `person` and `software` vertices each, and no edges defined.
```
cqlsh> select * from test.person;

 name  | ssn         | age | address            | coffeePerDay
-------+-------------+-----+--------------------+--------------
  elmo | 123-45-6789 |   4 | 123 sesame street |         1000
 rocco | 111-11-1111 |  21 | 123 sesame street |          100

(2 rows)
cqlsh> select * from test.software;

 name  | version | lang   | static_property | temp
-------+---------+--------+-----------------+-------
 timer |     2.0 | groovy |            beta |   100
  chat |     1.0 |  scala |   united states | mambo

(2 rows)
```

As a reminder, from DataStax Graph Frames it looks like this
```scala
scala> g.V.show(false)
+------------------------------+--------+-----+-------+------+-----------+----+---------------+-----+------------------+------------+
|id                            |~label  |name |version|lang  |ssn        |age |static_property|temp |address           |coffeePerDay|
+------------------------------+--------+-----+-------+------+-----------+----+---------------+-----+------------------+------------+
|software:timer:2.0:groovy#00  |software|timer|2.0    |groovy|null       |null|beta           |100  |null              |null        |
|software:chat:1.0:scala#20    |software|chat |1.0    |scala |null       |null|united states  |mambo|null              |null        |
|person:elmo:123-45-6789:4#33  |person  |elmo |null   |null  |123-45-6789|4   |null           |null |123 sesame street |1000        |
|person:rocco:111-11-1111:21#11|person  |rocco|null   |null  |111-11-1111|21  |null           |null |123 sesame street |100         |
+------------------------------+--------+-----+-------+------+-----------+----+---------------+-----+------------------+------------+

scala> g.E.show(false)
+---+---+------+------+
|src|dst|~label|weight|
+---+---+------+------+
+---+---+------+------+

```
 
Now updating or inserting multiple edge labels is fairly straight forward, once a data frame is properly constructed. 
We need a data frame that encapsulated the source and destination vertices, edge label name, and any additional properties targeted for updating.
In this example, the data frame could look like this
```scala
scala> newEdgeDataDF.show(false)
+------------------------------+----------------------------+-------+------+
|src                           |dst                         |~label |weight|
+------------------------------+----------------------------+-------+------+
|person:rocco:111-11-1111:21#11|software:chat:1.0:scala#20  |created|10.0  |
+------------------------------+----------------------------+-------+------+
``` 
Note, this was an simplified example only showing a __single edge label__ ("created"), but a user could mix in different edge labels 
by specifying the proper `~label` name and remaining fields using the principles described here.

With this data frame, we simply pass this along to `updateEdges` like so
```scala
scala> g.updateEdges(newEdgeDataDF)
```

But how is this data frame constructed? Let's take deeper look at one example. In the above case we have a data frame with a
row describing a `created` edge from a `person` (rocco) to a `software` vertex (chat).
Let's start by creating a simple person data frame from scratch that contains all the fields needed for describing a `person` vertex.
```scala
scala> val roccoDF = Seq(("rocco","111-11-1111",21,"123 sesame street",100)).toDF("name","ssn","age","address","coffeePerDay")
roccoDF: org.apache.spark.sql.DataFrame = [name: string, ssn: string ... 3 more fields]

scala> roccoDF.show(false)
+-----+-----------+---+------------------+------------+
|name |ssn        |age|address           |coffeePerDay|
+-----+-----------+---+------------------+------------+
|rocco|111-11-1111|21 |123 sesame street|100         |
+-----+-----------+---+------------------+------------+ 
```

When specifying an edge we need a `src` and `dst` column, each references a DSE-generated ID representing the source and destination vertices respectively.
`idColumn()` is a helper method that allows a user to construct these `src` and `dst` fields. 
To construct the `src` ID we supply the label name, and vertex primary keys. Here is an example showing the generated ID.
```scala
scala> roccoDF.select(g.idColumn(lit("person"), col("name"), col("ssn"), col("age")) as "src").show(false)
+------------------------------+
|src                           |
+------------------------------+
|person:rocco:111-11-1111:21#11|
+------------------------------+
``` 

We do the same for the destination vertex, in this case a `software` vertex
```scala
scala> chatDF.select(g.idColumn(lit("software"), col("name"), col("version"), col("lang")) as "dst").show(false)
+--------------------------+
|dst                       |
+--------------------------+
|software:chat:1.0:scala#20|
+--------------------------+
```

Now to construct the entire edge description, we simply need to add the `~label` and any additional properties targeted for updating.
```scala
scala> val srcDF = roccoDF.select(g.idColumn(lit("person"), col("name"), col("ssn"), col("age")) as "src")
srcDF: org.apache.spark.sql.DataFrame = [src: string]

scala> val dstDF = chatDF.select(g.idColumn(lit("software"), col("name"), col("version"), col("lang")) as "dst")
dstDF: org.apache.spark.sql.DataFrame = [dst: string]

scala> val personCreatedSoftwareDF = srcDF.crossJoin(dstDF).crossJoin(Seq(("created")).toDF("~label")).crossJoin(Seq((10.0)).toDF("weight"))
personCreatedSoftwareDF: org.apache.spark.sql.DataFrame = [src: string, dst: string ... 2 more fields]

scala> personCreatedSoftwareDF.show(false)
+------------------------------+--------------------------+-------+------+
|src                           |dst                       |~label |weight|
+------------------------------+--------------------------+-------+------+
|person:rocco:111-11-1111:21#11|software:chat:1.0:scala#20|created|10.0  |
+------------------------------+--------------------------+-------+------+
```
A user could repeat the same pattern for more edge labels and join results for constructing a single data frame with multiple edge labels targeted for updating.
```scala
scala> val newEdgeDataDF = personCreatedSoftwareDF.join(elmoCreatedTimerDF, Seq("src", "dst", "~label", "weight"), "full")
newEdgeDataDF: org.apache.spark.sql.DataFrame = [src: string, dst: string ... 2 more fields]

scala> newEdgeDataDF.show(false)
+------------------------------+----------------------------+-------+------+
|src                           |dst                         |~label |weight|
+------------------------------+----------------------------+-------+------+
|person:elmo:123-45-6789:4#33  |software:timer:2.0:groovy#00|created|100.0 |
|person:rocco:111-11-1111:21#11|software:chat:1.0:scala#20  |created|10.0  |
+------------------------------+----------------------------+-------+------+
```
Note, using the `src`, `dst`, and `~label` fields are the key components allowing a user to mix multiple edge labels in a single update operation.
For example, if we also had a "destroyed" edge label the data frame may look like this for updating/inserting both "created" and "destroyed" edge labels.
```scala
+------------------------------+----------------------------+---------+------+
|src                           |dst                         |~label   |weight|
+------------------------------+----------------------------+---------+------+
|person:rocco:111-11-1111:21#11|software:vlog:0.1:go#40     |destroyed|null  |
|person:rocco:111-11-1111:21#11|software:chat:1.0:scala#20  |created  |10.0  |
+------------------------------+----------------------------+---------+------+
```


##### Updating a single edge label (new API)
In most cases, a user is interested in updating a single edge label, and reasoning about the usage of `g.idcColumn()` may be less obvious for new users.
This new API was developed in response to these concerns and provides a simpler interface for common usage patterns.
```scala
def updateEdges(outVertexLabel: String, edgeLabel: String, inVertexLabel: String, df: DataFrame): Unit
```
The `df` data frame paramater contains the edge ID column names. Note, these column names should match the underlying 
table columns defined in DataStax Enterprise (DSE).

###### Example

Remember the `created` edge table is structured like this in DSE
```
CREATE TABLE test.person__created__software (
    person_name text,
    person_ssn text,
    person_age int,
    software_name text,
    software_version text,
    software_lang text,
    weight double,
    PRIMARY KEY ((person_name, person_ssn), person_age, software_name, software_version, software_lang)
)
```

Imagine we start with two data frames containing information on the vertices we wish to connect with an edge.
```scala
scala> roccoDF.show(false)
+-----+-----------+---+------------------+------------+
|name |ssn        |age|address           |coffeePerDay|
+-----+-----------+---+------------------+------------+
|rocco|111-11-1111|21 |123 sesame street|100         |
+-----+-----------+---+------------------+------------+

scala> chatDF.show(false)
+----+-------+-----+---------------+-----+
|name|version|lang |static_property|temp |
+----+-------+-----+---------------+-----+
|chat|1.0    |scala|united states  |mambo|
+----+-------+-----+---------------+-----+
```

We need to create single data frame containing the columns comprising the edge table's primary keys, with any additional properties we want to update.
```scala
scala> val df1 = roccoDF.select($"name" as "person_name", $"ssn" as "person_ssn", $"age" as "person_age")
df1: org.apache.spark.sql.DataFrame = [person_name: string, person_ssn: string ... 1 more field]

scala> val df2 = chatDF.select($"name" as "software_name", $"version" as "software_version", $"lang" as "software_lang", lit(100.0) as "weight")
df2: org.apache.spark.sql.DataFrame = [software_name: string, software_version: string ... 2 more fields]

scala> val createdDF = df1.crossJoin(df2)
createdDF: org.apache.spark.sql.DataFrame = [person_name: string, person_ssn: string ... 5 more fields]

scala> createdDF.show(false)
+-----------+-----------+----------+-------------+----------------+-------------+------+
|person_name|person_ssn |person_age|software_name|software_version|software_lang|weight|
+-----------+-----------+----------+-------------+----------------+-------------+------+
|rocco      |111-11-1111|21        |chat         |1.0             |scala        |100.0 |
+-----------+-----------+----------+-------------+----------------+-------------+------+
```

Now that `createdDF` contains all the necessary fields we simply provide it to `updateEdges` along with the vertex and edge label names.
```scala
scala> g.updateEdges("person", "created", "software", createdDF)

scala> g.E.show(false)
+------------------------------+--------------------------+-------+------+
|src                           |dst                       |~label |weight|
+------------------------------+--------------------------+-------+------+
|person:rocco:111-11-1111:21#11|software:chat:1.0:scala#20|created|100.0 |
+------------------------------+--------------------------+-------+------+
```
Note, this was a simplified example, `createdDF` can have several rows for a __single edge label__.

#### IOStep io() (TinkerPop 3.4)

The wrapper around updateEdges() and updateVertices() and df() methods. It provides import/export capabilities to 
DseGraphFrame with new TinkerPop API. The function parameter should be URL to the distributed file system. 
JDBC data source and some others do not use the url but only parameters, please still provide non-empty string for them

By default io().write() call will create to directory in provided DSEFS directory: "vertices" and "edges"
and store vertex and edge files in parquet format there.

```scala
scala> g.io("dsefs:///tmp/graph").write().iterate()

```

to restore saved data create new graph with the same schema and call: 

```scala
scala> g.io("dsefs:///tmp/graph").read().iterate()
```
The DGF IoStep follows TP extention convention. If directory name has "parquet", "csv", "orc", "json" extensions, 
coresponded format will be used for loading or saving data. "format" parameter override that convention

you can save and load data in any format supported by spark. use with() modificator to pass "format" and format related
options. Here is an example how to save graph with multi-line strings to CSV:

```scala
scala> g.io("dsefs:///tmp/graph").`with`("format", "csv").`with`("multiLine").write().iterate()
```
Format related options are spark options

Vertices and edges could be export and import separately. Use "vertices" and "edges" paramters for this

```scala
g.io("dsefs:///tmp/1.json").`with`("vertices").write().iterate()
g.io("dsefs:///tmp/1.json").`with`("edges").write().iterate()
```

One label edges can be loaded with decoded ids. see `updateEdges(outVertexLabel, edgeLabel, inVertexLabel, df)` documentation for details

```scala
g.io("dsefs://tmp/data.csv").`with`("outVertexLabel", "god").`with`("edgeLabel", "self").`with`("inVertexLabel", "god")
      .`with`("header").read().iterate()
```

To load singe vertex label use "vertexLabel" option:
```scala
g.io("dsefs://tmp/data.csv").`with`("vertexLabel", "god").`with`("header").read().iterate()
```
 
### Best Practices Guide for Loading Data
#### Common Pitfalls
##### Null unset issue can cause excessive tombstones
In version prior to 6.0.7 and 6.7.3, if a user omitted columns during DataStax Graph Frames edge updates, 
the missing columns fields were implicitly written to DSE with `null` values, causing unintended deletions, 
tombstone build up, and ultimately excessive stress on the system. 

The workaround at the time was to set `spark.setCassandraConf(Map("spark.cassandra.output.ignoreNulls" -> "true"))`, 
which will ignore unset or null-valued columns and not create unintended deletions on the server side. 
In DSE versions 6.0.7, 6.7.3, and higher the default for `ignoreNulls` is true.

##### Unintended caching can lead to OOM exceptions
Prior to DSE versions 5.1.14, 6.0.5, and 6.7.1, a problem existed such that during a DataStax Graph Frame bulk loading job, 
the Spark cache was being used by default, but not explicitly emptied. This lead to OutOfMemory(OOM) errors and other issues. 

`spark.dse.graphframes.update.persistLevel` Spark Cassandra Connector parameter was 
introduced that allows better control over Spark caching levels. 

Additionally, a new cache parameter was also introduced in the multi-label update methods that can be used as a workaround 
if the user wishes to explicitly uncache data after use. See [updateVertices()](#updatevertices) and [updateEdges()](#updateedges)
for more details.

##### How to workaround Materialized Views during bulk loading
When indexing with Materialized Views is desired, it is often recommended to enable this after the data has been loaded 
because it significantly affects insertions performance. We expect about a 10% performance penalty per MV, and there are 
some subtleties to be aware of when defining the data model, see this [blog](https://www.datastax.com/dev/blog/materialized-view-performance-in-cassandra-3-x) for more details.

After data is loaded, and one enables indexing, how do we know when it's done? There is a [nodetool viewbuildstatus](https://docs.datastax.com/en/dse/6.7/dse-admin/datastax_enterprise/tools/nodetool/toolsViewBuildStatus.html) command 
for accomplishing exactly this.

##### How to model multi/meta-properties
Multi and meta-properties are modeled differently in Classic DataStax Graph compared to the new DataStax Graph. In the new 
DataStax Graph, multi and meta-properties have been removed, and it is recommended to model them with collections/UDTs or
distinct elements. We will cover some of these details here. For broader guidance on migrating from Classic to the new
DataStax Graph see the [ClassicToCoreGraphMigration](ClassicToCoreGraphMigration.md) write-up.

###### How to manage multi/meta-properties for Classic DataStax Graph
Here is an example of updating vertex multi and meta-properties. Suppose we start with the schema shown directly below. 
Notice the god vertex has a multi-property called `nicknames`, which itself has meta-properties named `time` and `date`, 
we will show how to update all these properties.
```groovy
// properties
schema.propertyKey("time").Timestamp().single().create()
schema.propertyKey("reason").Text().single().create()
schema.propertyKey("age").Int().single().create()
schema.propertyKey("name").Text().single().create()
schema.propertyKey("date").Date().single().create()
schema.propertyKey("nicknames").Text().multiple().create()
schema.propertyKey("nicknames").properties("time", "date").add()

// vertex labels
schema.vertexLabel("god").properties("name", "age", "nicknames").create()schema.vertexLabel("god").index("god_age_index").secondary().by("age").add()
schema.vertexLabel("god").index("god_name_index").secondary().by("name").add()
...

// add vertex
Vertex jupiter = graph.addVertex(T.label, "god", "name", "jupiter", "age", 5000);
Vertex juno = graph.addVertex(T.label, "god", "name", "juno", "age", 5000);
Vertex minerva = graph.addVertex(T.label, "god", "name", "minerva", "age", 5000);
```

This gives us something like this to start with. Notice none of the vertices have nicknames set yet, omitted from the
gremlin console and represented with `null` from the DataStax Graph Frames view.
```groovy
gremlin> g.V().has("name", "jupiter").properties().valueMap(true)
==>{id={~label=age, ~out_vertex={~label=god, community_id=827187456, member_id=0}, ~local_id=00000000-0000-8001-0000-000000000000}, key=age, value=5000}
==>{id={~label=name, ~out_vertex={~label=god, community_id=827187456, member_id=0}, ~local_id=00000000-0000-8002-0000-000000000000}, key=name, value=jupiter}

gremlin> g.V().has("name", "juno").properties().valueMap(true)
==>{id={~label=age, ~out_vertex={~label=god, community_id=1316484224, member_id=0}, ~local_id=00000000-0000-8001-0000-000000000000}, key=age, value=5000}
==>{id={~label=name, ~out_vertex={~label=god, community_id=1316484224, member_id=0}, ~local_id=00000000-0000-8002-0000-000000000000}, key=name, value=juno}

gremlin> g.V().has("name", "minerva").properties().valueMap(true)
==>{id={~label=age, ~out_vertex={~label=god, community_id=2114931072, member_id=0}, ~local_id=00000000-0000-8001-0000-000000000000}, key=age, value=5000}
==>{id={~label=name, ~out_vertex={~label=god, community_id=2114931072, member_id=0}, ~local_id=00000000-0000-8002-0000-000000000000}, key=name, value=minerva}
```

Here is what it looks like from DataStax Graph Frames (DGF).
```scala
scala> val g = spark.dseGraph("gods")

scala> g.V().show(false)
+--------------------+------+------------+---------+-------+----+---------+
|id                  |~label|community_id|member_id|name   |age |nicknames|
+--------------------+------+------------+---------+-------+----+---------+
|god:MU3hAAAAAAAAAAAA|god   |827187456   |0        |jupiter|5000|null     |
|god:Tnf0gAAAAAAAAAAA|god   |1316484224  |0        |juno   |5000|null     |
|god:fg9JgAAAAAAAAAAA|god   |2114931072  |0        |minerva|5000|null     |
+--------------------+------+------------+---------+-------+----+---------+
```

Now let’s see how we can add nicknames and its meta-properties. First construct a data frame consisting of `community_id` and 
`member_id` of the vertex in question, along with the nicknames property and meta-properties we wish to update.
```scala
scala> val df = Seq(("827187456", "0", "overlords", java.sql.Date.valueOf("2017-01-01"), new java.sql.Timestamp(100L))).toDF("community_id", "member_id", "nicknames", "date", "time")

scala> df.show(false)
+------------+---------+---------+----------+---------------------+
|community_id|member_id|nicknames|date      |time                 |
+------------+---------+---------+----------+---------------------+
|827187456   |0        |overlords|2017-01-01|1969-12-31 16:00:00.1|
+------------+---------+---------+----------+---------------------+
```

Now we create a new data frame consisting of just the id of the vertex and the nickname property to update. Notice special 
care is taken in constructing this `id` field, composed of values of the known `~label`, `community_id`, and `member_id`.
```scala
scala> val updateDF = df.select(g.idColumn("god", $"community_id", $"member_id") as "id", array(struct($"nicknames", $"date", $"time")) as "nicknames")
```

Notice how we constructed the nicknames fields in this data frame, it is an array of `struct` types.
```scala
scala> updateDF.printSchema
root
 |-- id: string (nullable = false)
 |-- nicknames: array (nullable = false)
 |    |-- element: struct (containsNull = false)
 |    |    |-- nicknames: string (nullable = true)
 |    |    |-- date: date (nullable = true)
 |    |    |-- time: timestamp (nullable = true)

scala> updateDF.show(false)
+--------------------+------------------------------------------------+
|id                  |nicknames                                       |
+--------------------+------------------------------------------------+
|god:MU3hAAAAAAAAAAAA|[[overlords, 2017-01-01, 1969-12-31 16:00:00.1]]|
+--------------------+------------------------------------------------+
```

Now we update vertices using this updated data frame. Notice we are using the multi-vertex label flavor of the vertex
update method described in [Updating multiple vertex labels](#updating-multiple-vertex-labels).
```scala
scala> g.updateVertices(updateDF)

scala> g.V().show(false)
+--------------------+------+------------+---------+-------+----+------------------------------------------------+
|id                  |~label|community_id|member_id|name   |age |nicknames                                       |
+--------------------+------+------------+---------+-------+----+------------------------------------------------+
|god:MU3hAAAAAAAAAAAA|god   |827187456   |0        |jupiter|5000|[[overlords, 2017-01-01, 1969-12-31 16:00:00.1]]|
|god:Tnf0gAAAAAAAAAAA|god   |1316484224  |0        |juno   |5000|null                                            |
|god:fg9JgAAAAAAAAAAA|god   |2114931072  |0        |minerva|5000|null                                            |
+--------------------+------+------------+---------+-------+----+------------------------------------------------+
```

Another less commonly used approach is to use TinkerPop updates with DataStax Graph Frames
```scala
scala> g.V().has("community_id", "827187456").has("member_id", "0").has("~label", "god").property("nicknames", "overlords", "date", java.sql.Date.valueOf("2017-01-01"), "time", new java.sql.Timestamp(100L)).iterate()
```

It is worth noting that regardless of the approach used, updateVertices or TinkerPop updates with DGF, multi-properties are append only. In the example below, see what happens when we repeatedly update the nicknames property of the same vertex.
```scala
scala> g.V().has("community_id", "827187456").has("member_id", "0").has("~label", "god").property("nicknames", "overlords", "date", java.sql.Date.valueOf("2017-01-01"), "time", new java.sql.Timestamp(100L)).iterate()

scala> g.V().show(false)
+--------------------+------+------------+---------+-------+----+------------------------------------------------------------------------------------------------+
|id                  |~label|community_id|member_id|name   |age |nicknames                                                                                       |
+--------------------+------+------------+---------+-------+----+------------------------------------------------------------------------------------------------+
|god:fg9JgAAAAAAAAAAA|god   |2114931072  |0        |minerva|5000|null                                                                                            |
|god:MU3hAAAAAAAAAAAA|god   |827187456   |0        |jupiter|5000|[[overlords, 2017-01-01, 1969-12-31 16:00:00.1], [overlords, 2017-01-01, 1969-12-31 16:00:00.1]]|
|god:Tnf0gAAAAAAAAAAA|god   |1316484224  |0        |juno   |5000|null                                                                                            |
+--------------------+------+------------+---------+-------+----+------------------------------------------------------------------------------------------------+
```

Now update one more time using the same data frame used previously.
```scala
scala> g.updateVertices(updateDF)

scala> g.V().show(false)
+--------------------+------+------------+---------+-------+----+------------------------------------------------------------------------------------------------------------------------------------------------+
|id                  |~label|community_id|member_id|name   |age |nicknames                                                                                                                                       |
+--------------------+------+------------+---------+-------+----+------------------------------------------------------------------------------------------------------------------------------------------------+
|god:fg9JgAAAAAAAAAAA|god   |2114931072  |0        |minerva|5000|null                                                                                                                                            |
|god:MU3hAAAAAAAAAAAA|god   |827187456   |0        |jupiter|5000|[[overlords, 2017-01-01, 1969-12-31 16:00:00.1], [overlords, 2017-01-01, 1969-12-31 16:00:00.1], [overlords, 2017-01-01, 1969-12-31 16:00:00.1]]|
|god:Tnf0gAAAAAAAAAAA|god   |1316484224  |0        |juno   |5000|null                                                                                                                                            |
+--------------------+------+------------+---------+-------+----+------------------------------------------------------------------------------------------------------------------------------------------------+
```

As expected, now we see the jupiter vertex with `nicknames` set with meta-properties.
```groovy
gremlin> g.V().has("name", "jupiter").properties().valueMap(true)
==>{id={~label=age, ~out_vertex={~label=god, community_id=827187456, member_id=0}, ~local_id=00000000-0000-8001-0000-000000000000}, key=age, value=5000}
==>{id={~label=name, ~out_vertex={~label=god, community_id=827187456, member_id=0}, ~local_id=00000000-0000-8002-0000-000000000000}, key=name, value=jupiter}
==>{id={~label=nicknames, ~out_vertex={~label=god, community_id=827187456, member_id=0}, ~local_id=5c43be20-468c-11e9-abad-a5d0ff50b75d}, key=nicknames, value=overlords, date=2017-01-01, time=1970-01-01T00:00:00.100Z}
==>{id={~label=nicknames, ~out_vertex={~label=god, community_id=827187456, member_id=0}, ~local_id=98261110-468f-11e9-abad-a5d0ff50b75d}, key=nicknames, value=overlords, date=2017-01-01, time=1970-01-01T00:00:00.100Z}
==>{id={~label=nicknames, ~out_vertex={~label=god, community_id=827187456, member_id=0}, ~local_id=00000000-0000-8004-0000-000000000000}, key=nicknames, value=overlords, date=2017-01-01, time=1970-01-01T00:00:00.100Z}
```

###### How to manage multi/meta-properties for the new DataStax Graph

With the new DataStax Graph engine, a user has two options for handling multi and meta-properties: use CQL collection 
types or drop them altogether. Details on data modeling changes between the Classic DataStax Graph and the new DataStax Graph 
Engine can be found in our [ClassicToCoreGraphMigration](ClassicToCoreGraphMigration.md) write-up. Here is an example 
of updating vertex labels with complex types which includes User Defined Types (UDTs), collections, and nested collections.

Here is how we can define the new DataStax Graph data model using complex types.
```groovy
system.graph("complex").create()
:remote config alias g complex.g

schema.type('udt1').ifNotExists().property('name', Varchar).create()

schema.vertexLabel('collection').ifNotExists().partitionBy('name', Varchar).property('frozenList', frozen(listOf(Int))).property('frozenMap', frozen(mapOf(Int, Varchar))).property('frozenSet', frozen(setOf(Int))).property('list', listOf(Int)).property('map', mapOf(Int, Varchar)).property('set', setOf(Int)).create()

schema.vertexLabel('person').ifNotExists().partitionBy('name', Varchar).property('udt1', typeOf('udt1')).property('udt2', mapOf(Varchar, frozen(typeOf('udt1')))).create()

schema.vertexLabel('software').ifNotExists().partitionBy('name', Varchar).property('versions1', tupleOf(Int, Varchar)).property('versions2', tupleOf(frozen(setOf(Int)), frozen(listOf(Varchar)))).create()

// add data
g.addV('person').property( 'name', 'A').property('udt1', typeOf('udt1').create('B')).property('udt2', ['B': typeOf('udt1').create('C')])

g.addV('software').property( 'name', 'B').property('versions1', tupleOf(Int, Varchar).create(2, '3')).property('versions2', tupleOf(frozen(setOf(Int)), frozen(listOf(Varchar))).create([1, 2, 3].toSet(), ['1', '2', '3']))

g.addV('collection').property( 'name', 'C').property('map', [1:'1', 2:'2', 3:'3']).property('frozenMap', [1:'1', 2:'2', 3:'3']).property('set', [1, 2, 3].toSet()).property('frozenSet', [1, 2, 3].toSet()).property('list', [1, 2, 3]).property('frozenList', [1, 2, 3])
```

From DataStax Graph Frames (DGF) it looks like this.
```scala
scala> val g = spark.dseGraph(“complex”)

scala> g.V.show(false)
+---------------+----------+----+---------+----------------------+----+----------+----------+------------------------+---------+---------+------------------------+---------+
|id             |~label    |name|versions1|versions2             |udt1|udt2      |frozenList|frozenMap               |frozenSet|list     |map                     |set      |
+---------------+----------+----+---------+----------------------+----+----------+----------+------------------------+---------+---------+------------------------+---------+
|software:B#19  |software  |B   |[2, 3]   |[[1, 2, 3], [1, 2, 3]]|null|null      |null      |null                    |null     |null     |null                    |null     |
|person:A#10    |person    |A   |null     |null                  |[B] |[B -> [C]]|null      |null                    |null     |null     |null                    |null     |
|collection:C#11|collection|C   |null     |null                  |null|null      |[1, 2, 3] |[1 -> 1, 2 -> 2, 3 -> 3]|[1, 2, 3]|[1, 2, 3]|[1 -> 1, 2 -> 2, 3 -> 3]|[1, 2, 3]|
+---------------+----------+----+---------+----------------------+----+----------+----------+------------------------+---------+---------+------------------------+---------+
```

Let’s look at some examples with the data frame constructed from scratch. Suppose we want to update the software vertex 
in this example and change the `versions1` tuple values, we start by defining a data frame with `name` (partition key) and 
the column of interest, `versions1` in this case. Notice we use a scala tuple to wrap the new values to be inserted.
```scala
scala> val df = Seq(("B", (20, "30"))).toDF("name", "versions1")

scala> df.printSchema
root
 |-- name: string (nullable = true)
 |-- versions1: struct (nullable = true)
 |    |-- _1: integer (nullable = false)
 |    |-- _2: string (nullable = true)
```

We then create a data frame consisting of the properly formatted `id`, which is constructed from the `~label` and `id`, and the `versions1` field.
```scala
scala> val updateDF = df.select(g.idColumn("software", $"name") as "id", $"versions1")


scala> updateDF.show(false)
+-------------+---------+
|id           |versions1|
+-------------+---------+
|software:B#19|[20, 30] |
+-------------+---------+

scala> g.updateVertices(updateDF)

scala> g.V().show(false)
+---------------+----------+----+---------+----------------------+----+----------+----------+------------------------+---------+---------+------------------------+---------+
|id             |~label    |name|versions1|versions2             |udt1|udt2      |frozenList|frozenMap               |frozenSet|list     |map                     |set      |
+---------------+----------+----+---------+----------------------+----+----------+----------+------------------------+---------+---------+------------------------+---------+
|software:B#19  |software  |B   |[20, 30] |[[1, 2, 3], [1, 2, 3]]|null|null      |null      |null                    |null     |null     |null                    |null     |
|person:A#10    |person    |A   |null     |null                  |[B] |[B -> [C]]|null      |null                    |null     |null     |null                    |null     |
|collection:C#11|collection|C   |null     |null                  |null|null      |[1, 2, 3] |[1 -> 1, 2 -> 2, 3 -> 3]|[1, 2, 3]|[1, 2, 3]|[1 -> 1, 2 -> 2, 3 -> 3]|[1, 2, 3]|
+---------------+----------+----+---------+----------------------+----+----------+----------+------------------------+---------+---------+------------------------+---------+
```

Now let’s update the `versions2` column, we start by reminding ourselves what the schema looks like.
```scala
scala> g.V.printSchema
root
 |-- id: string (nullable = true)
 |-- ~label: string (nullable = false)
 |-- name: string (nullable = false)
 |-- versions1: struct (nullable = true)
 |    |-- 0: integer (nullable = true)
 |    |-- 1: string (nullable = true)
 |-- versions2: struct (nullable = true)
 |    |-- 0: array (nullable = true)
 |    |    |-- element: integer (containsNull = true)
 |    |-- 1: array (nullable = true)
 |    |    |-- element: string (containsNull = true)
 |-- udt1: struct (nullable = true)
 |    |-- name: string (nullable = true)
 |-- udt2: map (nullable = true)
 |    |-- key: string
 |    |-- value: struct (valueContainsNull = true)
 |    |    |-- name: string (nullable = true)
 |-- frozenList: array (nullable = true)
 |    |-- element: integer (containsNull = true)
 |-- frozenMap: map (nullable = true)
 |    |-- key: integer
 |    |-- value: string (valueContainsNull = true)
 |-- frozenSet: array (nullable = true)
 |    |-- element: integer (containsNull = true)
 |-- list: array (nullable = true)
 |    |-- element: integer (containsNull = true)
 |-- map: map (nullable = true)
 |    |-- key: integer
 |    |-- value: string (valueContainsNull = true)
 |-- set: array (nullable = true)
 |    |-- element: integer (containsNull = true)
```

Okay so we need to construct a data frame that matches this expectation, this is how we do it.
```scala
scala> val df = Seq(("B", (Seq(100,200,300).toSet, Seq("100","200","300").toList))).toDF("name", "versions2")

scala> df.printSchema
root
 |-- name: string (nullable = true)
 |-- versions2: struct (nullable = true)
 |    |-- _1: array (nullable = true)
 |    |    |-- element: integer (containsNull = false)
 |    |-- _2: array (nullable = true)
 |    |    |-- element: string (containsNull = true)
```

Now we simply massage it to properly format the `id` column as we’ve done before.
```scala
scala> val updateDF = df.select(g.idColumn("software", $"name") as "id", $"versions2")

scala> updateDF.show(false)
+-------------+----------------------------------+
|id           |versions2                         |
+-------------+----------------------------------+
|software:B#19|[[100, 200, 300], [100, 200, 300]]|
+-------------+----------------------------------+

scala> g.updateVertices(updateDF)
```

As we can see the `versions2` column has been properly updated with the new values.
```scala
scala> g.V().show(false)
+---------------+----------+----+---------+----------------------------------+----+----------+----------+------------------------+---------+---------+------------------------+---------+
|id             |~label    |name|versions1|versions2                         |udt1|udt2      |frozenList|frozenMap               |frozenSet|list     |map                     |set      |
+---------------+----------+----+---------+----------------------------------+----+----------+----------+------------------------+---------+---------+------------------------+---------+
|software:B#19  |software  |B   |[20, 30] |[[100, 200, 300], [100, 200, 300]]|null|null      |null      |null                    |null     |null     |null                    |null     |
|person:A#10    |person    |A   |null     |null                              |[B] |[B -> [C]]|null      |null                    |null     |null     |null                    |null     |
|collection:C#11|collection|C   |null     |null                              |null|null      |[1, 2, 3] |[1 -> 1, 2 -> 2, 3 -> 3]|[1, 2, 3]|[1, 2, 3]|[1 -> 1, 2 -> 2, 3 -> 3]|[1, 2, 3]|
+---------------+----------+----+---------+----------------------------------+----+----------+----------+------------------------+---------+---------+------------------------+---------+
```

Confirmed, the data looks good outside of DataStax Graph Frames as expected.
```groovy
gremlin> g.V().hasLabel("software").valueMap(true)
==>{id=software:B#19, label=software, versions2=[({100,200,300},['100','200','300'])], versions1=[(20,'30')], name=[B]}
```

These same principles apply when updating other collection types. Here is an example for updating the map column.
```scala
scala> val df = Seq(("C", Map(10->"10", 20->"20", 30->"30"))).toDF("name", "map")

scala> df.printSchema
root
 |-- name: string (nullable = true)
 |-- map: map (nullable = true)
 |    |-- key: integer
 |    |-- value: string (valueContainsNull = true)


scala> df.show(false)
+----+------------------------------+
|name|map                           |
+----+------------------------------+
|C   |[10 -> 10, 20 -> 20, 30 -> 30]|
+----+------------------------------+


scala> val updateDF = df.select(g.idColumn("collection", $"name") as "id", $"map")

scala> updateDF.show(false)
+---------------+------------------------------+
|id             |map                           |
+---------------+------------------------------+
|collection:C#11|[10 -> 10, 20 -> 20, 30 -> 30]|
+---------------+------------------------------+


scala> g.updateVertices(updateDF)

scala> g.V.show(false)
+---------------+----------+----+---------+----------------------------------+----+----------+----------+------------------------+---------+---------+------------------------------+---------+
|id             |~label    |name|versions1|versions2                         |udt1|udt2      |frozenList|frozenMap               |frozenSet|list     |map                           |set      |
+---------------+----------+----+---------+----------------------------------+----+----------+----------+------------------------+---------+---------+------------------------------+---------+
|software:B#19  |software  |B   |[20, 30] |[[100, 200, 300], [100, 200, 300]]|null|null      |null      |null                    |null     |null     |null                          |null     |
|person:A#10    |person    |A   |null     |null                              |[B] |[B -> [C]]|null      |null                    |null     |null     |null                          |null     |
|collection:C#11|collection|C   |null     |null                              |null|null      |[1, 2, 3] |[1 -> 1, 2 -> 2, 3 -> 3]|[1, 2, 3]|[1, 2, 3]|[10 -> 10, 20 -> 20, 30 -> 30]|[1, 2, 3]|
+---------------+----------+----+---------+----------------------------------+----+----------+----------+------------------------+---------+---------+------------------------------+---------+
```

Let’s look at one more example, updating the `udt2` column that is a map consisting of a struct. In this case we can use 
a case class to accomplish this goal.
```scala
scala> case class udt2Value(name: String)

scala> val df = Seq(("A",   Map("Rocco"->udt2Value("likes tacos")))).toDF("name", "udt2")

scala> val updateDF = df.select(g.idColumn("person", $"name") as "id", $"udt2")

scala> updateDF.printSchema
root
 |-- id: string (nullable = true)
 |-- udt2: map (nullable = true)
 |    |-- key: string
 |    |-- value: struct (valueContainsNull = true)
 |    |    |-- name: string (nullable = true)

scala> updateDF.show(false)
+-----------+------------------------+
|id         |udt2                    |
+-----------+------------------------+
|person:A#10|[Rocco -> [likes tacos]]|
+-----------+------------------------+

scala> g.updateVertices(updateDF)

scala> g.V().show(false)
+---------------+----------+----+---------+----------------------------------+----+------------------------+----------+------------------------+---------+---------+------------------------------+---------+
|id             |~label    |name|versions1|versions2                         |udt1|udt2                    |frozenList|frozenMap               |frozenSet|list     |map                           |set      |
+---------------+----------+----+---------+----------------------------------+----+------------------------+----------+------------------------+---------+---------+------------------------------+---------+
|software:B#19  |software  |B   |[20, 30] |[[100, 200, 300], [100, 200, 300]]|null|null                    |null      |null                    |null     |null     |null                          |null     |
|person:A#10    |person    |A   |null     |null                              |[B] |[Rocco -> [likes tacos]]|null      |null                    |null     |null     |null                          |null     |
|collection:C#11|collection|C   |null     |null                              |null|null                    |[1, 2, 3] |[1 -> 1, 2 -> 2, 3 -> 3]|[1, 2, 3]|[1, 2, 3]|[10 -> 10, 20 -> 20, 30 -> 30]|[1, 2, 3]|
+---------------+----------+----+---------+----------------------------------+----+------------------------+----------+------------------------+---------+---------+------------------------------+---------+
```

##### Multi-edge updates should include UUIDs to ensure idempotent upserts
In Classic DataStax Graph, when updating edges users should provide a valid and unique UUID for the `id` column. Here we will 
show two examples of using `updateEdges`, one in which we reuse an existing row’s data, and another that shows how to 
explicitly set the `id` with a UUID.

Suppose we start with the following graph schema, our examples will look at updates with the “lives” edge label.
```groovy
// truncated example for brevity
schema.propertyKey("nicknames").Text().multiple().create()
schema.propertyKey("reason").Text().single().create()
schema.propertyKey("age").Int().single().create()
schema.propertyKey("name").Text().single().create()
schema.propertyKey("date").Date().single().create()
schema.propertyKey("nicknames").properties("time", "date").add()

schema.vertexLabel("location").properties("name").create()
schema.vertexLabel("god").properties("name", "age", "nicknames").create()
schema.vertexLabel("god").index("god_age_index").secondary().by("age").add()
schema.vertexLabel("god").index("god_name_index").secondary().by("name").add()

schema.edgeLabel("lives").multiple().properties("reason").create()
schema.edgeLabel("lives").connection("god", "location").add()

// Add data
Vertex neptune = graph.addVertex(T.label, "god", "name", "neptune", "age", 4500);
Vertex sea = graph.addVertex(T.label, "location", "name", "sea");

neptune.addEdge("lives", sea).property("reason", "loves waves");
```

Here is what the edges table looks like from the DataStax Graph Frames perspective (truncated). Make note of the 
automatically-generated UUID generated for this edge, we will refer to this later.
```java
DseGraphFrame gf = DseGraphFrameBuilder.dseGraph(keyspace, spark);
gf.E().df().show(false);

+------------------------+-------------------------+-------+------------------------------------+-----------------------+----------------------------------+-------------------+
|src                     |dst                      |~label |id                                  |time                   |name                              |reason             |
+------------------------+-------------------------+-------+------------------------------------+-----------------------+----------------------------------+-------------------+
|god:RnS4AAAAAAAAAAAE    |location:RnS4AAAAAAAAAAAJ|lives  |f695a6b0-4500-11e9-8e88-fb68397e4bea|null                   |null                              |loves waves        |
+------------------------+-------------------------+-------+------------------------------------+-----------------------+----------------------------------+-------------------+
```

###### Using an existing row’s data

This is how we grab edge(s) that have a `reason` column set to “loves waves”, then overwrite this setting to “New”.

```java
DseGraphFrame gf = DseGraphFrameBuilder.dseGraph(keyspace, spark);
Dataset<Row> u = gf.gf().edges().filter("reason = 'loves waves'").drop("time").drop("reason").drop("name").withColumn("reason", functions.lit("New"));
```

This gives us the following dataset that will be used to update the edge(s). Notice that because we are using an existing 
row and simply reinserting it with a new reason, we get the unique id for free.
```java
u.show(false);
+--------------------+-------------------------+------+------------------------------------+------+
|src                 |dst                      |~label|id                                  |reason|
+--------------------+-------------------------+------+------------------------------------+------+
|god:RnS4AAAAAAAAAAAE|location:RnS4AAAAAAAAAAAJ|lives |f695a6b0-4500-11e9-8e88-fb68397e4bea|New   |
+--------------------+-------------------------+------+------------------------------------+------+
```

Updating the edge(s) can be accomplished by simply calling the following command
```java
gf.updateEdges(u);
```

As expected we see the edge with id "f695a6b0-4500-11e9-8e88-fb68397e4bea" has the reason set to “New”.
```java
gf.E().df().show(false);
+------------------------+-------------------------+-------+------------------------------------+-----------------------+----------------------------------+-------------------+
|src                     |dst                      |~label |id                                  |time                   |name                              |reason             |
+------------------------+-------------------------+-------+------------------------------------+-----------------------+----------------------------------+-------------------+
|god:RnS4AAAAAAAAAAAE    |location:RnS4AAAAAAAAAAAJ|lives  |f695a6b0-4500-11e9-8e88-fb68397e4bea|null                   |null                              |New                |
+------------------------+-------------------------+-------+------------------------------------+-----------------------+----------------------------------+-------------------+
```

###### Explicitly setting the “id” column
That was a simple example, but how does one extract this `id` field and explicitly use it when constructing the dataset 
when updating edges? It’s actually very simple.

Let’s start with the dataset used in the previous example
```java
DseGraphFrame gf = DseGraphFrameBuilder.dseGraph(keyspace, spark);
Dataset<Row> u = gf.gf().edges().filter("reason = 'loves waves'").drop("time").drop("reason").drop("name").withColumn("reason", functions.lit("New"));

u.show(false);
+--------------------+-------------------------+------+------------------------------------+------+
|src                 |dst                      |~label|id                                  |reason|
+--------------------+-------------------------+------+------------------------------------+------+
|god:RnS4AAAAAAAAAAAE|location:RnS4AAAAAAAAAAAJ|lives |f695a6b0-4500-11e9-8e88-fb68397e4bea|New   |
+--------------------+-------------------------+------+------------------------------------+------+
```

Now to extract the unique id, simply do this
```java
String newUUID = u.collectAsList().get(0).getString(3);
```

In this case we know we have only one row so we use `get(0)`, and we know the `id` column is at index 3 so we use `getString(3)`. 

Now to explicitly set the id field and update the edge we would do this.
```java
u = gf.gf().edges().filter("reason = 'New'").drop(“id”).drop("time").drop("reason").drop("name")
       .withColumn("reason", functions.lit("New1"))
       .withColumn("id", functions.lit(newUUID));

gf.updateEdges(u);
```

For demonstration purposes, we explicitly set the `id` columns with the UUID previously saved and inserted a new reason for this row.
```java
gf.E().df().show(false);
+------------------------+-------------------------+-------+------------------------------------+-----------------------+----------------------------------+-------------------+
|src                     |dst                      |~label |id                                  |time                   |name                              |reason             |
+------------------------+-------------------------+-------+------------------------------------+-----------------------+----------------------------------+-------------------+
|god:RnS4AAAAAAAAAAAE    |location:RnS4AAAAAAAAAAAJ|lives  |f695a6b0-4500-11e9-8e88-fb68397e4bea|null                   |null                              |New1                |
+------------------------+-------------------------+-------+------------------------------------+-----------------------+----------------------------------+-------------------+
```

In general, it’s important to note that when updating edges you must include the `id` column in the dataset for the _existing edges to be updated_. 
If a user omits the `id` column and instead only supplies the `src`, `dst`, and `~label`, they will end up duplicating 
edges with auto-generated IDs.

##### Key order matters when using the idColumn() method
When using the `idColumn()` method for vertices that have multiple key columns it is important to pass the key columns 
into the function in the same order in which they are defined in the schema.

Suppose we have the following name vertex label schema
```groovy
schema.vertexLabel("name")
        .partitionKey("internal_party_id")
        .clusteringKey(
        "prefx_nm",
        "first_nm",
        "mdl_nm",
        "last_nm",
        "sufx_nm",
        "name_line_desc"
)
```

Be careful when passing the keys to the `idColumn()` method, the order must match that defined in the schema. Here is an example of the correct way to pass in these keys.
```scala
scala> val hasNameEdges = nameVertices
  .drop(col("~label"))
  .withColumn("src", nameVertices.col("partyId"))
  .withColumn("dst", g.idColumn(
    lit("name"),
    nameVertices.col("internal_party_id"),
    nameVertices.col("prefx_nm"),
    nameVertices.col("first_nm"),
    nameVertices.col("mdl_nm"),
    nameVertices.col("last_nm"),
    nameVertices.col("sufx_nm"),
    nameVertices.col("name_line_desc")
  ))
  .withColumn("~label", lit("has_name"))
  .drop(col("internal_party_id"))
  .drop(col("partyId"))
  .drop(col("first_nm"))
  .drop(col("last_nm"))
  .drop(col("mdl_nm"))
  .drop(col("name_line_desc"))
  .drop(col("prefx_nm"))
  .drop(col("sufx_nm"))

scala> g.updateEdges(hasNameEdges)
```

The new API for updating single labels was introduced to address this issue and make the user experience more frictionless, 
see [updateVertices](#updatevertices) and [updateEdges](#updateedges) for more details.

#### Tuning Considerations for Loading Big Graphs
##### Spark Cassandra Connector tuning parameters still apply with DataStax Graph Frames
To increase write performance during DataStax Graph Frames (DGF) bulk loading, remember that our existing Spark Cassandra Connector 
tuning parameters still apply: [Setting Spark Cassandra Connector Specific Properties](https://docs.datastax.com/en/dse/6.7/dse-dev/datastax_enterprise/spark/sparkCassandraProperties.html?hl=setting%2Cspark%2Ccassandra%2Cconnector-specific%2Cproperties) 

For example, `spark.cassandra.output.concurrent.writes` has been found to be one of the most intuitive and effective parameters 
to play with during load testing. Other parameters such as `spark.cassandra.output.throughput_mb_per_sec` can be very helpful as well, 
especially in cases where one expects a long insertion workload it may be wise to down-tune this appropriately to avoid 
overwhelming the database cluster.

The `spark.cassandra.connection.keepAliveMs` may also be useful in scenarios with long running insertion workloads where 
connections may experience longer than expected periods of inactivity, a potential side-effect of periodic delays while 
processing insertions/updates on the server.

Here are examples of using these parameters:
```
dse spark-submit \
  --conf "spark.cassandra.output.concurrent.writes=100" \
  --conf "spark.cassandra.connection.keepAliveMS=120000" \
  --executor-memory=8g \
  --class com.datastax.DataImport target/data-import-1.0-SNAPSHOT.jar \
  newapi
```

##### Avoid over tuning your application on a small dataset
Be careful when tuning with a small dataset, very likely parameters tuned for short insertion workload will not behave 
similarly for longer more intensive workloads. A longer sustained insertion workload will lead to more data and more 
severe effects from background tasks such as memtable flushing, compaction, query routing, etc. In short, I recommend an 
incremental approach when loading data. Try loading say 10-20% of the data, making note of parameters, cluster size, 
and overall node health during the process (e.g. look out for obvious things like timeout exceptions, etc).

Also, increasing the cluster size can serve as an effective strategy in reducing individual node stress and improving 
overall ingestion performance. Again there is not a one-size-fits-all solution here, but an incremental approach with 
reasonably chosen tuning parameters and environment setup is a good approach.

### How-to

#### Copy a graph from one cluster to another

In DSE versions 5.1.15+, 6.0.8+, 6.7.4+, and 6.8.0+ (DSP-18605) we have the ability to specify which host a DseGraphFrame object should connect with. This allows a user to read graph contents from one cluster and write to another.

Suppose we want to copy vertices and edges from a remote cluster to the local cluster.
```scala
spark.setCassandraConf("cluster1", CassandraConnectorConf.ConnectionHostParam.option("10.0.0.1"))
spark.setCassandraConf("cluster2", CassandraConnectorConf.ConnectionHostParam.option("10.0.0.2"))

spark.conf.set("cluster", "cluster1")
val source = spark.dseGraph("srcGraph")

spark.conf.set("cluster", "cluster2")
val dst = spark.dseGraph("dstGraph")

dst.updateVertices(src.V)
dst.updateEdges(src.E)
```

In DSE 6.8 and newer, we can inline the cluster mapping step when creating the DseGraphFrame object.
```scala
spark.setCassandraConf("cluster1", CassandraConnectorConf.ConnectionHostParam.option("10.0.0.1"))
spark.setCassandraConf("cluster2", CassandraConnectorConf.ConnectionHostParam.option("10.0.0.2"))

val source =  spark.dseGraph("srcGraph", Map ("cluster" -> "cluster1") )
val dst =  spark.dseGraph("dstGraph", Map ("cluster" -> "cluster2"))

dst.updateVertices(src.V)
dst.updateEdges(src.E)

```