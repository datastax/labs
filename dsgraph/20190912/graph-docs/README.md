# DataStax Graph

## What is DataStax Graph?
DataStax Graph is a graph API that sits on top of the DSE stack.
It provides unified access to a subset of DSE features, and also allows complex queries via the Gremlin query language.  
 
## Why Core Engine?
The Core Engine initiative was started to bring enhanced usability and performance to DataStax Graph by:

1. Aligning the data model with regular C* tables - Many features from DSE work more naturally with Core Engine. 
In addition, users that have existing C* knowledge will have a good chance of understanding how their data model will 
affect performance of their queries.
2. Enhanced usability - We have taken on board feedback from users and support, in particular
reducing or removing configuration or features that confused, promoted bad practise or made support difficult.
Messaging for errors and profile output has been greatly improved.
3. Enhanced performance - Read and write path have been significantly simplified allowing for greater performance. 

Existing graph behaviour has been retained by splitting in to two engines: `Classic` and `Core`:

* `Classic` is maintained to allow backward compatibility for existing users.
* `Core` should be used for all new graphs.

## Overview

[What's new in DataStax Graph?](WhatsNewInDataStaxGraph.md)

[Deprecated features](DeprecatedFeatures.md)

[Classic to Core migration](ClassicToCoreGraphMigration.md)

[Getting started](GettingStarted.md)

## Schema

[System and schema API](SystemAndSchemaAPI.md)

[CQL graph extensions](CQLGraphExtensions.md)

## Traversal execution and indexing

[Traversal options](TraversalOptions.md)

[Index analyzer](IndexAnalyzer.md)

## Analytics

[DataStax Graph Frames](DseGraphFrames.md)

## Drivers

[Protocol compatibility](ProtocolCompatibility.md)

## Other

[Nested Access on UDT/Tuple Elements](NestedAccessOnUdtAndTupleElements.md)

