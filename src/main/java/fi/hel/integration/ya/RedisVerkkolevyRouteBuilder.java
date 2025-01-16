package fi.hel.integration.ya;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RedisVerkkolevyRouteBuilder extends RouteBuilder {

    @Inject
    RedisProcessor redisProcessor;

    private final String LOCK_KEY = "timer-route-lock";

    @Override
    public void configure() throws Exception {
        
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.

        from("{{VERKKOLEVY_QUARTZ_TIMER}}")
            .routeId("redis-verkkolevy")
            .autoStartup("{{VERKKOLEVY_AUTOSTARTUP}}")
            .log("Verkkolevy route started")
            .process(exchange -> {
                if (redisProcessor.acquireLock(LOCK_KEY, 300)) { 
                    exchange.getIn().setHeader("lockAcquired", true);
                    System.out.println("Lock acquired, processing starts");

                 } else {
                    exchange.getIn().setHeader("lockAcquired", false);
                    System.out.println("Lock not acquired, skipping processing");
                }
            })
            .filter(header("lockAcquired").isEqualTo(true))
                .log("Fetch data keys from Redis")
                .bean(redisProcessor, "getAllKeys(ready-to-send-verkkolevy:YA_p22_091*)")
                .log("Redis keys :: ${body}")
                .split(body())
                    .log("Fetching the data from redis with key ${body}")
                    .setHeader("redisKey").body()
                    .setHeader(Exchange.FILE_NAME)
                        .groovy("""
                            def body = exchange.in.body
                            def fileName = body.split(":")[1]  // Split the string at ":" and take the second part
                            return fileName
                        """)
                    .log("File name set in header: ${header.CamelFileName}")
                    .bean(redisProcessor, "get(${body})")
                    .log("Fetched data :: ${body}")
                    .to("file:outbox/verkkolevy")
                    //.to("direct:out-verkkolevy")
                    .setHeader("CamelFtpReplyString").simple("OK")
                    .choice()
                        .when(simple("${header.CamelFtpReplyString} == 'OK'"))
                            .log("deleting the redis key :: ${header.redisKey}")
                            .bean(redisProcessor, "delete(${header.redisKey})")
                        .otherwise()
                            .log("Error occurred when saving the data to verkkolevy")
        ;

        from("direct:out-verkkolevy")
            .log("send json via sftp to logs")
            //.to("file:outbox/logs")
            .to("sftp:{{VERKKOLEVY_SFTP_HOST}}:22/logs?username={{VERKKOLEVY_SFTP_USER}}&password={{VERKKOLEVY_SFTP_PASSWORD}}&throwExceptionOnConnectFailed=true&strictHostKeyChecking=no")
            .log("Verkkolevy SFTP response :: ${header.CamelFtpReplyCode}  ::  ${header.CamelFtpReplyString}")   
        ;

        from("{{REDIS_QUARTZ_TIMER}}")
            .routeId("redis")
            .autoStartup("{{REDIS_AUTOSTARTUP}}")
            .log("Verkkolevy route started")
            .process(exchange -> {
                if (redisProcessor.acquireLock(LOCK_KEY, 300)) { 
                    exchange.getIn().setHeader("lockAcquired", true);
                    System.out.println("Lock acquired, processing starts");

                 } else {
                    exchange.getIn().setHeader("lockAcquired", false);
                    System.out.println("Lock not acquired, skipping processing");
                }
            })
            .filter(header("lockAcquired").isEqualTo(true))
                .log("Fetch data keys from Redis")
                .bean(redisProcessor, "getAllKeys(ready-to-send-verkkolevy:YA_p24_091_20241031*)")
                .log("fetched Redis keys :: ${body}")
                .bean(redisProcessor, "combineData")
                .marshal(new JacksonDataFormat())
                //.log("Combined data :: ${body}")
                .to("direct:kirjanpito.controller")
                
            ;
    
    }
    
}
