# Introduction

DataStax Enterprise (DSE) is a distributed multi-model database built on Apache
Cassandra. The DSE Operator for Kubernetes simplifies the process of deploying
and managing DSE in a Kubernetes cluster.

# Install the operator

## Prerequisites

1. A Kubernetes cluster running version 1.13.0 or higher. The operator may
   function with older releases, but certification is pending.
2. The ability to download images from Docker Hub from within the Kubernetes
   cluster.
3. At least one Kubernetes worker node per DSE instance.

## Create a namespace

The DSE operator is built to watch over pods running DSE in a Kubernetes
namespace. Create a namespace for the cluster with:

```shell
$ kubectl create ns my-dse-ns
```

For the rest of this guide, we will be using the namespace `my-dse-ns`. Adjust
further commands as necessary to match the namespace you defined.

## Define a storage class

Kubernetes uses the `StorageClass` resource as an abstraction layer between pods
needing persistent storage and the physical storage resources that a specific
Kubernetes cluster can provide. We recommend using the fastest type of
networked storage available. On Google Kubernetes Engine, the following
example would define persistent network SSD-backed volumes.

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: dse-storage
provisioner: kubernetes.io/gce-pd
parameters:
  type: pd-ssd
  replication-type: none
volumeBindingMode: WaitForFirstConsumer
```

The above example can be customized to suit your environment and saved as
`dse-storage-class.yaml`. For the rest of this guide, we'll assume you've
defined a `StorageClass` and named it `dse-storage`. You can apply that file and
get the resulting storage classes from Kubernetes with:

```shell
$ kubectl -n my-dse-ns apply -f ./dse-storage-class.yaml

$ kubectl -n my-dse-ns get storageclass
NAME                 PROVISIONER            AGE
gce-ssd              kubernetes.io/gce-pd   1m
standard (default)   kubernetes.io/gce-pd   16m
```

## Deploy the operator

Within this guide, we have joined together a few Kubernetes resources into a
single YAML file needed to deploy the DSE operator. This file defines the
following:

1. `ServiceAccount`, `Role`, and `RoleBinding` to describe a user and set of
   permissions necessary to run the operator. _In demo environments that don't
   have role-based access-control enabled, these extra steps are unnecessary but
   are harmless._
2. `CustomResourceDefinition` for the `DseDatacenter` resources used to
   configure clusters managed by the `dse-k8s-operator`.
3. Deployment to start the operator in a state where it waits and watches for
   DseDatacenter resources.

Generally, `cluster-admin` privileges are required to register a
`CustomResourceDefinition` (CRD). All privileges needed by the operator are
present within the
[datastax-operator-manifests YAML](datastax-operator-manifests.yaml).
_Note the operator does not require `cluster-admin` privileges, only the user
defining the CRD requires those permissions._

Apply the manifest above, and wait for the deployment to become ready. You can
watch the progress by getting the list of pods for the namespace, as
demonstrated below:

```shell
$ kubectl -n my-dse-ns apply -f ./datastax-operator-manifests.yaml

