# CQL Grammar to specify Graph Metadata on Keyspaces/Tables
CQL extensions have been added to allow creation and maintenance of graph keyspaces directly via CQL.

## How to specify that a Keyspace should be a Graph?
In order to treat a keyspace as a graph, one needs to specify **graph_engine** when creating/altering a keyspace:
```
ALTER KEYSPACE test WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1} AND graph_engine = 'Core';
```
## Which Graph Engines are available?
The only engine type is **Core**. There's also **Classic** engine, but this can't be specified on a keyspace, because a **Classic** graph consists of multiple keyspaces and needs to be created via the traditional System API.

`system_schema.keyspaces` will also properly reflect the graph engine:

```
cqlsh> select * from system_schema.keyspaces;

 keyspace_name      | durable_writes | graph_engine | replication
--------------------+----------------+--------------+-------------------------------------------------------------------------------------
               test |           True |         Core | {'class': 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '1'}
        system_auth |           True |         null | {'class': 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '1'}
 ...
             system |           True |         null |                             {'class': 'org.apache.cassandra.locator.LocalStrategy'}
           dse_perf |           True |         null | {'class': 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '1'}
      system_traces |           True |         null | {'class': 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '2'}
       dse_security |           True |         null | {'class': 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '1'}

```

It is important to note that if a keyspace does not have **graph_engine** set, it won't be recognized as a graph.

## What's the difference between the Graph engines?

**Core** requires a keyspace and tables to specify how the Graph schema looks like. **Core** will keep all data of the graph in those tables.

---

## How to specify that a table should be a Vertex Label?
This can be achieved by specifying `VERTEX LABEL <optionalName>` when creating/altering a table.
If no name is specified, then the label will be equal to the table name.

```
CREATE TABLE test.person (
    firstname text,
    lastname text,
    age int,
    jobtitle text,
    phone text,
    PRIMARY KEY ((firstname, lastname), age)
) WITH CLUSTERING ORDER BY (age ASC) AND VERTEX LABEL person_label;
```

Another vertex label `software_label` is being created, which will be used in later examples.
```
CREATE TABLE test.software (
    software_name text,
    version_info text,
    software_age int,
    description text,
    PRIMARY KEY ((software_name, version_info), software_age)
) WITH CLUSTERING ORDER BY (software_age ASC) AND VERTEX LABEL software_label;

```

`system_schema.vertices` keeps track of all existing vertex labels and their keyspace/table names.
```
cqlsh> select * from system_schema.vertices;

 keyspace_name | table_name | label_name
---------------+------------+--------------
          test |     person | person_label
          test |   software | software_label
```

---
**NOTE**

A vertex label name must be unique within a keyspace. Also one table can only have one graph label definition.

---

## How to specify an Edge Label and what are the schema layout requirements?

This can be done through `EDGE LABEL <optionalName> FROM vLabelOne(...) TO vLabelTwo(...)`. If no name is specified, then the label will be equal to the table name.
The below example shows how to create the connection `person-created->software`:
```
CREATE TABLE test."personCreatedSoftware" (
    person_firstname text,
    person_lastname text,
    person_age int,
    sw_name text,
    sw_version_info text,
    sw_age int,
    creation_date date,
    PRIMARY KEY ((person_firstname, person_lastname), person_age, sw_name, sw_version_info, sw_age)
) WITH CLUSTERING ORDER BY (person_age ASC, sw_name ASC, sw_version_info ASC, sw_age ASC)
    AND EDGE LABEL created 
      FROM person_label((person_firstname, person_lastname), person_age) 
      TO software_label((sw_name, sw_version_info), sw_age);
```

---
**NOTE**

The v1-edge->v2 triplet must be unique within a keyspace. Also one table can only have one graph label definition.

---

The three important things to look out for when creating an edge label table is:

* the `PRIMARY KEY` definition
* the `FROM` and `TO` mapping definitions. Also `FROM` and `TO` must match existing vertex labels.
* type definitions of all mapping columns need to match the types defined in the vertex tables.

Any mistakes there will be rejected at creation time of the edge label definition.

