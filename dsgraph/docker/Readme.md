# Getting Started

To run the Labs docker images you must accept the [DataStax Labs Term](https://www.datastax.com/terms/datastax-labs-terms) by using the environmental variable DS_LICENSE.

To Start the images use the following commands 

```
docker run -e DS_LICENSE=accept --name my-dse -p 9042:9042 -d datastax/labs:dse-graph-experimental -s -k -g
```

```
docker run -e DS_LICENSE=accept --link my-dse --name my-studio -p 9091:9091 -d datastax/labs:dse-graph-studio-experimental
```

# Compose Example
Use the docker compose example provided in this repo to automate provising of a single Graph node and single Studio node 

# Support
Images contained in this repository are not intended for production use and are not "Supported Software" under any DataStax subscriptions or other agreements.

Join the [DataStax Community](https://community.datastax.com/spaces/11/index.html) to connect with peers who can help you out in sticky situations and give you pointers.
Please visit  [DataStax Lab Github](https://github.com/datastax/labs) to file any issues encountered

# Next Steps
Head over to the [DataStax Academy](https://academy.datastax.com/) for free training and tutorials.
