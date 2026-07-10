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

import org.eclipse.edc.identityhub.spi.transformation.ScopeMappingRegistry;
import org.eclipse.edc.spi.query.Criterion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An implementation of the {@link ScopeMappingRegistry} interface that maintains a list of regex-based scope mappings.
 * <p>
 * Thread-Safety: the class is designed to handle multiple threads concurrently accessing or modifying the mappings.
 */
public class ScopeMappingRegistryImpl implements ScopeMappingRegistry {

    // this might be accessed from multiple threads (API requests), so it needs to be thread-safe
    private final List<ScopeMapping> mappings = new CopyOnWriteArrayList<>();

    @Override
    public void addMapping(String regex, Criterion criterionTemplate) {
        // Pattern.compile throws PatternSyntaxException on an invalid regex, surfacing config errors early
        mappings.add(new ScopeMapping(Pattern.compile(regex), criterionTemplate));
    }

    @Override
    public List<Criterion> map(String scope) {
        var result = new ArrayList<Criterion>();
        if (scope == null) {
            return result;
        }
        for (var mapping : mappings) {
            var matcher = mapping.pattern().matcher(scope);
            if (matcher.matches()) {
                var template = mapping.template();
                var left = substitute(matcher, template.getOperandLeft());
                var right = substitute(matcher, template.getOperandRight());
                result.add(new Criterion(left, template.getOperator(), right));
            }
        }
        return result;
    }

    /**
     * Substitutes regex capture groups ({@code $0}, {@code $1}, {@code ${1}}, …) into a (String) operand. A group that
     * did not participate in the match is substituted with an empty string, and a reference to a non-existent group is
     * left as-is. Non-String operands are returned unchanged.
     */
    private static Object substitute(Matcher matcher, Object operand) {
        if (!(operand instanceof String template)) {
            return operand;
        }

        var sb = new StringBuilder();
        var i = 0;
        while (i < template.length()) {
            var c = template.charAt(i);
            if (c == '$' && i + 1 < template.length()) {
                var braced = template.charAt(i + 1) == '{';
                var start = braced ? i + 2 : i + 1;
                var j = start;
                while (j < template.length() && Character.isDigit(template.charAt(j))) {
                    j++;
                }
                var validBraces = !braced || (j < template.length() && template.charAt(j) == '}');
                if (j > start && validBraces) {
                    // the substring is all digits; parseInt can only fail on overflow, which can never be
                    // a valid group index, so an unparseable/out-of-range reference is left as a literal
                    var group = parseGroup(template.substring(start, j));
                    if (group >= 0 && group <= matcher.groupCount()) {
                        var value = matcher.group(group);
                        sb.append(value == null ? "" : value);
                        i = braced ? j + 1 : j;
                        continue;
                    }
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static int parseGroup(String digits) {
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            // digit run too long to fit in an int; cannot be a valid group index
            return -1;
        }
    }

    private record ScopeMapping(Pattern pattern, Criterion template) {
    }
}
