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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = TransitKeyConfig.Builder.class)
public class TransitKeyConfig {

    @JsonProperty("min_decryption_version")
    private Integer minDecryptionVersion;

    @JsonProperty("min_encryption_version")
    private Integer minEncryptionVersion;

    @JsonProperty("deletion_allowed")
    private Boolean deletionAllowed;

    @JsonProperty("exportable")
    private Boolean exportable;

    @JsonProperty("allow_plaintext_backup")
    private Boolean allowPlaintextBackup;

    // https://developer.hashicorp.com/vault/docs/concepts/duration-format
    @JsonProperty("auto_rotate_period")
    private String autoRotatePeriod;

    private TransitKeyConfig() {
    }

    public Integer getMinDecryptionVersion() {
        return minDecryptionVersion;
    }

    public Integer getMinEncryptionVersion() {
        return minEncryptionVersion;
    }

    public Boolean getDeletionAllowed() {
        return deletionAllowed;
    }

    public Boolean getExportable() {
        return exportable;
    }

    public Boolean getAllowPlaintextBackup() {
        return allowPlaintextBackup;
    }

    public String getAutoRotatePeriod() {
        return autoRotatePeriod;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final TransitKeyConfig entity;

        private Builder() {
            entity = new TransitKeyConfig();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder minDecryptionVersion(Integer minDecryptionVersion) {
            entity.minDecryptionVersion = minDecryptionVersion;
            return this;
        }

        public Builder minEncryptionVersion(Integer minEncryptionVersion) {
            entity.minEncryptionVersion = minEncryptionVersion;
            return this;
        }

        public Builder deletionAllowed(Boolean deletionAllowed) {
            entity.deletionAllowed = deletionAllowed;
            return this;
        }

        public Builder exportable(Boolean exportable) {
            entity.exportable = exportable;
            return this;
        }

        public Builder allowPlaintextBackup(Boolean allowPlaintextBackup) {
            entity.allowPlaintextBackup = allowPlaintextBackup;
            return this;
        }

        public Builder autoRotatePeriod(String autoRotatePeriod) {
            entity.autoRotatePeriod = autoRotatePeriod;
            return this;
        }

        public TransitKeyConfig build() {
            return entity;
        }
    }
}
