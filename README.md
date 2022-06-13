
# Yugabyte CDCSDK Server [BETA]

Yugabyte CDCSDK Server is an open source project that provides a streaming platform for change data capture from YugabyteDb. The server is based on the [Debezium|debezium.io].
CDCSDK Server uses [debezium-yugabytedb-connector|https://github.com/yugabyte/debezium-connector-yugabytedb] to capture change data events.
It supports a YugabyteDb instance as a source and supports the following sinks:
* Kafka
* HTTP REST Endpoint
* AWS S3

- [Yugabyte CDCSDK Server [BETA]](#yugabyte-cdcsdk-server-beta)
  - [Basic architecture](#basic-architecture)
    - [Engine is the Unit of Work](#engine-is-the-unit-of-work)
    - [CDCSDK Server](#cdcsdk-server)
  - [Operations](#operations)
    - [Topology](#topology)
    - [Networking](#networking)
  - [Building Debezium](#building-debezium)
    - [Why Docker?](#why-docker)
    - [Configure your Docker environment](#configure-your-docker-environment)
    - [Building the code](#building-the-code)
    - [Don't have Docker running locally for builds?](#dont-have-docker-running-locally-for-builds)
    - [Building just the artifacts, without running tests, CheckStyle, etc.](#building-just-the-artifacts-without-running-tests-checkstyle-etc)
    - [Running tests of the Postgres connector using the wal2json or pgoutput logical decoding plug-ins](#running-tests-of-the-postgres-connector-using-the-wal2json-or-pgoutput-logical-decoding-plug-ins)
    - [Running tests of the Postgres connector with specific Apicurio Version](#running-tests-of-the-postgres-connector-with-specific-apicurio-version)
    - [Running tests of the Postgres connector against an external database, e.g. Amazon RDS](#running-tests-of-the-postgres-connector-against-an-external-database-eg-amazon-rds)
    - [Running tests of the Oracle connector using Oracle XStream](#running-tests-of-the-oracle-connector-using-oracle-xstream)
    - [Running tests of the Oracle connector with a non-CDB database](#running-tests-of-the-oracle-connector-with-a-non-cdb-database)
  - [Contributing](#contributing)

## Basic architecture

### Engine is the Unit of Work
A [Debezium Engine](https://debezium.io/documentation/reference/1.9/development/engine.html) implementation is the unit of work. It implements a pipeline consisting of a source, sink and simple transforms. The only supported source is YugabyteDB.
The source is assigned a set of tablets that is polled at a configurable interval. An engine’s workflow is as follows:
* Connect to CDCSDK stream
* Get a list of tables and filter based on the include list.
* Get and record a list of tablets.
* Poll tablets in sequence every polling interval

### CDCSDK Server
A Debezium Engine is hosted within the CDCSDK server.The implementation is based on the [Debezium Server](https://debezium.io/documentation/reference/1.9/operations/debezium-server.html).
It uses the Quarkus framework and extensions to provide a server shell, metrics and alerts.
By default, a server runs one Engine implementation within a thread. A server can also run in multi-threaded mode wherein multiple engines are assigned to a thread each.
The server splits tablets into groups in a deterministic manner. Each group of tablets is assigned to an Engine.

## Quick Start

### Create a CDCSDK Stream in Yugabytedb

Use [yb-admin create_change_data_stream](https://docs.yugabyte.com/preview/admin/yb-admin/#create-change-data-stream) to create CDC Stream.
A successful operation returns a message with the Stream ID. Take note of the ID for later steps. For example:

```
CDC Stream ID: d540f5e4890c4d3b812933cbfd703ed3
```

### Download and run CDCSDK Server
CDCSDK Server distribution archives are available in [Github Releases](https://github.com/yugabyte/cdcsdk-server/releases) of the project.
Each of the releases has a tar.gz labelled as CDCSDK Server.

The archive has the following layout:

```
  cdcsdk-server
  |-- conf
  |-- cdcsdk-server-dist-<CDCSDK VERSION>-runner.jar
  |-- lib
  |-- run.sh
```

### Unpack and Run Instructions.

    export CDCSDK_VERSION=<x.y.z>
    wget https://github.com/yugabyte/cdcsdk-server/releases/download/v${CDCSDK_VERSION}/cdcsdk-server-dist-${CDCSDK_VERSION}.tar.gz

    # OR Using gh cli

    gh release download v{CDCSDK_VERSION} -A tar.gz --repo yugabyte/cdcsdk-server

    tar xvf cdcsdk-server-dist-${CDCSDK_VERSION}.tar.gz
    cd cdcsdk-server

    # Configure the application. Check next section
    touch conf/application.properties

    # Run the application
    ./run.sh
## Configuration

The main configuration file is conf/application.properties. There are multiple sections configured:
* `cdcsdk.source` is for source connector configuration; each instance of Debezium Server runs exactly one connector
* `cdcsdk.sink` is for the sink system configuration

An example configuration file can look like so:

```
debezium.sink.type=kafka
cdcsdk.sink.kafka.producer.bootstrap.servers=127.0.0.1:9092
cdcsdk.sink.kafka.producer.key.serializer=org.apache.kafka.common.serialization.StringSerializer
cdcsdk.sink.kafka.producer.value.serializer=org.apache.kafka.common.serialization.StringSerializer
cdcsdk.source.connector.class=io.debezium.connector.yugabytedb.YugabyteDBConnector
debezium.source.database.hostname=127.0.0.1
cdcsdk.source.database.port=5433
cdcsdk.source.database.user=yugabyte
cdcsdk.source.database.password=yugabyte
cdcsdk.source.database.dbname=yugabyte
cdcsdk.source.database.server.name=dbserver1
cdcsdk.source.database.streamid=<CDCSDK Stream>
cdcsdk.source.table.include.list=public.test
cdcsdk.source.database.master.addresses=127.0.0.1:7100
cdcsdk.source.snapshot.mode=never
```

For detailed documentation of the configuration, check [debezium docs](https://debezium.io/documentation/reference/stable/operations/debezium-server.html#_sink_configuration)

### Configuration using Environment Variables

Configuration using environment variables maybe useful when running in containers. The rule of thumb
is to convert the keys to UPPER CASE and replace `.` with `_`. For example, `cdcsdk.source.database.port`
has to be changed to `CDCSDK_SOURCE_DATABASE_PORT`

### HTTP Client
The HTTP Client will stream changes to any HTTP Server for additional processing.
|Property|Default|Description|
|--------|-------|-----------|
|cdcsdk.sink.type||Must be set to `http`|
|cdcsdk.sink.http.url||The HTTP Server URL to stream events to. This can also be set by defining the K_SINK environment variable, which is used by the Knative source framework.|
|cdcsdk.sink.http.timeout.ms|60000|The number of seconds to wait for a response from the server before timing out. (default of 60s)|

### Amazon S3

The Amazon S3 Sink streams changes to an AWS S3 bucket. Only **Inserts** are supported. The available configuration options are:

|Property|Default|Description|
|--------|-------|-----------|
|cdcsdk.sink.type||Must be set to `s3`|
|cdcsdk.sink.s3.bucket.name|| Name of S3 Bucket|
|cdcsdk.sink.s3.region|| Name of the region of the S3 bucket|
|cdcsdk.sink.s3.basedir||Base directory or path where the data has to be stored|
|cdcsdk.sink.s3.pattern||Pattern to generate paths (sub-directory and filename) for data files|
|cdcsdk.sink.s3.flushBytesMB|200|Trigger Data File Rollover on file size|
|cdcsdk.sink.s3.flushRecords|10000|Trigger Data File Rolloever on number of records|


### Mapping Records to S3 Objects

The Amazon S3 Sink only supports [create events](https://docs.yugabyte.com/preview/explore/change-data-capture/debezium-connector-yugabytedb/#create-events)
in the CDC Stream. It writes `payload.after` fields to a file in S3.

The filename in S3 is generated as `${cdcsdk.sink.s3.basedir}/${cdcsdk.sink.s3.pattern}`. Pattern can contain placeholders to customize the filenames. It
supports the following placeholders:

* `{YEAR}`: Year in which the sync was writing the output data in.
* `{MONTH}`: Month in which the sync was writing the output data in.
* `{DAY}`: Day in which the sync was writing the output data in.
* `{HOUR}`: Hour in which the sync was writing the output data in.
* `{MINUTE}`: Minute in which the sync was writing the output data in.
* `{SECOND}`: Second in which the sync was writing the output data in.
* `{MILLISECOND}`: Millisecond in which the sync was writing the output data in.
* `{EPOCH}`: Milliseconds since Epoch in which the sync was writing the output data in.
* `{UUID}`: random uuid string

For example, the following pattern can be used to create hourly partitions with multiple files each of which are no greater than 200MB

    {YEAR}-{MONTH}-{DAY}-{HOUR}/data-{UUID}.jsonl


#### IAM Policy

The AWS user account accessing the S3 bucket must have the following permissions:

* ListAllMyBuckets
* ListBucket
* GetBucketLocation
* PutObject
* GetObject
* AbortMultipartUpload
* ListMultipartUploadParts
* ListBucketMultipartUploads

Copy the following JSON to create the IAM policy for the user account. Change <bucket-name> to a real bucket name. For more information, see Create and attach a policy to an IAM user.

Note: This is the IAM policy for the user account and not a bucket policy.

```
{
   "Version":"2012-10-17",
   "Statement":[
     {
         "Effect":"Allow",
         "Action":[
           "s3:ListAllMyBuckets"
         ],
         "Resource":"arn:aws:s3:::*"
     },
     {
         "Effect":"Allow",
         "Action":[
           "s3:ListBucket",
           "s3:GetBucketLocation"
         ],
         "Resource":"arn:aws:s3:::<bucket-name>"
     },
     {
         "Effect":"Allow",
         "Action":[
           "s3:PutObject",
           "s3:GetObject",
           "s3:AbortMultipartUpload",
           "s3:ListMultipartUploadParts",
           "s3:ListBucketMultipartUploads"

         ],
         "Resource":"arn:aws:s3:::<bucket-name>/*"
     }
   ]
}
```


## Operations

### Topology
![CDCSDK Server Topology](topology.svg)


* A universe can have multiple namespaces.
* Each namespace can have multiple CDCSDK streams
* Each CDCSDK stream can have multiple servers associated with it. Default is 1. The group of multiple servers associated with a stream is called a ServerSet.

### Networking

A CDCSDK Server requires access to open ports in Yugabytedb. Therefore it has to run in the same VPC (or peered VPC) as the Yugabytedb.
The server also requires access to sinks in the case of Kafka or HTTP REST Endpoint and the appropriate credentials for writing to AWS S3.

### Healthchecks

CDCSDK Server exposes a simple health check REST API. Currently the health check only ensures that the
server is up and running.

#### Running the health check

The following REST endpoints are exposed:

* `/q/health/live` - The application is up and running.

* `/q/health/ready` - The application is ready to serve requests.


All of the health REST endpoints return a simple JSON object with two fields:

status — the overall result of all the health check procedures

checks — an array of individual checks

The general status of the health check is computed as a logical AND of all the declared health check procedures.
The checks array is currently empty as we have not specified any health check procedure yet.


Example output:

```
curl http://localhost:8080/q/health/live

{
    "status": "UP",
    "checks": [
        {
            "name": "debezium",
            "status": "UP"
        }
    ]
}

curl http://localhost:8080/q/health/ready

{
    "status": "UP",
    "checks": [
    ]
}
```
