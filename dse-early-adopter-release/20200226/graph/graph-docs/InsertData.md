# Long Traversals #

NOTE: this document is heavily based on https://tinkerpop.apache.org/docs/current/recipes/#long-traversals

It can be tempting to generate long traversals, e.g. to create a set of vertices and edges based on information that resides within an application. For example, let’s consider two lists - one that contains information about persons and another that contains information about the relationship between these persons.

Normally these lists would come in the form of a method parameter however, for the purpose of illustrating the problem, in this guide we will create two list with a few random map entries:

``` groovy
gremlin> :set max-iteration 10
gremlin> rnd = new Random(123) ; x = []
gremlin> persons = (1..100).collect {["id": it, "name": "person ${it}", "age": rnd.nextInt(40) + 20]}
==>[id:1,name:person 1,age:42]
==>[id:2,name:person 2,age:30]
==>[id:3,name:person 3,age:36]
==>[id:4,name:person 4,age:49]
==>[id:5,name:person 5,age:55]
==>[id:6,name:person 6,age:37]
==>[id:7,name:person 7,age:54]
==>[id:8,name:person 8,age:57]
==>[id:9,name:person 9,age:45]
==>[id:10,name:person 10,age:33]
...
gremlin> def dse_id = { 'dseg:/person/' + it }
==>Script68$_run_closure1@73d7b702
gremlin> relations = (1..500).collect {[rnd.nextInt(persons.size()), rnd.nextInt(persons.size())]}.
           unique().grep {it[0] != it[1] && !x.contains(it.reverse())}.collect {
             x << it
             minAge = Math.min(persons[it[0]].age, persons[it[1]].age)
             knowsSince = new Date().year + 1900 - rnd.nextInt(minAge)
             ["from": dse_id(persons[it[0]].id), "to": dse_id(persons[it[1]].id), "since": knowsSince]
           }
==>{from=dseg:/person/21, to=dseg:/person/11, since=2015}
==>{from=dseg:/person/59, to=dseg:/person/61, since=2013}
==>{from=dseg:/person/1, to=dseg:/person/37, since=2011}
==>{from=dseg:/person/83, to=dseg:/person/45, since=2007}
==>{from=dseg:/person/21, to=dseg:/person/51, since=2015}
==>{from=dseg:/person/27, to=dseg:/person/5, since=2012}
==>{from=dseg:/person/77, to=dseg:/person/89, since=2016}
==>{from=dseg:/person/16, to=dseg:/person/44, since=2011}
==>{from=dseg:/person/95, to=dseg:/person/53, since=1972}
==>{from=dseg:/person/20, to=dseg:/person/22, since=2017}
...
gremlin> [ "Number of persons": persons.size()
         , "Number of unique relationships": relations.size() ]
==>Number of persons=100
==>Number of unique relationships=478
```

We now have a list of people represented by their ID, name, and age, e.g. `{"id": 1, "name": "person 1", "age": 42}` and a list of relationships between people. These relationships are not indicated by the original raw person ID, but rather by what will eventually become their DSE Graph vertex ID (you can read more about these IDs [here](IdFormat.md)).

Now let's create a graph and its schema:

``` groovy
gremlin> system.graph("people").create()
==>OK
gremlin> :remote config alias g people.g
==>g=people.g
gremlin> schema.vertexLabel("person").partitionBy("person_id", Int).property("name", Text).property("age", Int).create()
==>OK
gremlin> schema.edgeLabel("knows").from("person").to("person").partitionBy("since", Int).create()
==>OK
```

To create the `person` vertices and the `knows` edges between them it may look like a good idea to generate a single graph-mutating traversal, just like this:

``` groovy
gremlin> t = g
==>people[Core]
gremlin> for (person in persons) {
           t = t.addV("person").
                   property(id, person.id).
                   property("name", person.name).
                   property("age", person.age).as("p${person.id}")
         } ; []
gremlin> for (relation in relations) {
           t = t.addE("knows").property("since", relation.since).
                   from("p${relation.from}").
                   to("p${relation.to}")
         } ; []
gremlin> t
The submitted traversal exceeded the maximum length allowed of '90'. Please split it into multiple smaller traversals.
Type ':help' or ':h' for help.
Display stack trace? [yN]
```

However, building this kind of traversal does not scale. Additionally, DSE Graph will complain about traversals longer than 90 steps as this is usually a sign that something is not quite right (not to mention that it could lead to bad performance).

A better way to make the same effect as in the traversal above is to inject the lists into the traversal and process them from there:

``` groovy
gremlin> g.withSideEffect("relations", relations).
           inject(persons).sideEffect(
             unfold().
             addV("person").
               property("person_id", select("id")).
               property("name", select("name")).
               property("age", select("age")).
             group("m").
               by(id).
               by(unfold())).
           select("relations").unfold().as("r").
           addE("knows").
             from(select("m").select(select("r").select("from"))).
             to(select("m").select(select("r").select("to"))).
             property("since", select("since")).iterate()
gremlin> ["V.count=" + g.with("label-warning", false).V().count().next(),
          "E.count=" + g.with("label-warning", false).E().count().next()]
==>V.count=100
==>E.count=478
```

These traversals are, unfortunately, more complicated at first sight, but the number of steps is known and thus it’s the best way to load data via traversals. Furthermore, shorter traversals like the one depicted above reduce the (de)serialization costs when they are sent over the wire to a Gremlin Server.

NOTE: Although the example was based on a graph-mutating traversal, the same rules apply for read-only and mixed traversals.
