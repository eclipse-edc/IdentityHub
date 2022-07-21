package org.eclipse.dataspaceconnector.identityhub.verifier;

import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONObject;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;

public class VerifiableCredentialUtil {

    private static final String VERIFIABLE_CREDENTIALS_KEY = "vc";
    private static final String CREDENTIALS_ID_KEY = "id";

    public static Result<Map.Entry<String, Object>> extractCredential(SignedJWT jwt) {
        try {
            var payload = jwt.getPayload().toJSONObject();
            var credentialId = extractVcId(payload);
            if (credentialId.failed()) {
                return Result.failure(credentialId.getFailureMessages());
            }

            return Result.success(new AbstractMap.SimpleEntry<>(credentialId.getContent(), payload));
        } catch (RuntimeException e) {
            return Result.failure(Objects.requireNonNullElseGet(e.getMessage(), () -> e.getClass().toString()));
        }
    }

    private static Result<String> extractVcId(JSONObject jsonPayload) {
        var payload = (Map<String, Object>) jsonPayload;

        if (!payload.containsKey(VERIFIABLE_CREDENTIALS_KEY)) {
            return Result.failure(String.format("No %s field found", VERIFIABLE_CREDENTIALS_KEY));
        }
        var vc = (Map<String, Object>) payload.get(VERIFIABLE_CREDENTIALS_KEY);
        var vcId = vc.get(CREDENTIALS_ID_KEY);
        return vcId == null ? Result.failure("vc id not found") : Result.success(vcId.toString());
    }
}
