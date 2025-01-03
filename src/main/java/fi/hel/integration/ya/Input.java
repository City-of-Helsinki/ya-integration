package fi.hel.integration.ya;

import org.w3c.dom.ls.LSInput;

import java.io.InputStream;
import java.io.Reader;

public class Input implements LSInput {
    private String publicId;
    private String systemId;
    private InputStream inputStream;

    public Input(String publicId, String systemId, InputStream inputStream) {
        this.publicId = publicId;
        this.systemId = systemId;
        this.inputStream = inputStream;
    }

    @Override
    public Reader getCharacterStream() {
        return null;
    }

    @Override
    public void setCharacterStream(Reader characterStream) {
    }

    @Override
    public InputStream getByteStream() {
        return inputStream;
    }

    @Override
    public void setByteStream(InputStream byteStream) {
        this.inputStream = byteStream;
    }

    @Override
    public String getStringData() {
        return null;
    }

    @Override
    public void setStringData(String stringData) {
    }

    @Override
    public String getSystemId() {
        return systemId;
    }

    @Override
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    @Override
    public String getPublicId() {
        return publicId;
    }

    @Override
    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    @Override
    public String getBaseURI() {
        return null;
    }

    @Override
    public void setBaseURI(String baseURI) {
    }

    @Override
    public String getEncoding() {
        return null;
    }

    @Override
    public void setEncoding(String encoding) {
    }

    @Override
    public boolean getCertifiedText() {
        return false;
    }

    @Override
    public void setCertifiedText(boolean certifiedText) {
    }
}

