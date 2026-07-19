package com.smartdine.service;

import com.smartdine.coreheart.SystemConfig;
import com.smartdine.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.*;
import java.util.Enumeration;
import java.util.Optional;
import java.util.UUID;

@Service
@Profile("!prod") // Runs ONLY on the local restaurant PC (not on Google Cloud Run)
public class CloudIpReporter implements CommandLineRunner {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final UUID FALLBACK_DEV_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"); // Dev ID

    // Deployed GCP Cloud Run Gateway URL
    private final String CLOUD_REPORT_URL = "https://smartdine-v1-0-git-635032287458.europe-west1.run.app/api/public/provision/report-ip?ip=";

    @Override
    public void run(String... args) {
        // Run network reporting asynchronously to avoid blocking boot time
        Thread.ofVirtual().start(this::reportIpToCloud);
    }

    public void reportIpToCloud() {
        UUID restaurantId = FALLBACK_DEV_ID;

        if (systemConfigRepository != null) {
            Optional<SystemConfig> configOpt = systemConfigRepository.findAll().stream().findFirst();
            if (configOpt.isPresent() && configOpt.get().isActivated() && configOpt.get().getRestaurantId() != null) {
                restaurantId = configOpt.get().getRestaurantId();
            }
        }

        InetAddress physicalIp = getPhysicalLocalIP();
        if (physicalIp == null) {
            System.err.println("⚠️ Local: No physical IP found. Cannot report local IP to Google Cloud.");
            return;
        }

        String localIp = physicalIp.getHostAddress() + ":8080"; // Include port
        String url = CLOUD_REPORT_URL + localIp;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Restaurant-ID", restaurantId.toString());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            System.out.println("☁️ Cloud Sync: Registered local IP [" + localIp + "] securely on Google Cloud (Restaurant: " + restaurantId + ").");
        } catch (Exception e) {
            System.err.println("⚠️ Cloud Sync: Failed to register local IP on Google Cloud (Offline mode): " + e.getMessage());
        }
    }

    // Scans all network interfaces, ignoring WSL, Docker, and VirtualBox network cards
    private InetAddress getPhysicalLocalIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;
                
                String displayName = iface.getDisplayName().toLowerCase();
                if (displayName.contains("virtual") || displayName.contains("wsl") || 
                    displayName.contains("docker") || displayName.contains("vbox") || 
                    displayName.contains("vmware") || displayName.contains("vpn")) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        // Only target home/restaurant LAN subnets (192.168.x.x or 10.x.x.x or 172.x.x.x)
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return addr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error scanning network cards: " + e.getMessage());
        }
        return null;
    }
}
