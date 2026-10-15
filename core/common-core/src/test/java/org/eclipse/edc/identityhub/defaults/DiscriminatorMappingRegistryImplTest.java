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

package org.eclipse.edc.identityhub.defaults;

import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscriminatorMappingRegistryImplTest {

    private final DiscriminatorMappingRegistryImpl registry = new DiscriminatorMappingRegistryImpl();

    @Test
    void addMapping_shouldStoreMapping() {
        registry.addMapping("alias1", "discriminator1");

        assertThat(registry.getMapping("alias1")).isEqualTo("discriminator1");
    }

    @Test
    void addMapping_shouldOverwriteExistingMapping() {
        registry.addMapping("alias1", "discriminator1");
        registry.addMapping("alias1", "discriminator2");

        assertThat(registry.getMapping("alias1")).isEqualTo("discriminator2");
    }

    @Test
    void getMapping_shouldReturnDefaultWhenNotFound() {
        assertThat(registry.getMapping("nonexistent")).isEqualTo("nonexistent");
    }

    @Test
    void getMapping_shouldReturnMappedDiscriminator() {
        registry.addMapping("short", "long.discriminator.value");

        assertThat(registry.getMapping("short")).isEqualTo("long.discriminator.value");
    }

    @Test
    void addMapping_withMultipleMappings_shouldStoreAll() {
        registry.addMapping("alias1", "discriminator1");
        registry.addMapping("alias2", "discriminator2");
        registry.addMapping("alias3", "discriminator3");

        assertThat(registry.getMapping("alias1")).isEqualTo("discriminator1");
        assertThat(registry.getMapping("alias2")).isEqualTo("discriminator2");
        assertThat(registry.getMapping("alias3")).isEqualTo("discriminator3");
    }

    @Test
    void addMapping_duplicateDiscriminator_shouldThrowException() {
        registry.addMapping("alias1", "discriminator1");
        assertThatThrownBy(() -> registry.addMapping("alias2", "discriminator1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addMapping_forParticipant_shouldOnlyApplyToThatParticipant() {
        registry.addMapping("participant1", "alias1", "discriminator1");

        assertThat(registry.getMapping("participant1", "alias1")).isEqualTo("discriminator1");
        assertThat(registry.getMapping("participant2", "alias1")).isEqualTo("alias1");
        assertThat(registry.getMapping("alias1")).isEqualTo("alias1");
    }

    @Test
    void addMapping_forParticipant_duplicateDiscriminator_shouldThrowException() {
        registry.addMapping("participant1", "alias1", "discriminator1");

        assertThatThrownBy(() -> registry.addMapping("participant1", "alias2", "discriminator1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addMapping_forParticipant_sameDiscriminatorAsOtherParticipant_shouldSucceed() {
        registry.addMapping("participant1", "alias1", "discriminator1");
        registry.addMapping("participant2", "alias2", "discriminator1");
        registry.addMapping("alias3", "discriminator1");

        assertThat(registry.getMapping("participant1", "alias1")).isEqualTo("discriminator1");
        assertThat(registry.getMapping("participant2", "alias2")).isEqualTo("discriminator1");
        assertThat(registry.getMapping("alias3")).isEqualTo("discriminator1");
    }

    @Test
    void getMapping_forParticipant_shouldFallBackToRuntimeMapping() {
        registry.addMapping("alias1", "discriminator1");

        assertThat(registry.getMapping("participant1", "alias1")).isEqualTo("discriminator1");
        assertThat(registry.getMapping("participant1", "unknown")).isEqualTo("unknown");
    }

    @Test
    void getMapping_forNullParticipant_shouldUseRuntimeMapping() {
        registry.addMapping("alias1", "discriminator1");
        registry.addMapping("participant1", "alias2", "discriminator2");

        assertThat(registry.getMapping(null, "alias1")).isEqualTo("discriminator1");
        assertThat(registry.getMapping(null, "alias2")).isEqualTo("alias2");
    }

    @Test
    void getMapping_forParticipant_shouldPreferParticipantMapping() {
        registry.addMapping("alias1", "discriminator1");
        registry.addMapping("participant1", "alias1", "discriminator2");

        assertThat(registry.getMapping("participant1", "alias1")).isEqualTo("discriminator2");
        assertThat(registry.getMapping("participant2", "alias1")).isEqualTo("discriminator1");
    }

    @Test
    void removeMappings_shouldOnlyRemoveParticipantMappings() {
        registry.addMapping("alias1", "discriminator1");
        registry.addMapping("participant1", "alias2", "discriminator2");
        registry.addMapping("participant2", "alias3", "discriminator3");

        registry.removeMappings("participant1");

        assertThat(registry.getMapping("participant1", "alias2")).isEqualTo("alias2");
        assertThat(registry.getMapping("participant1", "alias1")).isEqualTo("discriminator1");
        assertThat(registry.getMapping("participant2", "alias3")).isEqualTo("discriminator3");
    }

    @Test
    void removeMappings_unknownParticipant_shouldNotThrow() {
        registry.removeMappings("unknown");
    }

    @Test
    void on_participantContextDeleted_shouldRemoveParticipantMappings() {
        registry.addMapping("participant1", "alias1", "discriminator1");
        var event = ParticipantContextDeleted.Builder.newInstance().participantContextId("participant1").build();

        registry.on(EventEnvelope.Builder.newInstance().at(System.currentTimeMillis()).payload(event).build());

        assertThat(registry.getMapping("participant1", "alias1")).isEqualTo("alias1");
    }
}