$ kubectl -n my-dse-ns get pod
NAME                               READY   STATUS    RESTARTS   AGE
dse-operator-f74447c57-kdf2p       1/1     Running   0          1h
```

When the pod status is `Running`, the operator is ready to use.

# Provision a DSE cluster

The previous section created a new resource type in your Kubernetes cluster, the
`DseDatacenter`. By adding `DseDatacenter` resources to your namespace, you can
define a cluster topology for the DSE operator to create and monitor. In this
guide, a three node cluster is provisioned, with one datacenter made up of three
racks, with a total of one node per rack.

## Cluster and Datacenter

A DSE logical datacenter is the primary resource managed by the
dse-k8s-operator. Within a single Kubernetes namespace:

- A single `DseDatacenter` resource defines a single-datacenter DSE cluster.
- Two or more `DseDatacenter` resources with different `dseClusterName`'s define
  separate and unrelated single-datacenter DSE clusters. Note the operator
  manages both clusters since they reside within the same Kubernetes namespace.
- Two or more `DseDatacenter` resources that share the same `dseClusterName`
  define a multi-datacenter DSE cluster. The operator will join the DSE
  instances in each datacenter into a logical topology that acts as a single DSE
  cluster.

For this guide, we define a single-datacenter cluster. The cluster is named
`cluster1` with the datacenter named `dc1`.

## Racks

DSE is rack-aware, and the `racks` parameter will configure the DSE operator to
set up pods in a rack aware way. Note the Kubernetes worker nodes must have
labels matching `failure-domain.beta.kubernetes.io/zone`. Racks must have
identifiers. In this guide we will use `r1`, `r2`, and `r3`.

## DSE Node Count

The `size` parameter is the number of DSE instances to run in the datacenter.
For optimal performance, it's recommended to run only one DSE instance per
Kubernetes worker node. The operator will enforce that limit, and DSE
pods may get stuck in the `Pending` status if there are insufficient Kubernetes
workers available.

We'll assume you have at least three worker nodes available, if you're working
on a minikube or other setup with a single Kubernetes worker node, you must
reduce the `size` value accordingly or set the `allowMultipleNodesPerWorker`
parameter to `true`.

## Storage

Define the storage with a combination of the previously provisioned storage
class and size parameters. These inform the storage provisioner how much room to
require from the backend.

## Configuration of DSE

The `config` key in the `DseDatacenter` resource contains the parameters used to
configure the DSE process running in each pod. In general, it's not necessary to
specify anything here at all. Settings that omitted from the `config` key will
receive reasonable default values and its quite common to run demo clusters with
no custom configuration of DSE.

If you're familiar with configuring DSE outside of containers on traditional
operating systems, you may recognize that some familiar configuration parameters
have been specified elsewhere in the `DseDatacenter` resource, outside of the
`config` section. These parameters should not be repeated inside of the config
section, the operator will populate them from the `DseDatacenter` resource.

For example:
* `cluster_name`, which is normally specified in `cassandra.yaml`
* The rack and datacenter properties

Similarly, the operator will automatically populate any values which must
normally be customized on a per-DSE-instance basis. Do not specify these in the
`DseDatacenter` resource. For example:
basis
* `initial_token`
* `listen_address` and other ip-addresses.

A large number of keys and values can be specified in the `config` section, but
the details currently not well documented. The `config` key data structure
resembles the API for DataStax OpsCenter Lifecycle Manager (LCM) Configuration
Profiles. Translating LCM config profile API payloads to this format is
straightforward. Documentation of this section will be present in future
releases.

## Superuser credentials

By default, a publicly known superuser gets created. This leaves a window of 
vulnerability open from the time that the DseDatacenter gets created, 
up until someone updates the credentials. To instead create a superuser 
with your own credentials, you can create a secret with kubectl.

### Example superuser secret creation

```
kubectl create secret generic dse-superuser-secret \
    --from-literal=username=someuser \
    --from-literal=password=somepassword
```

To use this new superuser secret, specify the name of the secret from 
within the `DseDatacenter` config yaml that you load into the cluster:

```yaml
apiVersion: datastax.com/v1alpha1
kind: DseDatacenter
metadata:
  name: dtcntr
spec:
  dseSuperuserSecret: dse-superuser-secret
```

## Specifying DSE version and image

With the release of the operator v0.4.0 comes a new way to specify
which version of DSE and image you want to use. From within the config yaml
for your `DseDatacenter` resource, you can use the `dseVersion` and `dseImage`
spec properties.

`dseVersion` is required, and currently the only supported value is `6.8.0`. 

If `dseImage` is not specified, a default compatible image for the provided 
`dseVersion` will automatically be used. If you want to use a different image, specify the image in the format `<qualified path>:<tag>`.

### Using a default image

```yaml
apiVersion: datastax.com/v1alpha1
kind: DseDatacenter
metadata:
  name: dtcntr
spec:
  dseVersion: 6.8.0
```

### Using a specific image

```yaml
apiVersion: datastax.com/v1alpha1
kind: DseDatacenter
metadata:
  name: dtcntr
spec:
  dseVersion: 6.8.0
  dseImage: datastaxlabs/dse-k8s-server:6.8.0-20191113
```

## Example Config

The following example illustrates a `DseDatacenter` resource.

```yaml
apiVersion: datastax.com/v1alpha1
kind: DseDatacenter
metadata:
  name: dc1
spec:
  dseClusterName: cluster1
  dseImage: datastaxlabs/dse-k8s-server:6.8.0-20191113
  dseVersion: 6.8.0
  managementApiAuth:
    insecure: {}
  size: 3
  storageclaim:
    storageclassname: dse-storage
    resources:
      requests:
        storage: 20Gi
  racks:
    - name: r1
      zone: us-central1-a
    - name: r2
      zone: us-central1-b
    - name: r3
      zone: us-central1-f
  config:
    dse-yaml:
      authentication_options:
        enabled: False
    #cassandra-yaml:
    #  num_tokens: 32
    #jvm-server-options:
    #  initial_heap_size: "16g"
    #  max_heap_size: "16g"
    #10-write-prom-conf:
    #  enabled: True
```

Consider customizing the example above to suit your requirements, and save it as
`cluster1-dc1.yaml`. Apply this file via `kubectl` and watch the list of pods as
the operator deploys them. Completing a deployment may take several minutes per
DSE instance. When all DSE pods match the `Running` status, the cluster is ready
to use.

```shell
$ kubectl -n my-dse-ns apply -f ./cluster1-dc1.yaml

