package com.email.runner;

import com.email.service.EmailExtractor;
import com.email.service.GmailAuth;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Runner implements CommandLineRunner {

  private final GmailAuth gmailAuth;
  private final EmailExtractor emailExtractor;

  public Runner(GmailAuth gmailAuth, EmailExtractor emailExtractor) {
    this.gmailAuth = gmailAuth;
    this.emailExtractor = emailExtractor;
  }

  @Override
  public void run(String... args) throws Exception {
    System.out.println("🚀 Extract-only test started");

    String start = "2025/06/01";
    String endExclusive = "2025/07/01"; // includes full June 30

    var gmail = gmailAuth.getGmailService();
    var csv = emailExtractor.extractEmails(gmail, start, endExclusive);

    System.out.println("✅ Extraction completed: " + csv.toAbsolutePath());
  }
}
