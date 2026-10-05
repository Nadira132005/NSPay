# NSPay — Requirements, Specification and Tasks

## 1. Project objective

Build a fintech application in which customers manage payments, savings and investments, complete identity verification, request credit and report suspicious payments. Administrators review identity, credit applications and payment evidence, set account restrictions and adjust advisory daily limits. Analysts have access to their read-only workspace and audit records.

The application should support a clear classroom presentation while retaining realistic authorization, persistent state and transactional behavior. Financial state belongs to the backend; the interface should expose useful actions, readable statuses and current balances without decorative placeholder text.

Related documentation:

- [Technical architecture and local setup](README.md)
- [Database schema and initial login credentials](seed.sql)
- [Swagger UI](http://localhost:8082/swagger-ui/index.html)
- [OpenAPI specification](http://localhost:8082/v3/api-docs)

## 2. Roles and permissions

| Role | Responsibilities and access |
| --- | --- |
| Customer (`User`) | View owned accounts/activity, send or schedule payments, manage savings/investments, complete onboarding, submit eligible credit applications and report owned transactions |
| Administrator (`Admin`) | Review customer identity and credit applications, inspect payment scores/evidence, record restrictions, change daily limits and view administrative/audit information |
| Analyst (`Analyst`) | Read permitted account/user/transaction information and audit logs; no administrator money, limit or restriction commands |

Administrators cannot inspect or decide their own identity or review their own credit application. Their own user-directory actions cell is empty. Account and user deletion are unavailable. Customer ownership is checked on the server using the authenticated user ID, including document downloads and resources addressed by IDs.

## 3. Functional requirements

| ID | Requirement | Acceptance condition |
| --- | --- | --- |
| R01 | Authentication and session management | Customers and staff can sign in; Enter submits the login form; authenticated actions renew a 24-hour session; required password changes are enforced |
| R02 | Account overview | Show the signed-in user's available balance, savings and recent activity; refresh balances without resetting forms |
| R03 | Immediate and scheduled transfers | Debit/credit and activity entries commit together; scheduling uses a calendar/time dialog; funds and restrictions are checked at execution |
| R04 | Savings | Create named pockets with goals; deposits/withdrawals move actual main-account funds; closing returns the remaining balance |
| R05 | Stock discovery and charts | Search and filter instruments by category; show individual prices and live/historical charts in a responsive layout with contained scrolling |
| R06 | App-managed trading | Deposit, buy, sell and withdraw against persisted cash/holdings; calculate values from server quotes; matching retries do not execute investment commands twice |
| R07 | Investment performance | Show current holdings value, allocations, realized/unrealized gain and aggregate profit; deposits/withdrawals do not count as profit |
| R08 | Onboarding | Overview offers a compact unfinished-onboarding banner; completed steps cannot be reopened or edited; administrator verification is required |
| R09 | Customer credit applications | Restore all draft fields, retain documents during edits and submit complete applications; require verified identity and Active access for all write operations |
| R10 | Administrator credit review | Credit displays a paged/filterable/sortable application queue with document inspection and approve/decline controls; complete local submissions are available for presentation |
| R11 | Customer fraud reporting | A report records a case without freezing the account; customers see action confirmation only after an administrator decision |
| R12 | Administrator payment review | Security displays all-user evidence and scored payments with filters/sorting; only administrators receive payment risk signals and related report details |
| R13 | Restrictions and security activity | Use Active, Frozen, Suspended and Blocked access states; suspensions last 30–90 days; a second suspension blocks; public/private notes have separate visibility |
| R14 | Daily payment limits | Account limits shows count/EUR thresholds and usage; administrators can change only thresholds; warn at/over limits and raise scores without rejecting payments or changing status |
| R15 | Staff navigation and status presentation | Distinct icons/colors for access, verification and review states; role-relevant sidebar links; credit review belongs in Credit and payment review in Security |
| R16 | Setup, persistence and API documentation | Local MySQL setup is repeatable; existing records/passwords/balances survive reseeding; all application API calls are documented in Swagger |

## 4. Non-functional requirements

| ID | Requirement | Implementation / validation target |
| --- | --- | --- |
| N01 | Atomic operations | Transactional services commit balances, positions, activity and related command/decision records together; failed commands leave no partial financial changes |
| N02 | Concurrent correctness | Pessimistic account locks serialize money operations; two-account transfers lock in ascending ID order; version checks reject stale identity decisions |
| N03 | Authorization and IDOR protection | Resolve ownership from the authenticated session; check stored staff roles; test cross-user reads/writes and self-review rejection |
| N04 | Data privacy | Mask cards in administrator views; exclude passwords/document blobs from normal JSON; unrelated customers never receive private notes, reasons or transaction references |
| N05 | Bounded resource use | Server pagination is capped; score history/backfill, chart caches, tick history and SSE writer queues are bounded |
| N06 | Responsive and accessible UX | Desktop/mobile navigation, keyboard-capable forms/charts, labeled controls, readable disabled states and icons as well as colors |
| N07 | Live-update resilience | Reconnect interrupted price streams; preserve stored holdings/cash; keep last valid valuations when accounting state permits; reject orders without fresh valid quotes |
| N08 | Local operation | Run frontend, Java backend and MySQL as local processes; use local extraction tools; additional Docker services are not required |
| N09 | Repeatable sample data | Use reserved seed IDs and idempotent/upsert population; preserve existing customer decisions, financial balances and changed passwords |
| N10 | Technical documentation and verification | Maintain README, project specification and generated OpenAPI; verify backend behavior and frontend production compilation |

## 5. Technical specification

### 5.1 Architecture

| Layer | Specification |
| --- | --- |
| Browser | Angular 16 / TypeScript, RxJS, Angular Material/CDK, SCSS; lazy-loaded finance pages and shared SVG chart/status components |
| HTTP API | Spring MVC controllers, validated request DTOs, authenticated JSON views and SSE investment stream |
| Business layer | Java 17 / Spring Boot; transactional services for payments, savings, trading, onboarding, credit, risk review and limits |
| Persistence | MySQL with JPA/Hibernate repositories, account locks, optimistic entity versions and uniqueness/index constraints |
| External data | Backend CuckooTrade REST/SSE adapter; optional Twelve Data provider; clients do not call providers directly |
| Background work | Due scheduled transfers and suspension expiry approximately every 30 seconds; shared live-price broadcasts every second |
| Local tools | Python setup/population scripts; Tesseract and Poppler for document extraction |

The backend listens on port `8082`. The standard frontend development server listens on `4200` and proxies `/api` to the backend. The default database is `payments`, accessed by the application MySQL account described in `.env.example`. Database credentials and application login credentials are separate.

### 5.2 Principal data models

Most feature models extend `OwnedRecord`: primary key, owner ID, creation timestamp and optimistic-lock version. Relationships are represented primarily by scalar IDs checked in services.

| Domain | Models / tables | Important data and invariants |
| --- | --- | --- |
| Identity and accounts | `UserModel`, `AccountModel`, `CustomerProfile` | Roles and BCrypt passwords; account balance/IBAN; unique owned profile; access and verification states remain separate |
| Payment activity | `TransactionModel`, `ScheduledTransfer`, `AuditModel` | Successful/failed activity, paired transfer entries, due/state/failure metadata and actor/action history |
| Savings | `SavingsPocket` | Owned decimal balance, goal and name; moves are backed by main-account funds |
| Credit | `CreditApplication`, `CreditDocument` | Purpose/amount/income/term/state; linked identity/income evidence; file hash, content, extracted text and scan status |
| Trading | `InvestmentWallet`, `InvestmentPosition`, `TradeOrder`, `InvestmentCommand` | Cash, quantity, remaining basis, realized gain, fill provenance and unique per-user retry key |
| Market/performance | `MarketQuote`, `PortfolioPerformance` | Immutable expiring executable quote; unique daily profit snapshot with wallet version |
| Security | `RiskAssessment`, `PaymentSignal`, `SecurityReport`, `AccountRestriction`, `StatusChange` | Scores and evidence, unique report/payment cases, suspension metadata, atomic decisions and separated public/private notes |
| Advisory limits | Fields on `AccountModel`; `DailyPaymentUsage` response | Daily count/EUR thresholds, ledger-derived usage, percentages, warning level, limit score and next reset instant |

New monetary models use decimal arithmetic. EUR values normally have two decimal places; stock quantities/prices retain six and position cost basis retains eight. Legacy main-account and transaction amounts still use doubles and cent rounding. The activity model is not a double-entry ledger.

Key uniqueness rules include one profile/wallet/restriction per user, one signal/report per transaction, one command per `(userId,requestKey)`, one position per `(userId,provider,symbol)` and one performance snapshot per `(userId,day)`. Indexed history and queue queries support bounded reads; see [README.md](README.md#data-models) for the complete model table.

### 5.3 Workflow states and invariants

**Identity:** `NOT_STARTED` → `PROFILE_COMPLETE` → `UNDER_REVIEW` → `VERIFIED` or `DECLINED`. Saving personal details locks that step. Identity submission closes customer onboarding; subsequent review uses the same saved details/document. Customers cannot edit verified or declined records.

**Credit:** `DRAFT` → `UNDER_REVIEW` → `APPROVED` or `DECLINED`. Creation/editing, uploads and submission require an Active, verified customer. Submitted records are immutable to customers. Approval records a decision without disbursing funds.

**Account access:** `ACTIVE`, `FREEZE`, `SUSPENDED`, `BLOCKED`. Only explicit administrator decisions impose restrictions. A suspension lasts 30–90 days and expiry restores Active. A second suspension becomes Blocked; restoring Active does not reset the count. Customer reporting and advisory-limit consumption do not independently change status.

**Payment review:** New signals are `OPEN`. An administrator can dismiss or record Freeze/Suspended/Blocked. The decision, score snapshot, reports, restriction, notes, history and audit are committed together. Repeated review returns a conflict; dismissing a payment does not clear an unrelated restriction.

**Investment commands:** Main account ↔ investment cash; cash ↔ positions through buy/sell. Commands use server quote IDs and unique request keys; conflicting retries are rejected. Orders are reference-price fills inside NSPay, without brokerage execution.

### 5.4 Daily limits and review metrics

Successful card payments and outgoing transfers consume the Europe/Bucharest calendar day's count and EUR sum. Incoming, failed and internal savings/investment movements are excluded. Indexed ledger aggregates remain authoritative after midnight or backend downtime.

Customer warnings start at 80%, change to reached at 100%, and exceeded above 100%. Admin limit edits affect only the thresholds, refresh open payment scores and record audit history. Sufficient funds and ordinary access restrictions are still required for money movements.

The payment queue presents amount deviation, timing, frequency, recipient novelty, customer reporting, daily limit usage and combined misalignment. Each metric is 0–100. The combined calculation is:

```text
min(100, round(
  amount × 0.35 + timing × 0.15 + frequency × 0.15
  + recipient × 0.15 + reporting × 0.20 + limits × 0.25
))
```

Daily limit usage contributes zero below 80%, approximately 25–59 near the threshold, 60 when reached and 100 at 120% usage or higher. It uses the greater of count and EUR ratios. Scores support administrator triage; the implemented baseline does not claim validated fraud-prediction accuracy. `FraudScoringEngine` is the extension point for a trained service.

### 5.5 Live investments

Stock charts display Live, 1D, 1W, 1M and 1Y ranges. Portfolio value is cash plus quantity × current EUR prices. Unrealized gain subtracts remaining cost basis; total profit combines unrealized and realized gain. Deposits and withdrawals are excluded from profit.

CuckooTrade supplies synthetic prices; the default USD/EUR rate is configured. Source/dataset provenance prevents silently revaluing incompatible holdings. Two upstream SSE feeds are shared for the catalog, with bounded tick history. The backend broadcasts per-user valuations every second, refreshing accounting baselines approximately every 15 seconds. Incomplete pricing returns an incomplete valuation; stale quotes cannot execute orders.

### 5.6 API boundaries

| Feature | Principal API prefix / operation |
| --- | --- |
| Authentication | `/api/login/`, `/api/users/me`, `/api/users/me/password` |
| Immediate payments | `POST /api/transfers` |
| Dashboard, savings, scheduling | `/api/finance/overview`, `/api/finance/pockets`, `/api/finance/scheduled-transfers` |
| Onboarding and credit | `/api/finance/onboarding`, `/api/finance/credit`, `/api/finance/documents` |
| Credit review queue | `GET /api/finance/credit/reviews` |
| Investing | `/api/investments`, quotes/charts/orders/performance and `/api/investments/stream` |
| Payment evidence and decisions | `/api/security/payments`, `/api/security/transactions/{id}/evidence`, `/api/security/transactions/{id}/review` |
| Customer reports and account status | `/api/security/transactions/{id}/report`, `/api/security/users/{id}/status` |
| Advisory account limits | `GET /api/account-limits`, `PUT /api/account-limits/{accountId}` |

Swagger provides complete methods, paths, DTO constraints and responses. Ownership and role rules apply equally to browser and direct API requests.

## 6. Work allocation for two project members

Use **Member 1** and **Member 2** as placeholders for the actual names. Each owns **12 tasks totaling 36 effort points**. Points are relative estimates of implementation, integration, verification and documentation effort. Responsibilities are split by layer; both members collaborate at API handoffs and review each other's contracts.

### Member 1 — Frontend and UX

| Task | Points | Requirements | Deliverable / completion criterion |
| --- | ---: | --- | --- |
| F01 — Frontend setup and API client | 2 | R01, R16 | Angular setup, development proxy, typed services and session integration |
| F02 — Shell, navigation and authentication UX | 3 | R01, R15 | Responsive sidebar, role-aware links, avatar placement, Enter submission and password-change flow |
| F03 — Overview and live account feedback | 3 | R02, R14 | Balance/activity view, background refresh and advisory daily-limit warnings |
| F04 — Payments and scheduling controls | 3 | R03 | Validated transfer/review forms, calendar/time dialog, progress and failure feedback |
| F05 — Investment discovery and charts | 4 | R05, R07 | Search/categories, contained scrolling, responsive SVG stock/profit charts and keyboard/pointer interaction |
| F06 — Savings workflows | 3 | R04 | Pocket creation, goals/progress, deposit/withdraw and close confirmations |
| F07 — Trading and live-stream UX | 4 | R06, R07 | Deposit/withdraw/order controls, quote confirmation, holdings/performance updates and reconnect states |
| F08 — Onboarding and verification UX | 3 | R08, R15 | Overview banner, locked stepper states, document submission and administrator review controls |
| F09 — Customer credit and administrator queue | 3 | R09, R10 | Complete draft restoration, eligibility-disabled form, uploads and review table/dialog |
| F10 — Security reporting and payment review | 3 | R11–R13 | Customer reports/activity, staff score filters/evidence/decisions and privacy-aware note presentation |
| F11 — Account limits and status presentation | 3 | R14, R15 | Limits-only editor, usage bars, masked cards, colored/icon badges and empty self-actions cell |
| F12 — Frontend verification and documentation | 2 | N06, N10 | Production build, desktop/mobile and keyboard checks, frontend architecture notes and API-contract review |
| **Total** | **36** | | **12 tasks** |

### Member 2 — Backend, persistence and API

| Task | Points | Requirements | Deliverable / completion criterion |
| --- | ---: | --- | --- |
| B01 — Backend setup and database provisioning | 2 | R16 | Spring Boot/MySQL configuration, shared env settings, repeatable schema/seed and local startup scripts |
| B02 — Authentication and authorization | 3 | R01, N03 | BCrypt/JWT, sliding session, required-password flow, stored-role and ownership checks |
| B03 — Account/activity views and audit | 3 | R02, R15 | Owned overview, masked staff directory, persistent activity and audit records |
| B04 — Atomic payments and durable scheduling | 3 | R03, N01–N02 | Ordered account locks, paired transfer records, due-job execution and failure state |
| B05 — Market adapter and executable quotes | 4 | R05, N05, N07 | CuckooTrade REST/SSE integration, provider provenance, quote expiry and bounded shared caches |
| B06 — Savings persistence and transactions | 3 | R04 | Owned pockets, decimal balances and atomic account/pocket/ledger operations |
| B07 — Trading accounting and performance | 4 | R06, R07 | Cash/positions/orders, average-cost accounting, idempotency, durable profit and live valuation |
| B08 — Onboarding and document processing | 3 | R08, N03–N04 | Immutable steps, versioned staff decisions, self-review guard, checked uploads and local extraction |
| B09 — Credit lifecycle and administrator review | 3 | R09, R10 | Eligibility gates, owned draft updates, document completeness, paged queue and atomic review |
| B10 — Risk, reports, restrictions and privacy | 3 | R11–R13 | Stored metrics, evidence/decision APIs, no report-triggered freeze, escalation/expiry and public/private projections |
| B11 — Advisory daily limits and protected accounts | 3 | R14, N01–N02 | Ledger-derived usage, threshold-only updates, score contribution, audit and retired deletion APIs |
| B12 — Backend verification, OpenAPI and sample data | 2 | R10, R16, N10 | Integration/authorization checks, complete Swagger documentation, credit-data upserts and backend model notes |
| **Total** | **36** | | **12 tasks** |

The frontend member reviews API response usability and error handling; the backend member reviews frontend handling of ownership, invalid/stale state and private data. These reviews belong to F12 and B12 rather than creating a third allocation.

## 7. Milestones and task dependencies

| Milestone | Member 1 tasks | Member 2 tasks | Exit condition |
| --- | --- | --- | --- |
| M1 — Foundation | F01, F02 | B01, B02 | Both layers run locally; sign-in, session and route permissions work |
| M2 — Accounts and payments | F03, F04, F06 | B03, B04, B06 | Persistent account/savings state, atomic transfers and scheduled execution are integrated |
| M3 — Investments | F05, F07 | B05, B07 | Server-priced orders, holdings and live individual/aggregate charts work together |
| M4 — Identity and credit | F08, F09 | B08, B09 | Locked onboarding, verified-only applications and administrator credit review are integrated |
| M5 — Security and advisory limits | F10, F11 | B10, B11 | Report/review separation, restrictions/privacy and non-blocking daily limits are integrated |
| M6 — Verification and handover | F12 | B12 | Relevant checks pass; technical docs/OpenAPI and local presentation data are available |

API DTOs, status names, error responses and note visibility should be agreed before connecting each feature. Investment UI depends on executable quotes and stream contracts; credit UI depends on eligibility and document ownership; limit warnings depend on the backend's daily-usage projection.

## 8. Acceptance scenarios

1. **Local setup:** Provision the selected MySQL database, start both layers and sign in using credentials referenced by `seed.sql`. Repeat setup without replacing changed passwords or balances.
2. **Payment consistency:** Complete an immediate transfer and verify matching debit/credit/activity. Reject insufficient funds without partial changes. Execute a due scheduled transfer with the browser closed.
3. **Savings:** Add money to a pocket, withdraw and close it; confirm main-account and pocket balances reconcile.
4. **Trading:** Deposit account funds, buy/sell using server quotes and withdraw cash. Retry an investment command with the same key and confirm no duplicate execution. Verify stock and aggregate-profit updates.
5. **Onboarding:** Save details once, submit identity and verify completed steps cannot be reopened. Reject an administrator's self-review and another customer's direct resource access.
6. **Credit:** Keep application controls disabled for unverified or restricted customers. Submit a complete verified customer's application and inspect its documents in the administrator queue. Approve/decline it without an automatic money disbursement.
7. **Report versus decision:** Report an owned payment and confirm the account stays Active. Record an administrator freeze and confirm the customer then receives the action outcome. Test suspension duration, repeat-review conflict and second-suspension escalation.
8. **Privacy:** Show public notes to unrelated customers; return private notes/reasons/references only to the affected customer and permitted staff. Deny customer access to score/evidence endpoints.
9. **Advisory limits:** Consume and exceed daily count/EUR thresholds; verify warnings and a higher review score while payments remain allowed. Change limits without modifying money/status/identity. Confirm daily rollover and exclusion of incoming/internal/failed movements.
10. **UI and documentation:** Check desktop/mobile navigation, keyboard submission, contained table/Discover scrolling, readable badges and stream failure feedback. Confirm Swagger includes the application's routes and request/response models.

## 9. Scope boundaries

This project implements app-managed investments, savings goals and recorded identity/credit/security decisions. Brokerage settlement, loan disbursement, savings interest, dividends/corporate actions, a full double-entry ledger, versioned database migrations and validated fraud-model accuracy are outside the current specification. The interface should remain clear about actual operation outcomes and avoid implying that an unimplemented financial service has executed.
