package com.email.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.opencsv.CSVWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailExtractor {

  @Value("${mailbot.outputDir}")
  private String outputDir;

  private static final Pattern EMAIL_IN_ANGLE = Pattern.compile("<(.+?)>");

  public Path extractEmails(Gmail gmail, String start, String endExclusive) throws Exception {
    Set<String> emails = new HashSet<>();

    String inboxQuery = "in:inbox after:" + start + " before:" + endExclusive;
    String sentQuery  = "in:sent after:" + start + " before:" + endExclusive;

    emails.addAll(fetch(gmail, inboxQuery, true));
    emails.addAll(fetch(gmail, sentQuery, false));

    String safeStart = start.replace("/", "-");
    String safeEnd   = endExclusive.replace("/", "-");
    Path out = Path.of(outputDir, "emails_" + safeStart + "_to_" + safeEnd + ".csv");
    Files.createDirectories(out.getParent());

    try (var w = new CSVWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
      w.writeNext(new String[]{"Email Address"});
      emails.stream().sorted().forEach(e -> w.writeNext(new String[]{e}));
    }

    System.out.println("📁 Extracted emails: " + emails.size());
    return out;
  }

  private Set<String> fetch(Gmail gmail, String query, boolean extractFrom) throws Exception {
    Set<String> out = new HashSet<>();
    String pageToken = null;

    do {
      var res = gmail.users().messages().list("me")
          .setQ(query)
          .setMaxResults(500L)
          .setPageToken(pageToken)
          .execute();

      List<Message> messages = res.getMessages();
      if (messages != null) {
        for (Message m : messages) {
          var msg = gmail.users().messages().get("me", m.getId())
              .setFormat("metadata")
              .setMetadataHeaders(List.of("From", "To", "Subject"))
              .execute();

          String subject = header(msg.getPayload().getHeaders(), "Subject");
          if (subject == null || !subject.toLowerCase().contains("job")) continue;

          String hv = extractFrom ? header(msg.getPayload().getHeaders(), "From")
                                  : header(msg.getPayload().getHeaders(), "To");

          String email = extractEmail(hv);
          if (email == null) continue;

          email = email.trim().toLowerCase();
          if (!email.isBlank()) out.add(email);
        }
      }

      pageToken = res.getNextPageToken();
    } while (pageToken != null);

    return out;
  }

  private static String header(List<MessagePartHeader> headers, String name) {
    if (headers == null) return null;
    for (var h : headers) if (name.equalsIgnoreCase(h.getName())) return h.getValue();
    return null;
  }

  private static String extractEmail(String headerVal) {
    if (headerVal == null) return null;
    Matcher m = EMAIL_IN_ANGLE.matcher(headerVal);
    return m.find() ? m.group(1) : headerVal;
  }
}
