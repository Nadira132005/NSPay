package com.nspay.finance;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.*;

@io.swagger.v3.oas.annotations.tags.Tag(name="Savings, profiles and credit")
@RestController
@RequestMapping("/api/finance")
public class FinanceController {
    private final FinanceService finance;
    private final FraudService fraud;
    private final RiskAssessmentRepository risks;
    private final CreditApplicationRepository credits;
    private final CreditDocumentRepository documents;
    private final OnboardingService onboarding;
    public FinanceController(FinanceService finance,FraudService fraud,RiskAssessmentRepository risks,
                             CreditApplicationRepository credits,CreditDocumentRepository documents,OnboardingService onboarding) {
        this.finance=finance; this.fraud=fraud; this.risks=risks; this.credits=credits; this.documents=documents;
        this.onboarding=onboarding;
    }
    private long id(HttpServletRequest request) {
        Object id=request.getAttribute("authenticatedUserId");
        if (!(id instanceof Number)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sign in to continue");
        return ((Number)id).longValue();
    }
    private void reviewer(HttpServletRequest request) {
        if (finance.user(id(request)).getRole()!=com.nspay.enums.UserRole.Admin)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only administrators can review applications and risks");
    }
    @GetMapping("/overview") @io.swagger.v3.oas.annotations.Operation(summary="Get the current user’s financial overview", description="Returns only the signed-in user’s account, savings, scheduled payments, order history, credit applications, documents and profile.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.config.ApiViews.Overview.class)))
    public Map<String,Object> overview(HttpServletRequest request) { return finance.overview(id(request)); }
    @PostMapping("/pockets") @io.swagger.v3.oas.annotations.Operation(summary="Create a savings pocket", description="Initial funds are debited from the active main account in the same transaction as pocket creation.")
    public SavingsPocket pocket(HttpServletRequest request,@Valid @RequestBody FinanceRequests.Pocket body) { return finance.createPocket(id(request),body); }
    @PostMapping("/pockets/{pocketId}/deposit") @io.swagger.v3.oas.annotations.Operation(summary="Deposit into a savings pocket", description="Moves EUR from the main account into an owned savings pocket atomically.")
    public SavingsPocket deposit(HttpServletRequest request,@PathVariable long pocketId,@Valid @RequestBody FinanceRequests.Money body) { return finance.movePocket(id(request),pocketId,body.getAmount(),false); }
    @PostMapping("/pockets/{pocketId}/withdraw") @io.swagger.v3.oas.annotations.Operation(summary="Withdraw from a savings pocket", description="Moves EUR from an owned savings pocket back into the active main account atomically.")
    public SavingsPocket withdraw(HttpServletRequest request,@PathVariable long pocketId,@Valid @RequestBody FinanceRequests.Money body) { return finance.movePocket(id(request),pocketId,body.getAmount(),true); }
    @DeleteMapping("/pockets/{pocketId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @io.swagger.v3.oas.annotations.Operation(summary="Close a savings pocket", description="Returns the entire remaining pocket balance to the main account before deleting the pocket.")
    public void close(HttpServletRequest request,@PathVariable long pocketId) { finance.closePocket(id(request),pocketId); }
    @PostMapping("/scheduled-transfers") @io.swagger.v3.oas.annotations.Operation(summary="Schedule an IBAN transfer", description="scheduledAt must be a future instant with an explicit timezone, such as 2026-12-01T10:00:00Z. Funds are checked at execution and are not reserved. The backend checks due transfers every 30 seconds.")
    public ScheduledTransfer schedule(HttpServletRequest request,@Valid @RequestBody FinanceRequests.Payment body) { return finance.schedule(id(request),body); }
    @DeleteMapping("/scheduled-transfers/{scheduleId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @io.swagger.v3.oas.annotations.Operation(summary="Cancel a scheduled transfer", description="Only an owned PENDING transfer can be cancelled.")
    public void cancel(HttpServletRequest request,@PathVariable long scheduleId) { finance.cancelSchedule(id(request),scheduleId); }
    @Deprecated
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="410",description="Use /api/investments/orders instead.")
    @PostMapping("/trades") @io.swagger.v3.oas.annotations.Operation(summary="Legacy trading endpoint", description="Retired fixed-price endpoint. Returns 410 Gone; use POST /api/investments/orders with a current server-issued quote.")
    public TradeOrder trade(HttpServletRequest request,@Valid @RequestBody FinanceRequests.Trade body) { return finance.trade(id(request),body); }
    @GetMapping("/onboarding") @io.swagger.v3.oas.annotations.Operation(summary="Open the current user’s unfinished onboarding", description="Identity comes from the authenticated session, never a client user ID. Requires an active account. Returns DETAILS or IDENTITY for the next unfinished step. Submitted or verified onboarding returns 409 and cannot be reopened.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Next unfinished step",content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.config.ApiViews.Onboarding.class)))
    public Map<String,Object> onboarding(HttpServletRequest request){return onboarding.own(id(request));}
    @GetMapping("/onboarding/reviews") @io.swagger.v3.oas.annotations.Operation(summary="List onboarding records for administrator review", description="Administrator only, checked against the stored role. Returns at most 100 profiles with their original onboarding document metadata, current version and review outcome. Does not include credit documents or document bytes.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Onboarding review queue",content=@io.swagger.v3.oas.annotations.media.Content(array=@io.swagger.v3.oas.annotations.media.ArraySchema(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.config.ApiViews.OnboardingReview.class))))
    public List<Map<String,Object>> onboardingReviews(HttpServletRequest request){return onboarding.reviews(id(request));}
    @GetMapping("/onboarding/users/{subject}") @io.swagger.v3.oas.annotations.Operation(summary="Inspect a user’s onboarding for administrator review", description="Administrator only, checked before looking up the subject. Returns the user’s saved details, profile version and onboarding document metadata. Credit documents are excluded; originals can be downloaded through the protected document endpoint. Customers cannot inspect another user’s onboarding.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Onboarding review record",content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.config.ApiViews.OnboardingReview.class)))
    public Map<String,Object> inspectOnboarding(HttpServletRequest request,@PathVariable long subject){return onboarding.inspect(id(request),subject);}
    @PostMapping("/onboarding/users/{subject}/review") @io.swagger.v3.oas.annotations.Operation(summary="Approve or deny a user’s identity verification", description="Administrator only; requires submitted personal details and an onboarding IDENTITY document. VERIFIED enables credit only while the account is ACTIVE; DECLINED denies or revokes verification. Both decisions keep customer details and documents locked. Current profile version is required; stale decisions return 409. Account lock, profile version, reviewer, note, timestamp and audit record commit together, serialized with credit eligibility checks.")
    public CustomerProfile onboardingReview(HttpServletRequest request,@PathVariable long subject,@Valid @RequestBody FinanceRequests.OnboardingDecision body){return onboarding.review(id(request),subject,body);}
    @PutMapping("/profile") @io.swagger.v3.oas.annotations.Operation(summary="Submit personal details once", description="Stores the signed-in user’s contact details, date of birth and occupation. Age must be at least 18. Submitted details cannot be overwritten, including before verification; repeated writes return 409. Client ownership or verification fields are ignored. Saving details does not verify identity.")
    public CustomerProfile profile(HttpServletRequest request,@Valid @RequestBody FinanceRequests.Profile body) { return finance.saveProfile(id(request),body); }
    @PostMapping("/credit") @io.swagger.v3.oas.annotations.Operation(summary="Create a credit application draft", description="Requires administrator-approved VERIFIED identity and an ACTIVE account. Locks both account and profile before saving a DRAFT. Does not submit it or change the account balance.")
    public CreditApplication credit(HttpServletRequest request,@Valid @RequestBody FinanceRequests.Credit body) { return finance.createCredit(id(request),body); }
    @PutMapping("/credit/{applicationId}") @io.swagger.v3.oas.annotations.Operation(summary="Update an owned credit draft", description="Requires VERIFIED identity and an ACTIVE account. Updates purpose, amount, term and monthly income without creating another application or changing its documents. Account, profile and owned draft are locked in order; submitted applications return 409, another user’s application returns 404. Ownership and status cannot be set through the request.")
    public CreditApplication updateCredit(HttpServletRequest request,@PathVariable long applicationId,@Valid @RequestBody FinanceRequests.Credit body) { return finance.updateCredit(id(request),applicationId,body); }
    @GetMapping("/credit/reviews") @io.swagger.v3.oas.annotations.Operation(summary="Filter and sort the administrator credit review queue",description="Stored administrator role required. Server-side pagination (up to 100), purpose/username search, status filter and whitelisted createdAt/amount/monthlyIncome/termMonths/purpose sorting. Returns application details and applicant username; excludes the acting administrator’s own applications. Review and document endpoints remain protected independently.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Paged credit review queue",content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.config.ApiViews.CreditQueue.class)))
    public Map<String,Object> creditQueue(HttpServletRequest request,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="UNDER_REVIEW") String status,@RequestParam(defaultValue="createdAt") String sort,@RequestParam(defaultValue="desc") String direction){return finance.creditQueue(id(request),page,size,search,status,sort,direction);}
    @PostMapping("/credit/{applicationId}/submit") @io.swagger.v3.oas.annotations.Operation(summary="Submit a credit application", description="Rechecks VERIFIED identity and ACTIVE account under the same locks used for administrator status/verification changes. Requires an owned draft plus its owned IDENTITY and INCOME documents. Changes DRAFT to UNDER_REVIEW.")
    public CreditApplication submit(HttpServletRequest request,@PathVariable long applicationId) { return finance.submitCredit(id(request),applicationId); }
    @PostMapping(value="/documents", consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @io.swagger.v3.oas.annotations.Operation(summary="Upload an onboarding or credit document", description="PDF, PNG or JPEG content up to 5 MB. Without applicationId, only IDENTITY is accepted after saved personal details; the first successful upload atomically submits and locks onboarding. With applicationId, VERIFIED identity, ACTIVE account and ownership of a DRAFT are required. Extracted text is not identity verification.")
    public CreditDocument upload(HttpServletRequest request,@RequestParam(required=false) Long applicationId,
        @RequestParam String kind,@RequestParam MultipartFile file) throws IOException { return finance.upload(id(request),applicationId,kind,file); }
    @GetMapping("/documents/{documentId}/download") @io.swagger.v3.oas.annotations.Operation(summary="Download a document", description="Owner or administrator only. Returns the original PDF, PNG or JPEG as an attachment.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Original document attachment",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/octet-stream",schema=@io.swagger.v3.oas.annotations.media.Schema(type="string",format="binary")))
    public ResponseEntity<byte[]> download(HttpServletRequest request,@PathVariable long documentId) {
        long owner=id(request);boolean admin=finance.user(owner).getRole()==com.nspay.enums.UserRole.Admin;
        CreditDocument document=(admin?documents.findById(documentId):documents.findByIdAndUserId(documentId,owner)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(document.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(document.getFilename()).build().toString())
            .header("X-Content-Type-Options","nosniff").body(document.getContent());
    }
    @PostMapping("/risk-assessments") @io.swagger.v3.oas.annotations.Operation(summary="Record an administrator payment risk assessment", description="Administrator only. Stores a legacy behavioral risk score, reasons and model version. Scores do not determine misconduct; use the scored payment queue for customer transaction review.")
    public RiskAssessment assess(HttpServletRequest request,@Valid @RequestBody FinanceRequests.Payment body) {
        reviewer(request);
        return fraud.assess(id(request),body.getRecipientIban().replaceAll("\\s+", "").toUpperCase(Locale.ROOT),body.getAmount());
    }
    @GetMapping("/risk-assessments") @io.swagger.v3.oas.annotations.Operation(summary="List stored risk assessments for administrator review", description="Administrator only. Returns the latest 100 stored payment risk signals across users. Customer access is forbidden.")
    public List<RiskAssessment> risks(HttpServletRequest request) { reviewer(request);return risks.findTop100ByOrderByCreatedAtDesc(); }
    @PostMapping("/risk-preview") @io.swagger.v3.oas.annotations.Operation(summary="Preview payment risk without saving", description="Administrator only. Calculates a behavioral score for a proposed payment without saving an assessment or moving funds. Customer transfer confirmations do not request or display risk signals.")
    public RiskAssessment preview(HttpServletRequest request,@Valid @RequestBody FinanceRequests.Payment body) {
        reviewer(request);
        return fraud.preview(id(request),body.getRecipientIban().replaceAll("\\s+", "").toUpperCase(Locale.ROOT),body.getAmount());
    }
    @GetMapping("/reviews") @io.swagger.v3.oas.annotations.Operation(summary="Get the administrator review queue", description="Administrator only. Returns the latest 100 credit applications and payment risk assessments.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Successful response",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/json",schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=com.nspay.config.ApiViews.ReviewQueue.class)))
    public Map<String,Object> reviews(HttpServletRequest request) {
        reviewer(request); Map<String,Object> result=new LinkedHashMap<>();
        result.put("risks",risks.findTop100ByOrderByCreatedAtDesc()); result.put("credits",credits.findTop100ByOrderByCreatedAtDesc());
        return result;
    }
    @GetMapping("/credit/{applicationId}/documents") @io.swagger.v3.oas.annotations.Operation(summary="List documents for a credit application", description="Owner or administrator only. Returns document metadata and extracted text.")
    public List<CreditDocument> creditDocuments(HttpServletRequest request,@PathVariable long applicationId) {
        long owner=id(request);boolean admin=finance.user(owner).getRole()==com.nspay.enums.UserRole.Admin;
        CreditApplication credit=(admin?credits.findById(applicationId):credits.findByIdAndUserId(applicationId,owner)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return documents.findByApplicationIdAndUserId(applicationId,credit.getUserId());
    }
    @PostMapping("/credit/{applicationId}/review") @io.swagger.v3.oas.annotations.Operation(summary="Review a credit application", description="Administrator only. An UNDER_REVIEW application can become APPROVED or DECLINED with a note. Approval does not automatically disburse funds.")
    public CreditApplication review(HttpServletRequest request,@PathVariable long applicationId,@Valid @RequestBody FinanceRequests.Review body) {
        reviewer(request); return finance.reviewCredit(id(request),applicationId,body);
    }
    @PostMapping("/risk-assessments/{riskId}/review") @io.swagger.v3.oas.annotations.Operation(summary="Mark a risk assessment reviewed", description="Administrator only. This acknowledges a signal; use the security report workflow for account restrictions.")
    public RiskAssessment reviewRisk(HttpServletRequest request,@PathVariable long riskId,@Valid @RequestBody FinanceRequests.Review body) {
        reviewer(request); RiskAssessment risk=risks.findById(riskId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        risk.setStatus("REVIEWED"); return risks.save(risk);
    }
}
