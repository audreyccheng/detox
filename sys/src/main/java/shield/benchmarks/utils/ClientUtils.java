package shield.benchmarks.utils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.json.simple.parser.ParseException;
import shield.client.*;

/**
 * Wrapper class that switches between the different implementations of clients. Currently
 * supports Shield client and Dynamo client.
 */
public class ClientUtils {

  public enum ClientType {
    SHIELD,
    MYSQL,
    REDIS_POSTGRES,
    DUMMY,
    NONE
  }

  public static ClientType fromStringToClientType(String type) {
    ClientType ty = ClientType.SHIELD;
    if (type.equals("shield")) {
      ty = ClientType.SHIELD;
    } else if (type.equals("mysql")) {
      ty = ClientType.MYSQL;
    } else if (type.equals("dummy")) {
      ty = ClientType.DUMMY;
    } else if (type.equals("redis-postgres")) {
      ty = ClientType.REDIS_POSTGRES;
    }
    else {
      ty = ClientType.NONE;
    }
    return ty;
  }

  public static ClientBase createClient(ClientType ty, String expConfigFile)
      throws InterruptedException, ParseException, IOException, SQLException {
      ClientBase client = null;
      switch (ty) {
        case SHIELD: {
          client = new Client(expConfigFile);
          break;
        }
        case MYSQL: {
          client = new SQLClient(expConfigFile);
          break;
        }
        case DUMMY: {
          client = new DummyClient(expConfigFile);
          break;
        }
        case REDIS_POSTGRES: {
          client = new RedisPostgresClient(expConfigFile);
          break;
        }
        default:
          throw new RuntimeException("Incorrect Client Type");
      }
      return client;
  }

  public static ClientBase createClient(ClientType ty, String expConfigFile, Map<Long, ReadWriteLock> keyLocks, int port, int uid)
          throws InterruptedException, ParseException, IOException, SQLException {
    ClientBase client = null;
    switch (ty) {
      case SHIELD: {
        client = new Client(expConfigFile);
        break;
      }
      case MYSQL: {
        client = new SQLClient(expConfigFile);
        break;
      }
      case DUMMY: {
        client = new DummyClient(expConfigFile);
        break;
      }
      case REDIS_POSTGRES: {
        client = new RedisPostgresClient(expConfigFile, keyLocks, port, uid);
        break;
      }
      default:
        throw new RuntimeException("Incorrect Client Type");
    }
    return client;
  }

  public static ClientBase createClient(ClientType ty, String expConfigFile, int uid, int port)
      throws InterruptedException, ParseException, IOException, SQLException {
  ClientBase client = null;
      switch (ty) {
        case SHIELD: {
          client = new Client(expConfigFile, uid, port);
          break;
        }
        case MYSQL: {
          client = new SQLClient(expConfigFile);
          break;
        }
        case DUMMY: {
          client = new DummyClient(expConfigFile);
          break;
        }
        case REDIS_POSTGRES: {
          client = new RedisPostgresClient(expConfigFile);
          break;
        }
      }
      return client;
  }

  public  static ClientBase createClient(ClientType ty, String expConfigFile, String address, int uid, int port)
      throws InterruptedException, ParseException, IOException {
  ClientBase client = null;
      switch (ty) {
        case SHIELD: {
          client = new Client(expConfigFile, address, uid, port);
          break;
        }
        case DUMMY: {
          client = new DummyClient(expConfigFile);
          break;
        }
        case MYSQL:
        case REDIS_POSTGRES: {
          throw new RuntimeException("Not implemented");
        }
      }
      return client;
  }

}
