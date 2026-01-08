/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators;

import com.apicatalog.vc.suite.SignatureSuite;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.PresentationGenerator;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.security.signature.jws2020.JsonWebKeyPair;
import org.eclipse.edc.security.signature.jws2020.Jws2020ProofDraft;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpIssuer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_LD;
import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.PresentationGeneratorConstants.CONTROLLER_ADDITIONAL_DATA;
import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.PresentationGeneratorConstants.VERIFIABLE_CREDENTIAL_PROPERTY;
import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.PresentationGeneratorConstants.VP_TYPE_PROPERTY;

/**
 * LdpPresentationCreator is a class that implements the PresentationCreator interface to generate Verifiable Presentations based on Verifiable Credential Containers.
 * VPs are represented as {@link JsonObject}.
 */
public class LdpPresentationGenerator implements PresentationGenerator<JsonObject> {

    public static final String ID_PROPERTY = "id";

    public static final String TYPE_ADDITIONAL_DATA = "types";
    public static final String HOLDER_PROPERTY = "holder";
    public static final URI ASSERTION_METHOD = URI.create("https://w3id.org/security#assertionMethod");
    private final PrivateKeyResolver privateKeyResolver;
    private final SignatureSuiteRegistry signatureSuiteRegistry;
    private final String defaultSignatureSuite;
    private final LdpIssuer ldpIssuer;
    private final TypeManager typeManager;
    private final String typeContext;

    public LdpPresentationGenerator(PrivateKeyResolver privateKeyResolver,
                                    SignatureSuiteRegistry signatureSuiteRegistry, String defaultSignatureSuite, LdpIssuer ldpIssuer,
                                    TypeManager typeManager, String typeContext) {
        this.privateKeyResolver = privateKeyResolver;
        this.signatureSuiteRegistry = signatureSuiteRegistry;
        this.defaultSignatureSuite = defaultSignatureSuite;
        this.ldpIssuer = ldpIssuer;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    /**
     * Will always throw an {@link UnsupportedOperationException}.
     * Please use {@link PresentationGenerator#generatePresentation(String, List, String, String, String, Map)} instead.
     */
    @Override
    public JsonObject generatePresentation(List<VerifiableCredentialContainer> credentials, String privateKeyAlias, String privateKeyId) {
        throw new UnsupportedOperationException("Must provide additional data: '%s' and '%s'".formatted(TYPE_ADDITIONAL_DATA, CONTROLLER_ADDITIONAL_DATA));

    }

    /**
     * Creates a presentation with the given credentials, key ID, and additional data. Note that JWT-VCs cannot be represented in LDP-VPs - while the spec would allow that
     * the JSON schema does not.
     *
     * @param participantContextId The ID of the participant context for which the presentation is being generated.
     * @param credentials          The list of Verifiable Credential Containers to include in the presentation.
     * @param privateKeyAlias      The alias of the private key to be used for generating the presentation.
     * @param publicKeyId          The ID used by the counterparty to resolve the public key for verifying the VP.
     * @param issuerId             The ID of this issuer. Usually a DID.
     * @param additionalData       The additional data to be included in the presentation.
     *                             It must contain a "types" field and optionally, a "suite" field to indicate the desired signature suite.
     *                             If the "suite" parameter is specified, it must be a W3C identifier for signature suites.
     * @return The created presentation as a JsonObject.
     * @throws IllegalArgumentException If the additional data does not contain "types",
     *                                  if no {@link SignatureSuite} is found for the provided suite identifier,
     *                                  if the key ID is not in URI format,
     *                                  or if one or more VerifiableCredentials cannot be represented in the JSON-LD format.
     */
    @Override
    public JsonObject generatePresentation(String participantContextId, List<VerifiableCredentialContainer> credentials, String privateKeyAlias, String publicKeyId, String issuerId, Map<String, Object> additionalData) {
        if (!additionalData.containsKey(TYPE_ADDITIONAL_DATA)) {
            throw new IllegalArgumentException("Must provide additional data: '%s'".formatted(TYPE_ADDITIONAL_DATA));
        }
        if (!additionalData.containsKey(CONTROLLER_ADDITIONAL_DATA)) {
            throw new IllegalArgumentException("Must provide additional data: '%s'".formatted(CONTROLLER_ADDITIONAL_DATA));
        }

        var suiteIdentifier = additionalData.getOrDefault("suite", defaultSignatureSuite).toString();
        var suite = signatureSuiteRegistry.getForId(suiteIdentifier);
        if (suite == null) {
            throw new IllegalArgumentException("No SignatureSuite for identifier '%s' was found.".formatted(suiteIdentifier));
        }

        if (credentials.stream().anyMatch(c -> c.format() != CredentialFormat.VC1_0_LD)) {
            throw new IllegalArgumentException("One or more VerifiableCredentials cannot be represented in the desired format %s.".formatted(VC1_0_LD));
        }

        // check if private key can be resolved
        var pk = privateKeyResolver.resolvePrivateKey(participantContextId, privateKeyAlias)
                .orElseThrow(f -> new IllegalArgumentException(f.getFailureDetail()));

        // Extract contexts from credentials and merge with base contexts
        var credentialContexts = extractContextsFromCredentials(credentials);
        var mergedContexts = mergeContexts(credentialContexts);

        var types = (List) additionalData.get(TYPE_ADDITIONAL_DATA);
        var presentationObject = Json.createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, stringArray(mergedContexts))
                .add(ID_PROPERTY, DcpConstants.DCP_CONTEXT_URL + "/id/" + UUID.randomUUID())
                .add(VP_TYPE_PROPERTY, stringArray(types))
                .add(HOLDER_PROPERTY, issuerId)
                .add(VERIFIABLE_CREDENTIAL_PROPERTY, toJsonArray(credentials))
                .build();

        return signPresentation(presentationObject, suite, suiteIdentifier, pk, publicKeyId, additionalData.get(CONTROLLER_ADDITIONAL_DATA).toString());
    }

