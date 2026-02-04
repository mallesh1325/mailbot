package com.email.service;

import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;

@Service
public class GmailAuth {

  private static final GsonFactory JSON = GsonFactory.getDefaultInstance();

  @Value("${mailbot.credentialsJson}")
  private String credentialsJson;

  @Value("${mailbot.tokenDir}")
  private String tokenDir;

  public Gmail getGmailService() throws Exception {
    var http = GoogleNetHttpTransport.newTrustedTransport();

    var secrets = GoogleClientSecrets.load(JSON, new FileReader(credentialsJson));

    var flow = new GoogleAuthorizationCodeFlow.Builder(
        http, JSON, secrets, List.of(GmailScopes.MAIL_GOOGLE_COM)
    ).setDataStoreFactory(new FileDataStoreFactory(Path.of(tokenDir).toFile()))
     .setAccessType("offline")
     .build();

    var receiver = new LocalServerReceiver.Builder().setPort(0).build();

    var credential = new com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp(flow, receiver)
        .authorize("user");

    return new Gmail.Builder(http, JSON, credential)
        .setApplicationName("MailBot")
        .build();
  }
}
