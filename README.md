# NSPay

# Install app
Use a local Java JDK, Maven, Node.js/npm, and MySQL server.
Install frontend dependencies from `angular-front-end` with `npm ci`.
Build the backend from the folder containing `pom.xml` with `mvn clean install`.

# Run app locally
Use Python 3 and the MySQL command-line client to provision a demo:
```bash
cp .env.example .env.local
python3 scripts/setup-db.py --env-file .env.local
```
The script prompts for your **existing MySQL administrator password**. It cannot recover a forgotten administrator password.
The defaults are database `payments`, application database username `payadmin`, and password `paypassword`, on `127.0.0.1:3306`.
Edit `.env.local` to customize them or the administrator account. Environment variables override values in the file.
For a remote application server, set `DB_USER_HOST` to its client host as recognized by MySQL; this is separate from `DB_HOST`, the database server address.
Env files contain literal `KEY=value` entries with optional surrounding quotes, without shell expansion.

`seed.sql` is a template rendered by the setup script. It creates the database, configures its application account, and creates `ACCOUNTS`, `USERS`, `TRANSACTIONS`, `AUDIT`, and Hibernate's user-ID sequence in that database.
All demo rows use reserved IDs starting at `900001`. Rerunning preserves existing rows and balances, adds missing demo rows, and sets the database account password to the configured value.
The seed also upgrades the previous investment schema and normalizes legacy account statuses. Other incompatible schemas require a separate migration. All grants are scoped to the selected database.
Use `--dry-run` to validate settings without connecting. If `--env-file` is omitted, the scripts use the project `.env` when present and otherwise use defaults.

## Application login credentials

These credentials are created by `seed.sql` and are separate from the MySQL database account.

| Username | Password | Role | Access |
| --- | --- | --- | --- |
| `admin` | `DemoPay123!` | Admin | User and account administration |
| `analyst` | `DemoPay123!` | Analyst | Account, transaction and audit views |
| `alice` | `DemoPay123!` | User | Funded account and customer features |
| `bob` | `DemoPay123!` | User | Funded account and customer features |
| `newuser` | `DemoPay123!` | User | Must change password on first login |

Passwords already changed through the app are preserved when the seed is rerun. Users subsequently created by an administrator initially use their username as their temporary password and must change it on first login.

Default MySQL credentials: username `payadmin`, password `paypassword`, database `payments`. These are database connection settings, not application logins.

Seeded data includes six accounts, payments, a paired IBAN transfer, audit events, two savings pockets, an onboarding profile, and a credit draft.

# Features
The customer dashboard uses Angular Material forms, steppers, dialogs, and progress indicators with a responsive NSPay theme. Its overview shows your own available balance, savings, investments, and recent activity.

| Feature | Persisted backend behavior |
| --- | --- |
| Savings | Named pockets with goals; deposits and withdrawals atomically update the account balance and activity history |
| Payments | Immediate IBAN transfers; scheduled transfers execute every 30 seconds while the backend runs, even with the browser closed |
| Invest | Real Twelve Data quotes and charts; app-managed holdings and EUR investment cash; atomic deposit, buy, sell and withdrawal |
| Onboarding | Contact details and profile plus local document text extraction; uploaded documents are not verified identities |
| Credit | Draft → documents → submitted review → administrator decision; no loan disbursement |
| Fraud screening | Persisted behavioral risk signals, explanations, history sample counts, and an administrator review queue |

The APIs live under `/api/finance`, `/api/investments` and `/api/security`. User ownership comes from the signed session token. Customer access to legacy account administration endpoints is restricted; administrators and analysts retain their workspace access.
Savings amounts and order values use decimal arithmetic, and account locks prevent concurrent debits from spending the same funds. The original account and transaction entities still use doubles; this is a demo, not a production ledger.

The previous browser-only savings and pending-transfer services have been removed. Existing browser data is offered as an export for reference, **not automatically trusted or imported as money**. New financial state is stored in MySQL. The browser still holds the session cookie and transient form/UI state.

The fraud engine is a local statistical anomaly baseline with limited-history fallback, amount deviation, new-recipient, and payment-frequency signals. It is monitoring-only, has no validated accuracy claim, and does not block transfers. `FraudScoringEngine` is the integration point for a trained model later.
Quotes and historical charts come from Twelve Data. Holdings and order fills are managed inside NSPay; no broker order or real security purchase occurs. Credit decisions are recorded without automatic loan disbursement. Savings pockets do not accrue interest. Production banking would additionally need a double-entry ledger, idempotency beyond investment commands, session hardening, migrations, real KYC/broker/lending integrations, and validated fraud models.

