package utils;

import org.openqa.selenium.WebElement;

/** Moderate visible pacing; explicit waits control correctness and readiness. */
public final class DemoPace {
    private DemoPace() { }
    // Old values were 450 / 55 / 800; these make a 12-test run needlessly slow.
    private static final long ACTION_MS = Long.getLong("demo.actionMs", 220L);
    private static final long KEY_MS = Long.getLong("demo.keyMs", 22L);
    private static final long RESULT_MS = Long.getLong("demo.resultMs", 300L);

    public static void action() { sleep(ACTION_MS); }
    public static void result() { sleep(RESULT_MS); }

    public static void type(WebElement element, String value) {
        if (value == null || value.isEmpty()) return;
        for (char c : value.toCharArray()) {
            element.sendKeys(String.valueOf(c));
            sleep(KEY_MS);
        }
    }

    public static void sleep(long ms) {
        if (ms <= 0) return;
        try { Thread.sleep(ms); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while pacing browser actions", e);
        }
    }
}
