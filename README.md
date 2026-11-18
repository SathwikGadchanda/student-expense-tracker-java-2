# Campus Lost and Found System

A Java 17 desktop application for running a campus lost and found desk. Staff can file a lost or found report, attach a photo, search the board, record an ownership claim and log the handover.

## Run on Windows

Install JDK 17 and Maven 3.9 or later, then open a terminal in the repository.

```powershell
mvn clean verify
java -jar target/campus-lost-found.jar --demo
```

Run without `--demo` for a persistent workspace. The normal database is `%USERPROFILE%\.campus-lost-found\campus.db`. Use `-Dcampus.db=PATH` before `-jar` for another database location. The packaged JAR includes SQLite JDBC and can run offline. The launcher is `run-windows.bat`.

## Demonstration

1. Open demo mode. Create a **Found** report for a blue calculator in Lecture hall 2.
2. Search for **calculator**. Try the type, category and status filters.
3. Select the report and choose **Record claim**. Enter a fictional claimant and a distinguishing detail.
4. Choose **Confirm return** after the simulated handover. The status changes to **Returned** and SQLite retains the claim and return timestamps.

Demo mode creates a fresh temporary database with fictional reports each time. Use a separate named database to demonstrate persistence across normal restarts.

## Java and database design

- **Swing** handles the report form, report board and claim dialogs.
- **Records and enums** model reports and legal states. The `ItemRepository` interface separates persistence from business logic.
- **CampusService** validates photos, filters reports and records timestamps. **SqliteRepository** uses JDBC prepared statements and try-with-resources.
- **SQLite** stores reports in `items` and ownership records in `claims`. The claim's primary key is also a foreign key to the report, allowing at most one claim per item.
- An open found report can become claimed and then returned. Each transition updates the report and claim in a single transaction. Lost reports remain searchable reference records.
- Photos are optional JPEG or PNG files up to 4 MB. They are stored as BLOBs in SQLite.

The app is designed for a local desk on one computer. It has no in-app login or network service. The operating system controls access to the database. Use fictional names and records for the public demo.

## Verification

Java 17 and Maven successfully built the executable JAR. The 12 integration tests passed, including creation, editing, combined filters, photo validation and persistence, claim and return rules, rollback, and a race between two claims.

See [PBL demonstration and viva](docs/PBL.md), [lab file notes](docs/LAB_LOG.md), and [design notes](docs/DESIGN.md). The [PBL submission release](https://github.com/SathwikGadchanda/student-expense-tracker-java-2/releases/tag/pbl-v1.0.0) contains the packaged JAR, PowerPoint, report, walkthrough and test results after the packaging workflow succeeds.
