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


