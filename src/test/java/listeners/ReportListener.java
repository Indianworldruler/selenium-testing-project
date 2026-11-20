package listeners;

import base.BaseTest;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.IConfigurationListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import utils.ExtentManager;
import utils.ScreenshotUtil;
import utils.WaitUtil;

import java.util.Arrays;

/** Screenshot on EVERY pass, failure, or skip; beautiful Extent Spark HTML output. */
public class ReportListener implements ITestListener, IConfigurationListener {
    private static final Logger LOG = LogManager.getLogger(ReportListener.class);
    private final ThreadLocal<ExtentTest> current = new ThreadLocal<>();
    private ExtentReports reports;

    @Override
    public void onStart(ITestContext context) {
        reports = ExtentManager.getInstance();
        LOG.info("========== SUITE START: {} ==========", context.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        if (reports == null) reports = ExtentManager.getInstance();
        String parameters = result.getParameters().length == 0 ? "" :
                " " + Arrays.toString(result.getParameters());
        ExtentTest entry = reports.createTest(result.getMethod().getMethodName() + parameters,
                result.getMethod().getDescription());
        entry.assignCategory("OrangeHRM", "My Info");
        entry.info("Starting test: " + result.getName() + parameters);
        current.set(entry);
        LOG.info("TEST START: {}{}", result.getName(), parameters);
    }

    private void takeEvidence(ITestResult result, String status) {
        ExtentTest test = current.get();
        if (test == null) return;
        WebDriver driver = null;
        if (result.getInstance() instanceof BaseTest base) driver = base.getDriver();
        // Avoid a misleading green PASS screenshot of OrangeHRM's loading spinner.
        // On genuine timeouts the spinner may remain; in that case capture it as
        // diagnostic evidence and log that the page never became idle.
        if (driver != null) new WaitUtil(driver).settleForScreenshot();
        String suffix = result.getParameters().length == 0 ? "" :
                "_" + Arrays.toString(result.getParameters()).replaceAll("[^a-zA-Z0-9]", "_");
        String screenshot = ScreenshotUtil.capture(driver,
                status + "_" + result.getMethod().getMethodName() + suffix);
        if (screenshot != null) {
            try {
                test.addScreenCaptureFromPath(screenshot);
                test.info("Screenshot: " + screenshot);
            } catch (Exception e) {
                LOG.warn("Unable to embed screenshot {}: {}", screenshot, e.getMessage());
            }
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOG.info("PASS: {}", result.getName());
        if (current.get() != null) current.get().pass("All assertions passed");
        takeEvidence(result, "PASS");
        current.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LOG.error("FAIL: {}", result.getName(), result.getThrowable());
        if (current.get() != null) current.get().fail(result.getThrowable());
        takeEvidence(result, "FAIL");
        current.remove();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOG.warn("SKIP: {}", result.getName());
        if (current.get() != null) current.get().skip(
                result.getThrowable() != null ? result.getThrowable().getMessage() : "Skipped");
        takeEvidence(result, "SKIP");
        current.remove();
    }

    /** Show setup failures in the HTML report, even if no @Test has started. */
    @Override
    public void onConfigurationFailure(ITestResult result) {
        if (reports == null) reports = ExtentManager.getInstance();
        LOG.error("CONFIGURATION FAILED: {}", result.getName(), result.getThrowable());
        ExtentTest configuration = reports.createTest("SETUP/TEARDOWN FAILED: " + result.getName());
        if (result.getThrowable() != null) configuration.fail(result.getThrowable());
        WebDriver driver = null;
        if (result.getInstance() instanceof BaseTest base) driver = base.getDriver();
        String screenshot = ScreenshotUtil.capture(driver, "CONFIGURATION_FAILED_" + result.getName());
        if (screenshot != null) configuration.addScreenCaptureFromPath(screenshot);
    }

    @Override
    public void onFinish(ITestContext context) {
        LOG.info("========== SUITE FINISHED: {} ==========", context.getName());
        if (reports != null) {
            reports.flush();
            LOG.info("HTML report generated: reports/ExtentReport.html");
        }
    }
}
