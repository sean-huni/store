package com.example.store.exception;

public class CustomerNotFoundException extends WithMsgSrcArgs {

    public CustomerNotFoundException(final String message, Object[] args) {
        super(message, args);
    }
}
