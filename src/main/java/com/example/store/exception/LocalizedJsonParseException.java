package com.example.store.exception;

import com.google.gson.JsonParseException;
import lombok.Getter;

@Getter
public class LocalizedJsonParseException extends JsonParseException {
    private final String messageKey;
    private final Object[] args;

    public LocalizedJsonParseException(String messageKey, Object[] args, String fallbackMessage, Throwable cause) {
        super(fallbackMessage, cause);
        this.messageKey = messageKey;
        this.args = args;
    }
}