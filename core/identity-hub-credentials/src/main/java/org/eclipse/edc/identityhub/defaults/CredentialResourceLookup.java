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

package org.eclipse.edc.identityhub.defaults;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.query.ReflectionPropertyLookup;
import org.eclipse.edc.util.reflection.PathItem;
import org.eclipse.edc.util.reflection.ReflectionUtil;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * This class performs the lookup of properties in a {@link VerifiableCredentialResource}.
 * There is some special handling for raw JSON properties like the {@link VerifiableCredentialContainer#rawVc()} and the {@code credentialSubject}, as the latter is
 * basically schema-less.
 */
public class CredentialResourceLookup extends ReflectionPropertyLookup {
    @Override
    public Object getProperty(String key, Object object) {
        var fieldValue = super.getProperty(key, object);
        if (fieldValue instanceof Instant) {
            fieldValue = fieldValue.toString();
        }

        // the current implementation of the "likePredicate" method has a regex that doesn't account for newlines
        if (key.contains("rawVc")) {
            return fieldValue.toString().replace("\n", "");
        }

        // the VerifiableCredential has some dynamic types, such as the CredentialSubject
        if (fieldValue == null && key.contains("credentialSubject") && object instanceof VerifiableCredentialResource credentialResource) {
            fieldValue = handleCredentialSubject(key, credentialResource);
        }

        return fieldValue;
    }

    private Object handleCredentialSubject(String key, VerifiableCredentialResource credentialResource) {
        var path = PathItem.parse(key);
        var credentialSubjectPath = path.stream().dropWhile(p -> !p.toString().equals("credentialSubject"))
                .skip(1)
                .map(PathItem::toString)
                .collect(Collectors.joining("."));
        var subjects = credentialResource.getVerifiableCredential().credential().getCredentialSubject();

        return subjects.stream().map(subj -> ReflectionUtil.getFieldValue("claims." + credentialSubjectPath, subj))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
