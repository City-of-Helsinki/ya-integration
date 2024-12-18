package fi.hel.integration.ya;

import java.time.Duration;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;

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

    public boolean set(String key, String value, int ttlSeconds) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            //System.out.println("Redis key :: " + jedis.get(key));
            SetParams params = new SetParams().nx().ex(ttlSeconds);
            String result = jedis.set(key, value, params); // Attempt to acquire the lock
            System.out.println("Redis set result for key '" + key + "': " + result);
            return "OK".equals(result); // Return true if the lock was acquired
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to execute Redis operation [set]", e);
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
                String result = jedis.get(key);
            if (result == null) {
                SetParams params = new SetParams().nx().ex(ttlSeconds);
                jedis.set(key, "locked", params);
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

    @PreDestroy
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}
              