# DataStax Kafka Connector Source Demo

## Install Dependencies

1. Install Docker
2. Install Docker Compose
3. Download kafaka-connect-dse-2.0.0-20190925-LABS.jar from [DataStax Labs]() and place it within this directory.

## Start the components
1. Start up the stack `docker-compose up -d`

If for some reason a component does not come up due a dependency being down try running this command again.

## Configure DSE
1. Configure DSE schema 
   
   ```
   docker-compose exec dse cqlsh
   ```
   
   ```
   CREATE KEYSPACE demo_ks WITH replication = {'class': 'NetworkTopologyStrategy', 'dc1': 1};
   CREATE TABLE demo_ks.demo_table (
     k TEXT,
     v TEXT,
     PRIMARY KEY ((k))
   );
   ```

2. Enable DSE Advanced Replication Destination for Kafka
   
   ```
   docker-compose exec dse dse advrep destination create --name demo_destination --transmission-enabled true
   docker-compose exec dse dse advrep destination list
   ```

3. Enable DSE Advanced Replication Channel for `demo_ks.demo_table`
   
   ```
   docker-compose exec dse dse advrep channel create --data-center-id dc1 --source-keyspace demo_ks --source-table demo_table --destination demo_destination --transmission-enabled true --collection-enabled true
   docker-compose exec dse dse advrep channel status
   ```

## Configure Kafka and the Connector
1. Connect to the Confluent Control Panel http://localhost:9021/
2. Select the only cluster
3. Click "Topics" in the left sidebar
4. Click "Add a topic" in the top right corner
5. Enter the following parameters then click "Create with defaults"
    
    Topic name: demo-topic
    Number of partitions: 1

6. Open "Connect" in the left sidebar
7. Click "connect-default"
8. Click "Add Connector"
9. Click "Connect" under "DseSourceConnector"
10. Enter the following parameters and click "Continue"

    Name: demo-connector
    Tasks max: 1
    Key converter class: JsonConverter
    Value converter class: JsonConverter
    topic: demo-topic
    destination: demo_destination
    contact_points: dse
11. Verify configuration parameters and click "Launch"

## Insert data to be replicated
1. Start `cqlsh` and insert data
    
    ```
    docker-compose exec dse cqlsh
    ```

    ```
    INSERT INTO demo_ks.demo_table (k, v) VALUES ('a', 'b');
    INSERT INTO demo_ks.demo_table (k, v) VALUES ('c', 'd');
    INSERT INTO demo_ks.demo_table (k, v) VALUES ('e', 'f');
    ```

## Validate behavior

1. Look in to number of messages waiting to be replicated
    
    ```
    docker-compose exec dse dse advrep replog count --source-keyspace demo_ks --source-table demo_table --destination demo_destination
    ```
2. Navigate to the topic view in the confluent control center
3. Click "Topics" in the left side bar
4. Select "demo-topic"
5. Validate messages are being produced and consumed
6. Optionally look at the messages