For local document scanning install `tesseract` (image OCR) and `poppler` (`pdftotext` for text-based PDFs). On macOS:
```bash
brew install tesseract poppler
```
Only fictional demo documents should be uploaded. PDF/PNG/JPEG content is checked and limited to 5 MB. Files are persisted as blobs under the selected database and downloaded through an ownership-checked API. Extracted text must be reviewed manually. Missing tools or unreadable scans produce an explicit scan status; scanned PDFs without embedded text need manual review.

Run backend tests with `mvn test` and the frontend production build with `npm run build`.

Start the backend from the folder containing `pom.xml`:
```bash
python3 scripts/run-backend.py --env-file .env.local
```
Start the frontend in a separate terminal:
```bash
cd angular-front-end
npm start
```
Open `http://localhost:4200`; the frontend proxies API requests to port `8082`.
Docker files are available as an optional alternative; local startup does not require containers.
Direct `mvn spring-boot:run` uses the same defaults but does not load an env file; export `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_HOST`, and `DB_PORT` yourself when using custom settings.

# API documentation
Swagger UI: `http://localhost:8082/swagger-ui.html`
Every application endpoint is documented under its feature tag, with request schemas, validation constraints, response types and business rules. Use **Try it out** on `POST /api/login/`, copy its returned `token`, click **Authorize**, and paste the token without the `Bearer` prefix. Protected calls then send it in the Authorization header. Investment commands require a fresh quote ID and a unique request key.
OpenAPI JSON: `http://localhost:8082/v3/api-docs`
Generate a local JSON file with:
```bash
curl http://localhost:8082/v3/api-docs -o openapi.json
```
# Actuator
Actuator endpoints are exposed on:
`http://localhost:8082/actuator`

# Market data and investing
Add `TWELVE_DATA_API_KEY=your-key` to the env file used by `scripts/run-backend.py` and restart the backend. The key stays on the server. Create a key at https://twelvedata.com; check the provider plan and display permissions for your presentation. No additional service or Docker container is needed.

The curated stock/ETF catalog has Technology, Consumer, Finance and ETF groups. Charts support 1D, 1W, 1M and 1Y. Quotes, charts and USD/EUR rates are fetched through the backend with timeouts and a 60-second cache; the adapter limits requests to eight per minute. Prices display their timestamp and market-open status. Missing credentials, stale data, provider failures and rate limits disable orders rather than supplying invented prices. Existing synthetic instruments remain in historical records but cannot be traded.

Deposit debits the main account and credits investment cash. Buy debits investment cash and adds shares. Sell credits proceeds at the reviewed quote and USD/EUR rate. Withdraw debits available investment cash and credits the main account. Held shares must be sold before their proceeds can be withdrawn. Money changes, orders and duplicate-request records commit in one transaction using an account lock. Investment requests require a unique `requestKey`; reusing a key with different contents is rejected. Server quotes expire after 60 seconds. Quoted reference fills have no fees or slippage.

# Account restrictions
Only `ACTIVE`, `FREEZE`, `SUSPENDED` and `BLOCKED` are used as account/profile statuses. Reporting an owned transaction creates a security report and freezes the account atomically. Existing suspensions or blocks are not weakened by a report. Administrators review reports or change status from Administration with a recorded reason. A confirmed case can impose a 30–90 day suspension; a second suspension automatically becomes `BLOCKED`. Repeated decisions and repeated reports do not create another suspension. Restoring `ACTIVE` does not reset the suspension count. Administrators can explicitly override a block.

A backend job expires suspensions every 30 seconds. Accounts with another open report return to `FREEZE`; otherwise they return to `ACTIVE`. Restricted accounts can sign in and inspect their history but cannot transfer money, move savings, fund investments or place orders. The status history records actor, reason, suspension count and expiry. Full card numbers are excluded from the administrator directory response; it contains only the last four digits.

`seed.sql` defines `ACCOUNT_RESTRICTIONS`, `SECURITY_REPORTS`, `STATUS_CHANGES`, `INVESTMENT_WALLETS` and `INVESTMENT_COMMANDS` in the selected payments database. Re-run setup once when upgrading. Existing indefinite suspensions become 30-day suspensions; previous synthetic orders and balances are preserved. Newly seeded databases contain no synthetic trades.
