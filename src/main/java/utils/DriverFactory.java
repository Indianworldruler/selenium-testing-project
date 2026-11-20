package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/** One Chrome instance; reuse an installed matching ChromeDriver when possible. */
public final class DriverFactory {
    private static final Logger LOG = LogManager.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();
    private static final Pattern VERSION = Pattern.compile("\\b(\\d{2,3})\\.\\d+\\.");

    private DriverFactory() { }

    public static WebDriver getDriver() {
        if (DRIVER.get() != null) return DRIVER.get();

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--disable-notifications", "--no-first-run",
                "--no-default-browser-check", "--disable-search-engine-choice-screen");
        String explicitBinary = System.getProperty("chrome.binary", "");
        if (!explicitBinary.isBlank()) options.setBinary(explicitBinary);

        // Selenium Manager may query the network even when ChromeDriver was
        // previously downloaded. Prefer an executable whose major version
        // matches the local Chrome version, without disabling fallback discovery.
        if (System.getProperty("webdriver.chrome.driver", "").isBlank()) {
            findMatchingDriver().ifPresent(path -> {
                System.setProperty("webdriver.chrome.driver", path.toString());
                LOG.info("Using local matching ChromeDriver: {}", path);
            });
        } else {
            LOG.info("Using webdriver.chrome.driver property: {}",
                    System.getProperty("webdriver.chrome.driver"));
        }

        LOG.info("Launching Chrome. First-time Selenium Manager downloads can be slow; "
                + "later runs should reuse the matching driver.");
        long start = System.nanoTime();
        try {
            WebDriver driver = new ChromeDriver(options);
            DRIVER.set(driver); // keep handle even if maximise/window setup fails
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(40));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(20));
            try { driver.manage().window().maximize(); }
            catch (Exception e) { LOG.warn("Window maximise unavailable: {}", e.getMessage()); }
            LOG.info("Chrome ready in {} seconds", (System.nanoTime() - start) / 1_000_000_000L);
            return driver;
        } catch (RuntimeException e) {
            LOG.error("Could not start Chrome. Check Chrome/ChromeDriver versions and "
                    + "provide -Dwebdriver.chrome.driver=/path if necessary", e);
            quitDriver();
            throw e;
        }
    }

    private static java.util.Optional<Path> findMatchingDriver() {
        String chromeBinary = System.getProperty("chrome.binary",
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        String chromeVersion = commandOutput(Path.of(chromeBinary), "--version");
        Matcher match = VERSION.matcher(chromeVersion);
        if (!match.find()) {
            LOG.info("Chrome version not detected locally; Selenium Manager will choose the driver");
            return java.util.Optional.empty();
        }
        String major = match.group(1);
        List<Path> possible = new ArrayList<>();
        for (String dir : System.getenv().getOrDefault("PATH", "").split(":", -1)) {
            if (!dir.isBlank()) possible.add(Path.of(dir, "chromedriver"));
        }
        Path cache = Path.of(System.getProperty("user.home"), ".cache", "selenium", "chromedriver");
        if (Files.isDirectory(cache)) {
            try (Stream<Path> paths = Files.walk(cache, 4)) {
                paths.filter(p -> p.getFileName().toString().equals("chromedriver"))
                        .forEach(possible::add);
            } catch (IOException e) {
                LOG.debug("Could not scan Selenium driver cache: {}", e.getMessage());
            }
        }
        return possible.stream().filter(Files::isExecutable)
                .filter(p -> {
                    String version = commandOutput(p, "--version");
                    Matcher m = VERSION.matcher(version);
                    return m.find() && major.equals(m.group(1));
                })
                .sorted(Comparator.comparing(Path::toString)).findFirst();
    }

    private static String commandOutput(Path executable, String arg) {
        if (!Files.isExecutable(executable)) return "";
        Process process = null;
        try {
            process = new ProcessBuilder(executable.toString(), arg)
                    .redirectErrorStream(true).start();
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "";
            }
            return new String(process.getInputStream().readAllBytes());
        } catch (Exception e) {
            if (process != null) process.destroyForcibly();
            return "";
        }
    }

    public static WebDriver currentDriver() { return DRIVER.get(); }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        try { if (driver != null) driver.quit(); }
        finally { DRIVER.remove(); }
    }
}
