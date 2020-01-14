## What's New and Documentation

The new, experimental graph engine included in this Labs package
allows for users to work seamlessly across Cassandra and Graph APIs
while accessing the data stored in Cassandra. This is possible because
the experimental graph engine has been embedded into Cassandra's data
model metadata. DataStax is calling this novel approach to graph data
management, DataStax's OneModel.

Because this is an experimental preview of a new graph engine in
DataStax Graph, a subset of documentation is available that describes
the new concepts included in DataStax Graph.

Please review the [graph-docs](./graph-docs/) directory for an
overview of the features, behaviors, and functionality of the
experimental and new graph engine.

### Changes In This Release

#### Graph Engine Upates

- DSP-16572 - Enable InetAddress in sandbox
- DSP-16857 - Allow for logged batches with next generation graph
- DSP-16995 - Unlabelled queries should be flagged as an anti-pattern in profile, index analysis and explain
- DSP-17897 - ReadColumns Method Error Message is incorrect
- DSP-18868 - Use URIs for IDs
- DSP-18915 - Support querying nested elements of UDTs/Tuples via Search index
- DSP-19080 - Understand shortest path scenario with Security + Graph
- DSP-19262 - Add warning if the user tries to have a property called id or label
- DSP-19453 - Graph misdiagnoses query issue and suggests invalid Materialized Views
- DSP-19513 - Drop CQL script generation from the Graph Schema Migration Tool
- DSP-19605 - VertexInputRDD needs to deal with new ID format
- DSP-19615 - Investigate NGDG heap pressure
- DSP-19619 - Add DSE GraphFrame copy example to markdown documentation
- DSP-19681 - [performance] master-core regression for single vertex writes on 1 node
- DSP-19705 - Could not initialize class com.datastax.bdp.graphv2.dsedb.schema.SearchColumn on certain OLAP queries

### DataStax Studio Sample Notebooks

In addition to the documentation included here, DataStax is providing
a set of Getting Started Studio Notebooks to help users understand the
key concepts of the experimental graph engine build sample
applications quickly.

To use these notebooks, please look in the [studio-getting-started-guide-notebooks](./studio-getting-started-guide-notebooks/) directory and simply import each notebook using this instructions found on the [Datastax Documentation Studio page] (https://docs.datastax.com/en/studio/6.7/studio/importNotebook.html) 

Three different Getting Started notebooks are provided in this
package:

* DataStax Graph Gremlin - start here for a pure Gremlin experience.
* DataStax Graph CQL as Graph - start here to use CQL to create a
  new graph.
* DataStax Graph CQL Conversion to Graph - start here to see how
  to convert an existing CQL keyspace into a graph.

The Studio sample notebooks are already embedded in the Studio Docker
image and will be visible once the container is running.

### Classic Graph Schema Migration Example

The DataStax Graph engine available in DSE 6.7 and below is now
referred to as DataStax's Classic graph engine. If you are interested
in learning how to migrate from an existing graph built using the
Classic graph engine into a graph that's compatible with the new,
experimental graph engine included in DataStax Labs, please contact a
DataStax Services professional and a member of DataStax's Graph
Practice will be in touch to discuss how DataStax can help.

A simple migration example is available for review in this repo.
Please review the contents under [graph-migration](./graph-migration/).

## Java and Python Drivers

To write your own applications which work with the DataStax Graph Labs
preview, you'll need special Labs versions of the Java and Python
drivers. Visit the [DataStax Labs][1] website and download DataStax
Early Adopter Release. Then expand the zip file and refer to the `python-driver`
and `java-driver` directories for further instructions.

## Next Steps

We want to hear your feedback! Go to the Labs section of the new
DataStax Community forums:
<https://community.datastax.com/spaces/11/index.html>

You can also reach out on the Labs forums for any help needed with
these installation steps.

[1]: https://downloads.datastax.com/#labs
[4]: https://www.datastax.com/terms/datastax-labs-terms