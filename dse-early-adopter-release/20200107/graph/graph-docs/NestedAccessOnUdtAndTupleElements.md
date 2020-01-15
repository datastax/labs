# Nested Access on UDT & Tuple elements

It is possible to query **nested elements** of a UDT/Tuple with Graph. Here are a few examples that show their usage.

Let's create some complex schema with UDTs & Tuples:

```
schema.type('udt2').ifNotExists().
    property('udt2_int', Int).
    property('udt2_text', Text).
    property('udt2_tuple', tupleOf(Varchar, tupleOf(Int, Int))).
    create()

schema.type('udt1').ifNotExists().
    property('udt_int', Int).
    property('udt_text', Text).
    property('udt_tuple', tupleOf(Varchar, tupleOf(Int, Int))).
    property('udt_udt', typeOf('udt2').frozen()).
    create()

schema.vertexLabel('complex').ifNotExists().
    partitionBy('id', Int).
    property('tuple', tupleOf(Int, tupleOf(Int, typeOf('udt2')))).
    property('udtFirst', typeOf('udt1').frozen()).
    property('listOfUdt', listOf(typeOf('udt1').frozen())).
    property('setOfTuple', setOf(tupleOf(Int, tupleOf(Int, typeOf('udt2'))).frozen())).
    create()
```

Now let's add some data:
```
nested_tuple_a = ['x', [23, 67] as Tuple] as Tuple
nested_tuple_b = ['y', [24, 68] as Tuple] as Tuple
udt2_a = [udt2_int: 23, udt2_text: 'random code', udt2_tuple: nested_tuple_a] as udt2
udt2_b = [udt2_int: 24, udt2_text: 'other random stuff', udt2_tuple: nested_tuple_b] as udt2
complex_tuple_a = [55, [66, udt2_a] as Tuple] as Tuple
complex_tuple_b = [56, [67, udt2_b] as Tuple] as Tuple
complex_tuple_c = [155, [166, udt2_a] as Tuple] as Tuple
complex_tuple_d = [156, [167, udt2_b] as Tuple] as Tuple
udt1_a = [udt_int: 100, udt_text: 'some text', udt_tuple: nested_tuple_a, udt_udt: udt2_a] as udt1
udt1_b = [udt_int: 101, udt_text: 'other text', udt_tuple: nested_tuple_b, udt_udt: udt2_b] as udt1
udt1_c = [udt_int: 200, udt_text: 'more text', udt_tuple: nested_tuple_a, udt_udt: udt2_a] as udt1
udt1_d = [udt_int: 201, udt_text: 'less text', udt_tuple: nested_tuple_b, udt_udt: udt2_b] as udt1
list_of_udt_a = [udt1_a, udt1_c]
list_of_udt_b = [udt1_b, udt1_d]
set_of_tuple_a = [complex_tuple_a, complex_tuple_c] as Set
set_of_tuple_b = [complex_tuple_b, complex_tuple_d] as Set

g.addV('complex').
    property('id', 1).
    property('tuple', complex_tuple_a).
    property('udtFirst', udt1_a).
    property('listOfUdt', list_of_udt_a).
    property('setOfTuple', set_of_tuple_a).
    next()

g.addV('complex').
    property('id', 2).
    property('tuple', complex_tuple_b).
    property('udtFirst', udt1_b).
    property('listOfUdt', list_of_udt_b).
    property('setOfTuple', set_of_tuple_b).
   next()
```

In order to be able to query nested elements, we need a search index:
```
schema.vertexLabel('complex').searchIndex().ifNotExists().by('udtFirst').by('tuple').by('listOfUdt').by('setOfTuple').create()
```

We should now have 2 vertices:
```
gremlin> g.V().hasLabel("complex")
==>v[dseg:/complex/1]
==>v[dseg:/complex/2]
```

## Querying nested elements of a UDT/Tuple
Generally speaking, elements in a UDT/Tuple can be accessed via **dot-notation**, such as `rootElement.first.second.third`. 

