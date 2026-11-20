package utils;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Explicit waits for OrangeHRM's asynchronous Vue forms. Visibility alone is
 * NOT readiness: a visible input can still have an empty value while API
 * data is being fetched or the application is showing its loading circle.
 * This helper is a utility, not another business page object.
 */
public final class WaitUtil {
    private static final Logger LOG = LogManager.getLogger(WaitUtil.class);
    private static final By LOADERS = By.cssSelector(
            ".oxd-form-loader, .oxd-loading-spinner, .oxd-loading-spinner-container, "
                    + ".oxd-table-loader, .oxd-loading-spinner-overlay, [aria-busy='true']");
    private static final By SUCCESS_TOAST = By.xpath(
            "//div[contains(@class,'oxd-toast') and "
                    + "(contains(.,'Success') or contains(.,'Successfully'))]");
    private static final By ERROR_TOAST = By.xpath(
            "//div[contains(@class,'oxd-toast') and "
                    + "(contains(.,'Error') or contains(.,'Failed'))]");
    private final WebDriver driver;
    private final WebDriverWait wait;

    public WaitUtil(WebDriver driver) {
        this.driver = driver;
        long seconds = Long.getLong("wait.seconds", 30L);
        wait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
        wait.pollingEvery(Duration.ofMillis(250));
        wait.ignoring(StaleElementReferenceException.class);
    }

    public WebElement visible(By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            LOG.error("Element never became visible: {} | URL: {}", locator, safeUrl());
            throw e;
        }
    }

    public boolean isVisible(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            return elements.stream().anyMatch(e -> {
                try { return e.isDisplayed(); }
                catch (StaleElementReferenceException ignored) { return false; }
            });
        } catch (RuntimeException e) { return false; }
    }

    public boolean isBusy() {
        try {
            for (WebElement spinner : driver.findElements(LOADERS)) {
                if (spinner.isDisplayed()) return true;
            }
            return false;
        } catch (StaleElementReferenceException e) {
            return true;  // Vue re-rendering: retry rather than accepting transient UI.
        }
    }

    /** Wait for all known loading overlays to disappear and remain gone briefly. */
    public void waitForIdle() {
        AtomicLong idleSince = new AtomicLong(0L);
        try {
            wait.until(d -> {
                if (isBusy()) {
                    idleSince.set(0L);
                    return false;
                }
                Object ready = ((JavascriptExecutor) d).executeScript("return document.readyState");
                if (!"complete".equals(ready) && !"interactive".equals(ready)) {
                    idleSince.set(0L);
                    return false;
                }
                long now = System.nanoTime();
                if (idleSince.get() == 0L) idleSince.set(now);
                return now - idleSince.get() >= 350_000_000L;
            });
        } catch (TimeoutException ex) {
            throw new TimeoutException("OrangeHRM is still displaying a loading indicator after "
                    + Long.getLong("wait.seconds", 30L) + " s at " + safeUrl(), ex);
        }
    }

    /**
     * Wait for actual employee data, not merely input tags. Required fields
     * must be non-empty; a blank value here means the form is not ready (or
     * the remote demo failed to return usable employee data).
     */
    public void waitForNonBlank(By... locators) {
        AtomicLong stableSince = new AtomicLong(0L);
        try {
            wait.until(d -> {
                if (isBusy()) { stableSince.set(0L); return false; }
                for (By locator : locators) {
                    List<WebElement> elements = d.findElements(locator);
                    if (elements.isEmpty() || !elements.get(0).isDisplayed()) {
                        stableSince.set(0L); return false;
                    }
                    String value = elements.get(0).getDomProperty("value");
                    if (value == null || value.isBlank()) {
                        stableSince.set(0L); return false;
                    }
                }
                long now = System.nanoTime();
                if (stableSince.get() == 0L) stableSince.set(now);
                return now - stableSince.get() >= 400_000_000L;
            });
        } catch (TimeoutException ex) {
            throw new TimeoutException("Employee form data did not load. First/last name must "
                    + "be populated before a My Info test can proceed. URL: " + safeUrl(), ex);
        }
    }

    public void waitForValue(By locator, String expected) {
        try {
            wait.until(d -> {
                if (isBusy()) return false;
                List<WebElement> elements = d.findElements(locator);
                return !elements.isEmpty() && elements.get(0).isDisplayed()
                        && expected.equals(elements.get(0).getDomProperty("value"));
            });
        } catch (TimeoutException ex) {
            throw new TimeoutException("Expected saved value was not present after refresh "
                    + "in " + locator + ". URL: " + safeUrl(), ex);
        }
    }

    /** For fields that are legitimately blank, require stable content and no spinner. */
    public void waitForForm(By heading, By input) {
        visible(heading);
        visible(input);
        waitForIdle();
        visible(input);
    }

    public void click(By locator) {
        waitForIdle();
        try {
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center',inline:'nearest'});", element);
            element.click();
        } catch (ElementClickInterceptedException | StaleElementReferenceException e) {
            LOG.warn("Click intercepted or stale; retrying once: {}", locator);
            waitForIdle();
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center',inline:'nearest'});", element);
            element.click();
        }
        DemoPace.action();
    }

    public String value(By locator) {
        waitForIdle();
        String text = visible(locator).getDomProperty("value");
        return text == null ? "" : text;
    }

    public void fill(By locator, String text) {
        String desired = text == null ? "" : text;
        waitForIdle();
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(locator));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", input);
        input.click();
        // Native keyboard events, compatible with Vue's v-model and validation.
        Keys modifier = System.getProperty("os.name", "").toLowerCase(Locale.ROOT)
                .contains("mac") ? Keys.COMMAND : Keys.CONTROL;
        input.sendKeys(Keys.chord(modifier, "a"));
        input.sendKeys(Keys.BACK_SPACE);
        DemoPace.type(input, desired);
        input.sendKeys(Keys.TAB);
        waitForValue(locator, desired);
        LOG.info("Entered {} characters into {}", desired.length(), locator);
        DemoPace.action();
    }

    public void untilUrlContains(String fragment) {
        wait.until(ExpectedConditions.urlContains(fragment));
    }

    /** Save is not considered successful merely because the button was clicked. */
    public void waitForSaveSuccess() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .pollingEvery(Duration.ofMillis(200))
                    .until(d -> {
                        if (isVisible(ERROR_TOAST)) {
                            throw new IllegalStateException("OrangeHRM displayed an error toast "
                                    + "when saving data: " + d.findElement(ERROR_TOAST).getText());
                        }
                        return isVisible(SUCCESS_TOAST);
                    });
            waitForIdle();
            LOG.info("Save confirmed by OrangeHRM Success toast");
        } catch (TimeoutException ex) {
            throw new TimeoutException("Save did not display a Success confirmation within 15 s "
                    + "at " + safeUrl() + ". Do not refresh before save completion.", ex);
        }
    }

    /** Safety: wait briefly for spinners before evidence screenshots. */
    public void settleForScreenshot() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(7))
                    .pollingEvery(Duration.ofMillis(250))
                    .until(d -> !isBusy());
            DemoPace.sleep(250);
        } catch (RuntimeException e) {
            LOG.warn("Loading overlay did not disappear before evidence capture; "
                    + "the screenshot will document the unresolved loading state.");
        }
    }

    public void gone(By locator) { wait.until(ExpectedConditions.invisibilityOfElementLocated(locator)); }

    private String safeUrl() {
        try { return driver.getCurrentUrl(); }
        catch (RuntimeException e) { return "<unavailable>"; }
    }
}
