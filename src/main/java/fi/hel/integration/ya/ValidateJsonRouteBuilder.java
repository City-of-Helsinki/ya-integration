package fi.hel.integration.ya;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;

import fi.hel.integration.ya.exceptions.JsonValidationException;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ValidateJsonRouteBuilder extends RouteBuilder {

    @Inject
    RedisProcessor redisProcessor;

    private final String JSON_ERROR_EMAIL_RECIPIENTS = "{{JSON_ERROR_EMAIL_RECIPIENTS}}";

    @Override
    public void configure() throws Exception {

         // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.

        onException(JsonValidationException.class)
            .handled(true)
            .process(exchange -> {
                JsonValidationException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, JsonValidationException.class);

                Sentry.withScope(scope -> {
                    String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                    String uniqueId = UUID.randomUUID().toString(); // Generate a unique ID for the error

                    scope.setLevel(cause.getSentryLevel());
                    scope.setTag("error.type", cause.getTag());
                    scope.setTag("context.fileName", fileName);
                    scope.setFingerprint(Arrays.asList(uniqueId)); 
                    Sentry.captureException(cause);
                });

                Sentry.flush(2000);

            })
            .log("JsonValidationException occurred: ${exception.message}")
        ;

        from("direct:validate-json-file")
            .log("Processing file: ${header.CamelFileName}")
            .marshal(new JacksonDataFormat())
            .setVariable("originalFileName", simple("${header.CamelFileName}"))
            .process(exchange -> {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                String redisKey = "ready-to-send-verkkolevy:" + fileName;
            
                String fileContent = exchange.getIn().getBody(String.class);
            
                System.out.println("Setting the redis key :: " + redisKey);
                redisProcessor.setVerkkolevyData(redisKey, fileContent);
                        
            })
            .toD("direct:validate-json-${header.kipa_container}")
            .choice()
                .when(simple("${header.isJsonValid} == 'true'"))
                    .log("Json is valid continue processing ${header.CamelFileName}")
                    .unmarshal(new JacksonDataFormat())
                    .process(exchange -> {
                        Map<String, Object> fileContent = exchange.getIn().getBody(Map.class);
                        exchange.getIn().setBody(Map.of("isJsonValid", true, "fileContent", fileContent));
                    })
                    
                
                .otherwise()
                    .log("Json is not valid, ${header.CamelFileName}")
                    .log("Error message :: ${variable.error_messages}")
                    .setHeader("messageSubject", simple("Ya-integraatio, kipa: virhe json-sanomassa, ${header.kipa_container}"))
                    .setHeader("emailRecipients", constant(JSON_ERROR_EMAIL_RECIPIENTS))
                    .to("direct:sendErrorReport")
                    .doTry()
                        .process(exchange -> {
                            String errorMessage = exchange.getVariable("error_messages", String.class);
                            throw new JsonValidationException(
                                "Invalid json file. Error messages: " + errorMessage,
                                SentryLevel.ERROR,
                            "jsonValidationError"
                            );
                        })
                    .doCatch(JsonValidationException.class)
                        .log("Caught JsonValidationException: ${exception.message}")
                        .process(exchange -> {
                            JsonValidationException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, JsonValidationException.class);
            
                            Sentry.withScope(scope -> {
                                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                                String uniqueId = UUID.randomUUID().toString(); // Generate a unique ID for the error
            
                                scope.setLevel(cause.getSentryLevel());
                                scope.setTag("error.type", cause.getTag());
                                scope.setTag("context.fileName", fileName);
                                scope.setFingerprint(Arrays.asList(uniqueId));
                                Sentry.captureException(cause);
                            });
            
                            Sentry.flush(2000);
                        })
                        
                        .process(exchange -> {
                            String errorMessage = exchange.getVariable("error_messages", String.class);
                            exchange.getIn().setBody(Map.of(
                            "isJsonValid", false,
                            "errorMessage", errorMessage
                            ));
                        })
                        
            .end()
        ;
    }
}
