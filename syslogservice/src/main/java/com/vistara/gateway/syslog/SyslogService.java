package com.vistara.gateway.syslog;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.vistara.gateway.annotation.Service;
//import com.vistara.gateway.config.GatewayConfiguration;
//import com.vistara.gateway.objstore.CliObjStore;
//import com.vistara.gateway.service.GatewayService;
//import com.vistara.gateway.service.GatewayServiceState;
import com.vistara.gateway.syslog.config.SyslogConfigCollection;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @author Ambuj Jain
 * @author pavanguruvelli
 */
@Service(init = "AUTO", idx = 35, listener = "AUTO")
public class SyslogService extends GatewayService {
	public static final String SYSLOG_LOG = "syslog.log";
	private static final Logger LOG = LoggerFactory.getLogger(SyslogService.class);
	private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private static final Lock WRITE = lock.writeLock();

	private static SyslogService inst = null;

	private EventLoopGroup udpWorkerGroup = null;
	private EventLoopGroup tcpBossGroup = null;
	private EventLoopGroup tcpWorkerGroup = null;
	private SocketAddress channelAddr = null;
	private ExecutorService executor;
	private ExecutorService singleThreadExecutor;
	private int port;


	private static boolean isLogEnabled = false;

	private SyslogService() {
		if (inst != null) {
			throw new IllegalStateException("Only one instance of " + this.getClass().getName() + " can be created.");
		}

		try {
			port = 514;
			channelAddr = new InetSocketAddress("0.0.0.0", port);
		} catch (Exception e) {
			LOG.error("SyslogService : Not able to instantiate channel Address. Syslog Engine won't work.!!!");
		}
	}

	
	public String getKey() throws Exception {
		return this.getClass().getSimpleName();
	}

	public static SyslogService getInstance() {
		if (inst == null) {
			WRITE.lock();
			try {
				if (inst == null) {
					inst = new SyslogService();
				}
			} finally {
				WRITE.unlock();
			}
		}
		return inst;
	}

	
	public void initService() throws Exception {

		// init UDP Handler
		initUdpHandler();
		// init TCP Handler
		initTcpHandler();

		initLogFlagTimer();
		
		int corePoolSize = Integer.parseInt(getEnvOrDef("SYSLOG_CORE_POOL_SIZE","20"));
		int maxPoolSize = Integer.parseInt(getEnvOrDef("SYSLOG_max_POOL_SIZE","30"));
		int queueLength = Integer.parseInt(getEnvOrDef("SYSLOG_QUEUE.LENGTH","3000"));
		
		


		// Naming the threads.
		BasicThreadFactory namedThreadFactory = new BasicThreadFactory.Builder().namingPattern("Syslog_Worker-%d")
				.build();		

		// start the threadpool
		executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 120, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(queueLength), namedThreadFactory, rejectionHandler);

