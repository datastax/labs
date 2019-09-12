# DataStax Graph & Search Integration (for Developers)

## A. Creating a Search Index
```
schema.vertexLabel("recipe").
searchIndex().ifNotExists().
by("name").asString().
by("instructions").asText().
by("notes").
create()
```

#### Indexing Types
When indexing a text column, one can choose the following indexing types:
* `asString()`:  will be using a **non-tokenized** (`StrField`) field
* `asText()`: will be using a **tokenized** (`TextField`) field
* if no indexing type is specified, it will be using a `StrField` and a `TextField` copy field, which means that all textual predicates (`token`, `tokenPrefix`, `tokenRegex`, `eq`, `neq`, `regex`, `prefix`) will be usable.

#### Relevant Classes & Methods
* `SchemaApiImpl`: defines the entire user-facing Schema API
* `SchemaApiImpl#createSearchIndex(...)`: 
  * multiple overloads (for vertex/edge label)
  * calls underlying `QueryBuilderImpl` API to generate & run the CQL needed to create the actual index
  * uses `SearchApi` to deal with `asString()` / `asText()` or both
* `SearchApi`: 
  * modifies Solr Schema directly to index a particular column as `asString()` / `asText()` or both
* `QueryBuilderImpl`: API to generate CQL
  * `QueryBuilderImpl#createSearchIndex()`: generates the CQL for creating the Search index on the given columns

## B. General Read Path
Read path is divided into **3** main steps:
* Expression extraction from tinkerpop traversals
* Conversion of expressions in to a set of backend C* queries
* Execution of CQL queries in a given context

#### Relevant Classes and procedures:
* `TraversalModule` hooks `ExpressionStrategy` into the system
* `ExpressionStrategy`: Extracts expressions from traversals and sets them on `DseGraphStep` / `DseVertexStep` / `DseEdgeVertexStep` / `DseEdgeOtherVertexStep`
* `XStep` will build a `GraphQuery`
  * `GraphQuery` will have all necessary information to fetch the results
* `ElementQueryExecutor` will execute a `GraphQuery`:
  * `ElementQueryExecutor#createGraphQuery(..)` will prepare backend queries for each label
  * `ElementQueryExecutor#execute(..)` will execute it with the given parameters
* Example:
  * this given traversal: `g.V().has("recipe", "name", Search.prefix("R"))`
  * will lead to the expression: `(name prefix(R) & ~label = recipe)`
  * due to having a `Core` engine, we'll end up in `CoreEngineGraphStep`
  * `CoreEngineGraphStep` will construct a `GraphQuery` and execute it against `ElementQueryExecutor`

## C. Read Path & Search index

`ElementQueryExecutor#createBackendQueries(..)` will generate the underlying CQL to run against C*. For picking up a search index, there are essentially **two** paths:
1. if it's a **search-only predicate**, then this can only be fulfilled by a search index -> take a shortcut and construct CQL against search index right away. 

Example query: `g.V().has("recipe", "instructions", Search.tokenPrefix("med"))`

2. otherwise we don't know, so we have to check what indexes are available.

Example query: `g.V().has("recipe", "recipeId", 1)`

#### Order of index selection
For step 2 the order of index selection is **MV index > Secondary index > Search Index**. See also `Index#priority()`.

#### Relevant Classes & Methods:
* `ElementQueryExecutor#createBackendQueries(..)` contains the shortcut to `ElementQueryExecutor#createSearchBackendQuery(..)`
* `ElementQueryExecutor#createSearchBackendQuery(..)`
  * will tell `QueryBuilderImpl` to construct a direct CQL against the `solr_query` column
* `QueryBuilderImpl#selectQuery(..)`:
  * figures out what index to select for a particular query
  * constructs the SELECT CQL & calls `SearchCqlQueryGenerator#getCql(..)`
* `SearchCqlQueryGenerator#getCql(..)`:
  * generates the underlying CQL against the `solr_query` column
  * `SearchCqlQueryGenerator#getSearchQueryCondition(..)` is where the actual **fq** parameters are constructed
* `SearchIndex`: contains necessary metadata about the indexed columns
  * `SearchIndex#supports(..)`: returns `true`/`false` depending on if the given index can support the requested predicate


## Appendix: Schema & Data for the Examples
```
system.graph("test").create()
:remote config alias g test
```
```
schema.vertexLabel('recipe').
    ifNotExists().
    partitionBy("recipeId", Int).
    property("name", Text).
    property("instructions", Text).
    property("notes", Text).
    create()
```
```
g.addV("recipe").property("recipeId", 1).property("name", "Beef Bourguignon").property("instructions",
"Braise the beef. Saute the onions and carrots. Add wine and cook in a dutch oven at 425 degrees for 1 hour.")

g.addV("recipe").property("recipeId", 2).property("name", "Rataouille").property("instructions",
"Peel and cut the eggplant. Make sure you cut eggplant into lengthwise slices that are about 1-inch wide, 3-inches long, and 3/8-inch thick")

g.addV("recipe").property("recipeId", 3).property("name", "Salade Nicoise").property("instructions",
"Take a salad bowl or platter and line it with lettuce leaves, shortly before serving. Drizzle some olive oil on the leaves and dust them with salt.")

g.addV("recipe").property("recipeId", 4).property("name", "Wild Mushroom Stroganoff").property("instructions",
"Cook the egg noodles according to the package directions and keep warm. Heat 1 1/2 tablespoons of the olive oil in a large saute pan over medium-high heat.")

g.addV("recipe").property("recipeId", 5).property("name", "Spicy Meatloaf").property("instructions",
"Preheat the oven to 375 degrees F. Cook bacon in a large skillet over medium heat until very crisp and fat has rendered, 8-10 minutes.")

g.addV("recipe").property("recipeId", 6).property("name", "Oysters Rockefeller").property("instructions",
"Saute the shallots, celery, herbs, and seasonings in 3 tablespoons of the butter for 3 minutes. Add the watercress and let it wilt.")

g.addV("recipe").property("recipeId", 7).property("name", "Carrot Soup").property("instructions",
"In a heavy-bottomed pot, melt the butter. When it starts to foam, add the onions and thyme and cook over medium-low heat until tender, about 10 minutes.")

g.addV("recipe").property("recipeId", 8).property("name", "Roast Pork Loin").property("instructions",
"The day before, separate the meat from the ribs, stopping about 1 inch before the end of the bones. Season the pork liberally inside and out with salt and pepper and refrigerate overnight.")

```
