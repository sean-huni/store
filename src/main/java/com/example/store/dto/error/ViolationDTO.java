package com.example.store.dto.error;

public record ViolationDTO(
        String field,       // Field Name with error
        String rjctValue,   // Rejected Value
        String errMsg,      // Error Message
        String errCode)     // Error Code or Message Source Key
{
}