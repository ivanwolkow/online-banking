package com.example.onlinebanking.api;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingControllerContractTest {

    @Test
    void exposesOnlyTheThreeContractOperations() {
        assertThat(OnboardingController.class.getDeclaredMethods())
                .filteredOn(method -> method.isAnnotationPresent(PostMapping.class))
                .hasSize(2);
        assertThat(OnboardingController.class.getDeclaredMethods())
                .filteredOn(method -> method.isAnnotationPresent(GetMapping.class))
                .hasSize(1);
    }
}
