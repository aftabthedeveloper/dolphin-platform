package com.canoo.dp.impl.platform.client.session;

import com.canoo.dp.impl.platform.core.Assert;
import com.canoo.dp.impl.platform.core.PlatformConstants;
import com.canoo.platform.client.http.spi.HttpURLConnectionHandler;

import java.net.HttpURLConnection;

/**
 * Created by hendrikebbers on 19.09.17.
 */
public class ClientSessionSupportingURLConnectionResponseHandler implements HttpURLConnectionHandler {

    private final ClientSessionStoreImpl clientSessionStore;

    public ClientSessionSupportingURLConnectionResponseHandler(final ClientSessionStoreImpl clientSessionStore) {
        this.clientSessionStore = Assert.requireNonNull(clientSessionStore, "clientSessionStore");
    }

    @Override
    public void handle(final HttpURLConnection response) {
        Assert.requireNonNull(response, "response");
        String clientIdInHeader = response.getHeaderField(PlatformConstants.CLIENT_ID_HTTP_HEADER_NAME);
        clientSessionStore.setClientIdentifierForUrl(response.getURL(), clientIdInHeader);
    }
}
