package com.wanfajie.nttpclient.config;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.internal.ObjectUtil;

public class NttpClientConfigImpl implements NttpClientMutableConfig {

    private int connectionPerServer;
    private int connectTimeout;
    private EventLoopGroup group;
    private Class<? extends SocketChannel> channelClass;
    private volatile boolean frozen = false;

    @Override
    public NttpClientMutableConfig connectionPerServer(int seconds) {
        checkFrozen();
        ObjectUtil.checkPositiveOrZero(seconds, "connectionPerServer");
        this.connectionPerServer = seconds;
        return this;
    }

    @Override
    public NttpClientMutableConfig connectTimeout(int seconds) {
        checkFrozen();
        ObjectUtil.checkPositiveOrZero(seconds, "connectTimeout");
        this.connectTimeout = seconds;
        return this;
    }

    @Override
    public NttpClientMutableConfig group(EventLoopGroup group) {
        checkFrozen();
        ObjectUtil.checkNotNull(group, "group");
        this.group = group;
        return this;
    }

    @Override
    public NttpClientMutableConfig channelClass(Class<? extends SocketChannel> clazz) {
        checkFrozen();
        ObjectUtil.checkNotNull(clazz, "channelClass");
        this.channelClass = clazz;
        return this;
    }

    @Override
    public int connectionPerServer() {
        return connectionPerServer;
    }

    @Override
    public int connectTimeout() {
        return connectTimeout;
    }

    @Override
    public EventLoopGroup group() {
        return group;
    }

    @Override
    public Class<? extends SocketChannel> channelClass() {
        return channelClass;
    }

    private void checkFrozen() {
        if (frozen) {
            throw new UnsupportedOperationException("frozen");
        }
    }
}
