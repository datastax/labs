# ID Format in DataStax Graph

Graph uses URIs as ID format due to being a well-defined format and the encoding of special characters is well-known.

The general ID format is `dseg:/[vertexLabel]/[pk1Value]/[pkValue2]/[pkValue3]`. Below are a few sections with examples. All examples
will refer to this given schema:

```
schema.vertexLabel('person').
    partitionBy('name', Text).
    clusterBy('age', Int).
    property('address', Text).
    property('coffeePerDay', Int).
    create()

schema.vertexLabel('software').
    partitionBy('name', Text).
    clusterBy('version', Int).
    clusterBy('lang', Text).
    property('temp', Text).
    property('static_property', Text, Static).
    create()

schema.edgeLabel('created').
    from('person').to('software').
    partitionBy('creation_year', Int).
    property('weight', Double).
    create()
    
    
gremlin> g.addV("person").property("name", "eduard").property("age", 37).property("coffeePerDay", 3)
==>v[dseg:/person/eduard/37]

gremlin> g.addV("software").property("name", "hackertools").property("version", 1).property("lang", "Java")
==>v[dseg:/software/hackertools/1/Java]

gremlin> g.addE("created").
            from(__.V('dseg:/person/eduard/37')).
            to(__.V('dseg:/software/hackertools/1/Java')).
            property("creation_year", 2018).
            property("weight", 2.3)
==>e[dseg:/person-created-software/eduard/37/2018/hackertools/1/Java][dseg:/person/eduard/37-created->dseg:/software/hackertools/1/Java]

```

## Vertex ID

Performing a direct `person` vertex lookup can be done using an ID as shown below. Note that you only need to provide the primary key properties:
```
gremlin> g.V('dseg:/person/eduard/37').valueMap(true)
==>{id=dseg:/person/eduard/37, label=person, name=eduard, coffeePerDay=3, age=37}
```

Below is an example that shows how to directly lookup a `software` vertex using `hackertools/1/Java` for the primary property keys: 
```
gremlin> g.V('dseg:/software/hackertools/1/Java').valueMap(true)
==>{id=dseg:/software/hackertools/1/Java, label=software, name=hackertools, lang=Java, version=1}
```

## Edge ID

An edge ID is only special in the ordering of the primary key properties. The general expected format is 
`dseg:/[outVLabel]-[edgeLabel]-[inVLabel]/[outVPkProperty1]/[outVPkProperty2]/[edgePkProperty1]/[edgePkProperty2]/[inVPkProperty1]/[inVPkProperty2]`.

Below is an example that shows how to lookup a `created` edge between `person` and `software`, where `person's` primary key properties
`eduard/37` are provided before the creation_year `2018` and `software's` primary key properties `hackertools/1/Java`:
```
gremlin> g.E('dseg:/person-created-software/eduard/37/2018/hackertools/1/Java').valueMap(true)
==>{id=dseg:/person-created-software/eduard/37/2018/hackertools/1/Java, label=created, creation_year=2018, weight=2.3}
```


## How to escape things

Generally, the ID format is following the encoding requirements that are defined in the [RFC3986: Uniform Resource Identifier (URI)](https://www.ietf.org/rfc/rfc3986.txt) specification.

The specification defines the usage of a **percent-encoding** mechanism to represent a data octet in a component when that octet's corresponding character is outside the allowed set.

A percent-encoded octet is encoded as a character triplet, consisting of the percent character **%** followed by the two hexadecimal digits representing that octet's numeric value.  For example, **%20** corresponds to the space character.

The character encoding chart shows characters that require percent-encoding. For example, the encoded vertex ID `dseg:/uri_encoded/-%3E/%3A/%2F/%26` would be decoded to the vertex label `uri_encoded` with the primary key values `->`, `:`, `/`, `&`.

### Character encoding chart

| Classification | Included characters | Encoding required? |
| ------ | ------ | ------ |
| Safe characters | Alphanumerics `[0-9a-zA-Z]`, special characters `$-_.+!*'(),`, and reserved characters used for their reserved purposes (e.g., question mark used to denote a query string) | NO |
|Reserved characters | `; / ? : @ = &` (does not include blank space) | YES ( only need to be encoded when not used for their reserved purposes) |
| ASCII Control characters | Includes the ISO-8859-1 (ISO-Latin) character ranges 00-1F hex (0-31 decimal) and 7F (127 decimal.) |	YES |
| Non-ASCII characters	| Includes the entire “top half” of the ISO-Latin set 80-FF hex (128-255 decimal.) | YES |
| Unsafe characters | Includes the blank/empty space and ``` " < > # % { } \| ^ ~ [ ]` ``` | YES |