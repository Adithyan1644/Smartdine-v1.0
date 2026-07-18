package com.smartdine.coreheart;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Utility that detects and terminates any process occupying port 8080
 * before the Spring Boot embedded Tomcat server attempts to bind to it.
 *
 * This prevents the "Port 8080 already in use" BindException that occurs
 * when a previous JVM instance was not shut down cleanly (e.g. task kill,
 * IDE stop button, or window close without proper lifecycle handling).
 */
public class PortKiller {

    private static final int PORT = 8080;

    /**
     * Finds and forcefully terminates any process listening on PORT.
     * Safe to call if the port is already free — it does nothing.
     */
    public static void freePort() {
        try {
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                freePortWindows();
            } else {
                freePortUnix();
            }

        } catch (Exception e) {
            // Non-fatal: log and continue — Spring will throw a proper error if still occupied
            System.err.println("[PortKiller] Warning: could not free port " + PORT + ": " + e.getMessage());
        }
    }

    // ──────────────────────────────── Windows ─────────────────────────────────

    private static void freePortWindows() throws Exception {
        // Step 1: find the PID owning port 8080 via netstat
        Process netstat = Runtime.getRuntime().exec(
                new String[]{"cmd.exe", "/c", "netstat -ano | findstr :" + PORT}
        );
        String pid = extractPidFromNetstat(netstat);

        if (pid == null) {
            System.out.println("[PortKiller] Port " + PORT + " is free — nothing to kill.");
            return;
        }

        System.out.println("[PortKiller] Port " + PORT + " is occupied by PID " + pid + ". Terminating...");

        // Step 2: forcefully kill it
        Process kill = Runtime.getRuntime().exec(
                new String[]{"cmd.exe", "/c", "taskkill /PID " + pid + " /F"}
        );
        kill.waitFor();

        // Step 3: brief pause to let the OS release the socket
        Thread.sleep(800);
        System.out.println("[PortKiller] PID " + pid + " terminated. Port " + PORT + " is now free.");
    }

    // ──────────────────────────────── Unix/Mac ─────────────────────────────────

    private static void freePortUnix() throws Exception {
        // lsof prints "<pid>\n" for the process owning the port
        Process lsof = Runtime.getRuntime().exec(
                new String[]{"sh", "-c", "lsof -t -i:" + PORT}
        );
        String pid = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(lsof.getInputStream()))) {
            String line = reader.readLine();
            if (line != null && !line.isBlank()) {
                pid = line.trim();
            }
        }

        if (pid == null) {
            System.out.println("[PortKiller] Port " + PORT + " is free — nothing to kill.");
            return;
        }

        System.out.println("[PortKiller] Port " + PORT + " is occupied by PID " + pid + ". Terminating...");
        Runtime.getRuntime().exec(new String[]{"sh", "-c", "kill -9 " + pid}).waitFor();
        Thread.sleep(800);
        System.out.println("[PortKiller] PID " + pid + " terminated. Port " + PORT + " is now free.");
    }

    // ───────────────────────────── Helpers ─────────────────────────────────────

    private static String extractPidFromNetstat(Process process) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // netstat output format: "TCP 0.0.0.0:8080  0.0.0.0:0  LISTENING  <PID>"
                if (line.contains(":" + PORT) && line.contains("LISTENING")) {
                    // PID is always the last token
                    String[] parts = line.split("\\s+");
                    if (parts.length > 0) {
                        return parts[parts.length - 1];
                    }
                }
            }
        }
        return null;
    }
}
