package fi.hel.integration.ya;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.camel.Exchange;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@ApplicationScoped
@Named("jsonValidator")
public class JsonValidator {
    
     public void validateJson(Exchange ex, String schemaFile) throws FileNotFoundException {
    
        try {
            String json = ex.getIn().getBody(String.class);
            //System.out.println("json string :: " + json);
            JSONObject jsonData = new JSONObject(new JSONTokener(json));
            //System.out.println("jsonData :: " + jsonData );
            //System.out.println("Schemafile :: " + schemaFile);
            Schema schema = loadJsonSchema(schemaFile);
            System.out.println("Schema :: " + schema);
            schema.validate(jsonData);
            ex.getIn().setHeader("isJsonValid", true);

        } catch (IOException e  ) {
            e.printStackTrace();
            ex.getIn().setHeader("isJsonValid", false);

        } catch (JSONException e) {
            // Catch JSON parsing errors
            e.printStackTrace();
            System.out.println("Invalid JSON format: " + e.getMessage());
            ex.getIn().setHeader("isJsonValid", false);

        } catch (ValidationException e) {
            e.printStackTrace();
            logValidationExceptions(e);
            ex.getIn().setHeader("isJsonValid", false);
        }

    }

    private Schema loadJsonSchema(String schemaFilename) throws FileNotFoundException, IOException {
        String schemaString = readResource(schemaFilename);
        //System.out.println("schemaString :: " + schemaString);
        JSONObject rawSchema = new JSONObject(new JSONTokener(schemaString));
        Schema schema = SchemaLoader.load(rawSchema);
        return schema;
    }

    public String readResource(String resourcePathAndFile) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePathAndFile);
        if (inputStream == null) {
            throw new FileNotFoundException("Resource not found: " + resourcePathAndFile);
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            return sb.toString();
        }
    }


    private void logValidationExceptions(ValidationException e) {
        System.out.println("Validation failed with " + e.getViolationCount() + " violations:");
        e.getCausingExceptions().stream()
            .map(ValidationException::getMessage)
            .forEach(System.out::println);
        System.out.println("Root exception message: " + e.getMessage());
    }
}
