package shield.proxy.data.sync;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Wrapper around a Dynamo-based backend. This wrapper uses the synchronous
 * Dynamo client, ak: threads will block while the request is being satisfied.
 */

import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.google.protobuf.ByteString;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

import java.util.Queue;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import shield.BaseNode;
import shield.config.NodeConfiguration;
import shield.network.messages.Msg.DataMessageResp;
import shield.proxy.trx.data.Write;
import shield.util.Pair;

/**
 * Wrapper around a dynamo back-end. This uses the *synchronous* dynamo clients: threads block until
 * the request has been satisfied.
 *
 * Requests in a batch are executed sequentially.
 *
 * @author ncrooks
 */
public class SyncDynamoBackingStore implements ISyncBackingStore {

  /**
   * Shortcut to configuration file
   */
  private final NodeConfiguration config;

  /**
   * Reference to the Amazon client
   */
  private final AmazonDynamoDB[] dynamodb;
  private final DynamoDB[] dynamo;

  /**
   * Wrapper around AWS secret/access key
   */
  private AWSCredentials awsCredentials;


  public SyncDynamoBackingStore(NodeConfiguration conf) {

    System.out.println("Sync Dynamo Backing Store");

    config = conf;
    if (config.AWS_ACCESS_KEY == "" || config.AWS_SECRET_KEY == "") {
      config.AWS_ACCESS_KEY = System.getenv("AWS_ACCESS_KEY_ID");
      config.AWS_SECRET_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");
      if (config.AWS_ACCESS_KEY == null || config.AWS_SECRET_KEY == null) {
        throw new RuntimeException("Incorrect access/secret key pair");
      }
    }


    System.out.println(config.AWS_ACCESS_KEY + " " + config.AWS_SECRET_KEY);
    dynamo = new DynamoDB[config.AWS_DYN_CLIENT_NB];
    dynamodb = new AmazonDynamoDB[config.AWS_DYN_CLIENT_NB];

    for (int i = 0 ; i < config.AWS_DYN_CLIENT_NB ; i++) {
      awsCredentials =
          new BasicAWSCredentials(config.AWS_ACCESS_KEY, config.AWS_SECRET_KEY);
      dynamodb[i] = AmazonDynamoDBClientBuilder.standard()
          .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
          .withEndpointConfiguration(
              new EndpointConfiguration(config.AWS_ACCESS_POINT, config.AWS_ACCESS_REGION))
          .build();
      dynamo[i] = new DynamoDB(dynamodb[i]);
    }

    if (config.AWS_CREATE_TABLE) {
      // deleteTable();
      CreateTableRequest request = createTableRequest();
      createTableWithRetries(request, 10);
      waitForTableAvailable(config.AWS_BASE_TABLE_NAME, 10);
    }

    System.out.println("Successfully initialised DynamoDB");
  }

  public SyncDynamoBackingStore(BaseNode node) {
    this(node.getConfig());
  }

  public CreateTableRequest createTableRequest() {
    ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
    keySchema.add(new KeySchemaElement()
        .withAttributeName(config.AWS_BASE_TABLE_HASH_KEY)
        .withKeyType(KeyType.HASH)); // Partition
    // key

    ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
    attributeDefinitions
        .add(new AttributeDefinition()
            .withAttributeName(config.AWS_BASE_TABLE_HASH_KEY)
            .withAttributeType(ScalarAttributeType.S));

    CreateTableRequest request = new CreateTableRequest().withTableName(config.AWS_BASE_TABLE_NAME)
        .withKeySchema(keySchema)
        .withAttributeDefinitions(attributeDefinitions)
        .withProvisionedThroughput(
            new ProvisionedThroughput().withReadCapacityUnits(config.AWS_RCUS)
                .withWriteCapacityUnits(config.AWS_WCUS));

    return request;
  }

