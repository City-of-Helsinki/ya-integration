package fi.hel.integration.ya;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.camel.Exchange;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@ApplicationScoped
@Named("xmlValidator")
public class XmlValidator {

     /**
     * Validates XML content against a specified XML schema file.
     * If the XML content is valid according to the schema, sets the header "isXmlValid" to {@code true} in the Exchange object.
     * If validation fails, sets headers related to the error details.
     *
     * @param ex the Exchange object containing the XML content to be validated
     * @param schemaFile the file path to the XML schema file used for validation
     * @throws SAXParseException if the XML content does not conform to the specified schema
     * @throws SAXException if an error occurs during XML validation
     * @throws IOException if an I/O error occurs while reading the XML content or schema file
     */
    public void validateXml(Exchange ex, String schemaFile) {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            
            String xmlContent = ex.getIn().getBody(String.class);

            Schema schema = schemaFactory.newSchema(new File(schemaFile));
            Validator validator = schema.newValidator();
            Source source = new StreamSource(new StringReader(xmlContent));
            validator.validate(source);
            ex.getIn().setHeader("isXmlValid", true);
        
        } catch  (SAXParseException e){
            e.printStackTrace();
            ex.getIn().setHeader("xml_error_message", e.getMessage());
            ex.getIn().setHeader("xml_error_line_number", e.getLineNumber());
            ex.getIn().setHeader("xml_error_column_number", e.getColumnNumber());
            ex.getIn().setHeader("isXmlValid", false);
        
        } catch (SAXException | IOException e) {
            e.printStackTrace();
            ex.getIn().setHeader("isXmlValid", false);
        }
    }
}
