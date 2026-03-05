package org.eclipse.edc.identityhub.spi.participantcontext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ParticipantContextIdTest {

    @BeforeEach
    void setup(){
        ParticipantContextId.setMonitor(mock());
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "U29tZVRleHQ=", "SGVsbG9Xb3JsZA==" })
        // Base64 URL-safe encoded strings
    void onEncoded_validBase64_returnsDecodedString(String encoded) {
        var result = ParticipantContextId.onEncoded(encoded);
        assertThat(result.getContent()).isEqualTo(new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Invalid!!String", "----", "1234567890", "test", "aaaa" })
        // Invalid Base64 strings
    void onEncoded_invalidBase64_returnsSameString(String invalidEncoded) {
        var result = ParticipantContextId.onEncoded(invalidEncoded);

        assertThat(result.getContent()).isEqualTo(invalidEncoded);
    }

    @ParameterizedTest
    @ValueSource(strings = { "U29tZVRle#Q", "===", "abcd=aaa" })
        // Strings that fail decoding
    void onEncoded_encodedStringFailsDecoding_returnsOriginalString(String encoded) {
        var result = ParticipantContextId.onEncoded(encoded);
        assertThat(result.getContent()).isEqualTo(encoded);
    }

    @ParameterizedTest
    @ValueSource(strings = { "A", "AC", "ACD" })
        // Strings shorter than the length check
    void onEncoded_encodedStringShorterThanRequired_returnsSameString(String encoded) {
        var result = ParticipantContextId.onEncoded(encoded);

        assertThat(result.getContent()).isEqualTo(encoded);
    }
}
