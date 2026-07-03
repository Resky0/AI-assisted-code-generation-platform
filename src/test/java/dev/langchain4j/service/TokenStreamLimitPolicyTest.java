package dev.langchain4j.service;

import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class TokenStreamLimitPolicyTest {
    @Test
    void initialGenerationStopsBeforeEleventhToolRound() {
        assertNull(TokenStreamLimitPolicy.violation(new TokenUsage(20_000), 9,
                Duration.ofMinutes(1), 40_000, 10, Duration.ofMinutes(10)));
        assertNotNull(TokenStreamLimitPolicy.violation(new TokenUsage(20_000), 10,
                Duration.ofMinutes(1), 40_000, 10, Duration.ofMinutes(10)));
    }

    @Test
    void initialAndEditTokenCapsAreInclusive() {
        assertNull(TokenStreamLimitPolicy.violation(new TokenUsage(39_999), 1,
                Duration.ZERO, 40_000, 10, Duration.ofMinutes(10)));
        assertNotNull(TokenStreamLimitPolicy.violation(new TokenUsage(40_000), 1,
                Duration.ZERO, 40_000, 10, Duration.ofMinutes(10)));
        assertNotNull(TokenStreamLimitPolicy.violation(new TokenUsage(15_000), 1,
                Duration.ZERO, 15_000, 5, Duration.ofMinutes(10)));
    }

    @Test
    void missingUsageStillUsesRoundAndDurationFallbacks() {
        assertNull(TokenStreamLimitPolicy.violation(null, 4,
                Duration.ofMinutes(9), 15_000, 5, Duration.ofMinutes(10)));
        assertNotNull(TokenStreamLimitPolicy.violation(null, 5,
                Duration.ofMinutes(9), 15_000, 5, Duration.ofMinutes(10)));
        assertNotNull(TokenStreamLimitPolicy.violation(null, 0,
                Duration.ofMinutes(10), 15_000, 5, Duration.ofMinutes(10)));
    }
}
