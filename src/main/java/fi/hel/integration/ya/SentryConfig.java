package fi.hel.integration.ya;

import io.quarkus.runtime.Startup;
import io.sentry.Sentry;
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
        });

        if (Sentry.isEnabled()) {
            System.out.println("Sentry is initialized and ready.");
        } else {
            System.out.println("Sentry failed to initialize.");
        }
    }
}

