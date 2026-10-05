# NSPay

NSPay is an Angular and Spring Boot fintech application for payments, savings, app-managed investments, identity verification, credit applications and administrator payment reviews. MySQL persists financial and review state; the frontend displays authenticated API views and live market updates.

**Project requirements and work allocation:** [PROJECT.md](PROJECT.md)

**API reference:** [Swagger UI](http://localhost:8082/swagger-ui/index.html) · [OpenAPI JSON](http://localhost:8082/v3/api-docs)

## Technology and project structure

| Layer | Implementation |
| --- | --- |
| Frontend | Angular 16, TypeScript 5, RxJS 7, Angular Material/CDK, SCSS |
| Backend | Java 17, Spring Boot 2.6.3, Spring MVC, Spring Data JPA/Hibernate, Bean Validation |
| Persistence | MySQL, HikariCP connection pool, transactional services |
| Authentication | Signed JWTs with JJWT; BCrypt password hashing |
| API documentation | springdoc-openapi 1.6.15; schemas, feature tags and bearer authentication |
| Market data | CuckooTrade REST/SSE by default; optional Twelve Data integration |
| Document extraction | Local Tesseract for images; Poppler `pdftotext` for PDFs |
| Verification | JUnit 5, Spring Boot/MockMvc integration tests; Angular production build |

```text
client/src/app/
  dashboard/             Application shell, sidebar and role-aware navigation
  finance/               Lazy-loaded customer and administrator views
  utils/                 HTTP services, session handling and API view types
src/main/java/com/nspay/
  config/                JWT filter, legacy ownership policy and OpenAPI configuration
  controllers/           Authentication, legacy accounts/users and payment endpoints
  services/              Account operations, transfers, transactions and audit
  finance/               Feature controllers, transactional services, entities and repositories
  validators/            Structural and payment business validation
scripts/                 Database setup, backend startup and sample-data population
seed.sql                 Database/user provisioning template, schema and initial records
```

## Local setup

Install Java 17, Maven, Node.js/npm, Python 3, a local MySQL server and the MySQL command-line client. Containers are optional; the normal workflow uses local processes.

```bash
cp .env.example .env
python3 scripts/setup-db.py
mvn package
python3 scripts/run-backend.py --jar
```

In a separate terminal:

```bash
cd client
npm ci
npm start
```

The frontend normally runs at [localhost:4200](http://localhost:4200); its `/api` development proxy targets the backend at `localhost:8082`. A custom frontend port does not change the backend or Swagger URL. `python3 scripts/run-backend.py` starts through Maven instead of using a packaged jar.

### Database configuration

Use one set of application database settings in `.env`:

```dotenv
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=payments
DB_USERNAME=payadmin
DB_PASSWORD=paypassword
DB_ADMIN_USERNAME=root
# DB_ADMIN_PASSWORD=your-existing-local-mysql-password
```

`DB_USERNAME` and `DB_PASSWORD` identify the application MySQL account, whose grants are scoped to `payments`. `DB_ADMIN_*` identifies an existing MySQL administrator and is used only by the setup script; the backend launcher removes those settings from its environment. For a local root account whose password is `root`, set `DB_ADMIN_PASSWORD=root`, or omit it and answer the secure prompt. `DB_URL` is unnecessary: Spring constructs the JDBC URL from the five application settings.

`DB_USER_HOST` controls the application account's allowed MySQL client host; `DB_HOST` identifies the database server. Both scripts default to the project `.env`, accept `--env-file PATH`, and allow environment variables to override file values. Entries are literal `KEY=value` strings with optional quotes, without shell expansion. Use `python3 scripts/setup-db.py --dry-run` to validate settings without connecting or printing credentials.

The setup script renders [seed.sql](seed.sql), creates the database/application account and imports initial records into the selected database. Reserved record IDs start at `900001`. Repeat imports add missing records while preserving existing passwords, profiles, financial balances and orders. Startup currently uses Hibernate `ddl-auto=update`; the seed includes explicit compatibility upgrades for earlier investment schemas. There is no general versioned migration framework.

**Application login credentials are in [seed.sql](seed.sql):** usernames are in the `USERS` inserts, and the initial password is documented immediately above them. Application logins are separate from MySQL credentials. The seed preserves passwords changed through the app.

### Document scanning and populated applications

On macOS, install the optional local extraction tools:

```bash
brew install tesseract poppler
```

Files remain saved when a scanner is unavailable, with an explicit scan status for manual review. PDF, PNG and JPEG uploads are checked by content and limited to 5 MB.

After an active regular customer has completed identity verification, populate complete credit applications through the normal authenticated API:

```bash
python3 scripts/populate-credit-requests.py
```

The default customer is Bob. The script upserts Professional development, Apartment renovation and Electric vehicle purchase applications, uploads sample identity/income PDFs and submits them as `UNDER_REVIEW`. Existing submissions are preserved; it does not approve identity, issue credit or move money. Use `--username` and `NSPAY_LOGIN_PASSWORD` when the login differs. A fresh database requires onboarding and administrator verification first.

Optional investment population:

```bash
python3 scripts/populate-investments.py
python3 scripts/populate-investment-history.py
```

The first script uses authenticated deposit/order APIs and preserves existing portfolios. The history script upserts older purchases, a partial sale and profit records using a persisted command key. Neither replaces existing account balances with arbitrary values. Browser population uses the configured local database; automated backend tests use disposable in-memory fixtures.

## Frontend decisions

- **Lazy-loaded feature module.** `FinanceModule` contains customer pages, onboarding, investments and administrator review tables. Keeping feature components out of the initial module reduces the initial bundle and shares Material controls across workflows.
- **Role-aware navigation and views.** The sidebar holds navigation and session identity. Administrators see payment review in Security, application review in Credit, a user directory in Administration and a separate Account limits page. Customer pages show owned records and relevant actions. Route guards improve navigation; server authorization remains authoritative.
- **Reusable status presentation.** `StatusBadgeComponent` maps access, verification and review states to labels, colors and SVG icons. Meaning does not depend on color alone. The administrator's own directory row has an empty actions cell.
- **Material forms and dialogs.** Credit/onboarding use steppers and validated forms. Continue draft restores all application fields; saving updates the same draft and preserves documents. Scheduling a transfer uses a calendar/time dialog and converts browser-local time to an instant before submission. Enter submits forms.
- **Bounded, responsive tables.** Account, payment and credit views use server search/filter/sort/pagination. Table overflow stays inside the card, with sticky actions where needed. Accounts shows count/EUR usage bars; Discover has its own scrolling stock list and horizontal category controls.
- **Live data without overwriting forms.** Overview balances refresh every 15 seconds. Investment prices and valuations use authenticated server-sent events. RxJS teardown closes subscriptions on navigation, and table requests cancel superseded requests.
- **Code-native charts.** `PriceChartComponent` renders SVG paths with pointer/touch inspection and keyboard navigation. Stock and aggregate-profit charts use the same presentation. Live views update each second; historical ranges come from the backend. Price/profit history tables were removed from the UI.
- **Backend-owned financial state.** Balances, pockets, schedules, holdings, applications and review state are persisted in MySQL. The browser retains the session cookie and transient form/UI state. Earlier browser-only savings/schedule data can be exported, but is not imported as money.

## Backend decisions

Controllers validate input and delegate business operations to services; repositories handle persistence. Customer ownership derives from the authenticated user ID, rather than a submitted user/account ID. Administrator feature endpoints check the current stored role. `AccountAccessPolicy` also restricts legacy account and transaction routes.

Financial commands use database transactions and pessimistic account locks. Two-account transfers acquire locks in ascending account-ID order to prevent opposite-direction lock cycles. Debit, credit and paired transaction entries commit together. Savings and investment operations follow the account-first lock order; investment cash, positions, orders and command records commit atomically. Review operations use the same account-first order so restrictions and eligibility checks cannot race money movement.

New financial entities use `BigDecimal` and decimal columns. EUR amounts typically use `DECIMAL(19,2)`; quantities/prices retain six decimal places and position costs retain eight. Legacy `AccountModel` balances and `TransactionModel` amounts still use `double`, with cent rounding in transfer operations. `TRANSACTIONS` records activity; it is not a double-entry accounting ledger.

`OwnedRecord` adds `id`, `userId`, `createdAt` and an optimistic-lock `version` to feature entities. Profiles, wallets, restrictions, command keys and position projections have explicit uniqueness constraints. Ownership references mostly use scalar IDs checked in services, rather than object graphs exposed in API responses. Public API views whitelist fields; passwords are write-only and document bytes are excluded from normal JSON.

Investment commands require a `requestKey`, unique per user. A fingerprint rejects reuse with different input; matching retries return the existing result. Quotes are immutable, shared MySQL records with a 60-second expiry. Ordinary immediate transfers do not currently have a comparable request-key mechanism.

Background jobs execute due scheduled transfers in batches of at most 50 every 30 seconds and expire suspensions every 30 seconds. Scheduling does not reserve funds: balance and account eligibility are checked at execution. Jobs run while the backend is active, independently of browser sessions.

### Authentication and access

`JWT_EXPIRATION_MS=86400000` gives a 24-hour session. Authenticated requests renew the `auth_token` cookie and return `X-Session-Token` for bearer clients. Password-change responses issue their own updated token. The current frontend reads its cookie to display identity and evaluate route guards; this is not an HttpOnly-session implementation.

Customers cannot read another customer's onboarding, documents, applications or transaction-report evidence by changing an ID. Staff-only score endpoints reject regular users. Administrators cannot inspect or approve their own identity, apply for their own credit or review their own application. User/account deletion endpoints return `405`; the Accounts interface edits advisory limits only. Access restrictions are managed through explicit security commands with reasons and audit history.

## Data models

| Table / model | Main fields and relationships | Purpose / constraints |
| --- | --- | --- |
| `USERS` / `UserModel` | Username, names, BCrypt password, role, `accountId`, password-change flag | Authentication; account assignment is validated by services. Roles: `Admin`, `Analyst`, `User` |
| `ACCOUNTS` / `AccountModel` | Unique card number/IBAN, holder, access status, balance, daily limits and counters | Main account; limit updates do not modify balance, identity or status |
| `TRANSACTIONS` / `TransactionModel` | Card, amount, timestamp, return code, type, counterparty IBAN | Owned activity and paired transfers; `(cardNumber,timestamp)` index supports history and daily aggregates |
| `AUDIT` / `AuditModel` | Actor username, action, timestamp | Records administrative and financial actions |
| `SAVINGS_POCKETS` / `SavingsPocket` | `userId`, name, decimal balance/target | Savings goals backed by main-account money movements |
| `SCHEDULED_TRANSFERS` / `ScheduledTransfer` | Owner, recipient, amount, due instant, state/failure reason | Durable future transfers; locked state prevents duplicate execution |
| `CUSTOMER_PROFILES` / `CustomerProfile` | Unique owner, contact/personal details, onboarding status, reviewer/time/note | Identity workflow with versioned administrator decisions |
| `CREDIT_APPLICATIONS` / `CreditApplication` | Owner, amount, monthly income, term, purpose, status, review note | Draft → submitted review → approval/decline |
| `CREDIT_DOCUMENTS` / `CreditDocument` | Owner, optional application ID, kind, filename/type, SHA-256, blob, scan state/text | Identity and income evidence; ownership-checked downloads |
| `INVESTMENT_WALLETS` / `InvestmentWallet` | Unique owner, decimal cash, reconstruction flag | Available investment cash; guards one-time position reconstruction |
| `INVESTMENT_POSITIONS` / `InvestmentPosition` | Owner/provider/symbol, quantity, remaining cost basis, realized gain | Unique `(userId,provider,symbol)`; portfolio reads avoid rescanning orders |
| `TRADE_ORDERS` / `TradeOrder` | Owner, side/symbol/quantity, EUR total, market price, FX, source timestamps, quote/provider | Immutable fill record; `(userId,createdAt,id)` index supports paged history |
| `INVESTMENT_COMMANDS` / `InvestmentCommand` | Owner, request key, fingerprint, optional order ID | Unique `(userId,requestKey)` for idempotent commands |
| `MARKET_QUOTES` / `MarketQuote` | Quote UUID, provider/symbol, expiry, serialized price snapshot | Executable quotes shared across backend instances; lookup/expiry indexes |
| `PORTFOLIO_PERFORMANCE` / `PortfolioPerformance` | Owner/day, realized/unrealized/total gain, holdings value, wallet version | Unique `(userId,day)`; outdated snapshots cannot replace newer state |
| `RISK_ASSESSMENTS` / `RiskAssessment` | Owner/recipient/amount, score/level, explanations, samples, model version | Persisted transfer screening from `FraudScoringEngine` |
| `PAYMENT_SIGNALS` / `PaymentSignal` | Unique transaction, owner, seven metrics, daily usage/limit snapshot, reporting/review state and notes | Paged administrator triage; indexes on score, review/time and owner/review/ID |
| `SECURITY_REPORTS` / `SecurityReport` | Unique transaction, reporting customer, reason, resolution and public/private notes | A report is evidence for review; it does not restrict access |
| `ACCOUNT_RESTRICTIONS` / `AccountRestriction` | Unique owner, suspension count, expiry, reason | Restriction metadata; access state remains on `ACCOUNTS` |
| `STATUS_CHANGES` / `StatusChange` | Owner, actor, previous/new state, reason, expiry/count, score/transaction snapshot and notes | Persistent history and customer security activity |

### Identity and credit state transitions

Identity progresses through `NOT_STARTED` → `PROFILE_COMPLETE` → `UNDER_REVIEW` → `VERIFIED` or `DECLINED`. Details can be submitted once. After identity submission, onboarding closes and its Overview banner disappears; direct route access redirects to Overview. Verified or declined records stay locked to customer edits. Administrator re-review uses the existing record and requires its current version.

Credit uses `DRAFT` → `UNDER_REVIEW` → `APPROVED` or `DECLINED`. Draft creation/editing, supporting-document upload and submission require a verified identity and an active account. Submitted applications cannot be edited. Administrator approval records the decision but does not disburse a loan. Extraction never verifies identity or approves credit automatically.

### Advisory daily payment limits

`dailyTxLimit` is the number of payments per day; `dailyTxSumLimit` is their total EUR amount. `DailyPaymentUsage` derives `dailyTx` and `dailyTxSum` from successful card payments and outgoing transfers in the **Europe/Bucharest calendar day**. Incoming, failed and internal savings/investment movements are excluded. Legacy `PAYMENT` activity is included as a card payment.

Reads use the indexed ledger aggregate; successful payment writes synchronize the stored counters while holding the account lock. Day boundaries therefore work after downtime without a midnight reset job. Customers see Overview/Payments warnings at 80%, a reached warning at 100%, and an exceeded warning above 100%. These thresholds do not reject payments or change account status; ordinary balance and restriction checks still apply.

`PUT /api/account-limits/{accountId}` accepts only the count and EUR thresholds. It commits the new limits, refreshed open payment scores and audit entry together. Historic reviewed decisions retain their snapshots. The account list returns masked cards, live usage, server filters/sorting and pagination capped at 100 rows.

### Security review and scoring

Account access uses only `ACTIVE`, `FREEZE`, `SUSPENDED` and `BLOCKED`, presented as Active, Frozen, Suspended and Blocked. A customer fraud report leaves access unchanged. An administrator can dismiss evidence, freeze, suspend for 30–90 days or block the account. A second suspension becomes blocked. Restoring Active does not erase the suspension count; a suspension that expires returns to Active. An open report does not independently impose a freeze.

`PAYMENT_SIGNALS` stores the following 0–100 metrics:

| Metric | Calculation |
| --- | --- |
| Amount deviation | Payment amount relative to the median of up to 50 preceding successful transactions |
| Timing | Rarity of its UTC hour within the preceding history; limited-history fallback |
| Frequency | Preceding-hour payment count, capped at 100 |
| Recipient novelty | Familiar recipient 0; new recipient 50 |
| Reporting | This payment's active report and distinct customers reporting the same recipient |
| Daily limit usage | Larger of daily count/EUR usage ratios at payment time: 0 below 80%, 25–59 near the threshold, 60 at 100%, 100 at 120% or above |
| Misalignment | `min(100, round(amount×0.35 + timing×0.15 + frequency×0.15 + recipient×0.15 + reporting×0.20 + limits×0.25))` |

These are explainable review heuristics, not validated fraud probabilities. `FraudScoringEngine` is the extension point for a future trained model; the current transfer baseline uses amount, frequency and recipient history, with an additional limit-usage contribution.

Queue requests index at most 200 previously unscored payments. Score filters, whitelisted sorting, pagination and related-report inspection run on the backend. Related recipient reporting scores refresh after commit to avoid cross-account lock cycles. A payment review locks the account before its signal; decision, restriction, report resolution, score snapshot, notes, history and audit commit together. Repeated decisions return a state conflict. Dismissal does not clear an unrelated restriction; payment review does not weaken an existing suspension/block.

Public notes appear in every customer's Security activity. Private notes, reasons and transaction references are returned only to the affected customer and authorized administrators. Unrelated customers receive only the whitelisted public event fields. Full risk evidence remains administrator-only.

## Market data, holdings and live valuation

Configure the default provider in `.env`:

```dotenv
MARKET_PROVIDER=cuckootrade
CUCKOOTRADE_SEED=42
CUCKOOTRADE_GENERATION=1
CUCKOOTRADE_USD_EUR_RATE=0.92
CUCKOOTRADE_CLOCK=demo
```

The CuckooTrade adapter uses synthetic USD prices and a configured EUR conversion; holdings and fills are managed inside NSPay. No broker executes orders. Provider/dataset provenance is retained on positions/orders, so changing the source does not silently reprice incompatible holdings. Optional settings are `MARKET_PROVIDER=twelve-data` with `TWELVE_DATA_API_KEY`, or custom `CUCKOOTRADE_BASE_URL`/`CUCKOOTRADE_STREAM_URL`. `CUCKOOTRADE_CLOCK=real` selects the session-based mode; `MARKET_STREAMING_ENABLED=false` disables streaming.

Deposit debits the main account and credits investment cash. Buy reduces cash and adds shares. Sell removes shares and credits proceeds; withdrawal moves available cash back to the main account. Reviewed quotes remain immutable for 60 seconds, even while subsequent ticks change the chart. Missing/stale executable prices reject new orders without substituting a different provider or price clock.

Portfolio calculations use average-cost accounting:

```text
holding value       = quantity × current EUR price
unrealized gain     = holdings value − remaining cost basis
total gain          = realized gain + unrealized gain
portfolio value     = investment cash + holdings value
```

Partial sales allocate the remaining cost basis and record realized gain. Allocation percentages exclude cash. Deposits/withdrawals are excluded from profit. If a holding lacks a compatible current price, valuation becomes `INCOMPLETE` and aggregate values are null rather than treating its price as zero.

The backend shares two upstream SSE connections across the 11-stock catalog, keeps at most 600 ticks per symbol and broadcasts authenticated `/api/investments/stream` frames once per second. One cached accounting baseline per connected user refreshes every 15 seconds and is invalidated after investment commands. Per-tick valuation runs in memory; the daily UTC profit record is upserted during baseline refresh, rather than on every tick.

Browser streams have bounded writer queues, a 200-client backend limit and a 20-minute emitter timeout. Slow consumers skip intermediate frames. Historical REST loading coalesces requests, uses shared expiring quote records and bounded chart caches. These measures reduce database/provider work; larger deployments would need coordinated feed ownership and rate limiting across replicas.

During a feed interruption, recorded cash and holdings stay in MySQL. The UI retains the last valid valuation when accounting state is unchanged, and pauses new orders until fresh prices arrive. **Retry** reconnects the browser subscription. For persistent failures, check connectivity or restart the backend; rerunning seed/setup is unnecessary. On macOS, `caffeinate -di` can prevent laptop sleep during a presentation.

## API documentation and principal routes

Open the running backend's [Swagger UI](http://localhost:8082/swagger-ui/index.html). Every application route has a summary, behavior description and authentication/error documentation; composed responses and command DTOs describe their data models.

Use **Try it out** for `POST /api/login/`, copy the returned token, choose **Authorize** and paste the token without a `Bearer` prefix. Swagger then sends the bearer header. The [OpenAPI document](http://localhost:8082/v3/api-docs) is generated from the current controllers and schemas.

| Route family | Responsibility |
| --- | --- |
| `/api/login/`, `/api/users/me`, `/api/users/me/password` | Authentication, owned identity and password changes |
| `/api/transfers` | Atomic immediate IBAN transfers |
| `/api/finance/overview`, `/pockets`, `/scheduled-transfers` | Owned dashboard, savings and scheduled payments |
| `/api/finance/onboarding` | One-time identity submission and administrator reviews |
| `/api/finance/credit`, `/credit/reviews`, `/documents` | Owned drafts/submissions, administrator review queue and evidence |
| `/api/investments`, `/quotes`, `/{symbol}/quote`, `/{symbol}/chart`, `/stream` | Holdings, executable stock prices, charts and live valuation |
| `/api/investments/deposit`, `/withdraw`, `/orders`, `/performance` | Idempotent cash/orders, paged history and owned daily profit |
| `/api/security/users`, `/payments`, `/transactions/{id}/evidence` | Administrator directory, scored queue and payment evidence |
| `/api/security/transactions/{id}/report`, `/review`, `/users/{id}/status` | Customer reporting and explicit administrator decisions |
| `/api/account-limits`, `/{accountId}` | Paged advisory usage and limits-only updates |

In rows with a shared prefix, subsequent paths are relative to that prefix; consult Swagger for full method/path definitions. Health is available at [Actuator health](http://localhost:8082/actuator/health).

## Verification and implementation boundaries

```bash
mvn test
cd client
npm run build
```

Backend integration coverage includes ownership/role checks, atomic money operations, conflicting/idempotent investment commands, onboarding locks, credit eligibility, security-note visibility, report/review behavior, advisory limit usage and immutable balances during limit editing. `ApiDocumentationTest` checks that every application mapping appears in OpenAPI with a description and authentication responses. Browser verification/population uses the local MySQL database so records remain visible in the UI.

The current system records reference-price investment fills, manually reviewed identity/credit decisions and savings goals. It does not implement brokerage settlement, dividends/corporate actions, loan disbursement, savings interest or a double-entry ledger. Versioned migrations, broader payment idempotency, hardened session handling and validated fraud models remain separate engineering work.
