package fi.integration.ya;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TestUtils {

    @Inject
    CamelContext camelContext;

    //
    // Example usage:
    // @param resourcePathAndFile e.g. "src/test/resources/myfile.json"
    //
	public String readResource(String resourcePathAndFile) throws FileNotFoundException, IOException {
		var fileReader = new FileReader(resourcePathAndFile);
		StringBuilder sb = new StringBuilder();
		int byteInt = 0;
		while (byteInt != -1) {
			byteInt = fileReader.read();
			if (byteInt != -1) {
				sb.append((char) byteInt);
			}
		};
		return sb.toString();
	}

    public String readFile(String fileName) throws URISyntaxException, IOException {
        java.net.URI fileUri = this.getClass().getResource(fileName).toURI();
        String result = String.join("\n", Files.readAllLines(
                Paths.get(fileUri), Charset.defaultCharset()));
        return result;
    }

    

    public void resetAllMocks() {
        MockEndpoint.resetMocks(camelContext);
    }

    public Exchange createExchange() {
        CamelContext camelContext = new DefaultCamelContext();
        Exchange exchange = new ExchangeBuilder(camelContext).build();

        return exchange;
    }

    public Exchange createExchange(Object body) {
        CamelContext camelContext = new DefaultCamelContext();
        Exchange exchange = new ExchangeBuilder(camelContext).withBody(body).build();

        return exchange;
    }

    /** 
     * Retuns current data as String, formatted as "yyyy-MM-dd HH:mm:ss"
     * 
     */
    public String createDatetimeNow() {
        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"));
        String pattern = "yyyy-MM-dd HH:mm:ss";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        String formattedDateTime = currentDateTime.format(formatter);

        return formattedDateTime;
    }

    /** 
     * Retuns current date as String, formatted by param "pattern"
     * 
     */
    public String createDatetimeNow(String pattern) {
        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        String formattedDateTime = currentDateTime.format(formatter);

        return formattedDateTime;
    }

    public String createDatePlusDays(String date, Integer daysToAdd) {
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate resultDate = parsedDate.plusDays(daysToAdd);
        String formattedResult = resultDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return formattedResult;
    }

    public String createDatePlusDays(String date, Integer daysToAdd, String pattern) {
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern(pattern));
        LocalDate resultDate = parsedDate.plusDays(daysToAdd);
        String formattedResult = resultDate.format(DateTimeFormatter.ofPattern(pattern));

        return formattedResult;
    }

    public String createDateMinusDays(String date, Integer daysToAdd) {
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate resultDate = parsedDate.minusDays(daysToAdd);
        String formattedResult = resultDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return formattedResult;
    }

    public String createDateMinusDays(String date, Integer daysToAdd, String pattern) {
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern(pattern));
        LocalDate resultDate = parsedDate.minusDays(daysToAdd);
        String formattedResult = resultDate.format(DateTimeFormatter.ofPattern(pattern));

        return formattedResult;
    }

    public void addMockEndpointTo(String routeId, String mockEndpoint) throws Exception {
        addMockEndpointTo(routeId, mockEndpoint, false);
    }

    public void addMockEndpointTo(String routeId, String mockEndpoint, Boolean logXML) throws Exception {
        AdviceWith.adviceWith(camelContext, routeId, logXML,
                builder -> builder
                        .weaveAddLast()
                        .to(mockEndpoint)
                        .id("mockEndpoint"));
    }

    public void addMockEndpointsTo(String mockEndpoint, String... routeIds) throws Exception {
        addMockEndpointsTo(mockEndpoint, false, routeIds);
    }

    public void addMockEndpointsTo(String mockEndpoint, Boolean logXML, String... routeIds) throws Exception {
        for (String routeId : routeIds) {
            this.addMockEndpointTo(routeId, mockEndpoint, logXML);
        }
    }

    public void addFakeExceptionHandler(String mockEndpoint) throws Exception {
        RouteConfigurationBuilder fakeExceptionHandler = new RouteConfigurationBuilder() {
            @Override
            public void configuration() {
                routeConfiguration()
                        .onException(Exception.class)
                        .log("fake exception handler")
                        .log("message: ${exception.message}")
                        .handled(true)
                        .to(mockEndpoint);
            }
        };

        camelContext.addRoutes(fakeExceptionHandler);
    }

    public void addFakeExceptionHandler(String mockEndpoint, String routeConfigurationId) throws Exception {
        RouteConfigurationBuilder fakeExceptionHandler = new RouteConfigurationBuilder() {
            @Override
            public void configuration() {
                routeConfiguration(routeConfigurationId)
                        .onException(Exception.class)
                        .log("fake exception handler, configurationId: " + routeConfigurationId)
                        .log("message: ${exception.message}")
                        .handled(true)
                        .to(mockEndpoint);
            }
        };

        camelContext.addRoutes(fakeExceptionHandler);
    }

    public String minifyJson(String prettyPrintJson) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(prettyPrintJson);
        String minifiedJson = objectMapper.writeValueAsString(jsonNode);

        return minifiedJson;
    }

    public String writeAsJson(Object obj) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(obj);
    }

    public String writeAsJson(Object obj, Boolean prettyPrint) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        if (prettyPrint) {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        }

        return objectMapper.writeValueAsString(obj);
    }

    public Map<String,Object>  convertJsonStringToMap(String data) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        // Convert JSON string to Map
        Map<String,Object> map = objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {});
        return map;
    }
}

