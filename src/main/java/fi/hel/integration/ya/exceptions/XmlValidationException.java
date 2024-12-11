package fi.hel.integration.ya.exceptions;

import io.sentry.SentryLevel;

public class XmlValidationException extends Exception {

    private final SentryLevel sentryLevel;
    private final String tag;

    public XmlValidationException(String message, SentryLevel sentryLevel, String tag) {
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
    