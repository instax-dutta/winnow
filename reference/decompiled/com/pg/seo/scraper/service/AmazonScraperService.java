/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.TreeNode
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.pg.seo.scraper.service;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.seo.scraper.model.Product;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AmazonScraperService {
    private static final String SCRAPER_API = "http://localhost:3000/scrape?url=";
    private static final String AMAZON_BASE = "https://www.amazon.in/s";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Product> scrapeByKeyword(String keyword, int page) {
        ArrayList<Product> products = new ArrayList<Product>();
        try {
            String query = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String amazonUrl = "https://www.amazon.in/s?k=" + query + "&page=" + page;
            String fullUrl = SCRAPER_API + URLEncoder.encode(amazonUrl, StandardCharsets.UTF_8);
            HttpURLConnection conn = (HttpURLConnection)new URL(fullUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            int status = conn.getResponseCode();
            if (status != 200) {
                throw new RuntimeException("Scraper API error: HTTP " + status);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));){
                String line;
                StringBuilder jsonBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
                JsonNode root = this.objectMapper.readTree(jsonBuilder.toString());
                JsonNode items = root.get("products");
                if (items != null && items.isArray()) {
                    for (JsonNode item : items) {
                        Product product = (Product)this.objectMapper.treeToValue((TreeNode)item, Product.class);
                        products.add(product);
                    }
                }
            }
            conn.disconnect();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to scrape Amazon: " + e.getMessage(), e);
        }
        return products;
    }
}

