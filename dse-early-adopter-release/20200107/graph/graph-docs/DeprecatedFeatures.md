# What are the deprecated Features from Classic compared to Core Engine?

## Graph configuration

Configuration options such as `allow_scan` / `schema_mode` / `evaluation_timeout` were completely removed. 
The only traversal configuration that Core Engine supports are documented [here](TraversalOptions.md).


## dse.yaml settings
Classic Engine still supports **all** settings from `dse.yaml`. However, for Core Engine the only supported options are:

  * `analytic_evaluation_timeout_in_ms`: Maximum time to wait for an OLAP analytic (Spark) traversal to evaluate
  * `realtime_evaluation_timeout_in_ms`: Maximum time to wait for an OLTP real-time traversal to evaluate
  * `system_evaluation_timeout_in_ms`: Maximum time to wait for a graph-system request to evaluate. Creating/dropping a new graph is an example of a graph-system request
  * `gremlin_server`: Different options that configure the Gremlin server


## Transactions

Transaction handling has been changed:

  * `tx()` API calls will throw an unsupported operation exception.
  * All mutations are executed once a traversal has been exhausted. There are no guarantees that this will not result in partial commits in the event of node failure.
  * Mutations are no longer visible during the execution of a traversal. For instance: `g.addV('person').V()` will not return a vertex.  

Transactions are not supported in C* or DataStax Graph so we should generally move towards removing this terminology.

## Multi/Meta-Properties

Multi and meta-property support has been dropped. Use cases have previously included:

  * Access control: C* RLAC and RBAC are used to fulfill this use case. Unlike meta-properties, this solution will scale because CQL requests are pre-processed to check what data a user may see rather than fetching all data and filtering.
  * Collections: Core Engine graphs support CQL collection types and may be queried via the new collection predicates: `contains`, `containsKey`, `containsValue`, `entryEq`.
  * Time machine: This requires large amounts of filtering to achieve and does not scale outside of a small graphs. It also has the possibility of creating very large partitions. Users must explicitly create a model that takes timestamps into account.
  * Entity resolution: Use regular vertices and edges to model these relationships. Use a separate vertex for contributing datasource, and use edges to link them to a resolved entity.


## Graph API / graph.addVertex / graph.addEdge

The [graph API](https://docs.datastax.com/en/dse/6.0/dse-dev/datastax_enterprise/graph/reference/refGraphAPI.html) was removed.

This means that `graph.addVertex(label, 'label_name', 'key', 'value', 'key', 'value')` / `vertex1.addEdge('edgeLabel', vertex2, [T.id, 'edge_id'], ['key', 'value'] [,...])` are not supported.

Users must use the traversal API: `g.addV(vlabel).property('key', 'value')` / `g.addE(elabel).from(v1).to(v2).property('key', 'value')` respectively.

In addition, elements that are returned from traversals are **reference** elements. These do not include property information outside the primary key and are **immutable**.
Users should use `.valueMap().by(unfold())` to retrieve the data they are interested in.


## Edge directionality

In Classic Engine all edges were by default **bidirectional**. For performance reasons, edges are now always created in a **unidirectional** manner.

The implication is that some traversals are not possible without additional steps. For example, neither `g.V(id).in()` nor `g.V(id).both()` are possible and would result in an error.

To enable `.in()` or `.both()` a MV must be created on the edge table and this can be done transparently using the [Index Analyzer](IndexAnalyzer.md): `schema.indexFor(g.V(id).in()).apply()`

---

**NOTE**

There are significant advantages to using edges without a MV during data ingestion. Having a MV requires reading the old value before updating it, therefore resulting in a **read-before-write**.  

---

## Data types
Data types have been aligned with the java driver. All types that were supported in Classic are identical except `Duration`.


In Classic Engine graphs `Duration` is represented by `java.time.Duration`.

In Core Engine graphs `Duration` is represented by `com.datastax.driver.core.Duration`.

## Query caching
In Classic there was the the option to set graph and vertex queries to be cached. This is no longer an option in Core Engine graphs. We may add caching ability in a subsequent release.

## TTL support
TTL support via schema currently requires users set this via CQL in Core Engine graphs. This is a feature that will be added in a subsequent release.

## External ID construction
Core Engine does not support external ID construction and IDs must be obtained directly from elements if they are to be used for lookups.

```g.V(id)```

Users that wish to look up elements by ID should instead use `.inject()` and `.has()`. For example:

Get `bob`: `g.V().has('name', eq('bob'))`

Get `bob` and `alice`: `g.V().has('name', within(['bob', 'alice']))`


## DGL

DGL is deprecated and not supported by Core Engine. Users can instead use plain CQL for data ingestion or one of the bulk loading tools (GraphFrames, DS Bulk Loader).


## Lambdas

Lambdas are currently not supported in Core Engine, but they may be enabled by default in a subsequent release.

