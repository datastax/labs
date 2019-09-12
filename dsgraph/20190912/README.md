# DataStax Graph Labs

These are the instructions for installing and using the DataStax Graph
Labs preview using Docker images.

For the downloadable tarball version of the Graph preview, refer to
the [DataStax Labs][1] website and download DataStax Graph (DSG). Then
follow the instructions with the included README.md from the download.

The use of the software described here is subject to the [DataStax Labs
Terms][4].

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

## Using Labs Docker Images

Note: We intentionally do not have a 'latest' tag on the Labs Docker
images. Instead they are version tagged to ensure you're using the
intended preview version. See the examples below.

To run the Docker examples you must accept and agree to the [DataStax
Labs Terms][4]. Setting the `DS_LICENSE` variable as shown below
indicates your acceptance of those terms.

### DataStax Enterprise

To download and run the DataStax Enterprise Labs Docker image:

    docker run -e DS_LICENSE=accept --name my-dse -d datastaxlabs/datastax-graph:6.8.0.20190912 -s -k -g

This will start DataStax Enterprise Server with Graph, Search and
Analytics running (the recommended combination).

After several minutes, DSE should be running. You can confirm this with
a nodetool status command:

    docker exec -it my-dse dsetool status

In the status message you should see confirmation that the single node
is running and that Graph mode is enabled:

    DC: dc1         Workload: SearchAnalytics Graph: yes    Analytics Master: 172.17.0.2
    ====================================================================================
    Status=Up/Down
    |/ State=Normal/Leaving/Joining/Moving
    --   Address      Load         Effective-Ownership  Token                  Rack     Health [0,1]
    UN   172.17.0.2   124.07 KiB   100.00%              -2702044001711757463   rack1    0.20

Refer to [datastax/dse-server][2] for more details about running
DataStax Enterprise Server in a Docker container.

### DataStax Studio

To download and run the DataStax Studio Labs Docker image:

    docker run -e DS_LICENSE=accept --link my-dse --name my-studio -p 9091:9091 -d datastaxlabs/datastax-studio:6.8.0.20190912

This will start DataStax Studio and connect it with the running
DataStax Enterprise Server Docker container.

Once Studio has started it should be viewable in a browser at: <http://DOCKER_HOST_IP:9091>

Refer to [datastax/dse-studio][3] for more details about running
DataStax Studio in a Docker container.

### Docker Compose Example

Use the Docker Compose example file `docker-compose.yml` provided in
this repo to automate provisioning of a single DSE Graph node with a
single Studio node.

To use Docker Compose you must first signal your acceptance of the
[DataStax Labs Terms][4] by uncommenting the `DS_LICENSE` environment
variable setting under both `dse` and `studio` sections:

    dse:
      image: ...
      environment:
        - DS_LICENSE=accept


    studio:
      image: ...
      environment:
        - DS_LICENSE=accept

The combined environment can be brought up or torn down with Docker Compose commands:

    docker-compose up -d
    docker-compose down

Then follow the example steps listed above for working with DataStax
Enterprise and DataStax Studio.

## Java and Python Drivers

To write your own applications which work with the DataStax Graph Labs
preview, you'll need special Labs versions of the Java and Python
drivers. Visit the [DataStax Labs][1] website and download DataStax
Graph (DSG). Then expand the zip file and refer to the `python-driver`
and `java-driver` directories for further instructions.

## Next Steps

We want to hear your feedback! Go to the Labs section of the new
DataStax Community forums:
<https://community.datastax.com/spaces/11/index.html>

You can also reach out on the Labs forums for any help needed with
these installation steps.

[1]: https://downloads.datastax.com/#labs
[2]: https://hub.docker.com/r/datastax/dse-server
[3]: https://hub.docker.com/r/datastax/dse-studio
[4]: https://www.datastax.com/terms/datastax-labs-terms