/*
 * Copyright 2015-2018 Canoo Engineering AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.canoo.dp.impl.security;

import com.canoo.dp.impl.platform.core.Assert;
import com.canoo.dp.impl.platform.core.http.HttpClientConnection;
import com.canoo.platform.client.ClientConfiguration;
import com.canoo.platform.client.PlatformClient;
import com.canoo.platform.client.security.Security;
import com.canoo.platform.core.DolphinRuntimeException;
import com.canoo.platform.core.http.RequestMethod;
import com.google.gson.Gson;
import org.apiguardian.api.API;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.canoo.dp.impl.platform.core.http.HttpHeaderConstants.CONTENT_TYPE_HEADER;
import static com.canoo.dp.impl.platform.core.http.HttpHeaderConstants.FORM_MIME_TYPE;
import static com.canoo.dp.impl.platform.core.http.HttpHeaderConstants.TEXT_MIME_TYPE;
import static com.canoo.dp.impl.platform.core.http.HttpStatus.SC_HTTP_UNAUTHORIZED;
import static com.canoo.dp.impl.security.SecurityConfiguration.APPLICATION_PROPERTY_NAME;
import static com.canoo.dp.impl.security.SecurityConfiguration.AUTH_ENDPOINT_PROPERTY_DEFAULT_VALUE;
import static com.canoo.dp.impl.security.SecurityConfiguration.AUTH_ENDPOINT_PROPERTY_NAME;
import static com.canoo.dp.impl.security.SecurityConfiguration.DIRECT_CONNECTION_PROPERTY_DEFAULT_VALUE;
import static com.canoo.dp.impl.security.SecurityConfiguration.DIRECT_CONNECTION_PROPERTY_NAME;
import static com.canoo.dp.impl.security.SecurityConfiguration.REALM_PROPERTY_NAME;
import static com.canoo.dp.impl.security.SecurityHttpHeader.REALM_NAME_HEADER;
import static org.apiguardian.api.API.Status.INTERNAL;

@API(since = "0.19.0", status = INTERNAL)
public class KeycloakSecurity implements Security {

    //Setup for the realm / client -> "Direct Grand" must be enabled in keycloak admin

    private static final Logger LOG = LoggerFactory.getLogger(KeycloakSecurity.class);

    private final String authEndpoint;

    private final String realmName;

    private final String appName;

    private final ExecutorService executor;

    private final boolean directConnect;

    private KeycloakOpenidConnectResult connectResult;

    private long tokenCreation;

    public KeycloakSecurity(final ClientConfiguration configuration) {
        Assert.requireNonNull(configuration, "configuration");

        this.appName = configuration.getProperty(APPLICATION_PROPERTY_NAME);

        this.authEndpoint = configuration.getProperty(AUTH_ENDPOINT_PROPERTY_NAME, AUTH_ENDPOINT_PROPERTY_DEFAULT_VALUE);
        Assert.requireNonBlank(authEndpoint, "authEndpoint");

        this.realmName = configuration.getProperty(REALM_PROPERTY_NAME);

        this.directConnect = configuration.getBooleanProperty(DIRECT_CONNECTION_PROPERTY_NAME, DIRECT_CONNECTION_PROPERTY_DEFAULT_VALUE);

        this.executor = configuration.getBackgroundExecutor();
        Assert.requireNonNull(executor, "executor");
    }

    private HttpClientConnection createDirectConnection() throws URISyntaxException, IOException {
        final URI url = new URI(authEndpoint + "/auth/realms/" + realmName + "/protocol/openid-connect/token");
        final HttpClientConnection clientConnection = new HttpClientConnection(url, RequestMethod.POST);
        clientConnection.addRequestHeader(CONTENT_TYPE_HEADER, FORM_MIME_TYPE);
        return clientConnection;
    }

    private HttpClientConnection createServerProxyConnection() throws URISyntaxException, IOException {
        final URI url = new URI(authEndpoint);
        final HttpClientConnection clientConnection = new HttpClientConnection(url, RequestMethod.POST);
        clientConnection.addRequestHeader(CONTENT_TYPE_HEADER, TEXT_MIME_TYPE);
        if(realmName != null && !realmName.isEmpty()) {
            clientConnection.addRequestHeader(REALM_NAME_HEADER, realmName);
        }
        return clientConnection;
    }

    private void receiveToken(final HttpClientConnection connection, final String content) throws IOException {
        Assert.requireNonNull(content, "content");
        LOG.debug("receiving new token from keycloak server");
        connection.setDoOutput(true);
        connection.writeRequestContent(content);
        final int responseCode = connection.readResponseCode();
        if(responseCode == SC_HTTP_UNAUTHORIZED) {
            throw new DolphinRuntimeException("Invalid login!");
        }
        final String input = connection.readUTFResponseContent();
        final Gson gson = PlatformClient.getService(Gson.class);
        final KeycloakOpenidConnectResult result = gson.fromJson(input, KeycloakOpenidConnectResult.class);
        connectResult = result;
        tokenCreation = System.currentTimeMillis();
    }

    @Override
    public Future<Void> login(final String user, final String password) {
        return (Future<Void>) executor.submit(() -> {
            try {
                if(directConnect) {
                    receiveToken(createDirectConnection(), "client_id=" + appName + "&username=" + user + "&password=" + password + "&grant_type=password");
                } else {
                    receiveToken(createServerProxyConnection(), "username=" + user + "&password=" + password + "&grant_type=password");
                }
            } catch (IOException | URISyntaxException e) {
                throw new DolphinRuntimeException("Can not receive security token!", e);
            }
        });
    }

    public Future<Void> refreshToken() {
        if(connectResult == null) {
            throw new IllegalStateException("Can not refresh token without valid login");
        }
        final String refreshToken = connectResult.getRefresh_token();
        return (Future<Void>) executor.submit(() -> {
                try {
                    if(directConnect) {
                        receiveToken(createDirectConnection(), "grant_type=refresh_token&refresh_token=" + refreshToken + "&client_id=" + appName);
                    } else {
                        receiveToken(createServerProxyConnection(), "grant_type=refresh_token&refresh_token=" + refreshToken);
                    }
                } catch (IOException | URISyntaxException e) {
                    throw new DolphinRuntimeException("Can not refresh security token!", e);
                }
            });
    }

    public long tokenExpiresAt() {
        if (connectResult == null) {
            return -1;
        }
        return tokenCreation + connectResult.getExpires_in() * 1000;
    }

    @Override
    public Future<Void> logout() {
        //TODO: currently there is no perfect solution at keycloak to do such a logout
        connectResult = null;
        CompletableFuture<Void> resultHack = new CompletableFuture<>();
        resultHack.complete(null);
        return resultHack;
    }

    public boolean isAuthorized() {
        //REST Endpoint at Keycloak site -> Call must be done manually
        //http://www.keycloak.org/docs-api/3.3/rest-api/index.html
        //See /admin/realms/{realm}/users/{id}/sessions
        return connectResult != null;
    }

    public String getAccessToken() {
        if (connectResult == null) {
            return null;
        }
        return connectResult.getAccess_token();
    }
}