For UDTs, the nested element names need to refer to the column names of the UDT.

For Tuples, one can refer to fields by using `fieldX` where `X` is a positive integer. This is because Tuple elements themselves are lacking field names.

Note that indices are 1-based and so `field1` would refer to the first element inside a Tuple.


## Nested Tuple elements - Query examples 

Finding all the vertices that are `neq(23)` on the first field of `tuple`:
```
gremlin> g.V().hasLabel("complex").has("tuple.field1", neq(23))
==>v[dseg:/complex/1]
==>v[dseg:/complex/2]
```

Notice how type validation will kick in in case a wrong type was provided
```
gremlin> g.V().hasLabel("complex").has("tuple.field1", "abc")
Wrong value type provided for column 'tuple'. Provided type 'String' is not compatible with expected CQL type 'int' at location 'tuple.field1'.
Type ':help' or ':h' for help.
```

`tuple.field2.field2` refers to `udt2` and `udt2_int` refers to the column inside that UDT:

```
gremlin> g.V().hasLabel("complex").has("tuple.field2.field2.udt2_int", 24)
==>v[dseg:/complex/2]
```

The path can be arbitrarily long as shown with `tuple.field2.field2.udt2_tuple.field1` and `tuple.field2.field2.udt2_tuple.field2.field1`:
```
gremlin> g.V().hasLabel("complex").has("tuple.field2.field2.udt2_tuple.field1", "x")
==>v[dseg:/complex/1]

gremlin> g.V().hasLabel("complex").has("tuple.field2.field2.udt2_tuple.field1", "y")
==>v[dseg:/complex/2]

gremlin> g.V().hasLabel("complex").has("tuple.field2.field2.udt2_tuple.field1", neq("abc"))
==>v[dseg:/complex/1]
==>v[dseg:/complex/2]

gremlin> g.V().hasLabel("complex").has("tuple.field2.field2.udt2_tuple.field2.field1", gt(1))
==>v[dseg:/complex/1]
==>v[dseg:/complex/2]
```

Notice that `field999` is not valid and so an error will be shown:
```
gremlin> g.V().hasLabel("complex").has("tuple.field2.field2.udt2_tuple.field2.field999", 23)
Tuple type 'frozen<tuple<int, int>>' does not have field 'field999'
```


## Nested UDT elements - Query examples 

A few simple query examples on field `udt_int` of `udtFirst`:

```
gremlin> g.V().hasLabel("complex").has("udtFirst.udt_int", 100)
==>v[dseg:/complex/1]

gremlin> g.V().hasLabel("complex").has("udtFirst.udt_int", gte(23))
==>v[dseg:/complex/1]
==>v[dseg:/complex/2]

gremlin> g.V().hasLabel("complex").has("udtFirst.udt_int", neq(23))
==>v[dseg:/complex/1]
==>v[dseg:/complex/2]

gremlin> g.V().hasLabel("complex").has("udtFirst.udt_int", neq(100))
==>v[dseg:/complex/2]
```

With `udtFirst.udt_udt.udt2_tuple.field1` we're accessing the first field of the Tuple `udtFirst.udt_udt.udt2_tuple`, which is defined in `udt2`, which is a sub-element of `udtFirst`.
```
gremlin> g.V().hasLabel("complex").has("udtFirst.udt_udt.udt2_tuple.field1", "x")
==>v[dseg:/complex/1]
```

Type validation will throw an error in case the value type doesn't match the type of a nested element:
```
gremlin> g.V().hasLabel("complex").has("udtFirst.udt_udt.udt2_tuple.field1", 67)
Wrong value type provided for column 'udtFirst'. Provided type 'Integer' is not compatible with expected CQL type 'varchar' at location 'udtFirst.udt_udt.udt2_tuple.field1'.
```

