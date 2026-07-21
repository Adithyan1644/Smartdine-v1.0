package com.smartdine.service;

import org.springframework.stereotype.Service;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MdnsService {

    private JmDNS jmdns;
    private ServiceInfo serviceInfo;

    /**
     * Broadcasts the Biller PC service to the local area network using JmDNS.
     */
    public synchronized void registerService(UUID restaurantId) {
        if (jmdns != null) {
            unregisterService();
        }

        // Run network operation in virtual thread to avoid blocking main UI thread
        Thread.ofVirtual().start(() -> {
            try {
                InetAddress physicalIp = getPhysicalLocalIP();
                if (physicalIp == null) {
                    physicalIp = InetAddress.getLocalHost();
                }

                synchronized (this) {
                    jmdns = JmDNS.create(physicalIp);
                    
                    String serviceType = "_smartdine-pos._tcp.local.";
                    String serviceName = "smartdine-biller-" + restaurantId.toString().substring(0, 8);
                    int port = 8080;

                    // Standard TXT record property mapping for JmDNS
                    Map<String, String> properties = new HashMap<>();
                    properties.put("restaurantId", restaurantId.toString());
                    properties.put("version", "1.0.0");
                    properties.put("serviceName", "SmartDine Biller");

                    serviceInfo = ServiceInfo.create(serviceType, serviceName, port, 0, 0, properties);
                    jmdns.registerService(serviceInfo);
                }
                System.out.println("📶 [mDNS] Broadcasting Biller service: _smartdine-pos._tcp.local. on " 
                        + physicalIp.getHostAddress() + ":8080 (Restaurant: " + restaurantId + ")");
            } catch (IOException e) {
                System.err.println("❌ [mDNS] Failed to register service: " + e.getMessage());
            }
        });
    }

    private InetAddress getPhysicalLocalIP() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;
                
                String displayName = iface.getDisplayName().toLowerCase();
                if (displayName.contains("virtual") || displayName.contains("wsl") || 
                    displayName.contains("docker") || displayName.contains("vbox") || 
                    displayName.contains("vmware") || displayName.contains("vpn")) {
                    continue;
                }

                java.util.Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return addr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error scanning network cards for mDNS: " + e.getMessage());
        }
        return null;
    }

    /**
     * Unregisters the service and releases JmDNS resources on shutdown.
     */
    @PreDestroy
    public synchronized void unregisterService() {
        if (jmdns != null) {
            try {
                if (serviceInfo != null) {
                    jmdns.unregisterService(serviceInfo);
                }
                jmdns.close();
                System.out.println("📶 [mDNS] Stopped broadcasting Biller service.");
            } catch (IOException e) {
                System.err.println("❌ [mDNS] Error during mDNS teardown: " + e.getMessage());
            } finally {
                jmdns = null;
                serviceInfo = null;
            }
        }
    }
}
