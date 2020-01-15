# Migrate Classic Engine Graph to Core Engine Graph
If you are an existing DataStax Graph user and are looking to upgrade to this version of DataStax Graph, then please contact your 
DataStax representative to discuss best practices and lessons learned on how to do this with the DataStax services team.

## Schema migration
The migration tool gives users a way of converting the schema of an existing Classic Engine graph to Core Engine graph. 
The generated schema can either be CQL or Gremlin.

## Restrictions
Core Engine graphs do not support meta/multi-properties, and these schema elements will be translated into regular single cardinality properties.
Indexing, caching and TTL will also be dropped from schema. See [Deprecated features](DeprecatedFeatures.md) for more information.

## Usage 
```
dse graph-migrate-schema -cql|-gremlin <classic_graph> <core_graph>
```

## Example
Start DSE with Analytics enabled:
```
bin/dse cassandra -g -s -k
```
Create a Classic Engine graph:
```
bin/dse gremlin-console
system.graph('classic').engine(Classic).create()
:remote config alias g classic.g
schema.propertyKey('name').Text().single().ifNotExists().create()
schema.propertyKey('age').Bigint().single().ifNotExists().create()
schema.vertexLabel('person').partitionKey('name').properties('age').ifNotExists().create()
schema.vertexLabel('software').partitionKey('name').ifNotExists().create()
schema.edgeLabel('created').connection('person', 'software').create()
:exit
```

Create the Core Engine schema:
```
dse graph-migrate-schema -gremlin classic migrated
```
Output:
```
system.graph('migrated').
    ifNotExists().
    withReplication("{ 'class' : 'org.apache.cassandra.locator.NetworkTopologyStrategy', 'SearchGraphAnalytics': '1' }").
    andDurableWrites(true).
    create()


:remote config alias g migrated.g

schema.vertexLabel('software').
    ifNotExists().
    partitionBy('name', Varchar).
    create()
schema.vertexLabel('person').
    ifNotExists().
    partitionBy('name', Varchar).
    property('age', Bigint).
    create()
schema.edgeLabel('created').
    ifNotExists().
    from('person').to('software').
    clusterBy('id', Uuid, Asc).
    create()
```

# Modeling guidance
## Multi/meta-properties
As multi and meta-properties have been removed users should consider modeling such items either as collections/UDTs or distinct elements.

### Using a collection
Consider using a collection when searching for an entry point on the the graph for your traversal.
For instance a `list` (multi) of `Varchar`:  
```
schema.vertexLabel('person').
    ifNotExists().
    partitionBy('id', Varchar).
    property('name', listOf(Text)).
    create()

schema.vertexLabel('person').
    secondaryIndex('person_2i_by_names').
    ifNotExists().
    by('name').
    create()
    
g.addV('person').property('id', '1').property('name', ['alice', 'bob'])
g.V().has('name', contains('bob'))
```

### Using a collection of UDTs
Consider using a UDT if you need structured information.
For example a `list` (multi) of `addresse`s (meta):

```
schema.type('address').
    property('street1', Text).
    property('street2', Text).
    property('postCode', Text).
    create()
    
schema.vertexLabel('person').
    ifNotExists().
    partitionBy('id', Varchar).
    property('address', listOf(frozen(typeOf('address')))).
    create()
    
schema.vertexLabel('person').
    secondaryIndex('person_2i_by_address').
    ifNotExists().
    by('address').
    indexValues().
    create()
    
g.addV('person').
    property('id', '1').
    property('address', [[street1:'5b Tunn Street', 
                          street2:'Fakenham', 
                          postCode:'Norfolk'] as address])
```

Note that UDT elements currently may only be searched for as atomic value. For instance it is not possible currently 
to search for `address` by `street1` only. You must specify the full address:
```
g.V().has('address', contains([street1:'5b Tunn Street', 
                               street2:'Fakenham', 
                               postCode:'Norfolk'] as address))
```

### Using self edges
Use a loop edge if you need full index support. For instance, this model allows `name` edges (multi), that have a `since` property (meta):
```
schema.vertexLabel('person').
    ifNotExists().
    partitionBy('id', Varchar).
    create()
  
schema.edgeLabel('has_name').
    ifNotExists().
    from('person').
    to('person').
    partitionBy(OUT, 'id').
    clusterBy('name', Text).
    clusterBy(IN, 'id').
    property('since', Timestamp).
    create()
    
g.addV('person').
    property('id', '1').as('a').
    addE('has_name').
    from('a').to('a').
    property('name', 'bob').
    property('since', '2001-01-01T00:00:00Z' as Instant)
``` 
Core Engine allows graph indexes for edges:
```
schema.edgeLabel('has_name').
    from('person').
    to('person').
    materializedView('person__has_name__person_by_name').
    ifNotExists().
    partitionBy('name').
    clusterBy(OUT, 'id', Asc).
    clusterBy(IN, 'id', Asc).
    create()
```
Which allows direct lookup of edges:
```
g.E().hasLabel('has_name').has('name', 'bob').inV()
```  

# Edge traversal in a Core Engine Graph
In Core Engine graphs the edge layout can be completely customized. This allows users to avoid indexes in many cases, an important 
consideration for scalable graphs.

## Core Engine Graphs use only one row per edge by default
Classic Engine allowed edge navigation in both directions by default by inserting two records per edge.
However, this did not come for free. In particular `in()` edges had the potential to cause large partitions. For instance:
```
person-belongsTo->country
```
A country may have millions of people, and the reverse records that allowed navigation from `country` to `person` cause the C*
cluster to become imbalanced due to the large numbers of edges in the same partition.

It's also worth noting that typically OLTP traversals cannot realistically process every person in a country, and OLAP
traversals do not need the reverse record anyway.

For these reasons Core Engine graphs are more conservative in edge record creation, creating only one record for each edge and
thus speeding up ingestion. This also allows external tools such as DSBulk to be used to insert data.

### Indexing edges to allow navigation in different directions.
By default when creating an edge the default layout allows traversal via `out()`. For example creating an edge label 
with the default layout:
```
schema.edgeLabel('created').
    from('person').
    to('software').
    create()
```
Traversal from `person` to `software` is possible.
```
g.V(person).out() //Returns studio 
```
But trying to traverse in the opposite direction will throw an error.
```
g.V(software).in() //ERROR 
```
Core Engine will tell you what index you need to create to allow this traversal in the error message. 
Cut and paste the index schema statement to execute and create the index:
```
One or more indexes are required to execute the traversal: g.V().hasLabel("software").has("name","studio").in()
Failed step: __.in()
CQL execution: No table or view could satisfy the query 'SELECT * FROM crew.person__created__software WHERE software_name = ?'
The output of 'schema.indexFor(<your_traversal>).analyze()' suggests the following indexes could be created to allow execution:

schema.edgeLabel('created').from('person').to('software').materializedView('person__created__software_by_software_name').ifNotExists().partitionBy(IN, 'name').clusterBy(OUT, 'name', Asc).create()

Alternatively consider using:
g.with('ignore-unindexed') to ignore unindexed traversal. Your results may be incomplete.
g.with('allow-filtering') to allow filtering. This may have performance implications.
```
The error message includes a suggested index that will allow you to run your queries.
```
schema.edgeLabel('created').from('person').to('software').materializedView('person__created__software_by_software_name').ifNotExists().partitionBy(IN, 'name').clusterBy(OUT, 'name', Asc).create()

g.V(software).in() //Returns person
```


