package fi.hel.integration.ya;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named ("validateJsonProcessor")
public class ValidateJsonProcessor {

    @Inject
    ProducerTemplate producerTemplate;
    
    public void validateFiles(Exchange ex) {

        List<Map<String, Object>> files = ex.getIn().getBody(List.class);
        List<Map<String, Object>> validFiles = new ArrayList<>();
        List<Map<String, Object>> invalidFiles = new ArrayList<>();
        Map<String, Object> headers = ex.getIn().getHeaders();

        for (Map<String, Object> file : files) {
            
            Map<String,Object> delivery = (Map<String, Object>) file.get("delivery");
            String fileName = (String) delivery.get("fileName");
            System.out.println("FILE NAME :: " + fileName);
            headers.put("CamelFileName", fileName);
            Map<String, Object> result = producerTemplate.requestBodyAndHeaders("direct:validate-json-file", file, headers, Map.class);

            Boolean isJsonValid = (Boolean) result.get("isJsonValid");
            if (isJsonValid != null && isJsonValid) {
                validFiles.add(file);
            } else {
                String errorMessage = (String) result.get("errorMessage");
                System.out.println("Invalid JSON: " + errorMessage);
                invalidFiles.add(file);
            }
        }

        //System.out.println(("combined jsons :: " + combinedJsons));
        ex.setVariable("validFiles", validFiles);
        ex.setVariable("invalidFiles", invalidFiles);
    }
}
