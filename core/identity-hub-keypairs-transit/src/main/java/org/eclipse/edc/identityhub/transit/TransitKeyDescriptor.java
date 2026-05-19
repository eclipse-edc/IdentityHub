/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.transit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.edc.spi.result.Result;

import java.util.Comparator;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransitKeyDescriptor {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("data")
    private KeyData keyData;

    @JsonProperty("mount_type")
    private String mountType;

    public String getRequestId() {
        return requestId;
    }

    public KeyData getData() {
        return keyData;
    }

    public String getMountType() {
        return mountType;
    }

    @JsonIgnore
    public Result<KeyVersion> getLatestVersion() {
        var data = getData();
        if (data == null) {
            return Result.failure("no data returned from transit engine");
        }
        var keys = data.getKeys();
        if (keys.isEmpty()) {
            return Result.failure("no keys returned from transit engine");
        }
        var highestVersion = keys.keySet().stream().max(Comparator.comparing(Integer::valueOf)).orElse("1");
        var publicKey = keys.get(highestVersion);
        if (publicKey == null) {
            return Result.failure("no public key with version '%s' returned from transit engine".formatted(highestVersion));
        }
        return Result.success(publicKey);
    }

    @JsonIgnore
    private Map.Entry<String, KeyVersion> getHighestVersion() {
        return getData().getKeys().entrySet().stream().max(Map.Entry.comparingByKey()).orElse(null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyData {

        @JsonProperty("name")
        private String name;

        @JsonProperty("type")
        private String type;

        @JsonProperty("keys")
        private Map<String, KeyVersion> keys;

        @JsonProperty("latest_version")
        private int latestVersion;

        @JsonProperty("min_available_version")
        private int minAvailableVersion;

        @JsonProperty("min_decryption_version")
        private int minDecryptionVersion;

        @JsonProperty("min_encryption_version")
        private int minEncryptionVersion;

        @JsonProperty("exportable")
        private boolean exportable;

        @JsonProperty("deletion_allowed")
        private boolean deletionAllowed;

        @JsonProperty("supports_signing")
        private boolean supportsSigning;

        @JsonProperty("supports_encryption")
        private boolean supportsEncryption;

        @JsonProperty("supports_decryption")
        private boolean supportsDecryption;

        @JsonProperty("supports_derivation")
        private boolean supportsDerivation;

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public Map<String, KeyVersion> getKeys() {
            return keys;
        }

        public int getLatestVersion() {
            return latestVersion;
        }

        public int getMinAvailableVersion() {
            return minAvailableVersion;
        }

        public int getMinDecryptionVersion() {
            return minDecryptionVersion;
        }

        public int getMinEncryptionVersion() {
            return minEncryptionVersion;
        }

        public boolean isExportable() {
            return exportable;
        }

        public boolean isDeletionAllowed() {
            return deletionAllowed;
        }

        public boolean isSupportsSigning() {
            return supportsSigning;
        }

        public boolean isSupportsEncryption() {
            return supportsEncryption;
        }

        public boolean isSupportsDecryption() {
            return supportsDecryption;
        }

        public boolean isSupportsDerivation() {
            return supportsDerivation;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyVersion {

        @JsonProperty("name")
        private String name;

        @JsonProperty("public_key")
        private String publicKey;

        @JsonProperty("creation_time")
        private String creationTime;

        public String getName() {
            return name;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public String getCreationTime() {
            return creationTime;
        }
    }
}