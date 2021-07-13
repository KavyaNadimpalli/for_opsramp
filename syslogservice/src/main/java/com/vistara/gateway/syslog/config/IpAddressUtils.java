package com.vistara.gateway.syslog.config;

/**
 * 
 */

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.conn.util.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.StringUtils;
import com.opsramp.gateway.discovery.PluginConfig;
import com.opsramp.gateway.discovery.PrimitiveEntity;
import com.opsramp.gateway.monitor.model.IP4Address;
import com.opsramp.gateway.monitor.model.IPAddress;
import com.opsramp.gateway.monitor.model.IPAddressType;
import com.vistara.gateway.constants.StringConstants;
//import com.vistara.gateway.model.IP4Address;
//import com.vistara.gateway.model.IPAddress;
//import com.vistara.gateway.model.IPAddressType;
//import com.vistara.gateway.model.PluginConfig;
//import com.vistara.gateway.model.PrimitiveEntity;
//import com.vistara.gateway.plugin.discovery.config.NetworkDiscoveryConfig;
//import com.vistara.gateway.util.StringUtil;

/**
 * @author yesu.srungavruksham
 *
 */
public class IpAddressUtils implements StringConstants {
	
	private static final Logger LOG = LoggerFactory.getLogger(IpAddressUtils.class);

	public static final String LOOP_BACK_IP_PREFIX = "127";
	
	public static List<IPAddress> getScanIpAddrs(NetworkDiscoveryConfig netDiscoveryRequest) {
		List<IPAddress> ipAddresses = getIpAddrs(netDiscoveryRequest.getScanIps());
		return ipAddresses;
	}

	public static List<IPAddress> getIncludeIpAddrs(NetworkDiscoveryConfig netDiscoveryRequest) {
		List<IPAddress> ipAddresses = getIpAddrs(netDiscoveryRequest.getIncludeSubNet());
		return ipAddresses;
	}
	
	public static List<IPAddress> getExcludeIpAddrs(NetworkDiscoveryConfig netDiscoveryRequest) {
		List<IPAddress> ipAddresses = getIpAddrs(netDiscoveryRequest.getExcludeSubNet());
		return ipAddresses;
	}
	
	private static List<IPAddress> getIpAddrs(String ips) {
		List<IPAddress> ipAddresses = new ArrayList<>();
		if (!StringUtils.isEmpty(ips)) {
			String[] ipDetailTokens = ips.split("\\,");
			List<IpAddressExpander> ipExpanders = getIpAddrExpanders();
			for (String ipDetailToken : ipDetailTokens) {
				ipDetailToken = ipDetailToken.trim();
				for (IpAddressExpander ipExpander : ipExpanders) {
					if (ipExpander.isThisIpVersion(ipDetailToken)) {
						ipAddresses.addAll(ipExpander.expandIpAddresses(ipDetailToken));
						break;
					}
				}
			}
		}
		return ipAddresses;
	}
	
	public static Set<String> getIpAddrsStrList(String ips) {
		Set<String> ipAddresses = new HashSet<>();
		if (!StringUtils.isEmpty(ips)) {
			String[] ipDetailTokens = ips.split("\\,");
			List<IpAddressExpander> ipExpanders = getIpAddrExpanders();
			for (String ipDetailToken : ipDetailTokens) {
				ipDetailToken = ipDetailToken.trim();
				for (IpAddressExpander ipExpander : ipExpanders) {
					if (ipExpander.isThisIpVersion(ipDetailToken)) {
						ipAddresses.addAll(ipExpander.expandIpAddressesStrList(ipDetailToken));
						break;
					}
				}
			}
		}
		return ipAddresses;
	}

	private static List<IpAddressExpander> getIpAddrExpanders() {
		List<IpAddressExpander> ipExpanders = new ArrayList<>();
		ipExpanders.add(new Ipv4AddressExpanderImpl());
		return ipExpanders;
	}
	
	public static IPAddress buildIpAddress(String ipAddress, IPAddressType addressType) {
		if (ipAddress == null || addressType == null) {
			return null;
		}
		
		switch (addressType) {
		case V4:
			IP4Address ip4Address = new IP4Address();
			ip4Address.setIpAddress(ipAddress);
			return ip4Address;
		default:
			return null;
		}
	}
	
	public static boolean isInSameNetwork(String ipAddr1, String subNetMask1, String ipAddr2, String subNetMask2) {
		try {
			if (ipAddr1 == null || subNetMask1 == null 
					|| ipAddr2 == null || subNetMask2 == null) {
				return false;
			}
			
			InetAddress inetAddr1 = InetAddress.getByName(ipAddr1);
			InetAddress inetMask1 = InetAddress.getByName(subNetMask1);
			InetAddress inetAddr2 = InetAddress.getByName(ipAddr2);
			InetAddress inetMask2 = InetAddress.getByName(subNetMask2);
			
			if (!(inetAddr1 instanceof Inet4Address) || !(inetMask1 instanceof Inet4Address)
					|| !(inetAddr2 instanceof Inet4Address) || !(inetMask2 instanceof Inet4Address)) {
				return false;
			}
			
			byte[] addr1Octets = inetAddr1.getAddress();
			byte[] netMask1Octets = inetMask1.getAddress();
			byte[] addr2Octets = inetAddr2.getAddress();
			byte[] netMask2Octets = inetMask2.getAddress();
			
			for (int i = 0; i < 4; i++) {
				if ((addr1Octets[i] & netMask1Octets[i]) != (addr2Octets[i] & netMask2Octets[i])) {
					return false;
				}
			}
			return true;
		} catch (UnknownHostException e) {
			return false;
		}
	}
	
	public static void main(String[] args) {
		Map<String, String> scanInfo = new LinkedHashMap<>();
		
		scanInfo.put(NetworkDiscoveryConfig.IP_DISC_TYPE, "SEED");
		scanInfo.put(NetworkDiscoveryConfig.SCAN_IPS, "10.20.1.1, 10.20.1.2");
		
		PrimitiveEntity profileInfo = new PrimitiveEntity();
		profileInfo.putValue(NetworkDiscoveryConfig.SCAN_INFO, scanInfo);
		
		PluginConfig config = new PluginConfig();
		config.setProfileInfo(profileInfo);
		
		NetworkDiscoveryConfig configWrapper = new NetworkDiscoveryConfig(config);
		
		LOG.error("{}",getScanIpAddrs(configWrapper));
		
		LOG.error("{}",InetAddressUtils.isIPv4Address("30.200.1.1"));
	}
}
