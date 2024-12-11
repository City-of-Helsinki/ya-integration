package fi.hel.integration.ya.exceptions;

import io.sentry.SentryLevel;

public class JsonValidationException extends Exception{

    private final SentryLevel sentryLevel;
    private final String tag;

    public JsonValidationException(String message, SentryLevel sentryLevel, String tag) {
        super(message);
        this.sentryLevel = sentryLevel;
        this.tag = tag;
    }

    public SentryLevel getSentryLevel() {
        return sentryLevel;
    }
    
    public String getTag() {
            return tag;
    }   
}
