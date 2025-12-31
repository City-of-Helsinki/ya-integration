package fi.hel.integration.ya;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Properties;
import java.util.Set;

import org.jboss.logging.Logger;

import de.focus_shift.jollyday.core.Holiday;
import de.focus_shift.jollyday.core.HolidayManager;
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
        System.out.println("DEBUG: ZonedDateTime.now() = " + now);
        System.out.println("DEBUG: System timezone = " + ZoneId.systemDefault());
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

    public String xmlEscape(String value) {
        String result = value.replaceAll("&", "&amp;");
        result = result.replaceAll("<", "&lt;");
        result = result.replaceAll(">", "&gt;");
        result = result.replaceAll("\"", "&quot;");
        result = result.replaceAll("'", "&apos;");
        return result;
    }

    /**
     * Checks if the given date is a public holiday in Finland.
     *
     * @param date The date to check.
     * @return True if the date is a public holiday, false otherwise.
     */
    public boolean isFinnishPublicHoliday(LocalDate date) {

        try {

            System.out.println("Setting the properties");
            Properties properties = new Properties();
            properties.setProperty("manager.country", "fi");

            HolidayManager holidayManager = HolidayManager.getInstance(properties);

            if (holidayManager == null) {
                throw new RuntimeException("Failed to initialize HolidayManager");
            }

            Year year = Year.of(date.getYear());

            Set<Holiday> holidays = holidayManager.getHolidays(year, "");
            
            System.out.println("Holidays, year:" + year + " :: " + holidays);

            return holidays.stream()
                    .anyMatch(holiday -> holiday.getDate().equals(date));
        
        } catch (Exception e) {

            System.err.println("Error checking Finnish public holiday: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
