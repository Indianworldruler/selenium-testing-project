package tests;

import base.BaseTest;
import listeners.ReportListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import pages.ContactDetailsPage;
import pages.EmergencyContactsPage;
import pages.PersonalDetailsPage;
import utils.ScreenshotUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Project 8: My Information Update. All real assertions live in the test class;
 * locators, interactions and page waits stay in exactly three business pages.
 * DataProvider drives multiple update values. All saved changes are restored
 * or deleted in finally blocks, where the demo permits it.
 */
@Listeners(ReportListener.class)
public class UpdateMyInfoTest extends BaseTest {
    private static final Logger LOG = LogManager.getLogger(UpdateMyInfoTest.class);
    private static final boolean ALLOW_WRITES =
            Boolean.parseBoolean(System.getProperty("allowWrites", "true"));

    @DataProvider(name = "middleNames")
    public Object[][] middleNames() {
        return new Object[][] { {"QATest"}, {"AutomationDemo"} };
    }

    @DataProvider(name = "mobileNumbers")
    public Object[][] mobileNumbers() {
        return new Object[][] { {"9000012345"}, {"9000054321"} };
    }

    private void checkWritePermission() {
        if (!ALLOW_WRITES) {
            throw new org.testng.SkipException("Write test disabled: use -DallowWrites=true");
        }
    }

    // TC01
    @Test(priority = 1, description = "Navigation reaches My Info / Personal Details")
    public void personalDetailsPageOpens() {
        PersonalDetailsPage page = new PersonalDetailsPage(driver);
        page.open();
        Assert.assertTrue(page.isLoaded(), "Personal Details heading missing");
        Assert.assertFalse(page.getFirstName().isBlank(), "First name must be populated");
        Assert.assertFalse(page.getLastName().isBlank(), "Last name must be populated");
        LOG.info("TC01 PASS: My Info > Personal Details loaded");
    }

    // TC02, TC03 (DataProvider)
    @Test(priority = 2, dataProvider = "middleNames",
          description = "Update middle name, reload to verify persistence, restore original")
    public void updatePersonalMiddleNameAndRestore(String proposed) {
        checkWritePermission();
        PersonalDetailsPage page = new PersonalDetailsPage(driver);
        page.open();
        String original = page.getMiddleName();
        LOG.info("Personal Details: testing temporary middle name '{}'", proposed);
        try {
            page.enterMiddleName(proposed);
            page.save();
            page.reload();
            page.waitForMiddleName(proposed);
            ScreenshotUtil.capture(driver, "PersonalDetails_Saved_" + proposed);
            Assert.assertEquals(page.getMiddleName(), proposed,
                    "Personal Details change did not persist after refreshing");
        } finally {
            try {
                page.enterMiddleName(original);
                page.save();
                page.reload();
                page.waitForMiddleName(original);
                Assert.assertEquals(page.getMiddleName(), original,
                        "Cleanup failed: original middle name not restored");
                LOG.info("Personal Details: original middle name restored");
            } catch (Exception cleanupError) {
                LOG.error("IMPORTANT: Unable to restore original middle name", cleanupError);
                throw cleanupError;
            }
        }
    }

    // TC04
    @Test(priority = 3, description = "Empty mandatory first name is rejected")
    public void emptyFirstNameShowsValidation() {
        checkWritePermission();
        PersonalDetailsPage page = new PersonalDetailsPage(driver);
        page.open();
        String original = page.getFirstName();
        Assert.assertFalse(original.isBlank(), "Cannot test if original first name is empty");
        try {
            Assert.assertTrue(page.submitWithEmptyFirstNameAndHasValidation(),
                    "Required-field validation not displayed for empty first name");
        } finally {
            page.enterFirstName(original);
            page.save(); // restore original value; verify it after a full reload
            page.reload();
            Assert.assertEquals(page.getFirstName(), original, "Original first name not restored");
        }
    }

    // TC05
    @Test(priority = 4, description = "Navigate to Contact Details and inspect fields")
    public void contactDetailsPageOpens() {
        ContactDetailsPage page = new ContactDetailsPage(driver);
        page.open();
        Assert.assertTrue(page.isLoaded(), "Contact Details heading not visible");
        Assert.assertNotNull(page.getMobile(), "Mobile input does not exist");
        LOG.info("TC05 PASS: Contact Details page loaded");
    }

