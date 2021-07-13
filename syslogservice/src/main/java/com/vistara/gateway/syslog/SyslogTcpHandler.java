/*
 * This computer program is the confidential information and proprietary trade
 * secret of VistaraIT, Inc. Possessions and use of this program must
 * conform strictly to the license agreement between the user and
 * VistaraIT, Inc., and receipt or possession does not convey any rights
 * to divulge, reproduce, or allow others to use this program without specific
 * written authorization of VistaraIT, Inc.
 * 
 * Copyright  2014 VistaraIT, Inc. All Rights Reserved.
 */
package com.vistara.gateway.syslog;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opsramp.gateway.common.error.ProcessingError;

//import com.vistara.gateway.error.ProcessingError;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 
 * @author pavanguruvelli
 *
 */
public class SyslogTcpHandler extends ChannelInboundHandlerAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(SyslogTcpHandler.class);
	private static SyslogService instance = SyslogService.getInstance();
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// Ignore
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// Ignore
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try {

			String ipAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();

			ByteBuf buf = (ByteBuf) msg;
			final int rcvPktLength = buf.readableBytes();
			if (rcvPktLength < 0) {
				throw new ProcessingError("No readable Bytes of the packet");	
			}

			final byte[] rcvPktBuf = new byte[rcvPktLength];
			buf.readBytes(rcvPktBuf);

			instance.executeTask(ipAddress, rcvPktBuf);

		} catch (Exception e) {
			if(SyslogService.isDebugEnabled()) {
				LOG.error("TCP: Msg failed. {}", e.getMessage(), e);
			}
		}
	}

}
