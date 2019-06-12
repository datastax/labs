# DataStax Graph Labs

These are the instructions for installing and using the DataStax Graph
Labs preview.

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

Please review the ./graph-docs/README.html file for an overview of the
features, behaviors, and functionality of the experimental and new
graph engine.

### DataStax Studio Sample Notebooks

In addition to the documentation included in the ./graph-docs
directory, DataStax is providing a set of Getting Started Studio
Notebooks to help users understand the key concepts of the
experimental graph engine build sample applications quickly.

Three different Getting Started notebooks are provided in this
package:

* DataStax Graph Gremlin - start here for a pure Gremlin experience
* DataStax Graph CQL as Graph - start here to use CQL to create a new graph.
* DataStax Graph CQL Conversion to Graph - start here to see how to convert
  an existing CQL keyspace into a graph.

The Studio sample notebook can be found under the directory:
./studio-getting-started-guide-notebook

See the instructions under the DataStax Studio section further down in
this file to use these notebooks.

### Classic Graph Schema Migration Example

The DataStax Graph engine available in DSE 6.7 and below is now
referred to as DataStax's Classic graph engine. If you are interested
in learning how to migrate from an existing graph built using the
Classic graph engine into a graph that's compatible with the new,
experimental graph engine included in DataStax Labs, please contact a
DataStax Services professional and a member of DataStax's Graph
Practice will be in touch to discuss how DataStax can help.

A simple migration example is available for review in this package.
Please review the file ./graph-migration/README.md

## Installation

### Prerequisites

* Platform: Linux or MacOS (Studio can run on Windows but DSE server cannot)
* Java: Java 8 (1.8.0_151 minimum; Java 9 or higher not supported)
* Python: 2.7.x

### DataStax Enterprise

DataStax Enterprise Server is provided in tarball format for this labs
preview. To install you will follow similar instructions as explained
here for the latest released DSE which is version 6.7:
<https://docs.datastax.com/en/install/6.7/install/installTARdse.html>

In summary you will need to first expand the DSE tarball:

    tar xvzf dse-6.8.0*.tar.gz

Then make the default directories and change their ownership to the
current running user:

    sudo mkdir /var/{log,lib}/{dsefs,spark,cassandra}
    sudo mkdir /var/lib/spark/{rdd,worker}
    sudo chown -R $USER:$GROUP /var/{log,lib}/{dsefs,spark,cassandra}

Alternatively, if you want to use different directory locations, see
the 6.7 documentation referenced above.

Finally you can start DSE with DataStax Graph enabled:

    cd dse-6.8.0*
    bin/dse cassandra -g

To get the most value out of DataStax Graph, DSE Search is also
recommended to be enabled.

To start DSE with DataStax Graph and DSE Search enabled, run this
command instead:

    cd dse-6.8.0*
    bin/dse cassandra -s -g

To leverage DSEGraphFrames or any other analytical tool, like Gremlin
OLAP, run this command instead:

    cd dse-6.8.0*
    bin/dse cassandra -s -g -k

After a few minutes, DSE should be running. You can confirm this with
a nodetool status command:

    bin/nodetool status

In the status message you should see confirmation that the single node
is running and that Graph mode is enabled:

    DC: Graph           Workload: Cassandra       Graph: yes
    =======================================================
    Status=Up/Down
    |/ State=Normal/Leaving/Joining/Moving
    --   Address     Load         Owns    Token                   Rack    Health [0,1]
    UN   127.0.0.1   103.21 KiB   ?       -5463528168999689403    rack1   0.50

### DataStax Studio

DataStax Studio is provided in tarball format for this labs preview. To
install you will follow similar instructions as explained here for the
latest released Studio which is version 6.7:
<https://docs.datastax.com/en/install/6.7/install/installStudio.html>

Important: If an earlier version of DataStax Studio is already
installed, back up the user data directory before you install the labs
version. The user data directory is normally located under
`user_home_directory/.datastax_studio` (see the docs linked above for
details).

In summary you will need to first expand the Studio tarball:

    tar xvzf datastax-studio-6.8.0*.tar.gz

Next start the Studio server:

    cd datastax-studio-6.8.0*
    bin/server.sh

Once Studio is running it should report a status message similar to:

    Studio is now running at: http://127.0.0.1:9091

Finally, to access DataStax Studio, switch to your web browser and
access this URL: <http://URI_running_DSE:9091/>

For DSE running on localhost, the URI will be `localhost`. When DSE is
running on a different machine, the URI will be the hostname or IP
address for the remote machine.

### Java Driver

To use the Java driver with your application, follow these instructions.

Unpack the `dse-java-driver.tar.gz` file. You should see the following
contents under the newly-created `dse-java-driver` folder:

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

To use the Python driver with your application, follow these instructions.

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
