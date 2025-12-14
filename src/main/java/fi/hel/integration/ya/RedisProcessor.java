package fi.hel.integration.ya;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.resps.ScanResult;

@ApplicationScoped
@Named("redisProcessor")
public class RedisProcessor {

    private JedisSentinelPool jedisPool;

    @ConfigProperty(name = "REDIS_PASSWORD")
    String password;

    @ConfigProperty(name = "REDIS_HOST")
    String host;

    @ConfigProperty(name = "REDIS_PORT")
    int port;

    @ConfigProperty(name = "REDIS_MASTER_NAME", defaultValue = "mymaster")
    String masterName;

    @PostConstruct
    public void initRedis() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(false);
        poolConfig.setMinEvictableIdleDuration(Duration.ofMinutes(1));
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        poolConfig.setNumTestsPerEvictionRun(3);

        Set<String> sentinels = new HashSet<>();
        sentinels.add(host + ":" + port);

        int connectionTimeout = 10000;
        int soTimeout = 10000;

        this.jedisPool = new JedisSentinelPool(
            masterName,
            sentinels,
            poolConfig,
            connectionTimeout,
            soTimeout,
            password,  // Redis password
            0,         // database
            null,      // clientName
            connectionTimeout,
            soTimeout,
            password,  // Sentinel password
            null       // Sentinel clientName
        );
    }

    public void set(String key, String value) throws Exception { 
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to execute Redis operation [set]", e);
        }
    }

    public void setVerkkolevyData(String key, String value) {

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        
        } catch (Exception e) {
            e.printStackTrace();
            Log.errorf(e, "Failed to execute Redis operation [set] for key: %s. Continuing without stopping.", key);

        }
    }

    public String get (String key) throws Exception {
        String value = null;
            
        try (Jedis jedis = jedisPool.getResource()) {
            value = jedis.get(key);
                
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to execute Redis operation [get]", e);  
        }

        return value;
    }

    public void delete(String key) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to execute Redis operation [delete]", e);
        }
    }

    public boolean acquireLock(String key, int ttlSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            long delay = Duration.ofMillis((long) (Math.random() * 5000)).toMillis();
            
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread was interrupted while acquiring lock", e);
            }
            
            SetParams params = new SetParams().nx().ex(ttlSeconds);
            String result = jedis.set(key, "locked", params);
    
            if ("OK".equals(result)) {
                System.out.println("Lock acquired by pod");
                return true; 
            } else {
                System.out.println("Lock not acquired by pod");
                return false; 
            }
    
        } catch (Exception e) {
            throw new RuntimeException("Failed to acquire lock from Redis", e);
        }
    }

    public void releaseLock(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to release lock from Redis", e);
        }
    }

    public List<String> getAllKeys(String pattern) {

        List<String> result = null;
        try (Jedis jedis = jedisPool.getResource()) {
                
            result = new ArrayList<String>(getAllKeys(pattern, ScanParams.SCAN_POINTER_START, jedis));
            
        } catch (Exception e)  {

            e.printStackTrace();
            throw new RuntimeException("Failed to execute Redis operation [getAllKeys]", e);
            
        }
        
        return result;
    }

    private Set<String> getAllKeys(String pattern, String cursor, Jedis jedis) {
        Set<String> keysSet = new HashSet<>();
        ScanParams scanParams = new ScanParams()
                .count(10000)
                .match(pattern);

        ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
        keysSet.addAll(scanResult.getResult());

        if (!ScanParams.SCAN_POINTER_START.equals(scanResult.getCursor())) {
            keysSet.addAll(getAllKeys(pattern, scanResult.getCursor(), jedis));
        }
        return keysSet;
    }

    public void combineData (Exchange ex) {
        try {

            List<String> keys = ex.getIn().getBody(List.class);
            List<Map<String,Object>> combinedData = new ArrayList<>();

            for(String key : keys) {

                String data = get(key);
                Map<String,Object> jsonData = convertJsonToMap(data);
                combinedData.add(jsonData);
            }

            ex.getIn().setBody(combinedData);
    
        } catch (Exception e) {
            e.printStackTrace();
            ex.setException(e);
        }
    }

    private Map<String,Object> convertJsonToMap(String jsonString) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(jsonString, Map.class);
        return map;
    }

    @PreDestroy
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}