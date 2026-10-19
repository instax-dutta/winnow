/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.cfg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class PuppeteerRunner {
    private Process puppeteerProcess;

    public void startNodeServer() {
        try {
            File nodeDir = new File("src/main/resources/puppeteer");
            ProcessBuilder installBuilder = new ProcessBuilder("npm.cmd", "install");
            installBuilder.directory(nodeDir);
            installBuilder.redirectErrorStream(true);
            Process installProcess = installBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(installProcess.getInputStream()));){
                reader.lines().forEach(System.out::println);
            }
            if (!installProcess.waitFor(2L, TimeUnit.MINUTES)) {
                System.err.println("\u26a0\ufe0f npm install timed out");
                installProcess.destroy();
                return;
            }
            ProcessBuilder serverBuilder = new ProcessBuilder("node", "server.js");
            serverBuilder.directory(nodeDir);
            serverBuilder.redirectErrorStream(true);
            this.puppeteerProcess = serverBuilder.start();
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.puppeteerProcess.getInputStream()));){
                    reader.lines().forEach(System.out::println);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            System.out.println("\u2705 Puppeteer Node server started.");
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopNodeServer() {
        if (this.puppeteerProcess != null && this.puppeteerProcess.isAlive()) {
            this.puppeteerProcess.destroy();
            System.out.println("\ud83d\uded1 Puppeteer Node server stopped.");
        }
    }
}

