package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.DemoPace;
import utils.WaitUtil;

/** Supplied StarterKit page: login credentials are configurable system properties. */
public class LoginPage {
    private final WaitUtil ui;
    private final By txtUsername = By.xpath("//input[@name='username']");
    private final By txtPassword = By.xpath("//input[@name='password']");
    private final By btnLogin = By.xpath("//button[@type='submit']");

    public LoginPage(WebDriver driver) {
        ui = new WaitUtil(driver);
    }

    public void loginAsAdmin() {
        String user = System.getProperty("orange.username", "Admin");
        String pass = System.getProperty("orange.password", "admin123");
        ui.fill(txtUsername, user);
        ui.fill(txtPassword, pass);
        DemoPace.action();
        ui.click(btnLogin);
        ui.visible(By.xpath("//aside//span[normalize-space()='My Info']"));
    }
}
