package utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public final class ScreenshotUtil {
    private static final Logger LOG = LogManager.getLogger(ScreenshotUtil.class);
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private ScreenshotUtil() { }

    /** Returns a path relative to reports/ExtentReport.html for embedding. */
    public static String capture(WebDriver driver, String label) {
        if (driver == null) return null;
        try {
            Path folder = Path.of("screenshots");
            Files.createDirectories(folder);
            String safe = label.replaceAll("[^a-zA-Z0-9_-]", "_");
            String filename = safe + "_" + LocalDateTime.now().format(STAMP) + ".png";
            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(source.toPath(), folder.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Screenshot saved: screenshots/{}", filename);
            return "../screenshots/" + filename;
        } catch (Exception e) {
            LOG.warn("Could not capture screenshot: {}", e.toString());
            return null;
        }
    }
}
