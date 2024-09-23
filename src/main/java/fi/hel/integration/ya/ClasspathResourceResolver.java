package fi.hel.integration.ya;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.InputStream;

public class ClasspathResourceResolver implements LSResourceResolver {
    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("schema/tulorekisteri/" + systemId);
        if (resourceAsStream == null) {
            throw new RuntimeException("Could not find the resource: " + systemId);
        }
        return new Input(publicId, systemId, resourceAsStream);
    }
}

