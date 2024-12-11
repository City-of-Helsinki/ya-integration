package fi.hel.integration.ya;

import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.support.service.ServiceSupport;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RedisLockRoutePolicy extends ServiceSupport implements RoutePolicy {

    private final RedisProcessor redisProcessor;
    private final String lockKey;
    private final long lockTimeout;

    public RedisLockRoutePolicy(RedisProcessor redisProcessor, String lockKey, long lockTimeout) {
        this.redisProcessor = redisProcessor;
        this.lockKey = lockKey;
        this.lockTimeout = lockTimeout;
    }

    @Override
    public void onStart(Route route) {
        if (!redisProcessor.acquireLock(lockKey, lockTimeout)) {
            System.out.println("Lock not acquired for route: " + route.getId() + ". Route will not start.");
            throw new IllegalStateException("Could not acquire lock for route: " + route.getId());

        } else {
            System.out.println("Lock acquired for route: " + route.getId() + ". Route will start processing." );
        }
    }

    @Override
    public void onStop(Route route) {
        redisProcessor.releaseLock(lockKey);
    }

    @Override
    public void onSuspend(Route route) {
        redisProcessor.releaseLock(lockKey);
    }

    @Override
    public void onResume(Route route) {
        if (!redisProcessor.acquireLock(lockKey, lockTimeout)) {
            System.out.println("Lock not acquired for route: " + route.getId() + ". Route will not start.");
            throw new IllegalStateException("Could not acquire lock for route: " + route.getId());

        } else {
            System.out.println("Lock acquired for route: " + route.getId() + ". Route will start processing." );
        }
    }

    @Override
    public void onRemove(Route route) {
        redisProcessor.releaseLock(lockKey);
    }

    @Override
    public void onInit(Route route) {
        // Optional: Add initialization logic for the route here.
        
    }

    @Override
    public void onExchangeBegin(Route route, Exchange exchange) {
        // Optional: This is triggered when an exchange begins.
        
    }

    @Override
    public void onExchangeDone(Route route, Exchange exchange) {
        // Optional: This is triggered when an exchange is completed.
        
    }
}
