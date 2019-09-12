# GraphBinary DSE specific types.

DSE 6.8 comes with support for GraphBinary from the TinkerPop drivers and the DSE drivers. DSE has some additional types
from the standard GraphBinary set of supported types. These types are implemented via Custom types from the GraphBinary
protocol specification.

## Types 

### CQL Duration

Custom Type name: `"driver.core.Duration"`
 
Format: `{months}{days}{nanoseconds}`

Where:

* `months` is a 4 bytes `Int`
* `days` is a 4 bytes `Int`
* `nanoseconds` is an 8 bytes `Long`

### Geo Point / Geo LineString / Geo Polygon

Custom Type name: `"driver.dse.geometry.Point"` / `"driver.dse.geometry.LineString"` / `"driver.dse.geometry.Polygon"`

Format: `{wkb}`

Where:

* `wkb` is the bytes representing the Geo type encoded as a WKB

### Geo Distance

Custom Type name: `"driver.dse.geometry.Distance"`

Format: `{center}{radius}`

Where:

* `center` is encoded as a `Geo Point`
* `radius` is an 8 bytes `Double`

### EditDistance

Custom Type name: `"driver.dse.search.EditDistance"`

Format: `{distance}{query}`

Where:

* `distance` is a 4 bytes `Int` of the predicate's distance
* `query` is a `String` of the predicate's query

### Pair

Custom Type name: `"org.javatuples.Pair"`

Format: `{item1}{item2}`

Where:

* `item1` is a fully qualified value composed of `{type_code}{type_info}{value_flag}{value}`
* `item2` is a fully qualified value composed of `{type_code}{type_info}{value_flag}{value}`

### TupleValue / UDT Value

Custom Type name: `"driver.core.TupleValue"` / `"driver.core.UDTValue"`

Format: `{type_spec}{value_bytes}`

Where:

* `type_spec` is a full CQL type specification. For a UDT the specification for the type is defined 
here: https://github.com/apache/cassandra/blob/cassandra-3.11.0/doc/native_protocol_v5.spec#L641 and for Tuples the 
specification is defined here: https://github.com/apache/cassandra/blob/cassandra-3.11.0/doc/native_protocol_v5.spec#L653
* `value_bytes` is the values of the fields of the Tuple or UDT, encoded directly into the CQL native protocol format.
See CQL protocol specification for more information on the format of these values.






