package fi.integration.ya;

import fi.hel.integration.ya.maksuliikenne.Tasmaytysraportti.TasmaytysraporttiProcessor;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TasmaytysraporttiTest {

    @Inject
    CamelContext camelContext;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    TasmaytysraporttiProcessor processor;

    @Test
    public void testJsonToCsvConversion() throws Exception {
        String jsonContent = """
            [
                {
                    "participantMunicipalityCode": "091",
                    "businessUnit": "HELSINGIN KAUPUNKI",
                    "recordNumber": "PT/244216/2025",
                    "resolvedAt": "2025-06-30 07:53:46",
                    "paymentState": "PAID",
                    "auraAccount": "4746",
                    "invoiceNumber": 60523922,
                    "paymentPeriodStartDate": "2025-05-01 00:00:00",
                    "paymentPeriodEndDate": "2025-05-31 00:00:00",
                    "paymentAmount": {
                        "grossSum": 1057.1,
                        "netSum": 1057.1
                    },
                    "presenterName": "Titta Björkman",
                    "receiver": {
                        "name": "Helsingin kaupunki",
                        "businessId": "0201256-6"
                    },
                    "claimType": "PALKKATUKI"
                }
            ]
            """;

        Exchange exchange = ExchangeBuilder.anExchange(camelContext)
                .withBody(jsonContent)
                .withHeader(Exchange.FILE_NAME, "test.json")
                .build();

        processor.process(exchange);

        String result = exchange.getIn().getBody(String.class);
        assertNotNull(result);
        assertTrue(result.contains("participantMunicipalityCode"));
        assertTrue(result.contains("091"));
        assertTrue(result.contains("HELSINGIN KAUPUNKI"));
        assertTrue(result.contains("1057.1"));
        
        String fileName = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
        assertEquals("test.csv", fileName);
    }

    @Test
    public void testNestedJsonFields() throws Exception {
        String jsonContent = """
            [
                {
                    "recordNumber": "PT/244216/2025",
                    "paymentAmount": {
                        "grossSum": 1057.1,
                        "netSum": 1057.1
                    },
                    "receiver": {
                        "name": "Helsingin kaupunki",
                        "businessId": "0201256-6"
                    }
                }
            ]
            """;

        Exchange exchange = ExchangeBuilder.anExchange(camelContext)
                .withBody(jsonContent)
                .withHeader(Exchange.FILE_NAME, "nested_test.json")
                .build();

        processor.process(exchange);

        String result = exchange.getIn().getBody(String.class);
        assertNotNull(result);
        assertTrue(result.contains("paymentAmount.grossSum"));
        assertTrue(result.contains("paymentAmount.netSum"));
        assertTrue(result.contains("receiver.name"));
        assertTrue(result.contains("receiver.businessId"));
        assertTrue(result.contains("1057.1"));
        assertTrue(result.contains("Helsingin kaupunki"));
    }

    @Test
    public void testEmptyJsonArray() throws Exception {
        String jsonContent = "[]";

        Exchange exchange = ExchangeBuilder.anExchange(camelContext)
                .withBody(jsonContent)
                .withHeader(Exchange.FILE_NAME, "empty.json")
                .build();

        processor.process(exchange);

        String result = exchange.getIn().getBody(String.class);
        assertEquals("", result);
    }
}