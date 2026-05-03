package com.aciworldwide.aclabs26.validators;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ResponseCode { //ResponseCode.getCode() returns a String -> e.g "00"
    APPROVED("00"),
    INVALID_CARD_NUMBER("14"),
    NOT_SUFFICIENT_FUNDS("51"),
    DAILY_TX_SUM_EXCEEDED("61"),
    DAILY_TX_LIMIT_EXCEEDED("65");

    private final String code;

    ResponseCode(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}
