# DataStax Graph - Traversal Configurations

## Core Engine Graph

#### Changing the limits on allowed mutations

Traversals that change the data, e.g. traversals containing `addV()`, can
eventually lead up to too many underlying mutating queries. We're now
limiting these to a default of 10k underlying CQL queries, though this limit is
configurable.

To alter the mutations limit of a traversal, begin your traversal with:
`g.with('max-mutations', N)` where `N` is the new maximum of mutating statements
that can be executed as a result of executing the traversal, e.g.
`g.with('max-mutations', 10).addV(...)`.

To completely disable the limits check, set the limit to a number (any) that's
`<= 0`, e.g. start your traversal with `g.with('max-mutations', -1).addV(...)`.


#### Allow Filtering modulator

Allow filtering modulator is a mirror function of `ALLOW FILTERING` in C*.
It is not recommended to use in production.
The performance penalty with `ALLOW FILTERING` would still apply.
By default, it is disabled unless specified for a particular traversal.

Assume that there is a vertex label `customer` without any indexes, the following query:
```
g.V().has('customer', 'name', 'Cat')
```
would give you an error:
```
Traversal requires that the following indexes are created: ...
```

By adding the `with` step and `allow-filtering` on the traversal source `g`:
```
g.with('allow-filtering').V().has('customer', 'name', 'Cat')
```
The query doesn't require an index and fetches the results from all nodes in the cluster.

Please note that, if the query is satisfied by an index (e.g. MV, Secondary index, Search index),
the allow filtering option would be ignored.

#### Forcing vertex deletion in the absence of supporting indexes

Deleting a vertex also attempts to delete any edges incident on that vertex.  If a vertex label has an incident edge label uncovered by any index or table in at least one direction, then graph cannot efficiently find those incident edges for deletion (or determine that they do not exist).  By default, graph throws an exception in this case.

The `force-vertex-deletion` option can override this exception and perform deletion to the extent supported by the schema.  When `force-vertex-deletion` is true, the vertex will be deleted, and so will any incident edges covered by a usable index or for which the edge table is keyed by the deleted vertex ID.  Any other incident edges -- those for which a scan would be required -- will not be deleted.  This option can potentially leave edge table rows pointing to a vertex that no longer exists.

```
g.with('force-vertex-deletion').V().has('name', 'Cat').drop()
```

#### Ignoring non-indexed data

For graph exploration type applications it is sometimes useful to be able to execute traversals and get **partial** results back and ignore data that cannot be accessed without an index.

Assuming the existence of `customer` and `meal` vertex labels where only `customer` has an index on `name`, the query `g.V().has('name', 'Cat')` would result in:
```
Traversal requires that the following indexes are created: ...
```

By adding the `with` step and `ignore-unindexed` on the traversal source `g`, the below query will return only `customer` vertices:
```
g.with('ignore-unindexed').V().has('name', 'Cat')
```

---

**NOTE**

There's no additional performance penalty to using `with('ignore-unindexed')`. The result set might contain **partial** results and ignore data that cannot be accessed without an index.

---


#### Controlling read-before-write warnings

Graph looks for traversals matching one of two patterns:

* Read a vertex by its ID, then write a property to the vertex
* Read a pair of vertices by their IDs, then write an edge between the vertices

Here are sample traversals matching these patterns, in the same respective order as the list above:

* `g.V().hasLabel('customer').has('name', 'Cat').property('zone', 1)`
* `g.V().hasLabel('customer').has('name', 'A').as('a').V().hasLabel('customer').has('name', 'B').addE('knows').from('a')`

For the purpose of these two example traversals, assume that the `customer` vertex label's primary key consists only of the `name` property key.  `zone` is not part of the primary key.

If graph finds a traversal matching one of these general patterns, it will emit a warning message.  The warning message includes an alternative traversal that exchanges the read-before-write pattern for a write-only pattern. 

To override this behavior, begin the traversal by `g.with('read-before-write', '<val>')`, where `<val>` is either `warn` or `ignore`.  When set to `ignore`, warnings are suppressed.  When set to `warn`, warnings are emitted (the default).  All other values are reserved for potential future use.

For example, to disable read-before-write warnings on the first sample traversal in this section, run:

`g.with('read-before-write', 'ignore').V().hasLabel('customer').has('name', 'Cat').property('zone', 1)`


#### Setting Read/Write Consistency Levels

It is possible configure consistency levels for Reads & Writes. Available consistency levels are:

* ANY / ONE / TWO / THREE / QUORUM / ALL / LOCAL_QUORUM / EACH_QUORUM / LOCAL_ONE

Below are examples showing how to configure those consistency levels:

* Reads: `g.with("consistency", QUORUM).V().has('age', 30).out()`
* Writes: `g.with("consistency", LOCAL_QUORUM).addV('person').property('id', 232).property('age', 23)`


#### Controlling unlabelled element warnings

Graph looks for vertex and edge-traversal steps without label restrictions.  If it finds one or more such steps in a traversal, then it issues a warning.

Here are sample traversals that would trigger this warning:

* `g.V().has('name', 'Cat')`
* `g.V().hasLabel('customer').has('name', 'Cat').out()`
* `g.E().has('id', 'edge_id')`

To override this warning behavior, begin the traversal by `g.with('label-warning', false)`.

For example, to disable element warnings on the first sample traversal in this section, run:

`g.with('label-warning', false).V().has('name', 'Cat')`


#### Writing with Logged Batches

By default, graph traversals do not use batches.

Logged batching can be enabled by adding the `with` step and `batch` on the traversal source `g`.  This groups all data-modification CQL statements associated with the traversal into a single logged batch.

Consistent with preceding subsections in this document, here is a minimal traversal that activates logged batching using its `with` option:


```
g.with('batch').V().has('name', 'Cat').set('initial', 'C')
```

Here's an example traversal that inserts two vertices in a single logged batch:

```
schema.vertexLabel("person").partitionBy("name", Text).create()
g.with('batch').
	addV("person").property("name", "alice").
        addV("person").property("name", "bob")
```

Here's an example traversal that uses logged batching with TinkerPop's `inject` step.  This approach can help control traversal step bloat as the set of data mutated by a single batch grows.


```
persons = [ [ "name": "alice" ], [ "name": "bob" ], [ "name": "charlie" ] ]
g.with('batch').inject(persons).sideEffect(unfold().addV("person").property("name", select("name")))
```

Unlogged batches are currently unsupported (as are counter batches).
