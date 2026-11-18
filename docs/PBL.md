# Exhibition demonstration and viva

## Exactly four team members

Fill in the four approved students and their actual contributions. These are suggested speaking roles.

| Member | Suggested role | Demonstration |
| --- | --- | --- |
| Student 1 | Requirements and Swing forms | Introduce the problem and enter a report |
| Student 2 | Java model and search | Filter the report and explain records and enums |
| Student 3 | SQLite and JDBC | Record the claim and explain the transaction |
| Student 4 | Testing and documentation | Confirm the return and explain rollback and persistence |

Use demo mode and synthetic names for the exhibition. Each member should know the full flow.

## Two to three minute demo

- **0:00–0:30** Explain what campus staff currently need to record and show the board.
- **0:30–1:10** Create a found calculator report with today’s date and the lecture hall location. Show the optional photo control.
- **1:10–1:45** Search for the report and record a fictional claim, using a scratched cover as verification.
- **1:45–2:15** Confirm the return and inspect the stored timestamps.
- **2:15–2:45** Explain the repository interface, SQLite persistence and the 12 integration tests.

## Twenty viva questions

1. **What problem does the project solve?** A campus desk can find item reports and track verified handovers in one place.
2. **Why Java 17?** It supports records and modern language features while providing Swing and JDBC.
3. **Why Swing?** Java includes desktop forms, tables and dialogs in its standard library.
4. **What is a Java record?** A compact class for fixed data components with generated accessors and value methods.
5. **What is an enum?** A type with a fixed set of legal values, used here for status, category and report kind.
6. **Where is abstraction used?** ItemRepository describes storage operations independently of SQLite.
7. **Where is polymorphism used?** CampusService uses the repository interface and receives a concrete SQLite implementation.
8. **Where is encapsulation used?** The model validates input on construction and copies the draft’s photo bytes.
9. **Where is inheritance used?** CampusFrame extends JFrame and ReportDialog extends JDialog.
10. **What is JDBC?** The Java API for connecting to databases and running SQL.
11. **Why SQLite?** It persists data in a file and runs without a separate database server.
12. **Why prepared statements?** They bind data separately from SQL and avoid treating user text as part of a query.
13. **How are the tables related?** Each claim references an item through item_id, which also makes the relationship unique.
14. **What is a transaction?** A set of changes committed together or rolled back together.
15. **Why are claims transactional?** A report must never show Claimed if the corresponding claim record failed to save.
16. **How do you prevent duplicate claims?** The SQL update requires an open found item and claims.item_id is unique.
17. **How does search work?** Java combines a case-insensitive text match with optional type, category and status filters.
18. **How are photos stored?** The app validates JPEG or PNG bytes and saves them in a SQLite BLOB.
19. **What did you test?** Report operations, filters, validation, persistence, status rules, rollback and competing claims.
20. **What would you add next?** Operator sign-in, a multi-desk workflow and a way to resolve linked lost reports.

Bring the updated practical lab file. Record each member’s actual implementation and test contributions.
