package fi.hel.integration.ya;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.transform.Source;

import org.apache.camel.Exchange;
import org.jboss.logging.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named ("utils")
public class Utils {

    @Inject
    Logger log;

    /**
     * Retrieves the current date and time formatted according to the specified pattern
     *
     * @param pattern the pattern string used to format the date and time, following the conventions of {@link DateTimeFormatter}
     * @return a {@code String} representing the current date and time formatted as per the provided pattern
     */
    public String getCurrentTime(String pattern) {

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        String formattedDateTime = now.format(formatter);

        return formattedDateTime;
    }

    
    /**
     * Converts a date string from one format to another.
     *
     * @param date the date string to be converted
     * @param originalFormat the format of the original date string, following the conventions of {@link SimpleDateFormat}
     * @param desiredFormat the desired format for the converted date string, following the conventions of {@link SimpleDateFormat}
     * @return a {@code String} representation of the date converted to the desired format, or {@code null} if parsing fails
     */
    public String convertDate (String date, String originalFormat, String desiredFormat) {
        
        SimpleDateFormat originalFormatDate = new SimpleDateFormat(originalFormat);
        SimpleDateFormat desiredFormatDate = new SimpleDateFormat(desiredFormat);
        
        try {
            Date parsedDate = originalFormatDate.parse(date);
            date = desiredFormatDate.format(parsedDate);
            return date;
            
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    

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
