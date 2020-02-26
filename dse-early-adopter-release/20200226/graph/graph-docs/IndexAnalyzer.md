# Index Analyzer

Core Engine graphs benefits from automatic index analysis either explicitly via the schema API or via error messages.
 
The index analyzer has the ability to figure out what indexes a given traversal requires. It will propose those necessary indexes and the user has the option to either create those indexes manually (through copy/paste) or automatically (through `.apply()`).


## How to determine what indexes a provided Traversal requires?
In order to let the index analyzer figure out what indexes a particular traversal requires, a user needs to execute `schema.indexFor(<your_traversal>).analyze()` as shown below:

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

## How to automatically create an index for a provided Traversal?

This can be done through `schema.indexFor(<your_traversal>).apply()`. It is perfectly valid to skip `schema.indexFor(<your_traversal>).analyze()`
and execute `.apply()` directly. The `.apply()` step will indicate what indexes are being created.

```
gremlin> schema.indexFor(g.V().has("age", 23)).apply()
==>Creating the following indexes:
schema.vertexLabel('person').materializedView('person_by_age').ifNotExists().partitionBy('age').clusterBy('firstname', Asc).clusterBy('lastname', Asc).create()
OK
```

## What type of indexes can the Index Analyzer create?

Depending on the particular traversal, the index analyzer can suggest the creation of one or more indexes of the following types:

* a Materialized View for predicates that are not search-specific or specific to CQL collections
* a Search Index for specific predicates (e.g. `token` / `regex` / `phrase` / `neq` / ...) that can only be fulfilled by a search index
* a Secondary index for specific predicates (e.g. `contains(x)` / `containsKey(x)` / `containsValue(x)` / `entryEq(x, y)`) against CQL collections (Lists/Sets/Maps)

## When are Materialized Views created?

A Materialized View will generally be created if predicates in the traversal are not specific to CQL collections and are not search-specific.

The below example filters on `phone` and orders by `age`. Since this can be fulfilled by a Materialized View, the output will be:
```
gremlin> schema.indexFor(g.V().has("phone", "123-456-789").order().by("age", desc)).analyze()
==>Traversal requires that the following indexes are created:
schema.vertexLabel('person').materializedView('person_by_phone_age_Desc').ifNotExists().partitionBy('phone').clusterBy('age', Desc).clusterBy('firstname', Asc).clusterBy('lastname', Asc).create()
```

## When are Search Indexes created?

The index analyzer will pick a Search Index if the predicate is Search-specific, such as `token` / `tokenPrefix` / `tokenRegex` / `tokenFuzzy` / `phrase` / `regex` / `prefix` / `fuzzy`.
Additional predicates that require a Search Index are Geo predicates (`inside` / `insideCartesian`) and `neq` / `without`.
The index analyzer will suggest creating a Search index when having multiple conditions, where some of the conditions are against a CQL List/Set. However, note that
a Search index currently can't handle Maps.

The below example uses the predicate `regex` and filters on `age` and so the index analyzer will suggest a Search Index: 
```
gremlin> schema.indexFor(g.V().has('name', Search.regex('Alan')).has('age', gt(30))).analyze()
==>Traversal requires that the following indexes are created:
schema.vertexLabel('p').searchIndex().ifNotExists().by('name').by('age').create()
```

A slightly more complicated example that will result in the suggestion of a Search Index due to using the `regex` / `prefix` predicates:
```
gremlin> schema.indexFor(g.V().has('title', Search.regex('dse')).has('lang', Search.prefix('jav')).order().by('url')).analyze()
==>Traversal requires that the following indexes are created:
schema.vertexLabel('software').searchIndex().ifNotExists().by('title').by('lang').by('url').create()
```

## When are Secondary Indexes created?

The index analyzer will pick a Secondary Index for predicates (`contains(x)` / `containsKey(x)` / `containsValue(x)` / `entryEq(x, y)`)  against CQL collections, such as Lists/Sets/Maps. 

