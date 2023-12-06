/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core.creators;

import com.apicatalog.ld.signature.SignatureSuite;
import com.apicatalog.ld.signature.method.VerificationMethod;
import com.apicatalog.vc.integrity.DataIntegrityProofOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.edc.identityhub.spi.generator.PresentationCreator;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.identitytrust.verification.SignatureSuiteRegistry;
import org.eclipse.edc.security.signature.jws2020.JwkMethod;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.util.reflection.ReflectionUtil;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpIssuer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.IATP_CONTEXT_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.PRESENTATION_EXCHANGE_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.W3C_CREDENTIALS_URL;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;

/**
 * LdpPresentationCreator is a class that implements the PresentationCreator interface to generate Verifiable Presentations based on Verifiable Credential Containers.
 * VPs are represented as {@link JsonObject}.
 */
public class LdpPresentationCreator implements PresentationCreator<JsonObject> {

    public static final String ID_PROPERTY = "id";
    public static final String TYPE_PROPERTY = "type";
    public static final String HOLDER_PROPERTY = "holder";
    public static final String VERIFIABLE_CREDENTIAL_PROPERTY = "verifiableCredential";
    private final PrivateKeyResolver privateKeyResolver;
    private final String issuerId;
    private final SignatureSuiteRegistry signatureSuiteRegistry;
    private final String defaultSignatureSuite;
    private final LdpIssuer ldpIssuer;
    private final ObjectMapper mapper;

    public LdpPresentationCreator(PrivateKeyResolver privateKeyResolver, String ownDid,
                                  SignatureSuiteRegistry signatureSuiteRegistry, String defaultSignatureSuite, LdpIssuer ldpIssuer, ObjectMapper mapper) {
        this.privateKeyResolver = privateKeyResolver;
        this.issuerId = ownDid;
        this.signatureSuiteRegistry = signatureSuiteRegistry;
        this.defaultSignatureSuite = defaultSignatureSuite;
        this.ldpIssuer = ldpIssuer;
        this.mapper = mapper;
    }

    /**
     * Will always throw an {@link UnsupportedOperationException}.
     * Please use {@link LdpPresentationCreator#createPresentation(List, String, Map)} instead.
     */
    @Override
    public JsonObject createPresentation(List<VerifiableCredentialContainer> credentials, String keyId) {
        throw new UnsupportedOperationException("Must provide additional data: 'types'");

    }

    /**
     * Creates a presentation with the given credentials, key ID, and additional data. Note that JWT-VCs cannot be represented in LDP-VPs - while the spec would allow that
     * the JSON schema does not.
     *
     * @param credentials    The list of Verifiable Credential Containers to include in the presentation.
     * @param keyId          The key ID of the private key to be used for generating the presentation. Must be a URI.
     * @param additionalData The additional data to be included in the presentation.
     *                       It must contain a "types" field and optionally, a "suite" field to indicate the desired signature suite.
     *                       If the "suite" parameter is specified, it must be a W3C identifier for signature suites.
     * @return The created presentation as a JsonObject.
     * @throws IllegalArgumentException If the additional data does not contain "types",
     *                                  if no {@link SignatureSuite} is found for the provided suite identifier,
     *                                  if the key ID is not in URI format,
     *                                  or if one or more VerifiableCredentials cannot be represented in the JSON-LD format.
     */
    @Override
    public JsonObject createPresentation(List<VerifiableCredentialContainer> credentials, String keyId, Map<String, Object> additionalData) {
        if (!additionalData.containsKey("types")) {
            throw new IllegalArgumentException("Must provide additional data: 'types'");
        }

        var keyIdUri = URI.create(keyId);

        var suiteIdentifier = additionalData.getOrDefault("suite", defaultSignatureSuite).toString();
        var suite = signatureSuiteRegistry.getForId(suiteIdentifier);
        if (suite == null) {
            throw new IllegalArgumentException("No SignatureSuite for identifier '%s' was found.".formatted(suiteIdentifier));
        }

        if (credentials.stream().anyMatch(c -> c.format() != CredentialFormat.JSON_LD)) {
            throw new IllegalArgumentException("One or more VerifiableCredentials cannot be represented in the desired format " + CredentialFormat.JSON_LD);
        }

        // check if private key can be resolved
        var pk = ofNullable(privateKeyResolver.resolvePrivateKey(keyId, PrivateKeyWrapper.class))
                .orElseThrow(() -> new IllegalArgumentException("No key could be found with key ID '%s'.".formatted(keyId)));

        var types = (List) additionalData.get("types");
        var presentationObject = Json.createObjectBuilder()
                .add(CONTEXT, stringArray(List.of(W3C_CREDENTIALS_URL, PRESENTATION_EXCHANGE_URL)))
                .add(ID_PROPERTY, IATP_CONTEXT_URL + "/id/" + UUID.randomUUID())
                .add(TYPE_PROPERTY, stringArray(types))
                .add(HOLDER_PROPERTY, issuerId)
                .add(VERIFIABLE_CREDENTIAL_PROPERTY, toJsonArray(credentials))
                .build();

        return signPresentation(presentationObject, suite, pk, keyIdUri);
    }

    @NotNull
    private JsonArray toJsonArray(List<VerifiableCredentialContainer> credentials) {
        var array = Json.createArrayBuilder();
        credentials.stream()
                .map(VerifiableCredentialContainer::rawVc)
                .map(str -> {
                    try {
                        return mapper.readValue(str, JsonObject.class);
                    } catch (JsonProcessingException e) {
                        throw new EdcException(e);
                    }
                })
                .forEach(array::add);
        return array.build();
    }

    private JsonObject signPresentation(JsonObject presentationObject, SignatureSuite suite, PrivateKeyWrapper pk, URI keyId) {
        var type = URI.create(suite.getId().toString());
        var jwk = extractKey(pk);
        var keypair = new JwkMethod(keyId, type, null, jwk);

        var options = (DataIntegrityProofOptions) suite.createOptions();
        options.purpose(URI.create("https://w3id.org/security#assertionMethod"));
        options.verificationMethod(getVerificationMethod(keyId));
        return ldpIssuer.signDocument(presentationObject, keypair, options)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    private VerificationMethod getVerificationMethod(URI keyId) {
        return new JwkMethod(keyId, null, null, null);
    }

    private JWK extractKey(PrivateKeyWrapper pk) {
        // this is a bit of a hack. ultimately, the PrivateKeyWrapper class should have a getter for the actual private key
        return ReflectionUtil.getFieldValue("privateKey", pk);
    }

    private JsonArrayBuilder stringArray(Collection<?> values) {
        var ja = Json.createArrayBuilder();
        values.forEach(s -> ja.add(s.toString()));
        return ja;
    }

}
