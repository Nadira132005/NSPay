package com.nspay.validators;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ResponseCode { //ResponseCode.getCode() returns a String -> e.g "00"
    APPROVED("00"),
    INACTIVE_ACCOUNT("13"),
    INVALID_CARD_NUMBER("14"),
    NOT_SUFFICIENT_FUNDS("51"),
    DAILY_TX_SUM_EXCEEDED("61"),
    DAILY_TX_LIMIT_EXCEEDED("65");

    private final String code;

    ResponseCode(String code) {
        this.code = code;
    }

    @JsonValue //  enum -> JSON
    public String getCode() {
        return code;
    }
}
