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
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.resps.ScanResult;

@ApplicationScoped
@Named("redisProcessor")
public class RedisProcessor {

    private JedisPool jedisPool;

    @ConfigProperty(name = "REDIS_PASSWORD")
    String password;

    @ConfigProperty(name = "REDIS_HOST")
    String host;

    @ConfigProperty(name= "REDIS_PORT")
    int port;

    @PostConstruct
    public void initRedis() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setTestOnBorrow(true); // Validates a connection before borrowing
        poolConfig.setTestWhileIdle(false); // Checks idle connections
        poolConfig.setMinEvictableIdleDuration(Duration.ofMinutes(1)); // 1 minute idle timeout
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30)); // Run evictor every 30 seconds
        poolConfig.setNumTestsPerEvictionRun(3); // Test 3 idle connections per eviction run

        int connectionTimeout = 10000; // 10 seconds for connection

        this.jedisPool = new JedisPool(poolConfig, host, port, connectionTimeout, password);
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
                Thread.currentThread().interrupt(); // Restore the interrupted status
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

    //
    // Returns the jedis keys with the given pattern.
    //
    //   Example: getAllKeys("myintegration:data:*")
    //
    public List<String> getAllKeys(String pattern) {

        List<String> result = null;
        try (Jedis jedis = jedisPool.getResource()) {
                
            //Start the scan process using START pointer (0)
            result = new ArrayList<String>(getAllKeys(pattern, ScanParams.SCAN_POINTER_START, jedis));
            
        } catch (Exception e)  {

            e.printStackTrace();
            throw new RuntimeException("Failed to execute Redis operation [getAllKeys]", e);
            
        }
        
        return result;
    }

    //
    // Recursive
    //
    private Set<String> getAllKeys(String pattern, String cursor, Jedis jedis) {
        Set<String> keysSet = new HashSet<>();
        // Scan params used to construct arguments to the scan command    
        ScanParams scanParams = new ScanParams()
                .count(10000)
                .match(pattern);

        // fetch the result (keys returned) from the scanResult and add it to the
        // list of existing keys    
        ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
        keysSet.addAll(scanResult.getResult());

        // If the cursor returned by the scan result is not START(0) then
        // recursively call the function with returned cursor and aggregate the results     
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

        // Convert JSON string to Map
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
              