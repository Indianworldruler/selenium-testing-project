package pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.WaitUtil;

/** Business Page Object 3/3: My Info > Emergency Contacts. */
public final class EmergencyContactsPage {
    private static final Logger LOG = LogManager.getLogger(EmergencyContactsPage.class);
    private final WebDriver driver;
    private final WaitUtil ui;
    private final By tab = By.xpath("//a[normalize-space()='Emergency Contacts' "
            + "and contains(@href,'viewEmergencyContacts')]");
    private final By listHeading = By.xpath(
            "//h6[normalize-space()='Assigned Emergency Contacts']");
    private final By addButton = By.xpath(
            "//h6[normalize-space()='Assigned Emergency Contacts']"
                    + "/following::button[normalize-space()='Add'][1]");
    private final By name = By.xpath("//label[normalize-space()='Name']"
            + "/ancestor::div[contains(concat(' ',normalize-space(@class),' '),' oxd-input-group ')][1]//input");
    private final By relationship = By.xpath("//label[normalize-space()='Relationship']"
            + "/ancestor::div[contains(concat(' ',normalize-space(@class),' '),' oxd-input-group ')][1]//input");
    private final By mobile = By.xpath("//label[normalize-space()='Mobile']"
            + "/ancestor::div[contains(concat(' ',normalize-space(@class),' '),' oxd-input-group ')][1]//input");
    // The form heading may vary across OrangeHRM releases. Identify the form by
    // its actual Name and Relationship fields, rather than fixed heading text.
    private final By save = By.xpath(
            "//form[.//label[normalize-space()='Name'] and "
                    + ".//label[normalize-space()='Relationship']]"
                    + "//button[@type='submit' and normalize-space()='Save']");
    private final By cancel = By.xpath(
            "//form[.//label[normalize-space()='Name'] and "
                    + ".//label[normalize-space()='Relationship']]"
                    + "//button[normalize-space()='Cancel']");
    private final By requiredName = By.xpath(
            "//label[normalize-space()='Name']/ancestor::div[contains(concat(' ',normalize-space(@class),' '),' oxd-input-group ')][1]"
                    + "//span[contains(@class,'oxd-input-field-error-message')]");
    private final By requiredRelationship = By.xpath(
            "//label[normalize-space()='Relationship']"
                    + "/ancestor::div[contains(concat(' ',normalize-space(@class),' '),' oxd-input-group ')][1]"
                    + "//span[contains(@class,'oxd-input-field-error-message')]");

    public EmergencyContactsPage(WebDriver driver) {
        this.driver = driver;
        this.ui = new WaitUtil(driver);
    }

    public void open() {
        if (isAddFormVisible()) cancelAdd();
        if (!driver.getCurrentUrl().contains("/pim/viewEmergencyContacts/")) {
            LOG.info("Opening Emergency Contacts section");
            ui.click(tab);
            ui.untilUrlContains("/pim/viewEmergencyContacts/");
        }
        ui.visible(listHeading);
        ui.visible(addButton);
        ui.waitForIdle();  // Don't take a PASS screenshot of a list loading circle.
    }

    public boolean isLoaded() {
        ui.waitForIdle();
        return ui.isVisible(listHeading) && ui.isVisible(addButton);
    }

    public void startAdd() {
        open();
        ui.click(addButton);
        // Don't require an exact <h6> heading: the editable fields prove the
        // Add form is actually open and ready for input.
        ui.visible(name);
        ui.visible(relationship);
        ui.visible(save);
        ui.waitForIdle();
    }

    public boolean isAddFormVisible() {
        ui.waitForIdle();
        return ui.isVisible(name) && ui.isVisible(relationship)
                && ui.isVisible(save) && ui.isVisible(cancel);
    }

    public int requiredErrorsAfterEmptySave() {
        ui.click(save);
        ui.visible(requiredName);
        ui.visible(requiredRelationship);
        ui.waitForIdle();
        int total = 0;
        if (ui.isVisible(requiredName)) total++;
        if (ui.isVisible(requiredRelationship)) total++;
        return total;
    }

    public void enterContact(String contactName, String relation, String mobileNumber) {
        ui.fill(name, contactName);
        ui.fill(relationship, relation);
        ui.fill(mobile, mobileNumber);
    }

    public void saveContact() {
        LOG.info("Saving temporary emergency contact");
        ui.click(save);
        ui.waitForSaveSuccess();
        ui.visible(listHeading);
        ui.waitForIdle();
    }

    public void cancelAdd() {
        if (isAddFormVisible()) ui.click(cancel);
        ui.visible(listHeading);
        ui.waitForIdle();
    }

    private By contactRow(String contactName) {
        // Generated test contact names contain only alphanumeric characters.
        return By.xpath("//div[contains(@class,'oxd-table-card')]"
                + "[.//*[normalize-space()='" + contactName + "']]");
    }

    public boolean isContactListed(String contactName) {
        ui.waitForIdle();
        return ui.isVisible(contactRow(contactName));
    }

    public void waitForContact(String contactName) {
        ui.visible(contactRow(contactName));
        ui.waitForIdle();
    }

    public void deleteContact(String contactName) {
        LOG.info("Removing temporary emergency contact {}", contactName);
        waitForContact(contactName);
        By delete = By.xpath("//div[contains(@class,'oxd-table-card')]"
                + "[.//*[normalize-space()='" + contactName + "']]"
                + "//button[.//i[contains(@class,'bi-trash')]]");
        ui.click(delete);
        ui.click(By.xpath("//button[contains(@class,'oxd-button--label-danger') "
                + "and contains(.,'Yes, Delete')]"));
        ui.gone(contactRow(contactName));
        ui.waitForIdle();
        // Confirm the record is absent even after a new server read.
        driver.navigate().refresh();
        open();
        if (isContactListed(contactName)) {
            throw new AssertionError("Temporary emergency contact remains after deletion: "
                    + contactName);
        }
    }
}