    // TC06, TC07 (DataProvider)
    @Test(priority = 5, dataProvider = "mobileNumbers",
          description = "Update mobile number, reload to verify, restore original")
    public void updateContactMobileAndRestore(String proposed) {
        checkWritePermission();
        ContactDetailsPage page = new ContactDetailsPage(driver);
        page.open();
        String original = page.getMobile();
        LOG.info("Contact Details: testing temporary mobile '{}'", proposed);
        try {
            page.enterMobile(proposed);
            page.save();
            page.reload();
            page.waitForMobile(proposed);
            ScreenshotUtil.capture(driver, "ContactDetails_Saved_" + proposed);
            Assert.assertEquals(page.getMobile(), proposed,
                    "New mobile number not saved after refreshing Contact Details");
        } finally {
            try {
                page.enterMobile(original);
                page.save();
                page.reload();
                page.waitForMobile(original);
                Assert.assertEquals(page.getMobile(), original,
                        "Cleanup failed: original mobile number not restored");
                LOG.info("Contact Details: original mobile restored");
            } catch (Exception cleanupError) {
                LOG.error("IMPORTANT: Unable to restore original mobile", cleanupError);
                throw cleanupError;
            }
        }
    }

    // TC08
    @Test(priority = 6, description = "Contact Details rejects an invalid email format")
    public void invalidOtherEmailShowsValidation() {
        checkWritePermission();
        ContactDetailsPage page = new ContactDetailsPage(driver);
        page.open();
        String original = page.getOtherEmail();
        try {
            Assert.assertTrue(page.submitInvalidEmailAndHasValidation("not-an-email"),
                    "Invalid Other Email was not flagged by form validation");
        } finally {
            page.enterOtherEmail(original);
            page.save(); // restore the original email and verify it persists
            page.reload();
            Assert.assertEquals(page.getOtherEmail(), original, "Original Other Email not restored");
        }
    }

    // TC09
    @Test(priority = 7, description = "Emergency Contacts list is accessible")
    public void emergencyContactsPageOpens() {
        EmergencyContactsPage page = new EmergencyContactsPage(driver);
        page.open();
        Assert.assertTrue(page.isLoaded(), "Emergency Contacts list did not load");
    }

    // TC10
    @Test(priority = 8, description = "Emergency Contacts form requires name and relationship")
    public void emergencyContactRequiredFields() {
        checkWritePermission();
        EmergencyContactsPage page = new EmergencyContactsPage(driver);
        page.open();
        page.startAdd();
        try {
            Assert.assertTrue(page.isAddFormVisible(), "Add Emergency Contact form missing");
            Assert.assertTrue(page.requiredErrorsAfterEmptySave() >= 2,
                    "Expected validation for required Name and Relationship fields");
        } finally {
            page.cancelAdd();
        }
    }

    // TC11
    @Test(priority = 9, description = "Cancel Add Emergency Contact without creating a record")
    public void emergencyContactCancelReturnsToList() {
        EmergencyContactsPage page = new EmergencyContactsPage(driver);
        page.open();
        page.startAdd();
        page.enterContact("QACancelSample", "Friend", "9000000001");
        page.cancelAdd();
        Assert.assertTrue(page.isLoaded(), "Cancel did not return to the contact list");
        Assert.assertFalse(page.isContactListed("QACancelSample"),
                "Cancel unexpectedly saved an emergency contact");
    }

    // TC12
    @Test(priority = 10, description = "Add an emergency contact, confirm it and delete test record")
    public void addEmergencyContactVerifyAndRemove() {
        checkWritePermission();
        EmergencyContactsPage page = new EmergencyContactsPage(driver);
        page.open();
        String uniqueName = "QATest" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMddHHmmssSSS"));
        boolean created = false;
        try {
            page.startAdd();
            page.enterContact(uniqueName, "Friend", "9000098765");
            page.saveContact();
            created = true;
            page.waitForContact(uniqueName);
            ScreenshotUtil.capture(driver, "EmergencyContact_Added_" + uniqueName);
            Assert.assertTrue(page.isContactListed(uniqueName),
                    "New emergency contact not visible after Save");
            LOG.info("Emergency contact '{}' created successfully", uniqueName);
        } finally {
            // Check the list even when Save/Toast times out, so a created record
            // is not accidentally abandoned on the public demo.
            try {
                page.open();
                if (created || page.isContactListed(uniqueName)) {
                    page.deleteContact(uniqueName);
                    Assert.assertFalse(page.isContactListed(uniqueName),
                            "Cleanup failed: temporary emergency contact still present");
                    LOG.info("Removed test emergency contact '{}'", uniqueName);
                }
            } catch (Exception cleanupError) {
                LOG.error("IMPORTANT: Verify/remove test contact '{}' manually if it remains",
                        uniqueName, cleanupError);
                throw cleanupError;
            }
        }
    }
}
