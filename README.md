# OrangeHRM — Project 8: My Information Update

**Standalone Java + Selenium + TestNG + Maven project.**
No Bash scripts, no merging with a previous project, no custom website.

## Run on macOS with VS Code

1. Extract `OrangeHRM_MyInfo_Reliable_Standalone.zip` into a **new**, separate folder.
2. In VS Code open the `OrangeHRM_MyInfo_Reliable` folder containing `pom.xml`.
3. Open VS Code Terminal in that folder and run:

```bash
mvn clean test
```

Chrome launches, logs into the public OrangeHRM demo, opens My Info and runs
all 12 TestNG test executions in one Chrome session. The generated Extent
HTML report is **not** opened automatically.

- HTML report: `reports/ExtentReport.html`
- Screenshots of passes, failures and skips: `screenshots/`
- Log4j2 automation log: `logs/automation.log`
- Maven/TestNG output: `target/surefire-reports/`

## Corrections made after the blank-first-name failure

1. Personal Details is not ready merely because the First Name input is
   visible. The test now waits for the visible **First Name and Last Name to
   actually contain employee data**, with loading overlays gone, before the
   first assertion or update.
2. The helper waits for `.oxd-form-loader`, `.oxd-loading-spinner`,
   `.oxd-loading-spinner-container`, and `.oxd-table-loader` to disappear.
   Readiness is driven by explicit waits rather than longer fixed sleeps.
3. Refresh-based assertions wait for refreshed data and then check whether
   the exact saved value is present. A click or success toast alone does not
   count as successful persistence.
4. Contact Details and Emergency Contacts wait for their correct section,
   editable controls and the absence of loading overlays before interacting.
5. Required-field and email validation checks are scoped to the actual field
   to avoid passing on an unrelated error label.
6. Save actions wait for a success message rather than refreshing while the
   update request might still be running.
7. Screenshots wait briefly for loading overlays to clear. If the server gets
   stuck, a failed-test screenshot may still show a spinner, deliberately:
   that is evidence of the failure, not a false successful result.
8. The suite uses one login/session and moderately paced actions.

## Assignment coverage

- StarterKit: `BaseTest`, `LoginPage`, `NavigationBar`, `DriverFactory`, `ExtentManager`
- **Exactly 3 business page objects:** `PersonalDetailsPage`,
  `ContactDetailsPage`, `EmergencyContactsPage`
- `UpdateMyInfoTest` uses TestNG, **two DataProviders**, assertions, explicit
  waits and POM, with 12 test executions.
- Extent Spark HTML report, screenshots on PASS/FAIL/SKIP, Log4j2 logs.

## Notes

- `mvn clean test` builds all Java sources and runs the suite; `mvn clean` alone
  does not delete the separate `reports`, `logs`, and `screenshots` folders.
  To start with fresh evidence, clear those folders before a new run if needed.
- The write tests temporarily change account details and try to restore them.
  A generated emergency contact is removed after the test. The OrangeHRM demo
  is shared and its records/server responsiveness can change independently of
  the automation. Please avoid simultaneous runs on the same demo account.
- To run a read-only smoke check first:

```bash
mvn clean test -DallowWrites=false
```

- If your demo is exceptionally slow, increase the **explicit wait only**:

```bash
mvn clean test -Dwait.seconds=45
```

## Validation performed

- Java 21 source compilation was checked against local API-shape stubs.
- TestNG XML, POM XML, package layout, and ZIP content are checked.
- The live OrangeHRM website is not reachable from the build environment; a
  real end-to-end Chrome run must be completed on your Mac. Runtime behaviour
  on a changing public demo cannot be guaranteed in advance. Actual failures
  will remain visible rather than being marked as passes.


## 20 November update — Contact / Emergency locators

The Contact Details and Emergency Contacts page objects now locate the outer
`oxd-input-group` by exact CSS class token, avoiding a match on the nested
`oxd-input-group__label-wrapper`. The emergency add form waits for its visible
Name/Relationship inputs and Save control rather than an exact heading string.

Run normally: `mvn clean test`. Chrome, screenshots, reports and logs are
configured as before. The fixes are source-level and must be verified on the
actual OrangeHRM demo in your environment; do not regard them as proof of a
passing live test run.
