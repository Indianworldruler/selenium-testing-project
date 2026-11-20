package utils;

import java.nio.file.Files;
import java.nio.file.Path;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

/** Extent Spark: graphical summary, individual tests and linked screenshots. */
public final class ExtentManager {
    private static ExtentReports extent;
    private ExtentManager() { }

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            try {
                Files.createDirectories(Path.of("reports"));
                Files.createDirectories(Path.of("screenshots"));
                Files.createDirectories(Path.of("logs"));
            } catch (Exception e) {
                throw new IllegalStateException("Cannot initialise report folders", e);
            }
            ExtentSparkReporter spark = new ExtentSparkReporter("reports/ExtentReport.html");
            spark.config().setDocumentTitle("OrangeHRM | My Information Test Report");
            spark.config().setReportName("Project 8 • My Info — Automated Test Results");
            spark.config().setTheme(Theme.DARK);
            spark.config().setTimeStampFormat("dd MMM yyyy, HH:mm:ss");
            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Project", "OrangeHRM My Information Update");
            extent.setSystemInfo("Browser", "Google Chrome / Selenium Manager");
            extent.setSystemInfo("Framework", "Selenium + Java + TestNG + POM");
        }
        return extent;
    }
}
