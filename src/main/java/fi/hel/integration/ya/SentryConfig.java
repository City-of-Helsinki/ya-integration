package fi.hel.integration.ya;

import io.quarkus.runtime.Startup;
import io.sentry.DuplicateEventDetectionEventProcessor;
import io.sentry.EventProcessor;
import io.sentry.Sentry;
import io.sentry.SentryEvent;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class SentryConfig {

    @ConfigProperty(name = "SENTRY_DSN")
    String dsn;

    @Startup
    public void init() {
        Sentry.init(options -> {
            options.setDsn(dsn); 
            options.setDebug(true);
            options.setEnvironment("dev");
            options.setTracesSampleRate(1.0);  
            options.setSampleRate(1.0);  

            // Disable the DuplicateEventDetectionEventProcessor to allow each error (even if similar) 
            // to be reported as a separate event in Sentry.
            List<EventProcessor> processorsWithoutDuplicateDetection = options.getEventProcessors().stream()
                // Filter out the DuplicateEventDetectionEventProcessor, which is responsible for blocking duplicate events.
                .filter(p -> !(p instanceof io.sentry.DuplicateEventDetectionEventProcessor))
                // Collect the modified list, excluding the duplicate detection processor.
                .collect(Collectors.toList());

            // Clear the current event processors list in Sentry's options.
            // This prepares Sentry to accept a custom list that excludes duplicate detection.
            options.getEventProcessors().clear();
            // Add back only the filtered list of processors, now without the DuplicateEventDetectionEventProcessor.
            options.getEventProcessors().addAll(processorsWithoutDuplicateDetection);
            
        });

        if (Sentry.isEnabled()) {
            System.out.println("Sentry is initialized and ready.");
        } else {
            System.out.println("Sentry failed to initialize.");
        }
    }
}

