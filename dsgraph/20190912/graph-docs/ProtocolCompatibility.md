# DataStax Graph 6.8 Protocols compatibility

DataStax Graph 6.8 will make some protocol compatibility checks regarding the type of graph targeted, the
type of request sent and the protocol set for this request.

Here's the compatibility scheme:

* _GraphSON 1_, _Gryo 1_: only **scripts** allowed, only for **Classic** graph
* _GraphSON 2_: **Script** and **bytecode** queries allowed, only for **Classic** graph
* _GraphSON 3_, _Gryo 3_: **Scripts** and **bytecode** allowed for **Core**
 graph
* System requests without a Graph name defined / without alias (system queries) do not require a 
specific protocol.

In general the user should allow the DSE drivers to automatically select the correct protocol.