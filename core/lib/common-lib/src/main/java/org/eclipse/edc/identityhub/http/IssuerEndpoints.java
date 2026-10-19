/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.http;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Restricts the HTTP requests sent to an Issuer's {@code IssuerService} endpoint. That endpoint is taken from the
 * Issuer's DID document, i.e. whoever controls the DID decides where IdentityHub sends requests, so they must not be
 * able to point it at hosts that are only reachable from within the network IdentityHub runs in.
 * <p>
 * If the restriction is enabled with {@link #RESTRICT_SETTING}, requests must use {@code https} and their host must
 * resolve to public addresses only, unless the host is allow-listed. Redirects are not followed then, since they could
 * lead anywhere. It is disabled by default, as Issuers that run in the same cluster as IdentityHub are commonly reached
 * at internal addresses.
 */
public final class IssuerEndpoints {

    public static final String RESTRICT_SETTING = "edc.iam.issuer.endpoint.restrict";
    public static final String RESTRICT_DESCRIPTION = "Whether requests to an Issuer's IssuerService endpoint, which is taken from the Issuer's DID document, " +
            "must use HTTPS and may only be sent to hosts that resolve to public addresses, and whether redirects are not followed. " +
            "Should be enabled if participants or Issuers are not trusted to only publish endpoints of legitimate Issuers in their DID documents.";
    public static final String ALLOWED_HOSTS_SETTING = "edc.iam.issuer.endpoint.allowed.hosts";
    public static final String ALLOWED_HOSTS_DESCRIPTION = "Comma-separated list of hosts (host names or IP addresses) that requests to an Issuer's IssuerService " +
            "endpoint may be sent to, although they use plain HTTP or resolve to loopback, link-local or private addresses, e.g. an Issuer running in the same cluster. " +
            "Only has an effect if '" + RESTRICT_SETTING + "' is enabled.";

    /**
     * The maximum size of a response body that is read from an Issuer.
     */
    public static final long MAX_RESPONSE_SIZE = 1024 * 1024;

    private IssuerEndpoints() {
    }

    /**
     * Parses a comma-separated list of hosts, as configured with {@link #ALLOWED_HOSTS_SETTING}.
     */
    public static Set<String> parseAllowedHosts(@Nullable String allowedHosts) {
        if (allowedHosts == null || allowedHosts.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(allowedHosts.split(","))
                .map(String::trim)
                .filter(host -> !host.isEmpty())
                .map(IssuerEndpoints::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Derives a client from the given one that only sends requests to Issuer endpoints that pass
     * {@link #check(String, Set)}, connects to public addresses only, and does not follow redirects. Addresses are
     * checked when the host is resolved for the connection, so a host cannot pass a check and then resolve differently
     * when connecting.
     * <p>
     * Note that the target host is not resolved locally if the client is configured to use a proxy, in which case the
     * proxy needs to restrict the hosts it connects to.
     *
     * @param client       the client to derive from
     * @param allowedHosts the hosts that are exempt from the restrictions
     */
    public static OkHttpClient restrict(OkHttpClient client, Set<String> allowedHosts) {
        var dns = client.dns();
        return client.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .addInterceptor(chain -> {
                    var url = chain.request().url().toString();
                    var result = check(url, allowedHosts);
                    if (result.failed()) {
                        throw new IOException(result.getFailureDetail());
                    }
                    return chain.proceed(chain.request());
                })
                .dns(hostname -> {
                    var addresses = dns.lookup(hostname);
                    if (isAllowed(hostname, allowedHosts)) {
                        return addresses;
                    }
                    var publicAddresses = addresses.stream().filter(IssuerEndpoints::isPublic).toList();
                    if (publicAddresses.isEmpty()) {
                        throw new UnknownHostException("The Issuer endpoint host '%s' does not resolve to any public address".formatted(hostname));
                    }
                    return publicAddresses;
                })
                .build();
    }

    /**
     * Checks that the URL of an Issuer endpoint is an {@code https} URL, and that its host is not an IP address that is
     * not public, unless the host is allow-listed. Host names are checked when they are resolved, see
     * {@link #restrict(OkHttpClient, Set)}.
     *
     * @param url          the URL to check
     * @param allowedHosts the hosts that are exempt from the restrictions
     * @return the parsed URL, or a failure if the URL is not acceptable
     */
    public static Result<HttpUrl> check(String url, Set<String> allowedHosts) {
        var httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            return failure("The Issuer endpoint '%s' is not a valid HTTP URL".formatted(url));
        }
        if (isAllowed(httpUrl.host(), allowedHosts)) {
            return success(httpUrl);
        }
        if (!httpUrl.isHttps()) {
            return failure("The Issuer endpoint '%s' does not use HTTPS".formatted(url));
        }
        var address = ipLiteral(httpUrl.host());
        if (address != null && !isPublic(address)) {
            return failure("The Issuer endpoint '%s' does not point to a public address".formatted(url));
        }
        return success(httpUrl);
    }

    /**
     * Reads a response body as a string, failing if it is larger than {@link #MAX_RESPONSE_SIZE}.
     */
    public static String readBody(ResponseBody body) throws IOException {
        if (body.contentLength() > MAX_RESPONSE_SIZE) {
            throw new IOException("The response of the Issuer exceeds %d bytes".formatted(MAX_RESPONSE_SIZE));
        }
        var source = body.source();
        if (source.request(MAX_RESPONSE_SIZE + 1)) {
            throw new IOException("The response of the Issuer exceeds %d bytes".formatted(MAX_RESPONSE_SIZE));
        }
        var charset = body.contentType() != null ? body.contentType().charset(StandardCharsets.UTF_8) : StandardCharsets.UTF_8;
        return source.getBuffer().readString(charset);
    }

    /**
     * Whether an address may be connected to without being allow-listed, i.e. it is not a loopback, link-local,
     * private, unique local, shared (carrier-grade NAT), wildcard or multicast address.
     */
    static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() ||
                address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        var bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            var first = bytes[0] & 0xff;
            var second = bytes[1] & 0xff;
            // 0.0.0.0/8 "this network" and 100.64.0.0/10 shared address space
            return first != 0 && !(first == 100 && second >= 64 && second <= 127);
        }
        if (address instanceof Inet6Address) {
            // fc00::/7 unique local addresses
            return (bytes[0] & 0xfe) != 0xfc;
        }
        return true;
    }

    private static boolean isAllowed(String host, Set<String> allowedHosts) {
        return allowedHosts.contains(normalize(host));
    }

    private static String normalize(String host) {
        var normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        var address = ipLiteral(normalized);
        return address != null ? address.getHostAddress() : normalized;
    }

    /**
     * Parses an IP address literal without resolving anything.
     *
     * @return the address, or null if the host is not an IP address literal
     */
    private static @Nullable InetAddress ipLiteral(String host) {
        if (!host.contains(":") && !host.matches("[0-9.]+")) {
            return null;
        }
        try {
            // literals are not resolved, so this does not perform a DNS lookup
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            return null;
        }
    }

}
