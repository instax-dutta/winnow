/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.api.client.googleapis.auth.oauth2.GoogleCredential
 *  com.google.api.client.googleapis.auth.oauth2.GoogleCredential$Builder
 *  com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
 *  com.google.api.client.http.HttpRequestInitializer
 *  com.google.api.client.http.HttpTransport
 *  com.google.api.client.json.JsonFactory
 *  com.google.api.client.json.gson.GsonFactory
 *  com.google.api.services.gmail.Gmail
 *  com.google.api.services.gmail.Gmail$Builder
 *  org.springframework.stereotype.Service
 */
package com.pg.seo.cfg;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import org.springframework.stereotype.Service;

@Service
public class GmailServiceFactory {
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "MAIL ELEXYT";

    public Gmail createGmailService(String clientId, String clientSecret, String refreshToken) throws Exception {
        GoogleCredential credential = new GoogleCredential.Builder().setClientSecrets(clientId, clientSecret).setTransport((HttpTransport)GoogleNetHttpTransport.newTrustedTransport()).setJsonFactory(JSON_FACTORY).build().setRefreshToken(refreshToken);
        credential.refreshToken();
        return new Gmail.Builder((HttpTransport)GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, (HttpRequestInitializer)credential).setApplicationName(APPLICATION_NAME).build();
    }
}

