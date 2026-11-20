package base;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import pages.LoginPage;
import pages.NavigationBar;
import utils.DemoPace;
import utils.DriverFactory;
import utils.ScreenshotUtil;

/**
 * The provided StarterKit BaseTest, with one browser/login per class.
 * IMPORTANT: My Info is NOT reopened before each test. Each business page
 * object is responsible for opening its own tab only when necessary.
 */
public abstract class BaseTest {
    private static final Logger LOG = LogManager.getLogger(BaseTest.class);
    protected WebDriver driver;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        try {
            LOG.info("STEP 1/3: Starting Chrome (existing driver is reused when available)");
            driver = DriverFactory.getDriver();
            LOG.info("STEP 2/3: Opening OrangeHRM login page");
            driver.get(System.getProperty("orange.url",
                    "https://opensource-demo.orangehrmlive.com/web/index.php/auth/login"));
            new LoginPage(driver).loginAsAdmin();
            LOG.info("STEP 3/3: Navigating to My Info ONCE");
            new NavigationBar(driver).openMyInfo();
            LOG.info("SETUP PASSED: My Info Personal Details is ready. Starting form tests.");
        } catch (Exception e) {
            LOG.error("SETUP FAILED. Check screenshot SETUP_FAILED and details below", e);
            ScreenshotUtil.capture(driver, "SETUP_FAILED");
            throw e;
        }
    }

    public WebDriver getDriver() { return driver; }

    @AfterMethod(alwaysRun = true)
    public void holdResult() { DemoPace.result(); }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        LOG.info("Suite complete; closing the single Chrome session");
        try { DemoPace.sleep(350); } finally { DriverFactory.quitDriver(); }
    }
}
