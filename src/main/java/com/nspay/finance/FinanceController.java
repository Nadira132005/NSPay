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
    public FinanceController(FinanceService finance,FraudService fraud,RiskAssessmentRepository risks,
                             CreditApplicationRepository credits,CreditDocumentRepository documents) {
        this.finance=finance; this.fraud=fraud; this.risks=risks; this.credits=credits; this.documents=documents;
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
    @PutMapping("/profile") @io.swagger.v3.oas.annotations.Operation(summary="Save personal profile details", description="Stores contact details, date of birth and occupation. Age must be at least 18. Saving details does not verify identity.")
    public CustomerProfile profile(HttpServletRequest request,@Valid @RequestBody FinanceRequests.Profile body) { return finance.saveProfile(id(request),body); }
    @PostMapping("/credit") @io.swagger.v3.oas.annotations.Operation(summary="Create a credit application draft", description="Saves a DRAFT without submitting it or changing the account balance.")
    public CreditApplication credit(HttpServletRequest request,@Valid @RequestBody FinanceRequests.Credit body) { return finance.createCredit(id(request),body); }
    @PostMapping("/credit/{applicationId}/submit") @io.swagger.v3.oas.annotations.Operation(summary="Submit a credit application", description="Requires a saved profile plus IDENTITY and INCOME documents belonging to this draft. Changes DRAFT to UNDER_REVIEW.")
    public CreditApplication submit(HttpServletRequest request,@PathVariable long applicationId) { return finance.submitCredit(id(request),applicationId); }
    @PostMapping(value="/documents", consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @io.swagger.v3.oas.annotations.Operation(summary="Upload a supporting document", description="Multipart upload: kind is IDENTITY or INCOME; applicationId is optional for onboarding documents. PDF, PNG and JPEG content only, up to 5 MB. Extracted text requires manual review; document authenticity is not verified.")
    public CreditDocument upload(HttpServletRequest request,@RequestParam(required=false) Long applicationId,
        @RequestParam String kind,@RequestParam MultipartFile file) throws IOException { return finance.upload(id(request),applicationId,kind,file); }
    @GetMapping("/documents/{documentId}/download") @io.swagger.v3.oas.annotations.Operation(summary="Download a document", description="Owner or administrator only. Returns the original PDF, PNG or JPEG as an attachment.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Original document attachment",content=@io.swagger.v3.oas.annotations.media.Content(mediaType="application/octet-stream",schema=@io.swagger.v3.oas.annotations.media.Schema(type="string",format="binary")))
    public ResponseEntity<byte[]> download(HttpServletRequest request,@PathVariable long documentId) {
        CreditDocument document=documents.findById(documentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (document.getUserId()!=id(request)) reviewer(request);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(document.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(document.getFilename()).build().toString())
            .header("X-Content-Type-Options","nosniff").body(document.getContent());
    }
    @PostMapping("/risk-assessments") @io.swagger.v3.oas.annotations.Operation(summary="Record a payment risk assessment", description="Stores a behavioral risk score, reasons and model version for the current user. Scores do not determine misconduct.")
    public RiskAssessment assess(HttpServletRequest request,@Valid @RequestBody FinanceRequests.Payment body) {
        return fraud.assess(id(request),body.getRecipientIban().replaceAll("\\s+", "").toUpperCase(Locale.ROOT),body.getAmount());
    }
    @GetMapping("/risk-assessments") @io.swagger.v3.oas.annotations.Operation(summary="List the current user’s risk assessments", description="Returns stored payment risk signals for the authenticated user.")
    public List<RiskAssessment> risks(HttpServletRequest request) { return risks.findByUserIdOrderByCreatedAtDesc(id(request)); }
    @PostMapping("/risk-preview") @io.swagger.v3.oas.annotations.Operation(summary="Preview payment risk without saving", description="Calculates a behavioral score for a proposed payment without saving an assessment or moving funds.")
    public RiskAssessment preview(HttpServletRequest request,@Valid @RequestBody FinanceRequests.Payment body) {
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
        CreditApplication credit=credits.findById(applicationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (credit.getUserId()!=id(request)) reviewer(request);
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
