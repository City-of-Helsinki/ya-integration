package fi.hel.integration.ya;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.jboss.logging.Logger;

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

}
