package com.example.store.dto.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an error in the validation process.
 * Contains information about the field that failed validation,
 * the rejected value, and the error message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViolationDTO {
    private String field;
    private String rejectedValue;
    private String errMsg;
}