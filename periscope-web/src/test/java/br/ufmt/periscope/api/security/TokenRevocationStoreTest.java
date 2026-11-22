package br.ufmt.periscope.api.security;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenRevocationStoreTest {

    @Test
    void revokesAndPurgesExpiredInMemoryWithoutDatastore() {
        TokenRevocationStore store = new TokenRevocationStore();
        long future = Instant.now().getEpochSecond() + 3600;
        long past = Instant.now().getEpochSecond() - 10;

        store.revoke("jti-active", future);
        store.revoke("jti-expired", past);

        assertThat(store.isRevoked("jti-active")).isTrue();
        assertThat(store.isRevoked("jti-expired")).isFalse();
        assertThat(store.isRevoked("jti-unknown")).isFalse();
        assertThat(store.isRevoked(null)).isFalse();
    }
}
