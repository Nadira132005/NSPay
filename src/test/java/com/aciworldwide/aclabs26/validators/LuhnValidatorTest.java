package com.aciworldwide.aclabs26.validators;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LuhnValidatorTest {

    private final LuhnValidator luhnValidator = new LuhnValidator();

    @Test
    void shouldAcceptValidCardNumber() {
        assertThat(luhnValidator.isValid("4242424242424242")).isTrue();
    }

    @Test
    void shouldRejectInvalidCardNumber() {
        assertThat(luhnValidator.isValid("9999888877776666")).isFalse();
    }
}
