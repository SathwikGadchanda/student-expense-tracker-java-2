# Practical lab file notes

Copy these notes into the institution's required lab format. Complete names, roll numbers, dates and actual contributions in your own records.

**Aim**  
Develop a Java desktop application to log campus lost and found reports, search records and document verified returns.

**Requirements**  
JDK 17, Maven, a graphical desktop and disk access for SQLite. The packaged JAR bundles its JDBC dependencies.

**Procedure**

1. Initialize the SQLite schema and enable foreign keys.
2. Open a Swing form and collect the type, title, category, location, date and description.
3. Validate the input and optionally decode a JPEG or PNG.
4. Bind validated values to a prepared statement, insert the report and refresh the board.
5. Search by keyword and optionally filter by type, category and status.
6. For an open found item, enter the claimant and a distinguishing detail.
7. In one transaction, update the item to Claimed and insert the claim.
8. Confirm the physical handover and write the Returned status and timestamp together.

**Expected result**  
The selected item appears in filtered results and shows the return timestamp after handover. A duplicate claim is rejected.

**Observation**  
Java 17 compilation and Maven packaging succeeded. Twelve JUnit integration tests passed. Record your own launch, search, claim, return and restart observations in the lab file.

| Member | Roll number | Actual contribution | Verification initials |
| --- | --- | --- | --- |
| 1 | | | |
| 2 | | | |
| 3 | | | |
| 4 | | | |
