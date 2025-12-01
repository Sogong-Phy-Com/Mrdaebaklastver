package com.mrdabak.dinnerservice.voice;

public class VoiceOrderException extends RuntimeException {
    public VoiceOrderException(String message) {
        super(message);
    }

    public VoiceOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}


