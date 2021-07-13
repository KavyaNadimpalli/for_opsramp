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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

/**
 * SyslogNettyProtocolHandler
 * 
 * @author Ambuj Jain
 * @author pavanguruvelli
 */
public class SyslogMessageHandler extends SimpleChannelInboundHandler<DatagramPacket> {
	private static final Logger log = LoggerFactory.getLogger(SyslogMessageHandler.class);
	private static SyslogService instance = SyslogService.getInstance();

	
	protected void channelRead0(ChannelHandlerContext arg0, DatagramPacket packet) throws Exception {
		try {
			final String srcAddr = packet.sender().getAddress().getHostAddress();

			final byte[] rcvPktBuf = new byte[packet.content().readableBytes()];
			packet.content().readBytes(rcvPktBuf);

			instance.executeTask(srcAddr, rcvPktBuf);

		} catch (Exception e) {
			if (SyslogService.isDebugEnabled()) 
				log.error("UDP: Msg failed. {}", e.getMessage(), e);
		}
	}


	@Override
	protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
