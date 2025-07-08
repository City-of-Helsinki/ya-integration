package fi.integration.ya;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;

import fi.hel.integration.ya.starttiraha.processor.StarttirahaProcessor;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@TestProfile(StarttirahaTest.class)
@QuarkusTest
public class StarttirahaTest extends CamelQuarkusTestSupport {

    @Inject
    private fi.integration.ya.TestUtils tu;

    @Inject
    StarttirahaProcessor srProcessor;

    @Inject
    private ProducerTemplate pt;

    @Test
    public void testProcessPersonalData()  throws Exception {
        assertTrue(context.getRoutesSize() > 0);
        
        MockEndpoint mock = getMockEndpoint("mock:processPersonalData.result");
        assertTrue(mock != null);
        mock.expectedMinimumMessageCount(1);
        
        String startData = tu.readResource("src/test/java/fi/integration/ya/resources/starttiraha/starttiraha_data.json");
        String expectedResult = tu.readResource("src/test/java/fi/integration/ya/resources/starttiraha/starttiraha_result_henkilotiedot.csv");

        String result = pt.requestBody("direct:processPersonalData", startData, String.class);
        result = result.replace("\uFEFF", "");

        assertEquals(expectedResult, result);
        mock.assertIsSatisfied();
    }

    @Test
    public void testProcessPayrollTransaction()  throws Exception {
        assertTrue(context.getRoutesSize() > 0);
        
        MockEndpoint mock = getMockEndpoint("mock:processPayrollTransaction.result");
        assertTrue(mock != null);
        mock.expectedMinimumMessageCount(1);
        
        String startData = tu.readResource("src/test/java/fi/integration/ya/resources/starttiraha/starttiraha_data.json");
        String expectedResult = tu.readResource("src/test/java/fi/integration/ya/resources/starttiraha/starttiraha_result_palkkatapahtumat.csv");

        String result = pt.requestBody("direct:processPayrollTransaction", startData, String.class);
        result = result.replace("\uFEFF", "");
        System.out.println("Result :: " + result);

        assertEquals(expectedResult, result);
        mock.assertIsSatisfied();
    }

    @Test
    public void testFilterYaP22Files_WithMixedFiles() throws Exception {

        List<String> inputFiles = Arrays.asList(
            "YATE_tasmaytysraportti_p22-02012566_20250630.json",
            "YATE_tasmaytysraportti_p22-02012566_20250630.pdf", 
            "YA_p22_091_20250630200507_80_SR.json",
            "YA_p22_091_20250630200507_83_SR.json",
            "some_other_file.txt",
            "YA_p22_091_20250630200510_91_SR.json"
        );

        @SuppressWarnings("unchecked")
        List<String> result = pt.requestBody("direct:filter-ya-p22-files", inputFiles, List.class);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("YA_p22_091_20250630200507_80_SR.json"));
        assertTrue(result.contains("YA_p22_091_20250630200507_83_SR.json"));
        assertTrue(result.contains("YA_p22_091_20250630200510_91_SR.json"));
        assertFalse(result.contains("YATE_tasmaytysraportti_p22-02012566_20250630.json"));
        assertFalse(result.contains("some_other_file.txt"));
    }

    @Test
    public void testFilterYaP22Files_WithOnlyNonYaFiles() throws Exception {

        List<String> inputFiles = Arrays.asList(
            "YATE_tasmaytysraportti_p22-02012566_20250630.json",
            "YATE_tasmaytysraportti_p22-02012566_20250630.pdf",
            "some_other_file.txt"
        );

        @SuppressWarnings("unchecked")
        List<String> result = pt.requestBody("direct:filter-ya-p22-files", inputFiles, List.class);

        assertNotNull(result);
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilterYaP22Files_WithOnlyYaFiles() throws Exception {

        List<String> inputFiles = Arrays.asList(
            "YA_p22_091_20250630200507_80_SR.json",
            "YA_p22_091_20250630200507_83_SR.json",
            "YA_p22_091_20250630200510_91_SR.json"
        );

        @SuppressWarnings("unchecked")
        List<String> result = pt.requestBody("direct:filter-ya-p22-files", inputFiles, List.class);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(inputFiles, result);
    }

    @Test
    public void testFilterYaP22Files_WithEmptyList() throws Exception {
  
        List<String> inputFiles = Collections.emptyList();


        @SuppressWarnings("unchecked")
        List<String> result = pt.requestBody("direct:filter-ya-p22-files", inputFiles, List.class);

        assertNotNull(result);
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilterYaP22Files_WithNullInput() throws Exception {

        @SuppressWarnings("unchecked")
        List<String> result = pt.requestBody("direct:filter-ya-p22-files", null, List.class);


        assertNotNull(result);
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
    }
}
