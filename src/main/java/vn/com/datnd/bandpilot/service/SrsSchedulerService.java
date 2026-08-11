package vn.com.datnd.bandpilot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled service that logs a daily reminder when SRS words are due for review.
 *
 * <p>Email delivery has been replaced by in-app notification (due-count badge in the
 * navigation bar). This scheduler only logs to the server console so the application
 * operator can monitor review activity without any third-party dependency.</p>
 *
 * <p>Runs every day at 08:00 Vietnam time (UTC+7).</p>
 */
@Service
public class SrsSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SrsSchedulerService.class);

    private final SrsService srsService;

    public SrsSchedulerService(SrsService srsService) {
        this.srsService = srsService;
    }

    /**
     * Runs every day at 08:00 Vietnam time.
     * Logs the number of due words; the frontend badge handles user-facing notification.
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Ho_Chi_Minh")
    public void logDailyDueCount() {
        long dueCount = srsService.getDueCount();
        if (dueCount == 0) {
            log.info("SRS daily check: no words due today");
        } else {
            log.info("SRS daily check: {} word(s) due for review today", dueCount);
        }
    }
}
