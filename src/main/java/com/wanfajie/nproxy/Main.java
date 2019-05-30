package com.wanfajie.nproxy;

import com.wanfajie.nproxy.command.*;
import com.wanfajie.nproxy.pool.MemProxyPool;
import com.wanfajie.nproxy.pool.ProxyPool;
import com.wanfajie.nttpclient.NttpClient;
import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.scraper.DefaultScraperEngine;
import com.wanfajie.proxy.scraper.HttpProxyScraperEngine;
import com.wanfajie.proxy.scraper.ScraperEngine;
import com.wanfajie.proxy.scraper.inspect.Inspector;
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
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import sun.misc.Signal;

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

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Main.class);
    private static final List<ChannelFuture> closeFutures = new ArrayList<>(3);

    private static NioEventLoopGroup workers;
    private static ScraperEngine<HttpProxy> scraperEngine;
    private static Inspector inspector;
    private static ProxyPool proxyPool;

    private static void initEventLoopGroup() {
        int poolSize = (int) (Runtime.getRuntime().availableProcessors() * 1.5);
        workers = new NioEventLoopGroup(poolSize);
    }

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
                logger.info("closed channel {}", future.channel());
            } catch (Exception e) {
                logger.error("close channel {} fail", future.channel(), e);
            }
        }
    }

    private static void initInspector(InspectorParameter inspParams) {

        NttpClient.Builder builder = new NttpClient.Builder()
                .group(workers)
                .channel(NioSocketChannel.class)
                .connectTimeout(inspParams.connectTimeout());

        inspector = new HttpbinInspector(builder, inspParams.concurrent());
    }

    private static void initScraper(ScraperParameter scraParams)
            throws FileNotFoundException, MalformedURLException {

        Consumer<HttpProxy> proxyConsumer = proxyPool::add;

        DefaultScraperEngine<HttpProxy> engine = new HttpProxyScraperEngine(workers, new InspectorBridge(inspector, proxyConsumer));

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

        scraperEngine = engine;
    }

    private static void initProxyPool() {
        proxyPool = new MemProxyPool(workers.next());
    }

    private static boolean ensureBindProxyServer() {

        for (ChannelFuture future : closeFutures) {
            try {
                future.sync();
                logger.info("listening {}", future.channel().localAddress());
            } catch (Exception e) {
                logger.error("listening to {} failed", future.channel().localAddress(), e);
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

    private static boolean listenProxyPorts(ProxyServerParameter servParams) {

        Bootstrap bootstrap = new Bootstrap()
                .group(workers)
                .channel(NioSocketChannel.class);

        ServerBootstrap sb = new ServerBootstrap()
                .group(workers)
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

        return ensureBindProxyServer();
    }

    private static void waitingToExit() {
        Promise<Signal> signalPromise = workers.next().newPromise();
        Signal.handle(new Signal("INT"), signal -> {
            logger.debug("Hitting {}({})", signal, signal.getNumber());
            if (!signalPromise.trySuccess(signal)) {
                logger.debug("Close immediately");
                System.exit(10);
            }
            logger.debug("Closing program...");
        });

        signalPromise.awaitUninterruptibly();
        closeAll();
    }

    private static void closeAll() {
        try {
            if (scraperEngine != null) {
                logger.debug("Closing scraper engine...");
                scraperEngine.stop();
            }

            if (inspector != null) {
                logger.debug("Closing proxy inspector...");
                inspector.close();
            }

            closeProxyServerChannels();
        } finally {
            workers.shutdownGracefully().awaitUninterruptibly();
        }
    }

    public static void main(String[] args) {

        FullParameters params = FullParameters.parse(Main.class, args);
        if (params.help()) {
            params.usage();
            System.exit(0);
        }

        initLogging(params.logParams);

        initEventLoopGroup();

        initProxyPool();
        initInspector(params.inspParams);

        try {
            initScraper(params.scraParams);
        } catch (Exception e) {
            logger.error("Scraper engine initialization failed. A exception was thrown", e);

            closeAll();
            System.exit(1);
        }

        if (!listenProxyPorts(params.servParams)) {
            closeAll();
            System.exit(2);
        }

        scraperEngine.start();

        waitingToExit();
    }
}
