package chronocache.db;

import chronocache.core.qry.QueryResult;
import com.google.gson.Gson;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Connection;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPooled;
import chronocache.util.Configuration;

import java.util.List;
import java.util.Map;

public class RedisClient implements QueryResultCache {
    private JedisPooled pooled;
    private Gson gson;

    public RedisClient() {
        GenericObjectPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(1000);
        jedisPoolConfig.setMaxIdle(1000);
        jedisPoolConfig.setMinIdle(10);
        this.pooled = new JedisPooled(jedisPoolConfig, Configuration.getRedisAddress(), Configuration.getRedisPort());
        this.gson = new Gson();
    }

    @Override
    public QueryResult get(String key) {
        String result = pooled.get(key);
        if (result == null) {
            return null;
        }
        List<Map<String, Object>> selectResult = gson.fromJson(result, List.class);
        return new QueryResult(selectResult, null);
    }

    @Override
    public void put(String key, QueryResult result) {
        String json = gson.toJson(result.getSelectResult());
        pooled.set(key, json);
    }
}