		// Reload the configuration from Database.
		SyslogConfigCollection.getInstance().loadConfig();

	}
	
	static String getEnvOrDef(String property, String defaultVal)
    {
		String propertyVal = System.getenv(property);
		return null != propertyVal && !propertyVal.isEmpty() ? propertyVal : defaultVal;
	}

	private void initUdpHandler() {

		try {
			int threads = Runtime.getRuntime().availableProcessors();
			boolean epoll = Epoll.isAvailable();

			udpWorkerGroup = epoll ? new EpollEventLoopGroup(threads) : new NioEventLoopGroup(threads);

			// Create Boot strap
			Bootstrap bootstrap = new Bootstrap();
			EventExecutorGroup executorGroup = new DefaultEventExecutorGroup(8);
			bootstrap.group(udpWorkerGroup)
			.channel(epoll ? EpollDatagramChannel.class : NioDatagramChannel.class)
			.option(ChannelOption.SO_REUSEADDR, true)
			.option(ChannelOption.SO_KEEPALIVE, true)
			.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
			.option(ChannelOption.SO_RCVBUF, 102400)
			.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(2048));

			if (epoll) {
				LOG.error("Epoll Available");
				bootstrap.handler(new ChannelInitializer<EpollDatagramChannel>() {
					@Override
					protected void initChannel(EpollDatagramChannel arg0) throws Exception {
						ChannelPipeline p = arg0.pipeline();
						p.addLast(executorGroup, "udphandler", new SyslogMessageHandler());
					}
				});
			} else {
				bootstrap.handler(new ChannelInitializer<NioDatagramChannel>() {
					@Override
					protected void initChannel(NioDatagramChannel arg0) throws Exception {
						ChannelPipeline p = arg0.pipeline();
						p.addLast(executorGroup, "udphandler", new SyslogMessageHandler());
					}
				});
			}

			// Bind and start to accept incoming connections.
			ChannelFuture channelFuture = bootstrap.bind(channelAddr);

			// Handle Future
			channelFuture.addListener(future -> {
				// Perform post-closure operation
				if (channelFuture.isCancelled()) {
					// Connection attempt cancelled by user
					LOG.error("Failed(Cancelled) to start the UDP SysLogd @{}", port);
					setState(GatewayServiceState.INIT_FAILED);
				} else if (!channelFuture.isSuccess()) {
					// Failed
					LOG.error("Failed to start the UDP SysLogd @{}", port);
					setState(GatewayServiceState.INIT_FAILED);
				} else {
					// Connection established successfully
					LOG.error("Successfully started the UDP SysLogd @{}", port);
					setState(GatewayServiceState.INITIALIZED);
				}
			});

		} catch (Exception e) {
			LOG.error("Failed to start Syslog UDP Handler. Exception : {}", e.getMessage(), e);
		}

	}

	private void initTcpHandler() {
		try {

			tcpBossGroup = new NioEventLoopGroup();
			tcpWorkerGroup = new NioEventLoopGroup();

			final ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(tcpBossGroup, tcpWorkerGroup)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_REUSEADDR, true)
			.option(ChannelOption.SO_KEEPALIVE, true)
			.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
			.option(ChannelOption.SO_RCVBUF, 102400)
			.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(2048))
			.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel channel) throws Exception {
					final ChannelPipeline pipeline = channel.pipeline();

					pipeline.addLast("tcphandler", new SyslogTcpHandler());
				}
			});

			ChannelFuture channelFuture = bootstrap.bind(channelAddr);

			// Handle Future
			channelFuture.addListener(future -> {
				// Perform post-closure operation
				if (channelFuture.isCancelled()) {
					// Connection attempt cancelled by user
					LOG.error("Failed(Cancelled) to start the TCP SysLogd @{}", port);
					setState(GatewayServiceState.INIT_FAILED);
				} else if (!channelFuture.isSuccess()) {
					// Failed
					LOG.error("Failed to start the TCP SysLogd @{}", port);
					setState(GatewayServiceState.INIT_FAILED);
				} else {
					// Connection established successfully
					LOG.error("Successfully started the TCP SysLogd @{}", port);
					setState(GatewayServiceState.INITIALIZED);
				}
			});

		} catch (Exception e) {
			LOG.error("Failed to start the Syslog TCP Handler. Exception : {}", e.getMessage(), e);
		}
	}

	private void initLogFlagTimer() {

		singleThreadExecutor = Executors.newSingleThreadExecutor();
		Runnable r = () -> {
			for (;;) {
				try {
					setLogEnabled(CliObjStore.isOn(SYSLOG_LOG));

					// check every 10 sec.
					Thread.sleep(10000);
				} catch (Exception e) {
					//
				}
			}
		};
		singleThreadExecutor.submit(r);
	}

	private RejectedExecutionHandler rejectionHandler = (Runnable r, ThreadPoolExecutor exec) -> LOG.error("#### SYSLOG JOB REJECTED ######");

	public void executeTask(String ipAddress, byte[] byteArray) {
		executor.execute(new SyslogEventProcessor(ipAddress, byteArray));
	}

	
	public void run() {
		// Ignore
	}

	
	public void startService() throws Exception {
		try {
			LOG.error("{} has been started", this.getClass().getCanonicalName());
		} finally {
			state = GatewayServiceState.RUNNING;
		}
	}

	public void stopService() throws Exception {
		try {
			if (udpWorkerGroup != null) {
				udpWorkerGroup.shutdownGracefully();
			}

			if (tcpBossGroup != null) {
				tcpBossGroup.shutdownGracefully();
			}

			if (tcpWorkerGroup != null) {
				tcpWorkerGroup.shutdownGracefully();
			}

			// Shutdown the Threadpool.
			if (executor != null) {
				executor.shutdownNow();
			}

			if (singleThreadExecutor != null) {
				singleThreadExecutor.shutdownNow();
			}
		} catch (Exception e) {
			// ignore
		} finally {
			state = GatewayServiceState.STOPPED;
		}
	}

	private static void setLogEnabled(boolean on) {
		SyslogService.isLogEnabled = on;
	}

	public static boolean isDebugEnabled() {
		return isLogEnabled;
	}

}