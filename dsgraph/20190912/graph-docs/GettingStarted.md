# Getting started with Core Engine Graph

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
 
## Develop your queries
It is advised that while you are experimenting that you use the `dev` traversal source. This will allow you to query 
without indexing. Note that `dev` is only available from `Gremlin Console` or `Studio`.

Query for `bob`.
```
dev.V().hasLabel('person').has('name', 'bob')
```
```
==>v[person:bob#64]
```

Query for software that `bob` created.
```
dev.V().hasLabel('person').has('name', 'bob').out('created')
```
```
==>v[software:studio#37]
```
Query for software `studio`.
```
dev.V().hasLabel('software').has('name', 'studio')
```
```
==>v[software:studio#37]
```
Query for creator of `studio`.
```
dev.V().hasLabel('software').has('name', 'studio').in()
```
```
==>v[person:bob#64]
```

## Get ready for production
Once you are happy with your queries you need to switch to the regular `g` traversal source.
When using `g` filtering can be explicitly enabled, but it is recommended that you index your data for optimal performance.    

For example:

Query for creator of `studio`. This will throw an error as traversal from `software` to `person` will require an index  
```
g.V().hasLabel('software').has('name', 'studio').in()
```
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
Run your queries through `indexFor`
```
schema.indexFor(g.V().hasLabel('software').has('name', 'studio').in()).apply()
```
```
==>Creating the following indexes:
schema.edgeLabel('created').from('person').to('software').materializedView('person__created__software_by_software_name').ifNotExists().partitionBy(IN, 'name').clusterBy(OUT, 'name', Asc).create()
```

Run the query again.
```
g.V().hasLabel('software').has('name', 'studio').in()
```
```
==>v[person:bob#64]
```

## Generate your final schema
Once you have verified that all your queries work against `g` you can generate your final schema by using:
```
schema.describe()
``` 
```
==>schema.vertexLabel('person').ifNotExists().partitionBy('name', Varchar).property('age', Int).create()
schema.vertexLabel('software').ifNotExists().partitionBy('name', Varchar).property('lang', Varchar).property('version', Int).create()
schema.edgeLabel('created').ifNotExists().from('person').to('software').partitionBy(OUT, 'name', 'person_name').clusterBy(IN, 'name', 'software_name', Asc).property('weight', Double).create()

schema.edgeLabel('created').from('person').to('software').materializedView('person__created__software_by_software_name').ifNotExists().partitionBy(IN, 'name').clusterBy(OUT, 'name', Asc).create()
```
You can copy the script and run it on your production server to create your graph schema.
