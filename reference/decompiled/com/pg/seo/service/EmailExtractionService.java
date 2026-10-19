/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.poi.ss.usermodel.Row
 *  org.jsoup.Jsoup
 *  org.jsoup.nodes.Document
 *  org.jsoup.nodes.Element
 *  org.jsoup.select.Elements
 *  org.springframework.stereotype.Service
 */
package com.pg.seo.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Row;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
public class EmailExtractionService {
    public CompletableFuture<ConcurrentHashMap<String, String>> extracted(Row row, ConcurrentHashMap<String, String> map) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document doc = Jsoup.connect((String)("https://" + row.getCell(0).getStringCellValue())).get();
                Elements contact = doc.getAllElements();
                for (Element link : contact) {
                    Pattern pat = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9_.]*@[a-zA-Z0-9]+([.][a-zA-Z]+)+");
                    Pattern p = Pattern.compile("(0/91)?[7-9][0-9]{9}");
                    String l = link.html();
                    Matcher mat = pat.matcher(l);
                    while (mat.find()) {
                        map.put(mat.group(), row.getCell(0).getStringCellValue());
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return map;
        });
    }
}

