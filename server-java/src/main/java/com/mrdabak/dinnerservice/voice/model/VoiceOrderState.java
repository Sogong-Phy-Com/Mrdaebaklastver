package com.mrdabak.dinnerservice.voice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VoiceOrderState {

    private String dinnerType;
    private String servingStyle;
    private List<VoiceOrderItem> menuAdjustments = new ArrayList<>();
    private String deliveryDate;
    private String deliveryTime;
    private String deliveryDateTime;
    private String deliveryAddress;
    private String contactName;
    private String contactPhone;
    private String specialRequests;
    private Boolean readyForConfirmation;
    private Boolean finalConfirmation;
    private List<String> needsMoreInfo = new ArrayList<>();

    public boolean hasDinnerSelection() {
        return dinnerType != null && !dinnerType.isBlank();
    }

    public boolean hasServingStyle() {
        return servingStyle != null && !servingStyle.isBlank();
    }

    public boolean hasDeliverySlot() {
        return (deliveryDate != null && !deliveryDate.isBlank() && deliveryTime != null && !deliveryTime.isBlank())
                || (deliveryDateTime != null && !deliveryDateTime.isBlank());
    }

    public boolean hasAddress() {
        return deliveryAddress != null && !deliveryAddress.isBlank();
    }

    public boolean hasContactPhone() {
        return contactPhone != null && !contactPhone.isBlank();
    }

    public boolean isReadyForCheckout() {
        return hasDinnerSelection() && hasServingStyle() && hasDeliverySlot() && hasAddress() && hasContactPhone();
    }

    public List<VoiceOrderItem> safeMenuAdjustments() {
        return Optional.ofNullable(menuAdjustments).orElseGet(List::of);
    }
}


