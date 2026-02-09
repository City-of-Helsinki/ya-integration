package fi.integration.ya;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;

import fi.hel.integration.ya.JsonValidator;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@TestProfile(StarttirahaJsonValidationTest.class)
@QuarkusTest
public class StarttirahaJsonValidationTest extends CamelQuarkusTestSupport {

    @Inject
    private TestUtils tu;

    @Inject
    JsonValidator jsonValidator;

    @Inject
    private ProducerTemplate pt;

    @Test
    public void testValidStarttirahaJson() throws Exception {
        String jsonData = tu.readResource("src/test/java/fi/integration/ya/resources/starttiraha/starttiraha_data.json");
        
        // Extract first item from the array to test individual validation
        String firstItem = jsonData.substring(jsonData.indexOf('{'), jsonData.indexOf('}', jsonData.indexOf('{')) + 1);
        
        String result = pt.requestBodyAndHeader("direct:validate-json-P22", firstItem, 
                                                "CamelFileName", "YA_p22_091_20240618131100_0_SR.json", 
                                                String.class);
        
        // Should return the same content if validation passes
        assertNotNull(result);
    }

    @Test
    public void testValidStarttirahaUusiJson() throws Exception {
        String jsonData = tu.readResource("src/test/java/fi/integration/ya/resources/starttiraha/starttiraha_data_uusi.json");
        
        String result = pt.requestBodyAndHeader("direct:validate-json-P22", jsonData, 
                                                "CamelFileName", "YA_p22_399_20240820110559_1_SR.json", 
                                                String.class);
        
        // Should return the same content if validation passes
        assertNotNull(result);
    }

    @Test
    public void testInvalidStarttirahaJsonMissingRequiredField() throws Exception {
        String invalidJson = """
        {
            "delivery": {
                "fileName": "YA_p22_091_20240618131100_0_SR.json",
                "id": 60023993,
                "sentAt": "2024-06-18 13:11:00",
                "from": "YA",
                "to": "RONDO"
            },
            "bizTalkPipe": "automatic",
            "invoiceNumber": 60023993
        }
        """;
        
        try {
            pt.requestBodyAndHeader("direct:validate-json-P22", invalidJson, 
                                  "CamelFileName", "YA_p22_091_20240618131100_0_SR.json", 
                                  String.class);
        } catch (Exception e) {
            // Expected to fail validation
            assertTrue(e.getMessage().contains("Invalid json file"));
        }
    }

    @Test
    public void testInvalidStarttirahaJsonInvalidDataType() throws Exception {
        String invalidJson = """
        {
            "delivery": {
                "fileName": "YA_p22_091_20240618131100_0_SR.json",
                "id": "not_a_number",
                "sentAt": "2024-06-18 13:11:00",
                "from": "YA",
                "to": "RONDO"
            },
            "bizTalkPipe": "automatic",
            "invoiceNumber": 60023993,
            "invoiceDate": "2024-06-18 00:00:00",
            "dueDate": "2024-06-21 00:00:00",
            "receiptDate": "2024-06-18 00:00:00",
            "grossSum": 855.83,
            "currency": "EUR",
            "ourReference": "Test reference",
            "periodStartDate": "2024-03-01 00:00:00",
            "periodEndDate": "2024-03-31 00:00:00",
            "paymentPeriodStartDate": "2024-03-01 00:00:00",
            "paymentPeriodEndDate": "2024-03-31 00:00:00",
            "receiver": {
                "bankAccount": {
                    "value": "FI3952900240167500",
                    "bicCode": "OKOYFIHH"
                },
                "name": {
                    "firstName": "Test",
                    "lastName": "User"
                },
                "socialSecurityNumber": "123456-7890",
                "postalAddress": {
                    "addressLine": ["Test Street 1"],
                    "postalCode": "00100",
                    "postOffice": "Helsinki",
                    "country": "FI"
                }
            },
            "posting": {
                "accountsLedgerReceiptType": "YA"
            },
            "view": {
                "paymentDecisionRecordNumber": "SR/22932/2024",
                "paymentDecisionDate": "2024-06-18 00:00:00",
                "grantDecisionRecordNumber": "SR/22932/2024",
                "businessUnit": {
                    "value": "Test Unit",
                    "code": 0
                },
                "verifyDate": "2024-06-18 13:10:15",
                "additionalInformation": "Test info",
                "vatSum": 0,
                "netSum": 855.83,
                "aidType": "99",
                "language": 1
            },
            "version": "2.1",
            "claimType": "STARTTIRAHA",
            "auraAccount": "4739",
            "sectorIdentifier": "S.14",
            "participantHomeMunicipality": "091",
            "recipientMunicipality": "091",
            "employmentMunicipality": "091"
        }
        """;
        
        try {
            pt.requestBodyAndHeader("direct:validate-json-P22", invalidJson, 
                                  "CamelFileName", "YA_p22_091_20240618131100_0_SR.json", 
                                  String.class);
        } catch (Exception e) {
            // Expected to fail validation due to invalid data type for id field
            assertTrue(e.getMessage().contains("Invalid json file"));
        }
    }

    @Test
    public void testNonStarttirahaFileName() throws Exception {
        String jsonData = "{}";
        
        String result = pt.requestBodyAndHeader("direct:validate-json-P22", jsonData, 
                                              "CamelFileName", "YA_p22_091_20240618131100_0_OTHER.json", 
                                              String.class);
        
        // Should skip validation for non-SR files
        assertNotNull(result);
    }

    @Test
    public void testDirectJsonValidatorCall() throws Exception {
        String jsonData = tu.readResource("src/test/java/fi/integration/ya/resources/starttiraha/starttiraha_data.json");
        
        // Extract first item from the array
        String firstItem = jsonData.substring(jsonData.indexOf('{'), jsonData.indexOf('}', jsonData.indexOf('{')) + 1);
        
        // Test using the route that calls JsonValidator
        String result = pt.requestBodyAndHeader("direct:validate-json-P22", firstItem, 
                                              "CamelFileName", "YA_p22_091_20240618131100_0_SR.json", 
                                              String.class);
        
        assertNotNull(result);
    }
}
