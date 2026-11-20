package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.WaitUtil;

/** Shared StarterKit navigation (not counted as one of the three business pages). */
public class NavigationBar {
    private final WaitUtil ui;
    public NavigationBar(WebDriver driver) { ui = new WaitUtil(driver); }

    private void openSection(String section) {
        ui.click(By.xpath("//aside//span[normalize-space()='" + section + "']"));
    }

    public void openMyInfo() {
        openSection("My Info");
        // OrangeHRM routes My Info to /pim/viewPersonalDetails/empNumber/{id}.
        // Wait for actual page content instead of relying on a version-specific URL.
        ui.visible(By.xpath("//h6[normalize-space()='Personal Details']"));
        ui.waitForNonBlank(By.cssSelector("input[name='firstName']"),
                By.cssSelector("input[name='lastName']"));
    }

    public void openPIM() { openSection("PIM"); }
    public void openAdmin() { openSection("Admin"); }
    public void openLeave() { openSection("Leave"); }
    public void openDirectory() { openSection("Directory"); }
    public void openRecruitment() { openSection("Recruitment"); }
}
