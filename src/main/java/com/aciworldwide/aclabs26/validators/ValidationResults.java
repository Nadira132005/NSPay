package com.aciworldwide.aclabs26.validators;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class ValidationResults {

    private final boolean isValid;
    private boolean isBusinessLogicViolation;
    private final String code;
    private final String message;

public static ValidationResults success( boolean isBusinessLogicViolation, String code, String message) {
return new ValidationResults(true, isBusinessLogicViolation, code, message);
}

public static ValidationResults failure(String responseCode, String responseMessage){
return new ValidationResults(false, responseCode, responseMessage);
}



}