##### PRIMARY KEY definition for Edge Label Tables
The partition keys of `person_label` also need to be partition keys in the edge table definition, which are in this case `person_firstname, person_lastname`. Clustering columns of `person_label` (`person_age`) and partition keys/clustering columns of `software_label` will end up all being clustering columns in the new edge table.
The final `PRIMARY KEY` definition will therefore be: 
`PRIMARY KEY ((person_firstname, person_lastname), person_age, sw_name, sw_version_info, sw_age)`

##### FROM/TO mapping definitions
Notice how the definitions in the `FROM` and `TO` part need to match the PK definitions of the vertex tables `person` and `software`.
Since the `FROM` part is for the `person` table, it needs to be `FROM person_label((person_firstname, person_lastname), person_age)`.
The `TO` part is for the `software` table and so that needs to be `TO software_label((sw_name, sw_version_info), sw_age)`.

`system_schema.edges` keeps track of all existing edge labels and their keyspace/table names. In addition, it also keeps track of the **mapping** columns that are being used in order to connect a `person_label` to a `software_label`.
```
cqlsh> select * from system_schema.edges ;

 keyspace_name | table_name            | label_name | from_clustering_columns | from_partition_key_columns              | from_table | to_clustering_columns | to_partition_key_columns       | to_table
---------------+-----------------------+------------+-------------------------+-----------------------------------------+------------+-----------------------+--------------------------------+----------
          test | personCreatedSoftware |   created |          ['person_age'] | ['person_firstname', 'person_lastname'] |     person |            ['sw_age'] | ['sw_name', 'sw_version_info'] | software

```

## How to customize Edge Table partitioning?

Let's say one wants to partition an edge table by `X` and cluster it by `Y`. This can be easily achieved by specifying `X` and `Y` in the PK definitions in addition to the other mapping columns.

```
CREATE TABLE test.person__created__software (
    person_firstname text,
    person_lastname text,
    "X" int,
    person_age int,
    sw_name text,
    sw_version_info text,
    sw_age int,
    "Y" int,
    creation_date date,
    PRIMARY KEY ((person_firstname, person_lastname, "X"), person_age, sw_name, sw_version_info, sw_age, "Y")
) WITH CLUSTERING ORDER BY (person_age ASC, sw_name ASC, sw_version_info ASC, sw_age ASC, "Y" ASC)
    AND EDGE LABEL created
      FROM person((person_firstname, person_lastname), person_age)
      TO software((sw_name, sw_version_info), sw_age);
```

Running a traversal without specifying the additional partition key `X` will result in:
```
g.V().outE("created")
One or more indexes are required to execute the traversal: g.V().outE("created")
Failed step: __.outE().hasLabel("created")
CQL execution: No table or view could satisfy the query 'SELECT * FROM test.person__created__software WHERE person_firstname = ? AND person_lastname = ? AND person_age = ?'
The output of 'schema.indexFor(<your_traversal>).analyze()' suggests the following indexes could be created to allow execution:

schema.edgeLabel('created').from('person').to('software').materializedView('person__created__software_by_firstname_lastname_age').ifNotExists().partitionBy('person_firstname').partitionBy('person_lastname').partitionBy('person_age').clusterBy('X', Asc).clusterBy('sw_name', Asc).clusterBy('sw_version_info', Asc).clusterBy('sw_age', Asc).clusterBy('Y', Asc).create()
```
So in order for the traversal to succeed, one either needs to create an index, or include `X` in the traversal as shown below:
```
g.V().outE("created").has("X", 1)
==>e[person:hans:wurst:23#68->created:1:2#45->software:DSE:123:3#06][person:hans:wurst:23#68-created->software:DSE:123:3#06]
```

## How to rename a Vertex/Edge Label?

A vertex label can be renamed as shown below:
```
ALTER TABLE test.person RENAME VERTEX LABEL TO "personX";
```

An edge label can be renamed as shown below:
```
ALTER TABLE test."personCreatedSoftware" RENAME EDGE LABEL TO "createdX";
```

## How to remove a Vertex/Edge Label?

A vertex label can be removed as shown below:
```
ALTER TABLE test.person WITHOUT VERTEX LABEL "personX";
```

An edge label can be removed as shown below:
```
ALTER TABLE test."personCreatedSoftware" WITHOUT EDGE LABEL "createdX";
```
