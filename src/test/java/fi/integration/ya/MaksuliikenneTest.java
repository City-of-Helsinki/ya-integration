package fi.integration.ya;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;


import fi.hel.integration.ya.maksuliikenne.processor.MaksuliikenneProcessor;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@TestProfile(MaksuliikenneTest.class)
@QuarkusTest
public class MaksuliikenneTest extends CamelQuarkusTestSupport {

    @Inject
    MaksuliikenneProcessor mlProcessor;

    @Inject
    private ProducerTemplate pt;

    @Inject
    private fi.integration.ya.TestUtils tu;

   @ConfigProperty(name = "MAKSULIIKENNE_BANKING_FILENAMEPREFIX")
    private String FILE_NAME_PREFIX;

    @Test
    public void testMapPaymentTransactions() throws Exception {

        String startData = tu.readResource("src/test/java/fi/integration/ya/resources/maksuliikenne/maksuliikenne_data.json");
        String expectedResult = tu.readResource("src/test/java/fi/integration/ya/resources/maksuliikenne/maksuliikenne_expectedResult.xml");

        MockEndpoint mock = getMockEndpoint("mock:mapPaymentTransactions.result");

        mock.expectedMessageCount(1);

        String result = pt.requestBody("direct:mapPaymentTransactions", startData, String.class);
        result = result
                    .replaceAll("<MsgId>.*?</MsgId>", "<MsgId>REPLACED</MsgId>")
                    .replaceAll("<CreDtTm>.*?</CreDtTm>", "<CreDtTm>REPLACED</CreDtTm>")
                    .replaceAll("(?s)<DbtrAcct>\\s*<Id>\\s*<IBAN>.*?</IBAN>\\s*</Id>\\s*</DbtrAcct>",
                                    "<DbtrAcct><Id><IBAN>FI1234567891011121</IBAN></Id></DbtrAcct>")
                    .replaceAll("(?s)<Dbtr>\\s*<Nm>.*?</Nm>", "<Dbtr><Nm>MAKSAJA</Nm>")
                    .replaceAll("(?s)<InitgPty>\\s*<Nm>.*?</Nm>", "<InitgPty><Nm>NAME</Nm>")
                    .replaceAll("<ReqdExctnDt>.*?</ReqdExctnDt>", "<ReqdExctnDt>REPLACED</ReqdExctnDt>")
                    .replaceAll("<PmtInfId>.*?</PmtInfId>", "<PmtInfId>REPLACED</PmtInfId>")
                    ;

        assertEquals(expectedResult, result);
        mock.assertIsSatisfied();
    
    }

    @Test
    public void testFilterYaP24Files_WithMixedFiles() throws Exception {

        List<String> inputFiles = Arrays.asList(
            "YATE_tasmaytysraportti_p24-02012566_20250630.json",
            "YATE_tasmaytysraportti_p24-02012566_20250630.pdf", 
            "YA_p24_091_20250630200507_80_PT.json",
            "YA_p24_091_20250630200507_83_PT.json",
            "some_other_file.txt",
            "YA_p24_091_20250630200510_91_HKK.json"
        );

        @SuppressWarnings("unchecked")
        List<String> result = pt.requestBody("direct:filter-ya-p24-files", inputFiles, List.class);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("YA_p24_091_20250630200507_80_PT.json"));
        assertTrue(result.contains("YA_p24_091_20250630200507_83_PT.json"));
        assertTrue(result.contains("YA_p24_091_20250630200510_91_HKK.json"));
        assertFalse(result.contains("YATE_tasmaytysraportti_p24-02012566_20250630.json"));
        assertFalse(result.contains("some_other_file.txt"));
    }

    @Test
    public void testFilterYaP24Files_WithOnlyNonYaFiles() throws Exception {

        List<String> inputFiles = Arrays.asList(
            "YATE_tasmaytysraportti_p24-02012566_20250630.json",
            "YATE_tasmaytysraportti_p24-02012566_20250630.pdf",
            "some_other_file.txt"
        );

        @SuppressWarnings("unchecked")
        List<String> result = pt.requestBody("direct:filter-ya-p24-files", inputFiles, List.class);

        assertNotNull(result);
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilterYaP24Files_WithOnlyYaFiles() throws Exception {

        List<String> inputFiles = Arrays.asList(
            "YA_p24_091_20250630200507_80_PT.json",
            "YA_p24_091_20250630200507_83_PT.json",
            "YA_p24_091_20250630200510_91_HKK.json"
        );

        @SuppressWarnings("unchecked")
        List<String> result = pt.requestBody("direct:filter-ya-p24-files", inputFiles, List.class);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(inputFiles, result);
    }

    @Test
    public void testFilterYaP24Files_WithEmptyList() throws Exception {
  
        List<String> inputFiles = Collections.emptyList();


        @SuppressWarnings("unchecked")
        List<String> result = pt.requestBody("direct:filter-ya-p24-files", inputFiles, List.class);

        assertNotNull(result);
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilterYaP24Files_WithNullInput() throws Exception {

        @SuppressWarnings("unchecked")
        List<String> result = pt.requestBody("direct:filter-ya-p24-files", null, List.class);


        assertNotNull(result);
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
    }

}
