-- Create a keyspace
-- CREATE KEYSPACE IF NOT EXISTS ycsb WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': '2' };
CREATE KEYSPACE IF NOT EXISTS ycsb WITH REPLICATION = {'class': 'NetworkTopologyStrategy', 'europe-west1': '1', 'europe-west2': '1', 'europe-west3': '1' };

-- Use the keyspace
USE ycsb; 


-- Create a table
CREATE TABLE IF NOT EXISTS ycsb.usertable (
    y_id varchar primary key,
    field0 varchar,
    field1 varchar,
    field2 varchar,
    field3 varchar,
    field4 varchar,
    field5 varchar,
    field6 varchar,
    field7 varchar,
    field8 varchar,
    field9 varchar) WITH caching = {
    'keys' : 'NONE',
    'rows_per_partition' : 'NONE'
};