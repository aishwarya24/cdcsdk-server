# Performance Framework

## Setup the Dev Server

    #install docker
    ./setup.sh

    # Build CDCSDK Docker Image if required
    git clone https://github.com/yugabyte/cdcsdk-server.git
    cd cdcsdk-server
    mvn package -Dquick

    # Install Yugabyte binaries: https://docs.yugabyte.com/preview/quick-start/

    # Setup maven and java 11
    sudo yum install -y maven java-11-openjdk-devel java-11-openjdk
    sudo alternatives --config java # Choose Java 11
    echo "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.15.0.9-2.el8_5.x86_64/" >> $HOME/.bashrc
    ## Ensure Java 11 is chosen
    source $HOME/.bashrc
    java -version
    mvn -version


## Setup Tables in Yugabyte

    ./tables.sh

# Create a settings file

Rest of the instructions assume the name is `settings.env`


|Settings|Description|
|--------|------------|
|YBPATH| Path to yugabyte binaries. `ysqlsh` and `ybadmin` are used to setup.|
|PGUSER| Username in Yugabytedb|
|PGHOST| IP/Hostname of Yugabytedb|
|PGPORT| Port of Yugabytedb|
|PGDATABASE| Database where tables should be created|
|MASTER_ADDRESSES| Address of yugabyte cluster addresses|
|KAFKA_HOST| Host and port where Kafka is running|
|CDC_SDK_STREAM_ID| ID of CDCSDK Stream. Created by `tables.sh`|
|TABLES| List of tables in namespace that have to be processed|

##  Start all services in docker

### Start CDCSDK Server with NullChangeConsumer

    # Start CDCSDK Server
    docker compose -f cdcsdk-base.yaml -f cdcsdk-null.yaml --env-file settings.env up -d

### Start CDCSDK Server with Kafka + PG on local system.

    docker compose -f kafka-pg.yaml --env-file settings.env up -d
    docker compose -f cdcsdk-base.yaml -f cdcsdk-kafka-local.yaml --env-file \
        settings.env up -d

## Configure and Run Workloads

### Setup Kafka Connect

    # Setup consumers
    ./kafka_setup.sh

### Setup and run workloads

    ./setup_workloads.sh
    ./run_workloads.sh