  private void waitForTableAvailable(String tableName, int retries) {
    DescribeTableRequest request = new DescribeTableRequest();
    request.setTableName(tableName);

    for (int i = 1; i < retries; i++) {
      DescribeTableResult result = dynamodb[0].describeTable(request);
      if ("ACTIVE".equals(result.getTable().getTableStatus())) {
        return;
      }

      System.out.println(String.format(
          "Table %s not available yet, try %d of %d!", tableName, i, retries));
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e1) {
        // ignore
      }
    }
  }

  private void deleteTable() {
    boolean isPresent = true;
    boolean success = false;
    while (!success) {
      try {
        dynamodb[0].deleteTable(config.AWS_BASE_TABLE_NAME);
        success = true;
      } catch (ResourceNotFoundException e) {
        isPresent = false;
      } catch (ResourceInUseException e) {
        success = false;
      }
    }
    while (isPresent) {
      try {
        ListTablesResult result = dynamodb[0].listTables();
        isPresent = result.getTableNames().contains(config.AWS_BASE_TABLE_NAME);
        System.out.println("IsPresent " + isPresent);
        Thread.sleep(5000);
      } catch (Exception e) {
        System.out.println(e.getMessage());
        isPresent = false;
      }
    }
  }

  private void createTableWithRetries(CreateTableRequest request, int retries) {
    for (int i = 1; i < retries; i++) {
      try {
        TableUtils.createTableIfNotExists(dynamodb[0], request);
        return;
      } catch (Exception e) {
        System.out.println(
            String.format("Failed to create table %s, try %d of %d : %s",
                request.getTableName(), i, retries, e.getMessage()));
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e1) {
          // ignore
        }
      }
    }
  }

  /**
   * Generates an Amazon read request
   */
  public GetItemRequest createGetItemRequest(Long key) {
    Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
    // Represents the key
    item.put(config.AWS_BASE_TABLE_HASH_KEY,
        new AttributeValue(key.toString()));
    return new GetItemRequest().withTableName(config.AWS_BASE_TABLE_NAME)
        .withKey(item);
  }

  public BatchGetItemRequest createBatchedGetItemRequest(List<Long> keys) {

    // This is the limit enforced by DynamoDB
    assert (keys.size() <= 100);
    HashMap<String, KeysAndAttributes> map = new HashMap<>();
    BatchGetItemRequest req = new BatchGetItemRequest();
    List<Map<String, AttributeValue>> items = new LinkedList<>();

    for (Long key : keys) {
      Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
      // Represents the key
      item.put(config.AWS_BASE_TABLE_HASH_KEY,
          new AttributeValue(key.toString()));
      items.add(item);
    }

    KeysAndAttributes attr = new KeysAndAttributes().withKeys(items);
    attr.withConsistentRead(true);
    req.addRequestItemsEntry(config.AWS_BASE_TABLE_NAME, attr);

    return req;
  }


  public BatchWriteItemRequest createBatchedWriteItemRequest(List<Write> writes) {
    System.out.println("Creating Batched Write Request");
    // This is the limit enforced by DynamoDB
    assert (writes.size() <= 25);
    BatchWriteItemRequest req = new BatchWriteItemRequest();
    List<WriteRequest> writesInBatch = new LinkedList<>();

    for (Write write : writes) {
      WriteRequest writeReq = new WriteRequest();
      if (write.isDelete()) {
        DeleteRequest deleteReq = new DeleteRequest();
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        // Represents the key
        item.put(config.AWS_BASE_TABLE_HASH_KEY,
            new AttributeValue(write.getKey().toString()));
        deleteReq.setKey(item);
        writeReq.setDeleteRequest(deleteReq);
      } else {
        PutRequest putReq = new PutRequest();
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        // Represents the key
        item.put(config.AWS_BASE_TABLE_HASH_KEY,
            new AttributeValue(write.getKey().toString()));
        // Represents the value
        AttributeValue val = new AttributeValue();
        val.setB(ByteBuffer.wrap(write.getValue()));
        item.put(config.AWS_BASE_TABLE_VALUE_KEY,
            // new AttributeValue(new String(value)));
            val);
        putReq.setItem(item);
        writeReq.setPutRequest(putReq);
      }
      writesInBatch.add(writeReq);
    }
    req.addRequestItemsEntry(config.AWS_BASE_TABLE_NAME, writesInBatch);
    return req;
  }

  /**
   * Generates an Amazon put request
   */
  public PutItemRequest createPutItemRequest(Long key, byte[] value) {
    Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
    // Represents the key
    item.put(config.AWS_BASE_TABLE_HASH_KEY,
        new AttributeValue(key.toString()));
    // Represents the value
    AttributeValue val = new AttributeValue();
    val.setB(ByteBuffer.wrap(value));
    item.put(config.AWS_BASE_TABLE_VALUE_KEY,
        // new AttributeValue(new String(value)));
        val);
    return new PutItemRequest().withTableName(config.AWS_BASE_TABLE_NAME)
        .withItem(item);
  }

  @Override
  public byte[] read(Long key) {

    byte[] value = null;
    Map<String, AttributeValue> item;
    ByteBuffer b;

    GetItemRequest req = createGetItemRequest(key);
    GetItemResult result = dynamodb[0].getItem(req);
    // TODO(natacha): find out what happens if key doesn't exist
    item = result.getItem();
    b = item == null ? null : item.get(config.AWS_BASE_TABLE_VALUE_KEY).getB();
    if (b != null) {
      value = b.array();
    } else {
      value = "".getBytes();
    }
    return value;
  }

  @Override
  // TODO(Natacha): might be better to use batching and/or
  // parallelism
  public List<byte[]> read(List<Long> keys) {

    System.out.println("Read Sizes " + keys.size());

    ConcurrentHashMap<Integer,LinkedList<byte[]>> values = new ConcurrentHashMap<>();
    LinkedList<Long> currentBatch = new LinkedList<>();
    LinkedList<byte[]> valuesToReturn = new LinkedList<>();

    int nbInBatch = 0;
    int batchId = 0;

    // TODO(natacha): cleanup hacky
    assert (keys.size() / config.AWS_BATCH_SIZE < config.AWS_DYN_CLIENT_NB);

    ArrayList<Pair<Integer,LinkedList<Long>>> batches = new ArrayList<>();

    for (Long key : keys) {

      currentBatch.add(key);
      nbInBatch++;

      if (nbInBatch == config.AWS_BATCH_SIZE) {
        batches.add(new Pair(batchId,currentBatch));
        batchId++;
        // Reset Batch
        nbInBatch = 0;
        currentBatch = new LinkedList<>();
      }
    }

    if (nbInBatch > 0) {
      batches.add(new Pair(batchId,currentBatch));
    }

    assert(batches.size() <= config.AWS_DYN_CLIENT_NB);
    AtomicInteger clientId = new AtomicInteger();
    batches.parallelStream().forEach(b -> doBatchRead(b.getRight(), b.getLeft(), clientId.getAndIncrement(), values));

    for (int i = 0 ; i < batchId ; i++ ) {
        valuesToReturn.addAll(values.get(i));
    }

    assert(valuesToReturn.size() == keys.size());

    return valuesToReturn;
  }

  void  doBatchRead(List<Long> currentBatch, int batchId, int index, ConcurrentHashMap<Integer,LinkedList<byte[]>> valueMap) {
    ByteBuffer b;
    byte[] value;
    System.out.println("Start batch Read " + index);
    LinkedList<byte[]> values = new LinkedList<>();
    BatchGetItemRequest req = createBatchedGetItemRequest(currentBatch);
    BatchGetItemResult result = dynamodb[index].batchGetItem(req);
    String currentKey;
    HashMap<String, byte[]> results = new HashMap<>();

    List<Map<String, AttributeValue>> replies = result.getResponses()
        .get(config.AWS_BASE_TABLE_NAME);

    assert(result.getUnprocessedKeys().size() == 0);

    for (Map<String, AttributeValue> rep : replies) {
      currentKey = rep.get(config.AWS_BASE_TABLE_HASH_KEY).getS();
      b = rep.get(config.AWS_BASE_TABLE_VALUE_KEY).getB();
      if (b != null) {
        value = b.array();
      } else {
        value =  null;
      }
      results.put(currentKey, value);
    }

    for (Long key: currentBatch) {
      value = results.get(key.toString());
      if (value == null) value = "".getBytes();
      values.add(value);
    }
    System.out.println("Finish batch Read " + index);
    valueMap.put(batchId, values);
  }

  public DeleteItemRequest createDeleteItemRequest(Long key) {
    Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
    // Represents the key
    item.put(config.AWS_BASE_TABLE_HASH_KEY,
        new AttributeValue(key.toString()));
    return new DeleteItemRequest().withTableName(config.AWS_BASE_TABLE_NAME)
        .withKey(item);
  }

  @Override
  public void write(Write write) {
    if (write.isDelete()) {
      DeleteItemRequest req = createDeleteItemRequest(write.getKey());
    } else {
      PutItemRequest req = createPutItemRequest(write.getKey(), write.getValue());
      PutItemResult res = dynamodb[0].putItem(req);
    }
  }

  @Override
  // TODO(Natacha): might be better to use batching and/or
  // parallelism
  public void write(Queue<Write> writes) {

    System.out.println("Write Sizes " + writes.size());

    LinkedList<Write> currentBatch = new LinkedList<>();
    int nbInBatch = 0;

    assert (writes.size() / config.AWS_BATCH_SIZE < config.AWS_DYN_CLIENT_NB);

    ArrayList<LinkedList<Write>> batches = new ArrayList<>();

    for (Write write : writes) {

      currentBatch.add(write);
      nbInBatch++;

      if (nbInBatch == config.AWS_BATCH_SIZE) {
        batches.add(currentBatch);
        // Reset Batch
        nbInBatch = 0;
        currentBatch = new LinkedList<>();
      }
    }

    if (nbInBatch > 0) {
      batches.add(currentBatch);
    }

    assert(batches.size() <= config.AWS_DYN_CLIENT_NB);
    AtomicInteger clientId = new AtomicInteger();
    batches.parallelStream().forEach( b -> doBatchWrite(b, clientId.getAndIncrement()));

  }

  private void doBatchWrite(List<Write> currentBatch, int index) {
    System.out.println("Start Batch Write " + index);
    boolean success = false;
    while (!success) {
      BatchWriteItemRequest req = createBatchedWriteItemRequest(currentBatch);
      BatchWriteItemResult result = dynamodb[index].batchWriteItem(req);
      success = result.getUnprocessedItems().size() == 0;
      if (result.getUnprocessedItems().size()>0) System.err.println("Retrying ");
    }
    System.out.println("End Batch Write " + index);
  }


  @Override
  public void onHandleRequestResponse(DataMessageResp msgResp) {
    throw new RuntimeException("Unimplemented");
  }

}
