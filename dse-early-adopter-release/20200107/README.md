# DataStax Early Adopter Release Labs

These are the instructions for using the DataStax Early Adopter Release using Docker images.

For the downloadable tarball version of the DSE EAP preview, refer to
the [DataStax Labs][1] website and download DataStax EAP preview. Then,
follow the instructions with the included README.md from the download.

For the Docker version of the DSE EAP preview, refer to the [DataStax Labs][1] and then read the Docker section in this README for installation instructions. 

The use of the software described here is subject to the [DataStax Labs
Terms][4].

## What's New and Documentation

The EAP release included in this Labs package includes the following
new features and enhancements:
- DataStax Graph Core Engine - New Graph for Cassandra engine
- DB-424 - Zero Copy Streaming
- DB-1960 - Incremental Nodesync
- DB-3289 - Opt-In Guardrails (# of Cols in a Table, # of tables, # of MVs and 2is, and partition size)
- DB-3170 - Allow setting of pre-hashed passwords via CQL
- DB-74 - New TRUNCATE and Update Permissions
- DSP-17044 - User’s can now supply TTL and WriteTime based on Column in a DataFrame
- DSP-18261 - Consolidate AOSS log files
- DB-413 - Implement encryption on the SSTable Partition Index 
- DB-468 - Experimental Java 11 Support for DSE Core Only (C* Only)
- DB-2831 - Allow filtering using IN restrictions
- DSP-17586 - DSE Tools startup are faster 
- DSP-13680, DSP-16873 - DSEFS more reliable startup, shutdown 
- DSP-17823 - Spark 2.4 Upgrade 
- DSP-15762 - Improve Spark Job Performance (by up to 60%) by Reducing Tombstones 
- DSP-17431 - Remove Legacy Solr Join Syntax for Non Partition Key JOINS 
- DB-2509 - Chunk cache heap overhead is too high 

### Graph Documentation
Please review the [graph/graph-docs](./graph/graph-docs/) directory for an
overview of the features, behaviors, and functionality of the
experimental and new graph engine.

### Zero Copy Streaming
Zero Copy Streaming improves the performance of Streaming operations up to 4X. This is done by changing the streaming process to avoid any serialization during streaming such that the entire streaming process becomes a network copy.  Zero Copy Streaming is enabled by default. 

Zero Copy Streaming functions by streaming the required ranges of an sstables to separate data files, while the sstable metadata is streamed in its entirety and linked to every data file produced on the destination node.  This avoids the costly overhead of rebuilding the metadata at the expense of additional disk usage (see zerocopy_max_unused_metadata_in_mb). All sstables and their components are copied via zero-copy operations, greatly reducing GC pressure and improving overall speed.

This item introduces the following new cassandra.yaml properties. Please refer to the cassandra.yaml file for more details on each property:
* zerocopy_streaming_enabled -- Enabled by default
* zerocopy_max_sstables -- Determines the max number of sstables a *single* sstable can be split into to actually use zero-copy rather than legacy streaming.
* zerocopy_max_unused_metadata_in_mb -- Determines how many megabytes *per sstable* of excess metadata are allowed in order to actually use zero-copy rather than legacy streaming
* stream_outbound_buffer_in_kb -- Buffer size for stream writes: each outbound streaming session will buffer writes according to such size.
* stream_max_outbound_buffers_in_kb -- Max amount of pending data to be written before pausing outbound streaming: this value is shared among all outbound streaming session, in order to cap the overall memory used by all streaming processes (bootstrap, repair etc).

### Incremental Nodesync
NodeSync has a new incremental mode, which can be enabled on a per-table basis with:
       ALTER TABLE t WITH nodesync = { 'enabled': 'true', 'incremental': 'true'}

When enabled, new validations will not re-validate previously validated data, drastically lowering the work done by NodeSync (and thus its impact on the cluster). One (current) downside however is that if a node loses an sstable (for instance an sstable gets corrupted and either needs to be entirely deleted, or scrub is not able to recover all of its data), then a manual user validation needs to be triggered to ensure the lost data is recovered.

This item will be enabled by default when the next version of DSE is GA. DataStax recommends using Incremental Nodesync over Nodesync.

### Guardrails
With this release of DSE, DataStax is introducing a new concept to help users avoid making mistakes and implementing known anti-patterns in Cassandra.  We call these items Guardrails. With this release of DSE, the Guardrails are not enabled by default unless otherwise noted. They are optional. Based on the experiences and feedback from DSE users, DataStax may enable more of these items by default in future releases.
​
Here is the list of Guardrails introduced in this release. Please refer to the cassandra.yaml file for more details on each guardrail:
* tombstone_warn_threshold: Default tombstone_warn_threshold is 1000
* tombstone_failure_threshold: Default tombstone_failure_threshold is 100000
* partition_size_warn_threshold_in_mb: Log a warning when compacting partitions larger than this value.
* batch_size_warn_threshold_in_kb: Log WARN on any multiple-partition batch size that exceeds this value. 64kb per batch by default.
* batch_size_fail_threshold_in_kb: Fail any multiple-partition batch that exceeds this value. The calculated default is 640kb (10x warn threshold).
* unlogged_batch_across_partitions_warn_threshold: Log WARN on any batches not of type LOGGED than span across more partitions than this limit.
* column_value_size_failure_threshold_in_kb: Failure threshold to prevent writing large column value into Cassandra.
* columns_per_table_failure_threshold: Failure threshold to prevent creating more columns per table than threshold. 
* fields_per_udt_failure_threshold: Failure threshold to prevent creating more fields in user-defined-type than threshold.
* collection_size_warn_threshold_in_kb: Warning threshold to warn when encountering larger size of collection data than threshold.
* items_per_collection_warn_threshold: Warning threshold to warn when encountering more elements in collection than threshold.
* read_before_write_list_operations_enabled: Whether read-before-write operation is allowed, eg. setting list element by index, removing list element
* secondary_index_per_table_failure_threshold: Failure threshold to prevent creating more secondary index per table than threshold.
* materialized_view_per_table_failure_threshold:  Failure threshold to prevent creating more materialized views per table than threshold.
* tables_warn_threshold: Warn threshold to warn creating more tables than threshold.
* tables_failure_threshold: Failure threshold to prevent creating more tables than threshold.
* table_properties_disallowed: Preventing creating tables with provided configurations.
* user_timestamps_enabled: Whether to allow user-provided timestamp in write request. Default is true.
* write_consistency_levels_disallowed: Preventing query with provided consistency levels
* page_size_failure_threshold_in_kb: Failure threshold to prevent providing larger paging by bytes than threshold, also served as a hard paging limit
* in_select_cartesian_product_failure_threshold: Failure threshold to prevent IN query creating size of cartesian product exceeding threshold, eg. "a in (1,2,...10) and b in (1,2...10)" results in cartesian product of 100.
* partition_keys_in_select_failure_threshold: Failure threshold to prevent IN query containing more partition keys than threshold
* disk_usage_percentage_warn_threshold: Warning threshold to warn when local disk usage exceeding threshold. Valid values: (1, 100)
* disk_usage_percentage_failure_threshold: Failure threshold to reject write requests if replica disk usage exceeding threshold. Valid values: (1, 100)

## Using Labs Docker Images

Note: We intentionally do not have a 'latest' tag on the Labs Docker
images. Instead they are version tagged to ensure you're using the
intended preview version. See the examples below.

To run the Docker examples you must accept and agree to the [DataStax
Labs Terms][4]. Setting the `DS_LICENSE` variable as shown below
indicates your acceptance of those terms.

### DataStax Enterprise
To download and run the DataStax Enterprise Labs Docker image:

    docker run -e DS_LICENSE=accept --name my-dse -d datastaxlabs/dse-server-early-adopter-release:6.8.0.20200107 -s -k -g

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

    docker run -e DS_LICENSE=accept --link my-dse --name my-studio -p 9091:9091 -d datastaxlabs/dse-studio-early-adopter-release:6.8.0.20191202

This will start DataStax Studio and connect it with the running
DataStax Enterprise Server Docker container.

Once Studio has started it should be viewable in a browser at: <http://DOCKER_HOST_IP:9091>

Update the default connection or create a new connection using my-dse as the hostname, see DataStax Studio User Guide > Creating a new connection for further instructions.

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

## Next Steps

We want to hear your feedback! Go to the Labs section of the new
DataStax Community forums:
<https://community.datastax.com/spaces/11/index.html>

You can also reach out on the Labs forums for any help needed.

[1]: https://downloads.datastax.com/#labs
[2]: https://hub.docker.com/r/datastax/dse-server
[3]: https://hub.docker.com/r/datastax/dse-studio
[4]: https://www.datastax.com/terms/datastax-labs-terms