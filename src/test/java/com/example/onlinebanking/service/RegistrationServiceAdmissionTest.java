package com.example.onlinebanking.service;

import com.example.onlinebanking.model.AddressRequest;
import com.example.onlinebanking.model.RegisterRequest;
import com.example.onlinebanking.config.AppProperties;
import com.example.onlinebanking.persistence.DatabaseBusyException;
import com.example.onlinebanking.persistence.DatabaseOperationGate;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegistrationServiceAdmissionTest {
    @Test
    void rejectsRegistrationBeforePersistenceWorkWhenNoPermitIsAvailable() {
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        IbanProvider ibanProvider = mock(IbanProvider.class);
        RegistrationPersistenceService persistence = mock(RegistrationPersistenceService.class);
        DatabaseOperationGate gate = new DatabaseOperationGate(properties());
        gate.acquirePermit();
        RegistrationService service = new RegistrationService(
                properties(),
                Clock.systemUTC(),
                encoder,
                new SecureRandom(),
                ibanProvider,
                gate,
                persistence
        );

        assertThatThrownBy(() -> service.register(request()))
                .isInstanceOf(DatabaseBusyException.class);

        verifyNoInteractions(persistence);
    }

    @Test
    void acquiresOnePermitImmediatelyBeforePersistenceWork() {
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        IbanProvider ibanProvider = mock(IbanProvider.class);
        RegistrationPersistenceService persistence = mock(RegistrationPersistenceService.class);
        DatabaseOperationGate gate = mock(DatabaseOperationGate.class);
        when(encoder.encode(anyString())).thenReturn("hash");
        when(ibanProvider.provide()).thenReturn("iban");
        RegistrationService service = new RegistrationService(
                properties(),
                Clock.systemUTC(),
                encoder,
                new SecureRandom(),
                ibanProvider,
                gate,
                persistence
        );

        service.register(request());

        var order = inOrder(gate, persistence);
        order.verify(gate).acquirePermit();
        order.verify(persistence).createCustomerAndAccount(
                request(),
                "ada.lovelace",
                "NL",
                "hash",
                "iban"
        );
        verify(gate).acquirePermit();
    }

    private static RegisterRequest request() {
        return new RegisterRequest(
                "Ada Lovelace",
                "ada.lovelace",
                LocalDate.of(1990, 12, 10),
                new AddressRequest("Keizersgracht", "123A", "1015 CJ", "Amsterdam", "NL")
        );
    }

    private static AppProperties properties() {
        return new AppProperties(
                "UTC",
                new AppProperties.Registration(18, List.of("NL", "BE")),
                new AppProperties.Account("NL", "RBNK", "CURRENT", "EUR", BigDecimal.ZERO),
                new AppProperties.Security(new AppProperties.Jwt("online-banking", java.time.Duration.ofMinutes(15), "secret")),
                new AppProperties.Database(new AppProperties.RateLimit(true, 2))
        );
    }
}
