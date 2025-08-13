package com.example.store.exception;

import lombok.Getter;

public abstract class WithMsgSrcArgs extends RuntimeException {
    @Getter
    private final Object[] args;


    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     * @param args    args with error values.
     */
    WithMsgSrcArgs(String message, Object[] args) {
        super(message);
        this.args = args;
    }
}