    @NotNull
    private JsonArray toJsonArray(List<VerifiableCredentialContainer> credentials) {
        var array = Json.createArrayBuilder();
        credentials.stream()
                .map(VerifiableCredentialContainer::rawVc)
                .map(str -> {
                    try {
                        return typeManager.getMapper(typeContext).readValue(str, JsonObject.class);
                    } catch (JsonProcessingException e) {
                        throw new EdcException(e);
                    }
                })
                .forEach(array::add);
        return array.build();
    }

    /**
     * Extracts @context values from all credentials.
     * Handles both string and array context formats.
     *
     * @param credentials The list of credential containers
     * @return Set of context URLs found in the credentials
     */
    private Set<String> extractContextsFromCredentials(List<VerifiableCredentialContainer> credentials) {
        Set<String> contexts = new LinkedHashSet<>();
        
        for (var credential : credentials) {
            try {
                var credentialJson = typeManager.getMapper(typeContext)
                        .readValue(credential.rawVc(), JsonObject.class);
                
                if (credentialJson.containsKey(JsonLdKeywords.CONTEXT)) {
                    var contextValue = credentialJson.get(JsonLdKeywords.CONTEXT);
                    
                    // Handle array of contexts
                    if (contextValue instanceof JsonArray) {
                        var contextArray = (JsonArray) contextValue;
                        for (int i = 0; i < contextArray.size(); i++) {
                            var value = contextArray.get(i);
                            if (value instanceof jakarta.json.JsonString) {
                                contexts.add(((jakarta.json.JsonString) value).getString());
                            }
                        }
                    } else if (contextValue instanceof jakarta.json.JsonString) {
                        // Handle single context string
                        contexts.add(((jakarta.json.JsonString) contextValue).getString());
                    }
                }
            } catch (JsonProcessingException e) {
                // Skip credentials with invalid JSON - they will fail later in toJsonArray
            }
        }
        
        return contexts;
    }

    /**
     * Merges base VP contexts with contexts extracted from credentials.
     * Ensures W3C_CREDENTIALS_URL is first, PRESENTATION_EXCHANGE_URL is second,
     * followed by credential-specific contexts. Duplicates are avoided.
     *
     * @param credentialContexts Contexts extracted from credentials
     * @return Merged list of context URLs
     */
    private List<String> mergeContexts(Set<String> credentialContexts) {
        List<String> merged = new ArrayList<>();
        
        // Always add base contexts first
        merged.add(VcConstants.W3C_CREDENTIALS_URL);
        merged.add(VcConstants.PRESENTATION_EXCHANGE_URL);
        
        // Add credential contexts, avoiding duplicates
        for (String context : credentialContexts) {
            if (!merged.contains(context)) {
                merged.add(context);
            }
        }
        
        return merged;
    }

    private JsonObject signPresentation(JsonObject presentationObject, SignatureSuite suite, String suiteIdentifier, PrivateKey pk, String publicKeyId, String controller) {
        var composedKeyId = publicKeyId;
        if (!publicKeyId.startsWith(controller)) {
            composedKeyId = controller + "#" + publicKeyId;
        }

        var keyIdUri = URI.create(composedKeyId);
        var controllerUri = URI.create(controller);
        var verificationMethodType = URI.create(suiteIdentifier);

        var jwk = CryptoConverter.createJwk(new KeyPair(null, pk));

        var keypair = new JsonWebKeyPair(keyIdUri, verificationMethodType, controllerUri, jwk);

        var proofDraft = Jws2020ProofDraft.Builder.newInstance()
                .proofPurpose(ASSERTION_METHOD)
                .verificationMethod(new JsonWebKeyPair(keyIdUri, verificationMethodType, controllerUri, null))
                .created(Instant.now())
                .mapper(typeManager.getMapper(typeContext))
                .build();

        return ldpIssuer.signDocument(suite, presentationObject, keypair, proofDraft)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    private JsonArrayBuilder stringArray(Collection<?> values) {
        var ja = Json.createArrayBuilder();
        values.forEach(s -> ja.add(s.toString()));
        return ja;
    }

}
