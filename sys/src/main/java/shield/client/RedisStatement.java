package shield.client;

import redis.clients.jedis.JedisPooled;

public class RedisStatement {

    public enum RedisRequestType {
        GET,
        PUT,
        DELETE
    }

    public RedisRequestType type;
    public String table;
    public Long id;
    public byte[] value;
    public int txn_type;
    public long txn_id;
    public boolean prefetch;

    public RedisStatement(RedisRequestType type, String table, Long id, byte[] value, int txn_type, long txn_id) {
        this(type, table, id, value, txn_type, txn_id, false);
    }

    public RedisStatement(RedisRequestType type, String table, Long id, byte[] value, int txn_type, long txn_id, boolean prefetch) {
        this.type = type;
        this.table = table;
        this.id = id;
        this.value = value;
        this.txn_type = txn_type;
        this.txn_id = txn_id;
        this.prefetch = prefetch;
    }

    public byte[] execute(JedisPooled jedis) {
        switch (this.type) {
            case GET:
                return jedis.get(Long.toString(id).getBytes());
            case PUT:
                jedis.setnx(Long.toString(id).getBytes(), this.value);
                break;
            case DELETE:
                jedis.del(Long.toString(id).getBytes());
                break;
        }
        return null;
    }
}
