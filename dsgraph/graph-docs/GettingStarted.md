# Getting started with Native Engine Graph

## Setup
Create a graph.
```
system.graph('crew').create()
```
If using gremlin console then alias the graph.
```
:remote config alias g crew.g
```
## Add schema
```
schema.vertexLabel('person').
    partitionBy('name', Text).
    property('age', Int).
    create()

schema.vertexLabel('software').
    partitionBy('name', Text).
    property('version', Int).
    property('lang', Text).
    create()

schema.edgeLabel('created').
    from('person').to('software').
    property('weight', Double).
    create()
```
## Add some data
```
g.addV('person').
     property('name', 'bob').
     property('age', 30).
     as('bob').
 addV('software').
     property('name', 'studio').
     property('lang', 'java').
     property('version', 1).
     as('studio').
 addE('created').from('bob').to('studio').
     property('weight', 0.8).
     iterate()
```
Warning! Remember that a scripts with multiple traversals will only iterate the last traversal automatically. It
is important to end traversals with `.iterate()` to force iteration in this situation. 
```
g.addV('person').
     property('name', 'bob').
     property('age', 30).
     iterate() //Required `bob` will NOT be inserted
g.addV('software').
     property('name', 'studio').
     property('lang', 'java').
     property('version', 1).
     iterate()
```
It is generally good practice to add `.iterate()` if you do not need the results to be returned to you. This saves the
overhead of sending the results back to the client and improves performance.
 
## Query your graph
Query for `bob`.
```
g.V().hasLabel('person').has('name', 'bob')
```
```
==>v[person:bob#64]
```

Query for software that `bob` created.
```
g.V().hasLabel('person').has('name', 'bob').out('created')
```
```
==>v[software:studio#37]
```
Query for software `studio`.
```
g.V().hasLabel('software').has('name', 'studio')
```
```
==>v[software:studio#37]
```

## Perform a query that requires the existence of an index
Query for creator of `studio`. This will throw an error as traversal from `software` to `person` will require an index  
```
g.V().hasLabel('software').has('name', 'studio').in()
```
```
One or more indexes are required to execute the traversal: g.V().hasLabel("software").has("name","studio").in()
Failed step: DseVertexStep(__.in())
CQL execution: No table or view could satisfy the query 'SELECT * FROM crew.person__created__software WHERE software_name = ?'
The output of 'schema.indexFor(<your_traversal>).analyze()' suggests the following indexes could be created to allow execution:

schema.edgeLabel('created').from('person').to('software').materializedView('person__created__software_by_software_name').ifNotExists().partitionBy(IN, 'name').clusterBy(OUT, 'name', Asc).create()

Alternatively consider using:
g.with('ignore-unindexed') to ignore unindexed traversal. Your results may be incomplete.
g.with('allow-filtering') to allow filtering. This may have performance implications.
```
Create the required index as specified in the error message.
```
schema.edgeLabel('created').from('person').to('software').materializedView('person__created__software_by_software_name').ifNotExists().partitionBy(IN, 'name').clusterBy(OUT, 'name', Asc).create()
```
Run the query again.
```
g.V().hasLabel('software').has('name', 'studio').in()
```
```
==>v[person:bob#64]
```