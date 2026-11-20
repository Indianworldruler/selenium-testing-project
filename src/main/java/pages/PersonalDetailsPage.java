package pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.WaitUtil;

/** Business Page Object 1/3: My Info > Personal Details. */
public final class PersonalDetailsPage {
    private static final Logger LOG = LogManager.getLogger(PersonalDetailsPage.class);
    private final WebDriver driver;
    private final WaitUtil ui;
    private final By tab = By.xpath("//a[normalize-space()='Personal Details' "
            + "and contains(@href,'viewPersonalDetails')]");
    private final By heading = By.xpath("//h6[normalize-space()='Personal Details']");
    private final By firstName = By.cssSelector("input[name='firstName']");
    private final By lastName = By.cssSelector("input[name='lastName']");
    private final By middleName = By.cssSelector("input[name='middleName']");
    private final By save = By.xpath("//h6[normalize-space()='Personal Details']"
            + "/following::form[1]//button[@type='submit' and normalize-space()='Save']");
    private final By firstNameRequired = By.xpath(
            "//input[@name='firstName']/ancestor::div[contains(@class,'oxd-input-group')][1]"
                    + "//span[contains(@class,'oxd-input-field-error-message')]");

    public PersonalDetailsPage(WebDriver driver) {
        this.driver = driver;
        this.ui = new WaitUtil(driver);
    }

    public void open() {
        if (!driver.getCurrentUrl().contains("/pim/viewPersonalDetails/")) {
            LOG.info("Opening Personal Details section");
            ui.click(tab);
            ui.untilUrlContains("/pim/viewPersonalDetails/");
        }
        // Critical fix: first and last name must be loaded from the API.
        // A visible but blank input is NOT a loaded employee profile.
        ui.visible(heading);
        ui.visible(middleName);
        ui.waitForNonBlank(firstName, lastName);
    }

    public boolean isLoaded() {
        ui.waitForNonBlank(firstName, lastName);
        return ui.isVisible(heading) && ui.isVisible(middleName);
    }

    public String getFirstName() { return ui.value(firstName); }
    public String getLastName() { return ui.value(lastName); }
    public String getMiddleName() { return ui.value(middleName); }
    public void enterMiddleName(String name) { ui.fill(middleName, name); }
    public void enterFirstName(String name) { ui.fill(firstName, name); }

    public void save() {
        LOG.info("Submitting Personal Details");
        ui.click(save);
        ui.waitForSaveSuccess();
    }

    public boolean submitWithEmptyFirstNameAndHasValidation() {
        ui.fill(firstName, "");
        ui.click(save);
        // Check validation specifically under First Name, not any other field.
        ui.visible(firstNameRequired);
        ui.waitForIdle();
        return ui.isVisible(firstNameRequired);
    }

    public void reload() {
        LOG.info("Reloading Personal Details; waiting for employee data to return");
        driver.navigate().refresh();
        ui.visible(heading);
        ui.waitForNonBlank(firstName, lastName);
    }

    public void waitForMiddleName(String expected) {
        ui.waitForValue(middleName, expected);
    }
}
