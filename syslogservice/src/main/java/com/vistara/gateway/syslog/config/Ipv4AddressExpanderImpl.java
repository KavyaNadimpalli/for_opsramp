package com.vistara.gateway.syslog.config;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.conn.util.InetAddressUtils;

import com.opsramp.gateway.mask.util.GatewayMaskingUtil;
import com.opsramp.gateway.monitor.model.IP4Address;
import com.opsramp.gateway.monitor.model.IPAddress;

//import com.vistara.gateway.model.IP4Address;
//import com.vistara.gateway.model.IPAddress;
//import com.vistara.gateway.util.masking.GatewayMaskingUtil;

/**
 * @author yesu.srungavruksham
 *
 */
public class Ipv4AddressExpanderImpl implements IpAddressExpander {

	private IpRangeType ipRangeType = null;
	
	@Override
	public boolean isThisIpVersion(String ipDetailToken) {
		if (ipDetailToken.contains(IpRangeType.HIPEN.getTypeChar())) {
			return checkHipenRangeOfIpAddrs(ipDetailToken);
		} else if (ipDetailToken.contains(IpRangeType.STAR.getTypeChar())) {
			return checkStarRangeOfIpAddrs(ipDetailToken);
		} else if (ipDetailToken.contains(IpRangeType.SUBNET.getTypeChar())) {
			return checkSubnetRangeOfIpAddrs(ipDetailToken);
		} else {
			return checkDefaultIpAddrs(ipDetailToken);
		}
	}
	
	private boolean checkDefaultIpAddrs(String ipDetailToken) {
		ipRangeType = IpRangeType.DEFAULT;
		return InetAddressUtils.isIPv4Address(ipDetailToken);
	}

	private boolean checkSubnetRangeOfIpAddrs(String ipDetailToken) {
		ipRangeType = IpRangeType.SUBNET;
		int index = ipDetailToken.indexOf('/');
		String startIpAddr = ipDetailToken.substring(0, index);
		return InetAddressUtils.isIPv4Address(startIpAddr);
	}

	private boolean checkHipenRangeOfIpAddrs(String ipDetailToken) {
		ipRangeType = IpRangeType.HIPEN;
		String[] tokens = ipDetailToken.split("\\-");
		String startIpAddr = tokens[0].trim();
		String endIpAddr = tokens[1].trim();
		return InetAddressUtils.isIPv4Address(startIpAddr) 
				&& InetAddressUtils.isIPv4Address(endIpAddr);
	}

	private boolean checkStarRangeOfIpAddrs(String ipDetailToken) {
		ipRangeType = IpRangeType.STAR;
		StringTokenizer st = new StringTokenizer(ipDetailToken, STRING_PERIOD);
		return (st.countTokens() == 4);
	}

	@Override
	public List<IPAddress> expandIpAddresses(String ipDetailToken) {
		List<IPAddress> ipAddresses = new ArrayList<>();
		List<String> ipAddrs = new ArrayList<>();
		if (ipRangeType == IpRangeType.HIPEN) {
			getHipenRangeOfIpAddresses(ipAddrs, ipDetailToken);
		} else if (ipRangeType == IpRangeType.STAR) {
			getStarRangeOfIpAddresses(ipAddrs, ipDetailToken);
		} else if (ipRangeType == IpRangeType.SUBNET) {
			ipAddrs.add(ipDetailToken);
		} else {
			ipAddrs.add(GatewayMaskingUtil.getInstance().getUnMaskIpAddress(ipDetailToken));
		}
		
		for (String ip : ipAddrs) {
			IP4Address ip4Address = new IP4Address();
			ip4Address.setIpAddress(ip);
			ipAddresses.add(ip4Address);
		}
		return ipAddresses;
	}
	
	@Override
	public List<String> expandIpAddressesStrList(String ipDetailToken) {
		List<String> ipAddrs = new ArrayList<>();
		if (ipRangeType == IpRangeType.HIPEN) {
			getHipenRangeOfIpAddresses(ipAddrs, ipDetailToken);
		} else if (ipRangeType == IpRangeType.STAR) {
			getStarRangeOfIpAddresses(ipAddrs, ipDetailToken);
		} else if (ipRangeType == IpRangeType.SUBNET) {
			ipAddrs.add(ipDetailToken);
		} else {
			ipAddrs.add(GatewayMaskingUtil.getInstance().getUnMaskIpAddress(ipDetailToken));
		}
		
		return ipAddrs;
	}

