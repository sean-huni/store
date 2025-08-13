package com.example.store.dto.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViolationDTO {
    private String field;       // Field Name with error
    private String rjctValue;   // Rejected Value
    private String errMsg;      // Error Message
    private String errCode;     // Error Code or Message Source Key
}