$ kubectl -n my-dse-ns get pods
NAME                            READY   STATUS    RESTARTS   AGE
dse-operator-f74447c57-kdf2p    1/1     Running   0          13m
gke-cluster1-dc1-r1-sts-0       1/1     Running   0          5m38s
gke-cluster1-dc1-r2-sts-0       1/1     Running   0          42s
gke-cluster1-dc1-r3-sts-0       1/1     Running   0          6m7s
```
# Using Your DSE cluster

## Connecting from inside the Kubernetes cluster

The DSE operator makes a Kubernetes headless service available at
`<dseClusterName>-<datacenterName>-service`. Any CQL client inside the
Kubernetes cluster should be able to connect to
`cluster1-dc1-service.my-dse-cluster` and use the nodes in a round-robin fashion
as contact points.

## Connecting from outside the Kubernetes cluster

Accessing the DSE instances from CQL clients located outside the Kubernetes
cluster is an advanced topic, for which a detailed discussion is outside the
scope of this document.

Note that exposing DSE on the public internet with authentication disabled or
with the default username and password in place is extremely dangerous. Its
strongly recommended to protect your DSE cluster with a network firewall during
deployment, and [secure the default superuser
account](https://docs.datastax.com/en/security/6.7/security/Auth/secCreateRootAccount.html)
before exposing any ports publicly.

## Scale up

The `size` parameter on the `DseDatacenter` determines how many DSE instances
are present in the datacenter. To add more DSE nodes, edit the YAML file from
the `Example Config` section above, and re-apply it precisely as before. The
operator will add DSE pods to your datacenter, provided there are sufficient
Kubernetes worker nodes available.

For racks to act effectively as a fault-containment zone, each rack in the DSE
cluster must contain the same number of DSE instances.

## Change DSE configuration

To change the DSE configuration, update the `DseDatacenter` and edit the
`config` section of the `spec`. The operator will update the config and restart
one node at a time in a rolling fashion.

## Multiple Datacenters in one Cluster

To make a multi-datacenter cluster, create two `DseDatacenter` resources and
give them the same `dseClusterName` in the `spec`.

_Note that multi-region clusters and advanced workloads are not supported, which
makes many multi-dc use-cases inappropriate for the operator._

# Maintaining Your DSE Cluster

## Repair

The operator does not automate the process of performing traditional repairs on
keyspace ranges where the data has become inconsistent due to a DSE instance
becoming unavailable in the past.

Instead, DSE provides
[NodeSync](https://www.datastax.com/2018/04/dse-nodesync-operational-simplicity-at-its-best),
a continuous background repair service that is declarative and
self-orchestrating. After creating your cluster, [Enable
NodeSync](https://docs.datastax.com/en/dse/6.7/dse-admin/datastax_enterprise/tools/dseNodesync/dseNodesyncEnable.html)
on all new tables.

## Backup

The operator does not automate the process of scheduling and taking backups at
this time.

# Known Issues and Limitations

1. The DSE Operator is not recommended nor supported for production use.  This
   release is an early preview of an unfinished product intended to allow proof
   of concept deployments and to facilitate early customer feedback into the
   software development process.
2. The operator is compatible with DSE 6.8.0 and above. It will not function
   with prior releases of DSE. Furthermore, version 0.4.0 of the operator is
   compatible only with a specific DSE docker image co-hosted in the labs
   [Docker Hub
   repository](https://cloud.docker.com/u/datastaxlabs/repository/docker/datastaxlabs/dse-k8s-server).
   Other labs releases of DSE 6.8.0 will not function with the operator.
3. The operator is compatible with DSE and the Cassandra workload only. It does
   not support DDAC or Advanced Workloads like Analytics, Graph, and Search.
4. There is no facility for multi-region DSE clusters. The operator functions
   within the context of a single Kubernetes cluster, which typically also
   implies a single geographic region.
5. The operator does not automate the repair or decommission/bootstrap of nodes
   that lose access to their data volume. With NodeSync enabled, the DSE
   instance should recover over time. The operator will not be aware that the
   DSE instance is unable to serve traffic and might make incorrect
   `podDisruptionBudget` decisions. Due to this limitation, it's not recommended
   to use local volumes.
6. The operator does not automate the creation of key stores and trust stores
   for client-to-node and internode encryption.

# Changelog

## v0.4.0
* KO-97  Faster cluster deployments
* KO-123 Custom CQL super user. Clusters can now be provisioned without the
  publicly known super user `cassandra` and publicly known default password
  `cassandra`.
* KO-42  Preliminary support for DSE upgrades
* KO-87  Preliminary support for two-way SSL authentication to the DSE
  management API. At this time, the operator does not automatically create
  certificates.
* KO-116 Fix pod disruption budget calculation. It was incorrectly calculated
  per-rack instead of per-datacenter.
* KO-129 Provide `allowMultipleNodesPerWorker` parameter to enable testing
  on small k8s clusters.
* KO-136 Rework how DSE images and versions are specified.

## v0.3.0
* Initial labs release.
