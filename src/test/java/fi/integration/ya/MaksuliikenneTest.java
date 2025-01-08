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
}
