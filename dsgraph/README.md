# DataStax Graph Labs

These are the instructions for installing and using the DataStax Graph
Labs preview using Docker images.

For the downloadable tarball version of the Graph preview, refer to
the [DataStax Labs][1] website and download DataStax Graph (DSG). Then
follow the instructions with the included README.md from the download.

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

Please review the [graph-docs](./graph-docs/) directory file for an
overview of the features, behaviors, and functionality of the
experimental and new graph engine.

### DataStax Studio Sample Notebooks

In addition to the documentation included in the ./graph-docs
directory, DataStax is providing a set of Getting Started Studio
Notebooks to help users understand the key concepts of the
experimental graph engine build sample applications quickly.

Three different Getting Started notebooks are provided in this
package:

* DataStax Graph Gremlin - start here for a pure Gremlin experience
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
Please review the contents under [.graph-migration](./graph-migration/).

## Using Labs Docker Images

### DataStax Enterprise

To download and run the DataStax Enterprise Labs Docker image:

    docker run -e DS_LICENSE=accept --name my-dse -d datastax/labs-dse-graph-experimental -s -k -g

This will start DataStax Enterprise Server with Graph, Search and
Analytics running (the recommended combination).

After a few minutes, DSE should be running. You can confirm this with
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

    docker run -e DS_LICENSE=accept --link my-dse --name my-studio -p 9091:9091 -d datastax/labs-dse-graph-studio-experimental

This will start DataStax Studio and connect it with the running
DataStax Enterprise Server Docker container.

Once Studio has started it should be viewable in a browser at: <http://DOCKER_HOST_IP:9091>

Refer to [datastax/dse-studio][3] for more details about running
DataStax Studio in a Docker container.

### Java and Python Drivers

To write your own applications which work with the DataStax Graph Labs
preview, you'll need special Labs versions of the Java and Python
drivers. Visit the [DataStax Labs][1] website and download DataStax
Graph (DSG). Then expand the `graph-labs.zip` file and continue with
the appropriate section below.

### Java Driver

To use the Java driver with your application, follow these
instructions.

Find the `dse-java-driver.tar.gz` file and expand it. You should see
the following contents under the newly-created `dse-java-driver`
folder:

    # Core DSE driver
    dse-java-driver-core-1.8.1.20190510-LABS.jar
    dse-java-driver-core-1.8.1.20190510-LABS-shaded.jar # shaded deps
    dse-java-driver-core-1.8.1.20190510-LABS.pom
    # Graph fluent API
    dse-java-driver-graph-1.8.1.20190510-LABS.jar
    dse-java-driver-graph-1.8.1.20190510-LABS.pom
    # Object mapper
    dse-java-driver-mapping-1.8.1.20190510-LABS.jar
    dse-java-driver-mapping-1.8.1.20190510-LABS.pom

You can then reference the above jars directly by placing them in your
application's classpath.

Alternatively, you can install the above jars and poms in your local
Maven repository, so that they can be declared as regular dependencies
in your Maven or Gradle build. Refer to Maven's [Guide to installing
3rd party JARs] for further information.

[Guide to installing 3rd party JARs]:https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html

### Python Driver

To use the Python driver with your application, follow these
instructions.

Find the `dse-java-driver.tar.gz` file and expand it. You should see
`dse-driver*.tar.gz` and`dse-graph*.tar.gz` files. These do not need
to be expanded.

A virtual environment is recommended, especially so that these Labs
drivers don't mix or conflict with any released drivers on the same
system. For example to use pyenv with the virtualenv plugin you would
follow these steps to create a virtual environment:

    pyenv virtualenv 2.7.15 labs
    pyenv activate labs

Next install the main Python driver and the DataStax Graph
enhancements driver:

    pip install --upgrade pip
    pip install dse-driver-2.8.1.20190509+labs.tar.gz
    pip install dse-graph-1.6.0.20190509+labs.tar.gz

If the install completes without any errors, you can confirm that both
drivers are loadable:

    python -c 'import dse_graph; print dse_graph.__version__'
    python -c 'import dse; print dse.__version__'

## Next Steps

We want to hear your feedback! Go to the Labs section of the new
[DataStax Community forums](https://community.datastax.com/spaces/11/index.html).

You can also reach out on the Labs forums for any help needed with
these installation steps.

[1]: https://downloads.datastax.com/#labs
[2]: https://hub.docker.com/r/datastax/dse-server
[3]: https://hub.docker.com/r/datastax/dse-studio
