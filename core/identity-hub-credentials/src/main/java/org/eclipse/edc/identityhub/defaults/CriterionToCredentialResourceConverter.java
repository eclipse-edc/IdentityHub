/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

import org.eclipse.edc.connector.core.store.CriterionToPredicateConverterImpl;
import org.eclipse.edc.identityhub.spi.store.model.VerifiableCredentialResource;
import org.eclipse.edc.spi.query.CriterionToPredicateConverter;
import org.eclipse.edc.spi.types.PathItem;
import org.eclipse.edc.util.reflection.ReflectionUtil;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;

public class CriterionToCredentialResourceConverter extends CriterionToPredicateConverterImpl implements CriterionToPredicateConverter {
    @Override
    protected Object property(String key, Object object) {
        var fieldValue = super.property(key, object);
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

        var claims = subjects.stream().map(subj -> ReflectionUtil.getFieldValue("claims." + credentialSubjectPath, subj))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        return claims;
    }
}
