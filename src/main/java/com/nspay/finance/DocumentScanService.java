package com.nspay.finance;

import org.springframework.stereotype.Service;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** Local PDF text extraction and image OCR. Extracted text must be confirmed by a person. */
@Service
public class DocumentScanService {
    public void scan(CreditDocument document) {
        Path input=null, output=null;
        Process process=null;
        try {
            input=Files.createTempFile("nspay-document-", document.getContentType().equals("application/pdf") ? ".pdf" : ".png");
            output=Files.createTempFile("nspay-scan-", ".txt");
            Files.write(input, document.getContent());
            ProcessBuilder builder = document.getContentType().equals("application/pdf")
                ? new ProcessBuilder("pdftotext", "-layout", input.toString(), "-")
                : new ProcessBuilder("tesseract", input.toString(), "stdout", "-l", "eng");
            process=builder.redirectOutput(output.toFile()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly(); document.setScanStatus("TIMED_OUT"); return;
            }
            if (process.exitValue()!=0) { document.setScanStatus("NEEDS_MANUAL_REVIEW"); return; }
            String text;
            try (java.io.InputStream stream=Files.newInputStream(output)) {
                byte[] buffer=new byte[16000]; int size=stream.read(buffer);
                text=size<=0 ? "" : new String(buffer, 0, size, StandardCharsets.UTF_8).trim();
            }
            document.setExtractedText(text.substring(0, Math.min(text.length(), 4000)));
            document.setScanStatus(text.isEmpty() ? "NEEDS_MANUAL_REVIEW" : "TEXT_EXTRACTED");
        } catch (IOException exception) {
            document.setScanStatus("SCANNER_UNAVAILABLE");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt(); document.setScanStatus("INTERRUPTED");
        } finally {
            if (process!=null && process.isAlive()) process.destroyForcibly();
            for (Path file : new Path[]{input, output}) {
                if (file!=null) try { Files.deleteIfExists(file); } catch (IOException ignored) { }
            }
        }
    }
}
