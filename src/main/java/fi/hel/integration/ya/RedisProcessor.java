package fi.hel.integration.ya;

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
        this.jedisPool = new JedisPool(poolConfig, host, port, 10000, password);
    }

    public void set(String key, String value) throws Exception {

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to execute Redis operation", e);
        }
    }

    public String get (String key) throws Exception {
        String value = null;
            
        try (Jedis jedis = jedisPool.getResource()) {
            value = jedis.get(key);
                
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to execute Redis operation", e);  
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
              