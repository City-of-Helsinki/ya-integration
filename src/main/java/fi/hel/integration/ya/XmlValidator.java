package fi.hel.integration.ya;

import java.util.ArrayList;
import java.util.List;
import java.io.*;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.camel.Exchange;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;

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
    public void validateXml(Exchange ex, String schemaFilePath) {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        // Set custom resource resolver for resolving schemas from the classpath
        schemaFactory.setResourceResolver(new ClasspathResourceResolver());

        try {
            
            String xmlContent = ex.getIn().getBody(String.class);

            Schema schema = loadSchemaFromClasspath(schemaFilePath, schemaFactory);
            //Schema schema = schemaFactory.newSchema(new File(schemaFilePath));
            Validator validator = schema.newValidator();

            // Create a custom error handler to collect errors
            List<String> errorMessages = new ArrayList<>();
            List<Integer> errorLineNumbers = new ArrayList<>();
            List<Integer> errorColumnNumbers = new ArrayList<>();

            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) throws SAXException {
                    // Collect warnings if needed (optional)
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    // Collect recoverable errors
                    errorMessages.add(exception.getMessage());
                    errorLineNumbers.add(exception.getLineNumber());
                    errorColumnNumbers.add(exception.getColumnNumber());
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    // Collect fatal errors
                    errorMessages.add(exception.getMessage());
                    errorLineNumbers.add(exception.getLineNumber());
                    errorColumnNumbers.add(exception.getColumnNumber());
                }
            });

            
            Source source = new StreamSource(new StringReader(xmlContent));
            validator.validate(source);
            // ex.getIn().setHeader("isXmlValid", true);
            // Check if there were any errors
            if (!errorMessages.isEmpty()) {
                ex.getIn().setHeader("isXmlValid", false);
                ex.getIn().setHeader("xml_error_messages", String.join(", ", errorMessages));
                ex.getIn().setHeader("xml_error_line_numbers", errorLineNumbers.toString());
                ex.getIn().setHeader("xml_error_column_numbers", errorColumnNumbers.toString());
            } else {
                ex.getIn().setHeader("isXmlValid", true);
            }
        /* } catch  (SAXParseException e){
            e.printStackTrace();
            ex.getIn().setHeader("xml_error_message", e.getMessage());
            ex.getIn().setHeader("xml_error_line_number", e.getLineNumber());
            ex.getIn().setHeader("xml_error_column_number", e.getColumnNumber());
            ex.getIn().setHeader("isXmlValid", false); */
        
        } catch (SAXException | IOException e) {
            e.printStackTrace();
            ex.getIn().setHeader("isXmlValid", false);
        }
    }

    private Schema loadSchemaFromClasspath(String schemaFilePath, SchemaFactory schemaFactory) throws SAXException, IOException {
        // Load the schema file from the classpath
        InputStream schemaInputStream = getClass().getClassLoader().getResourceAsStream(schemaFilePath);
        if (schemaInputStream == null) {
            throw new FileNotFoundException("Schema file not found in classpath: " + schemaFilePath);
        }
    
        // Use the input stream to load the schema
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(schemaInputStream)) {
            return schemaFactory.newSchema(new StreamSource(bufferedInputStream));
        }
    }
}


