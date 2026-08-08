package com.smartdine.service;

import com.smartdine.coreheart.BillingConfig;
import com.smartdine.repository.BillingConfigRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service responsible for generating formatted monospace text receipts for thermal printers (80mm & 58mm).
 */
@Service
public class ReceiptPrintService {

    private final BillingConfigRepository billingConfigRepository;

    public ReceiptPrintService(BillingConfigRepository billingConfigRepository) {
        this.billingConfigRepository = billingConfigRepository;
    }

    /**
     * Generates receipt text formatted for ESC/POS thermal printers.
     * Grid width: 80mm = 48 columns | 58mm = 32 columns.
     */
    public String generateReceiptText(Map<String, Object> orderDetails, List<Map<String, Object>> items) {
        BillingConfig config = billingConfigRepository.findFirstByOrderByIdAsc()
                .orElseGet(BillingConfig::new);

        int maxCols = "58mm".equalsIgnoreCase(config.getPaperSize()) ? 32 : 48;
        StringBuilder sb = new StringBuilder();

        String lineSeparator = "=".repeat(maxCols) + "\n";
        String dashSeparator = "-".repeat(maxCols) + "\n";

        // Header
        sb.append(centerText(config.getRestaurantName(), maxCols)).append("\n");
        if (config.getAddressLine1() != null && !config.getAddressLine1().trim().isEmpty()) {
            sb.append(centerText(config.getAddressLine1().trim(), maxCols)).append("\n");
        }
        if (config.getGstin() != null && !config.getGstin().trim().isEmpty()) {
            sb.append(centerText("GSTIN: " + config.getGstin().trim(), maxCols)).append("\n");
        }

        sb.append(lineSeparator);

        // Order Metadata
        String type = orderDetails.getOrDefault("type", "TABLE").toString();
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));

        sb.append("Date: ").append(dateStr).append("\n");

        if ("DELIVERY".equalsIgnoreCase(type)) {
            sb.append("Type: DELIVERY\n");
            String cName = orderDetails.getOrDefault("customerName", "").toString();
            String cPhone = orderDetails.getOrDefault("customerPhone", "").toString();
            String address = orderDetails.getOrDefault("deliveryAddress", "").toString();
            if (address.isEmpty() && orderDetails.containsKey("address")) {
                address = orderDetails.get("address").toString();
            }
            if (address.isEmpty() && orderDetails.containsKey("tableName")) {
                String tbl = orderDetails.get("tableName").toString();
                if (tbl.startsWith("Delivery: ")) {
                    address = tbl.substring("Delivery: ".length()).trim();
                }
            }

            if (!cName.isEmpty()) sb.append("Cust : ").append(cName).append("\n");
            if (!cPhone.isEmpty()) sb.append("Phone: ").append(cPhone).append("\n");
            if (!address.isEmpty()) {
                sb.append("Addr : ");
                List<String> wrappedLines = wrapTextToLines(address, maxCols - 7);
                for (int i = 0; i < wrappedLines.size(); i++) {
                    if (i == 0) {
                        sb.append(wrappedLines.get(0)).append("\n");
                    } else {
                        sb.append("       ").append(wrappedLines.get(i)).append("\n");
                    }
                }
            }
        } else if ("PICKUP".equalsIgnoreCase(type)) {
            sb.append("Type: PICKUP\n");
            String cName = orderDetails.getOrDefault("customerName", "").toString();
            if (!cName.isEmpty()) sb.append("Cust : ").append(cName).append("\n");
        } else {
            String tableName = orderDetails.getOrDefault("tableName", "T-01").toString();
            String waiter = orderDetails.getOrDefault("waiterName", "Staff").toString();
            sb.append(formatTwoColumn("Table: " + tableName, "Waiter: " + waiter, maxCols)).append("\n");
        }

        sb.append(dashSeparator);

        int itemWidth = maxCols == 32 ? 14 : 24;

        // Item Columns Header
        if (maxCols == 32) {
            // 58mm Layout (32 cols): Item (14) Qty(3) Rate(6) Total(6)
            sb.append(String.format("%-14s %3s %6s %6s\n", "ITEM", "QTY", "RATE", "TOTAL"));
        } else {
            // 80mm Layout (48 cols): Item (24) Qty(5) Rate(8) Total(8)
            sb.append(String.format("%-24s %5s %8s %8s\n", "ITEM", "QTY", "RATE", "TOTAL"));
        }
        sb.append(dashSeparator);

        // Items Loop
        double subtotal = 0.0;
        for (Map<String, Object> item : items) {
            String name = item.getOrDefault("name", "Item").toString();
            int qty = Integer.parseInt(item.getOrDefault("qty", 1).toString());
            double rate = Double.parseDouble(item.getOrDefault("rate", 0.0).toString());
            double itemTotal = qty * rate;
            subtotal += itemTotal;

            List<String> wrappedLines = wrapTextToLines(name, itemWidth);
            String firstLine = wrappedLines.isEmpty() ? name : wrappedLines.get(0);

            if (maxCols == 32) {
                sb.append(String.format("%-14s %3d %6.2f %6.2f\n", firstLine, qty, rate, itemTotal));
            } else {
                sb.append(String.format("%-24s %5d %8.2f %8.2f\n", firstLine, qty, rate, itemTotal));
            }

            // Print multi-line overflow below without repeating quantity/price
            for (int i = 1; i < wrappedLines.size(); i++) {
                if (maxCols == 32) {
                    sb.append(String.format("%-14s %3s %6s %6s\n", wrappedLines.get(i), "", "", ""));
                } else {
                    sb.append(String.format("%-24s %5s %8s %8s\n", wrappedLines.get(i), "", "", ""));
                }
            }
        }

        sb.append(dashSeparator);

        // Subtotal & Taxes
        sb.append(formatTwoColumn("Subtotal:", String.format("%.2f", subtotal), maxCols)).append("\n");

        if (config.isShowTaxDetails()) {
            double cgst = subtotal * 0.025;
            double sgst = subtotal * 0.025;
            sb.append(formatTwoColumn("CGST (2.5%):", String.format("%.2f", cgst), maxCols)).append("\n");
            sb.append(formatTwoColumn("SGST (2.5%):", String.format("%.2f", sgst), maxCols)).append("\n");
            double grandTotal = subtotal + cgst + sgst;
            sb.append(lineSeparator);
            sb.append(formatTwoColumn("GRAND TOTAL:", String.format("%.2f", grandTotal), maxCols)).append("\n");
        } else {
            sb.append(lineSeparator);
            sb.append(formatTwoColumn("GRAND TOTAL:", String.format("%.2f", subtotal), maxCols)).append("\n");
        }

        sb.append(lineSeparator);

        // Footer
        if (config.getFooterMessage() != null && !config.getFooterMessage().trim().isEmpty()) {
            sb.append(centerText(config.getFooterMessage().trim(), maxCols)).append("\n");
        }

        return sb.toString();
    }

    private List<String> wrapTextToLines(String text, int limit) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return lines;
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + (currentLine.length() > 0 ? 1 : 0) > limit) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
                if (word.length() > limit) {
                    lines.add(word.substring(0, limit));
                    currentLine.append(word.substring(limit));
                } else {
                    currentLine.append(word);
                }
            } else {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private String centerText(String text, int width) {
        if (text == null) return "";
        if (text.length() >= width) return text.substring(0, width);
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text;
    }

    private String formatTwoColumn(String left, String right, int width) {
        int leftWidth = width - right.length();
        if (leftWidth < 0) leftWidth = 0;
        String truncLeft = left.length() > leftWidth ? left.substring(0, leftWidth) : left;
        return String.format("%-" + leftWidth + "s%s", truncLeft, right);
    }
}
