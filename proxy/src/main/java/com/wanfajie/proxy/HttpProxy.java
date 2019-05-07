package com.wanfajie.proxy;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;


public final class HttpProxy {

    private Type type;
    private InetSocketAddress address;
    private ProxyAuth auth;

    private URI proxyUri;

    public HttpProxy(Type type, String host, int port, ProxyAuth auth) {
        ObjectUtil.checkNotNull(type, "type");
        ObjectUtil.checkNotNull(host, "host");
        ObjectUtil.checkPositive(port, "port");

        this.type = type;
        this.address = new InetSocketAddress(host, port);
        this.auth = auth;

        try {
            String authStr = auth == null ? null : auth.toString(true);
            proxyUri = new URI(type.toString(), authStr, host, port,
                    null, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public HttpProxy(Type type, String host, int port, String user, String pwd) {
        this(type, host, port, new ProxyAuth(user, pwd));
    }

    public HttpProxy(Type type, String host, int port) {
        this(type, host, port, null);
    }

    public Type getType() {
        return type;
    }

    public String getHost() {
        return address.getHostString();
    }

    public int getPort() {
        return address.getPort();
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public ProxyAuth getAuth() {
        return auth;
    }

    public URI toURI() {
        return proxyUri;
    }

    public static HttpProxy create(URI uri) {
        String scheme = uri.getScheme();
        Type type = Type.valueOf(scheme);
        String authStr = uri.getUserInfo();
        ProxyAuth auth = null;
        if (!StringUtil.isNullOrEmpty(authStr)) {
            auth = ProxyAuth.create(authStr);
        }

        return new HttpProxy(type, uri.getHost(), uri.getPort(), auth);
    }

    public static HttpProxy create(String uri) {
        return create(URI.create(uri));
    }

    @Override
    public String toString() {
        return proxyUri.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpProxy httpProxy = (HttpProxy) o;
        return type == httpProxy.type &&
                Objects.equals(address, httpProxy.address) &&
                Objects.equals(auth, httpProxy.auth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, address, auth);
    }

    public enum Type {
        HTTP, HTTPS, SOCKS, SOCKS4, SOCKS5
    }
}
