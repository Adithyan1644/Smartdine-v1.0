package com.smartdine.coreheart;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;

/**
 * Entity representing billing receipt layout configuration.
 * Configures restaurant header, address, GSTIN, paper width (80mm/58mm),
 * tax details toggle, and footer messages.
 */
@Entity
@Table(name = "billing_configurations")
public class BillingConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id")
    private UUID restaurantId;

    @Column(name = "restaurant_name")
    private String restaurantName = "Surabhi Foods";

    @Column(name = "address_line1")
    private String addressLine1 = "";

    @Column(name = "gstin")
    private String gstin = "";

    @Column(name = "footer_message")
    private String footerMessage = "Thank you! Visit again.";

    @Column(name = "paper_size")
    private String paperSize = "80mm";

    @Column(name = "show_tax_details")
    private boolean showTaxDetails = true;

    public BillingConfig() {}

    public BillingConfig(UUID restaurantId, String restaurantName, String addressLine1,
                         String gstin, String footerMessage, String paperSize, boolean showTaxDetails) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.addressLine1 = addressLine1;
        this.gstin = gstin;
        this.footerMessage = footerMessage;
        this.paperSize = paperSize;
        this.showTaxDetails = showTaxDetails;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }

    public String getFooterMessage() {
        return footerMessage;
    }

    public void setFooterMessage(String footerMessage) {
        this.footerMessage = footerMessage;
    }

    public String getPaperSize() {
        return paperSize;
    }

    public void setPaperSize(String paperSize) {
        this.paperSize = paperSize;
    }

    public boolean isShowTaxDetails() {
        return showTaxDetails;
    }

    public void setShowTaxDetails(boolean showTaxDetails) {
        this.showTaxDetails = showTaxDetails;
    }
}
