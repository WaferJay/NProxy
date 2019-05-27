package com.wanfajie.nproxy;

import com.wanfajie.nproxy.command.FullParameters;
import com.wanfajie.nproxy.command.LogParameter;
import com.wanfajie.nproxy.command.ProxyServerParameter;
import com.wanfajie.nproxy.command.ScraperParameter;
import com.wanfajie.nproxy.pool.MemProxyPool;
import com.wanfajie.nproxy.pool.ProxyPool;
import com.wanfajie.nttpclient.NttpClient;
import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.scraper.DefaultScraperEngine;
import com.wanfajie.proxy.scraper.ScraperEngine;
import com.wanfajie.proxy.scraper.inspect.InspectorBridge;
import com.wanfajie.proxy.scraper.inspect.httpbin.HttpbinInspector;
import com.wanfajie.proxy.server.DefaultProxyLinker;
import com.wanfajie.proxy.server.NProxyInitializer;
import com.wanfajie.proxy.server.NProxyLinker;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Main {

    private static InternalLogger logger = InternalLoggerFactory.getInstance(Main.class);
    private static final List<ChannelFuture> closeFutures = new ArrayList<>(3);

    private static void initLogging(LogParameter parameter) {
        File logConfigFile = parameter.getLogConfigFile();
        if (logConfigFile != null) {

            ConfigurationSource source = null;
            try {
                source = new ConfigurationSource(new FileInputStream(logConfigFile), logConfigFile);
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }

            Configurator.initialize(null, source);
        }

        Level level = parameter.getLogLevel();
        if (level != null) {
            Configurator.setRootLevel(level);
        }
    }

    private static void closeProxyServerChannels() {

        for (ChannelFuture future : closeFutures) {
            try {
                future.channel().close().sync();
            } catch (Exception e) {
                logger.error("close channel {} fail", future.channel(), e);
            }
        }
    }

    private static ScraperEngine<HttpProxy> initScraper(ScraperParameter scraParams, ProxyPool pool, NioEventLoopGroup worker)
            throws FileNotFoundException, MalformedURLException {

        NttpClient.Builder builder = new NttpClient.Builder()
                .group(worker)
                .channel(NioSocketChannel.class)
                .connectTimeout(scraParams.connectTimeout());

        HttpbinInspector inspector = new HttpbinInspector(builder);
        Consumer<HttpProxy> proxyConsumer = pool::add;

        DefaultScraperEngine<HttpProxy> engine = new DefaultScraperEngine<HttpProxy>(worker, new InspectorBridge(inspector, proxyConsumer)) {};

        if (!scraParams.isDisableDefault()) {
            URL defaultScrapersConfig = Main.class.getResource("/scrapers.properties");
            engine.loadScrapers(defaultScrapersConfig);
        }

        if (scraParams.scrapersConfig() != null) {
            File file = scraParams.scrapersConfig();

            if (!file.exists() || !file.isFile()) {
                throw new FileNotFoundException(file.toString());
            }

            engine.loadScrapers(file.toURI().toURL());
        }

        return engine;
    }

    private static boolean ensureBindProxyServer() {

        for (ChannelFuture future : closeFutures) {
            try {
                future.sync();
                logger.info("listening {}", future.channel().localAddress());
            } catch (Exception e) {
                logger.error("listening to {} failed", future.channel().localAddress(), e);
                closeProxyServerChannels();
                return false;
            }
        }

        return true;
    }

    private static void openProxyServer(ServerBootstrap sb, Bootstrap cb, ProxyPool pool, String host, int port) {
        NProxyLinker linker = new DefaultProxyLinker(cb, pool.supplier(HttpProxy.Type.HTTPS));
        ChannelInitializer initializer = new NProxyInitializer(linker);
        ChannelFuture future = sb.childHandler(initializer).bind(host, port);
        closeFutures.add(future);
    }

    public static void main(String[] args) throws Exception {

        FullParameters params = FullParameters.parse(Main.class, args);
        if (params.help()) {
            params.usage();
            System.exit(0);
        }

        initLogging(params.logParams);

        ProxyServerParameter servParams = params.servParams;

        int poolSize = (int) (Runtime.getRuntime().availableProcessors() * 1.5);
        NioEventLoopGroup worker = new NioEventLoopGroup(poolSize);

        ProxyPool proxyPool = new MemProxyPool(worker.next());

        ScraperEngine<HttpProxy> engine = initScraper(params.scraParams, proxyPool, worker);

        Bootstrap bootstrap = new Bootstrap()
                .group(worker)
                .channel(NioSocketChannel.class);

        ServerBootstrap sb = new ServerBootstrap()
                .group(worker)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.AUTO_READ, false)
                .childOption(ChannelOption.SO_LINGER, 0);

        String host = servParams.bindHost();
        if (servParams.httpProxyPort() > 0) {
            openProxyServer(sb, bootstrap, proxyPool, host, servParams.httpProxyPort());
        }

        if (servParams.httpsProxyPort() > 0) {
            openProxyServer(sb, bootstrap, proxyPool, host, servParams.httpsProxyPort());
        }

        if (servParams.socksProxyPort() > 0) {
            openProxyServer(sb, bootstrap, proxyPool, host, servParams.socksProxyPort());
        }

        if (!ensureBindProxyServer()) {
            System.exit(-1);
        }

        engine.start();

        try {
            worker.next().newPromise().sync();
        } catch (InterruptedException ignored) {

        } finally {

            try {
                engine.stop();
                closeProxyServerChannels();
            } finally {
                worker.shutdownGracefully().awaitUninterruptibly();
            }
        }
    }
}
