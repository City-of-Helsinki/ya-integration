package fi.hel.integration.ya.maksuliikenne.processor;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class HolidayService {

    private final Set<LocalDate> holidays = new HashSet<>();

    @PostConstruct
    void init() {
        loadHolidays(LocalDate.now().getYear());
    }

    public void loadHolidays(int year) {
        String resourcePath = "maksuliikenne/holidays/" + year + ".txt";
        
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath)) {
            
            if (is == null) {
                System.out.println("Holiday file not found: " + resourcePath);
                return;
            }
            
            new BufferedReader(new InputStreamReader(is))
                .lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .forEach(line -> holidays.add(LocalDate.parse(line)));
                
            System.out.println("Loaded " + holidays.size() + " holidays for " + year);
            
        } catch (Exception e) {
            System.err.println("Error loading holidays: " + e.getMessage());
        }
    }

    public boolean isHoliday(LocalDate date) {
        // Varmista että vuoden pyhäpäivät on ladattu
        if (holidays.stream().noneMatch(h -> h.getYear() == date.getYear())) {
            loadHolidays(date.getYear());
        }
        return holidays.contains(date);
    }
}
