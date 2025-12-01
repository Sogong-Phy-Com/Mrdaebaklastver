package com.mrdabak.dinnerservice.voice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoiceOrderItem {
    @JsonProperty("item")
    private String key;
    private String name;
    private Integer quantity;
    private String action;
}


