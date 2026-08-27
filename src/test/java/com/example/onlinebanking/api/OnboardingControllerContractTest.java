package com.example.onlinebanking.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingControllerContractTest {

    private final OnboardingController controller = new OnboardingController();

    @Test
    void exposesOnlyTheThreeContractOperations() {
        assertThat(controller.register(null).getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(controller.login(null).getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(controller.overview().getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    }
}
