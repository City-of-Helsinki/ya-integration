package fi.integration.ya;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        System.out.println("Result :: " + result);

        assertEquals(expectedResult, result);
        mock.assertIsSatisfied();
    }
}
