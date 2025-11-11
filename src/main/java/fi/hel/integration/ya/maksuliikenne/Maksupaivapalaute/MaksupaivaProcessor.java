package fi.hel.integration.ya.maksuliikenne.Maksupaivapalaute;

import org.apache.camel.Exchange;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Named ("maksupaivaProcessor")
public class MaksupaivaProcessor {

    @Inject
    Logger log;
    
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void mapPaymentFeedback(Exchange ex) {
        
        try {
            List<Map<String, Object>> inputList = ex.getIn().getBody(List.class);
            log.info("Processing payment feedback data");
            
            List<PaymentFeedback> feedbackList = new ArrayList<>();
            String currentDate = LocalDate.now().format(dateFormatter);
            
            for (Map<String, Object> item : inputList) {
                Long invoiceNumber = ((Number) item.get("invoiceNumber")).longValue();
                
                PaymentFeedback feedback = new PaymentFeedback();
                feedback.setInvoiceNumber(invoiceNumber);
                feedback.setState("PAID");
                feedback.setPaidAt(currentDate);
                feedback.setReceiptDate(currentDate);
                
                feedbackList.add(feedback);
            }
            
            ex.getIn().setBody(feedbackList);
            
            log.info("Successfully processed " + feedbackList.size() + " payment feedback records");
            
        } catch (Exception e){
            log.error("Error processing payment feedback: " + e.getMessage(), e);
            e.printStackTrace();
            ex.setException(e);
        }
    }
    

}
