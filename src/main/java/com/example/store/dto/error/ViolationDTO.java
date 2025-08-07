package com.example.store.dto.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViolationDTO {
    private String field;
    private String rejectedValue;
    private String errMsg;
}