## Other Search predicates
Other Search predicates can be used as well on nested elements
```
g.V().hasLabel("complex").has("udtFirst.udt_text", TextP.containing("text"))
==>v[dseg:/complex/1]
==>v[dseg:/complex/2]

gremlin> g.V().hasLabel("complex").has("udtFirst.udt_text", Search.regex(".*om.*"))
==>v[dseg:/complex/1]
```

---

**NOTE**

Search Fulltext predicates (token / tokenRegex / tokenPrefix / tokenFuzzy / Phrase) are not supported on Text fields when those Text fields are located inside a UDT/Tuple

---


## Querying nested elements of a UDT/Tuple inside a List/Set
When a UDT/Tuple are contained in a List/Set, then the elements can be accessed via **dot-notation**, such as `rootElement.first.second.third`.

It is important to note that the name of a UDT/Tuple isn't specified when the UDT/Tuple is inside a List/Set. The `rootElement` needs then to refer to the name of the List/Set and `first` needs to refer to the actual element of the UDT/Tuple.

Below are a few query examples that show this.

## Querying Tuple fields with a List/Set of Tuples

Finding all the vertices that are `neq(23)` on the first field of all `tuple` items that are inside `setOfTuple`:

```
gremlin> g.V().hasLabel("complex").has("setOfTuple.field1", neq(23))
==>v[dseg:/complex/1]
==>v[dseg:/complex/2]
```

Notice how `setOfTuple` already refers to the list that contains all `tuple` items.

Notice how type validation will kick in in case a wrong type was provided
```
gremlin> g.V().hasLabel("complex").has("setOfTuple.field1", "abc")
Wrong value type provided for column 'setOfTuple'. Provided type 'String' is not compatible with expected CQL type 'int' at location 'setOfTuple.field1'.
Type ':help' or ':h' for help.
```

`setOfTuple.field2.field2` refers to `udt2` and `udt2_int` refers to the column inside that UDT:

```
gremlin> g.V().hasLabel("complex").has("setOfTuple.field2.field2.udt2_int", 24)
==>v[dseg:/complex/2]

```

Using `between(20,30)` should give us both vertices:

```
gremlin> g.V().hasLabel("complex").has("setOfTuple.field2.field2.udt2_int", between(20,30))
==>v[dseg:/complex/1]
==>v[dseg:/complex/2]
```


## Querying UDT fields with a List/Set of UDTs


A few simple query examples on field `udt_int` of multiple `udtFirst` items that are contained inside `listOfUdt`:

```
gremlin> g.V().hasLabel("complex").has("listOfUdt.udt_int", 100)
==>v[dseg:/complex/1]

gremlin> g.V().hasLabel("complex").has("listOfUdt.udt_int", gte(23))
==>v[dseg:/complex/1]
==>v[dseg:/complex/2]

gremlin> g.V().hasLabel("complex").has("listOfUdt.udt_int", neq(23))
==>v[dseg:/complex/1]
==>v[dseg:/complex/2]

gremlin> g.V().hasLabel("complex").has("listOfUdt.udt_int", neq(100))
==>v[dseg:/complex/1]
==>v[dseg:/complex/2]

gremlin> g.V().hasLabel("complex").has("listOfUdt.udt_int", gt(200))
==>v[dseg:/complex/2]
```

With `listOfUdt.udt_udt.udt2_tuple.field1` we're accessing the first field of the Tuple `listOfUdt.udt_udt.udt2_tuple`, which is defined in `udt2`, which is a sub-element of `udtFirst`.
```
gremlin> g.V().hasLabel("complex").has("listOfUdt.udt_udt.udt2_tuple.field1", "x")
==>v[dseg:/complex/1]
```

Type validation will throw an error in case the value type doesn't match the type of a nested element:
```
gremlin> g.V().hasLabel("complex").has("listOfUdt.udt_udt.udt2_tuple.field1", 67)
Wrong value type provided for column 'listOfUdt'. Provided type 'Integer' is not compatible with expected CQL type 'varchar' at location 'listOfUdt.udt_udt.udt2_tuple.field1'.
```


