package pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.WaitUtil;

/** Business Page Object 2/3: My Info > Contact Details. */
public final class ContactDetailsPage {
    private static final Logger LOG = LogManager.getLogger(ContactDetailsPage.class);
    private final WebDriver driver;
    private final WaitUtil ui;
    private final By tab = By.xpath("//a[normalize-space()='Contact Details' "
            + "and contains(@href,'contactDetails')]");
    private final By heading = By.xpath("//h6[normalize-space()='Contact Details']");
    private final By mobile = By.xpath("//label[normalize-space()='Mobile']"
            + "/ancestor::div[contains(concat(' ',normalize-space(@class),' '),' oxd-input-group ')][1]//input");
    private final By otherEmail = By.xpath("//label[normalize-space()='Other Email']"
            + "/ancestor::div[contains(concat(' ',normalize-space(@class),' '),' oxd-input-group ')][1]//input");
    private final By save = By.xpath("//h6[normalize-space()='Contact Details']"
            + "/following::form[1]//button[@type='submit' and normalize-space()='Save']");
    private final By otherEmailValidation = By.xpath(
            "//label[normalize-space()='Other Email']"
                    + "/ancestor::div[contains(concat(' ',normalize-space(@class),' '),' oxd-input-group ')][1]"
                    + "//span[contains(@class,'oxd-input-field-error-message')]");

    public ContactDetailsPage(WebDriver driver) {
        this.driver = driver;
        this.ui = new WaitUtil(driver);
    }

    public void open() {
        if (!driver.getCurrentUrl().contains("/pim/contactDetails/")) {
            LOG.info("Opening Contact Details section");
            ui.click(tab);
            ui.untilUrlContains("/pim/contactDetails/");
        }
        // Mobile and Other Email may legitimately be blank: don't require values.
        ui.waitForForm(heading, mobile);
        ui.visible(otherEmail);
    }

    public boolean isLoaded() {
        ui.waitForForm(heading, mobile);
        return ui.isVisible(otherEmail);
    }

    public String getMobile() { return ui.value(mobile); }
    public String getOtherEmail() { return ui.value(otherEmail); }
    public void enterMobile(String number) { ui.fill(mobile, number); }
    public void enterOtherEmail(String address) { ui.fill(otherEmail, address); }

    public void save() {
        LOG.info("Submitting Contact Details");
        ui.click(save);
        ui.waitForSaveSuccess();
    }

    public boolean submitInvalidEmailAndHasValidation(String invalidEmail) {
        ui.fill(otherEmail, invalidEmail);
        ui.click(save);
        ui.visible(otherEmailValidation);
        ui.waitForIdle();
        return ui.isVisible(otherEmailValidation);
    }

    public void reload() {
        LOG.info("Reloading Contact Details to verify server persistence");
        driver.navigate().refresh();
        ui.waitForForm(heading, mobile);
        ui.visible(otherEmail);
    }

    public void waitForMobile(String expected) { ui.waitForValue(mobile, expected); }
}