The below example suggests a Secondary Index when filtering data in a **list**:
```
gremlin> schema.indexFor(g.V().has("list", contains(23))).analyze()
==>Traversal requires that the following indexes are created:
schema.vertexLabel('collections').secondaryIndex('collections_2i_by_list').ifNotExists().by('list').indexValues().create()
```

The same applies for queries against a **set**:
```
gremlin> schema.indexFor(g.V().has("set", contains(45))).analyze()
==>Traversal requires that the following indexes are created:
schema.vertexLabel('collections').secondaryIndex('collections_2i_by_set').ifNotExists().by('set').indexValues().create()
```

The below examples show usage of the **map** predicates `containsKey(x)` / `containsValue(x)` / `entryEq(x, y)` and the suggested indexes:
```
gremlin> schema.indexFor(g.V().has("map", containsKey(45))).analyze()
==>Traversal requires that the following indexes are created:
schema.vertexLabel('collections').secondaryIndex('collections_2i_by_map1').ifNotExists().by('map').indexKeys().create()


gremlin> schema.indexFor(g.V().has("map", containsValue("some item"))).analyze()
==>Traversal requires that the following indexes are created:
schema.vertexLabel('collections').secondaryIndex('collections_2i_by_map2').ifNotExists().by('map').indexValues().create()


gremlin> schema.indexFor(g.V().has("map", entryEq(45, "some item"))).analyze()
==>Traversal requires that the following indexes are created:
schema.vertexLabel('collections').secondaryIndex('collections_2i_by_map3').ifNotExists().by('map').indexEntries().create()
```

## When does the Index Analyzer create multiple indexes?

This is generally the case when the traversal will hit more than one table (e.g. when we're not filtering on a particular label).
Given the traversal `g.V().has("list", contains(23))` and the three vertex labels shown below will require three separate indexes.

```
gremlin> schema.describe()
==>schema.vertexLabel('a').ifNotExists().partitionBy('id', Int).property('age', Int).property('name', Varchar).property('list', listOf(Int)).create()
schema.vertexLabel('b').ifNotExists().partitionBy('id', Int).property('age', Int).property('name', Varchar).property('list', listOf(Int)).create()
schema.vertexLabel('c').ifNotExists().partitionBy('id', Int).property('age', Int).property('name', Varchar).property('list', listOf(Int)).create()
```

```
gremlin> schema.indexFor(g.V().has("list", contains(23))).analyze()
==>Traversal requires that the following indexes are created:
schema.vertexLabel('c').secondaryIndex('c_2i_by_list').ifNotExists().by('list').indexValues().create()
schema.vertexLabel('a').secondaryIndex('a_2i_by_list').ifNotExists().by('list').indexValues().create()
schema.vertexLabel('b').secondaryIndex('b_2i_by_list').ifNotExists().by('list').indexValues().create()
```

The traversal `g.V().has("list", contains(23)).has("age", 23)` would require a Search index for each table because a Secondary index can always only be applied to a single column:
```
gremlin> schema.indexFor(g.V().has("list", contains(23)).has("age", 23)).analyze()
==>Traversal requires that the following indexes are created:
schema.vertexLabel('b').searchIndex().ifNotExists().by('age').by('list').create()
schema.vertexLabel('a').searchIndex().ifNotExists().by('age').by('list').create()
schema.vertexLabel('c').searchIndex().ifNotExists().by('age').by('list').create()
```

The traversal `g.V().has("list", contains(23)).has("age", 23).has("name", Search.regex(".*ohn"))` would also require a single Search index per table:

```
gremlin> schema.indexFor(g.V().has("list", contains(23)).has("age", 23).has("name", Search.regex(".*ohn"))).analyze()
==>Traversal requires that the following indexes are created:
schema.vertexLabel('b').searchIndex().ifNotExists().by('age').by('list').by('name').create()
schema.vertexLabel('a').searchIndex().ifNotExists().by('age').by('list').by('name').create()
schema.vertexLabel('c').searchIndex().ifNotExists().by('age').by('list').by('name').create()
```

---

**NOTE**

It is generally always recommended to use an element label when filtering as otherwise multiple CQL statements will be executed and data might get filtered in-memory by Tinkerpop and not by C* 

---