	public List<String> getSubNetIpAddresses(List<String> ipAddresses, String ipDetail) {
		int index = ipDetail.indexOf('/');
		long startIp = getIpValue(GatewayMaskingUtil.getInstance().getUnMaskIpAddress(ipDetail.substring(0, index)));
		
		int mask = Integer.parseInt(ipDetail.substring(index + 1));
		int maxHostPart = (int) (Math.pow(2, (32 - mask)) - 1);
		startIp = startIp & (~ maxHostPart);
		
		long endIp = startIp | maxHostPart;
		
		return getIpAddresses(ipAddresses, startIp, endIp);
	}
	
	public int getSubNetIpValue(String ipDetail) {
		int index = ipDetail.indexOf('/');
		return Integer.parseInt(ipDetail.substring(index + 1));
	}

	public List<String> getStarRangeOfIpAddresses(List<String> ipAddresses, String ipDetail) {
		StringTokenizer st = new StringTokenizer(ipDetail, STRING_PERIOD);
		StringBuilder startIpString = new StringBuilder();
		StringBuilder endIpString = new StringBuilder();
		
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.equals("*")) {
				startIpString.append("0");
				endIpString.append("255");
			} else {
				startIpString.append(token);
				endIpString.append(token);
			}
			if (st.countTokens() != 0) {
				startIpString.append(STRING_PERIOD);
				endIpString.append(STRING_PERIOD);
			} 
		}
		
		long startIp = getIpValue(startIpString.toString());
		long endIp = getIpValue(endIpString.toString());
		return getIpAddresses(ipAddresses, startIp, endIp);
	}

	public List<String> getHipenRangeOfIpAddresses(List<String> ipAddresses, String ipDetail) {
		String[] tokens = ipDetail.split("\\-");
		
		long startIp = getIpValue(tokens[0]);
		long endIp = getIpValue(tokens[1]);
		return getIpAddresses(ipAddresses, startIp, endIp);
	}
	
	public long getIpValue(String ipString) {
		ipString = ipString.trim();
		StringTokenizer st = new StringTokenizer(ipString, STRING_PERIOD);
		long ipVal = 0;
		while (st.hasMoreTokens()) {
			long octet = Long.parseLong(st.nextToken());
			ipVal |= (octet << (8 * st.countTokens()));
		}
		return ipVal;
	}
	
	public List<String> getIpAddresses(List<String> ipAddresses, long startIp, long endIp) {
		while (startIp <= endIp) {
			StringBuilder ipAddr = new StringBuilder();
			ipAddr.append(startIp >> 24 & 0xff).append(".");
			ipAddr.append(startIp >> 16 & 0xff).append(".");
			ipAddr.append(startIp >> 8 & 0xff).append(".");
			ipAddr.append(startIp & 0xff);

			ipAddresses.add(ipAddr.toString());
			
			startIp++;
		}
		return ipAddresses;
	}
	
	public static void main(String[] args) throws IOException {
//		PrimitiveEntity scanInfo = new PrimitiveEntity();
//		scanInfo.putValue(NetDiscConstants.IP_DISC_TYPE.getName(), "IP_RANGE");
//		scanInfo.putValue(NetDiscConstants.INCLUDE_IPS.getName(), "10.20.1.*, 10.20.2.0 - 10.20.3.255, 10.20.4.0/24");
//		scanInfo.putValue(NetDiscConstants.EXCLUDE_IPS.getName(), "10.20.2.*, 10.20.2.0/24, 10.20.4.4 - 10.20.4.220");
//		IpAddressExpander expander = new Ipv4AddressExpanderImpl();
//		
//		long time = System.currentTimeMillis();
//		List<IPAddress> ips = expander.expandIpAddresses(scanInfo);
//		time = (System.currentTimeMillis() - time);
		
//		for (IPAddress ipAddr : ips) {
//			System.out.println(ipAddr.getIpAddress());
//		}
		
//		System.out.println("Time taken :" + time);
		
//		System.out.println(ipAddrs);
//		FileUtils.writeStringToFile(new File("E:/ip-addrs"), ipAddrs.toString());
	}
}
