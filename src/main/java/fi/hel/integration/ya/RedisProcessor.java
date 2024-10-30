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

    @PreDestroy
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}
              