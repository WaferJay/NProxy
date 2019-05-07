package com.wanfajie.proxy;

import com.wanfajie.netty.util.HttpUtils;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

import java.util.Objects;

public final class ProxyAuth {
    private String user;
    private String password;

    public ProxyAuth(String user, String password) {
        ObjectUtil.checkNotNull(user, "user");

        this.user = HttpUtils.urlencode(user);
        this.password = HttpUtils.urlencode(password);
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String toString(boolean hidePwd) {
        if (StringUtil.isNullOrEmpty(password)) {
            return user;
        }

        String secret = hidePwd ? "****" : password;
        return user + ":" + secret;
    }

    public static ProxyAuth create(String userInfo) {
        String[] parts = userInfo.split(":");

        String rawUser;
        String rawPwd = null;
        switch (parts.length) {
            case 2:
                rawPwd = parts[1];
            case 1:
                rawUser = parts[0];
                break;
            default:
                throw new IllegalArgumentException(userInfo);
        }


        String user;
        String pwd = null;
        try {
            user = HttpUtils.urldecode(rawUser);
            if (rawPwd != null) {
                pwd = HttpUtils.urldecode(rawPwd);
            }
        } catch (Exception e) {
            String message = "Raw: " + userInfo + ", User: " + rawUser + ", Password: " + rawPwd;
            throw new IllegalArgumentException(message, e);
        }

        return new ProxyAuth(user, pwd);
    }

    @Override
    public String toString() {
        return toString(true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProxyAuth proxyAuth = (ProxyAuth) o;
        return Objects.equals(user, proxyAuth.user) &&
                Objects.equals(password, proxyAuth.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, password);
    }
}
