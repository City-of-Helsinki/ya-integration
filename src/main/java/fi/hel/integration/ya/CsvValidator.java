package fi.hel.integration.ya;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import org.apache.camel.Exchange;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@ApplicationScoped
@Named("csvValidator")
public class CsvValidator {
    
     public void validateCsv(Exchange ex) {
        String csvContent = ex.getIn().getBody(String.class);
        Integer expectedColumnCount = ex.getIn().getHeader("columns", Integer.class);
        Integer expectedEmptyFieldsCount = ex.getIn().getHeader("emptyColumns", Integer.class);

        System.out.println("EMPTY COLUMNS :: " + expectedEmptyFieldsCount);

        CSVParser parser = new CSVParserBuilder().withSeparator(';').build();

        try (BufferedReader bufferedReader = new BufferedReader(new StringReader(csvContent));
                CSVReader reader = new CSVReaderBuilder(bufferedReader).withCSVParser(parser).build()) {

            String[] line;
            int lineNumber = 0;
            while ((line = reader.readNext()) != null) {
                lineNumber++;
                //System.out.println("Line " + lineNumber + " length: " + line.length); // Debugging statement

                //Check if the line has the correct number of columns
                if (line.length != expectedColumnCount) {
                    throw new IllegalArgumentException("Line " + lineNumber + " does not match expected column count.");
                    
                }

                if (expectedEmptyFieldsCount != null) {
                    int emptyFieldCount = (int) Arrays.stream(line).filter(String::isEmpty).count();
                    if (emptyFieldCount != expectedEmptyFieldsCount) {
                        throw new IllegalArgumentException("Line " + lineNumber + " does not have the expected number of empty fields. Found " + emptyFieldCount + " empty fields.");
                    }
                }
            }

            ex.getIn().setHeader("isCsvValid", true);
    
        } catch (IOException | CsvValidationException | IllegalArgumentException e) {
            e.printStackTrace();
            ex.getIn().setHeader("isCsvValid", false);
        }
    }
}
