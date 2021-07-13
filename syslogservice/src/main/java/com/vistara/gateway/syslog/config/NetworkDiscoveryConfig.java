package com.vistara.gateway.syslog.config;

/**
 * 
 */

import java.util.List;

import org.eclipse.persistence.internal.sessions.factories.model.transport.discovery.DiscoveryConfig;

import com.opsramp.gateway.discovery.PluginConfig;
import com.opsramp.gateway.discovery.PrimitiveEntity;

//import com.vistara.gateway.model.PluginConfig;
//import com.vistara.gateway.model.PrimitiveEntity;

/**
 * @author yesu.srungavruksham
 *
 */
public class NetworkDiscoveryConfig extends DiscoveryConfig {

	// Static Info
	public static final String SCAN_INFO = "scanInfo";
	public static final String DISCOVERY_OPTIONS = "options";
	public static final String PASWD_VAULTS = "passwdVaults";
	public static final String IS_NMAP_DISCOVERY_ENABLED = "runNmapScan";
	public static final String IS_SNMP_DISCOVERY_ENABLED = "runSnmpScan";

	public static final String IP_DISC_TYPE = "discoveryType";
	
	public static final String IP_ADDRESS_TYPE = "ipAddrType";
	public static final String IP_ADDRESS_V4  = "IPV4";
	public static final String IP_ADDRESS_V6 = "IPV6";
	public static final String SCAN_IPS = "ipDetails";
	public static final String INCLUDE_SUBNET = "includeSubnet";
	public static final String EXCLUDE_SUBNET = "excludeSubnet";
	public static final String TCP_PORTS = "tcpPorts";
	public static final String CRED_IDS = "credIds";
	public static final String SCAN_DEPTH = "depth";
	public static final String GATEWAY_IP = "gatewayIp";
	
	public static final String IS_VOIP_DISCOVERY_ENABLED = "discoverVoip";
	public static final String IS_L2_LINK_DISCOVERY = "discoverL2Link";
	public static final String IS_BGP_DISCOVERY_ENABLED = "discoverBgp";
	public static final String IS_OSPF_DISCOVERY_ENABLED = "discoverOspf";
	public static final String IS_BRIDGE_DISCOVERY_ENABLED = "discoverBridge";
	public static final String IS_LOADBALANCER_ENABLED = "discoverLoadbalancer";
	
	public static final String IS_SNMP_SCAN_AGAINST_DISCOVER_HOST = "scanSnmpOnDiscoverHost";
	public static final String IS_SNMP_ENABLED_DEVICE = "snmpEnabledDevice";
	public static final String IS_WMI_ENABLED_DEVICE = "wmiEnabledDevice";
	public static final String IS_SSH_ENABLED_DEVICE = "sshEnabledDevice";
	public static final String IS_REVERSE_DNS_LOOKUP = "enableReverseDNS";
	public static final String DONT_OVERWRITE_HOSTNAME_WITH_DNS = "dontOwHostnameWithDns";
	
	public static final String PASWD_VAULT_MGR = "passwdMgr";
	public static final String PASWD_VAULT_QUERY = "passwdQryJson";
	public static final String PASWD_VAULT_CRED_ID = "credUuid";
	
	private PrimitiveEntity profileInfo;
	private PrimitiveEntity discoveryOptions;
	private PrimitiveEntity scanInfo;
	private List<PrimitiveEntity> passwdVaults;
	
	public NetworkDiscoveryConfig(PluginConfig config) {
		super();
		profileInfo = config.getProfileInfo();
		scanInfo = profileInfo.getAsPrimitiveEntity(SCAN_INFO);
		discoveryOptions = profileInfo.getAsPrimitiveEntity(DISCOVERY_OPTIONS);
		passwdVaults = profileInfo.getAsArrayList(PASWD_VAULTS, PrimitiveEntity.class);
	}

	public boolean runNmapScan() {
		return profileInfo.getAsBoolean(IS_NMAP_DISCOVERY_ENABLED);
	}

	public boolean runSnmpScan() {
		return profileInfo.getAsBoolean(IS_SNMP_DISCOVERY_ENABLED);
	}

	public String getIpAddrType() {
		return scanInfo.getAsString(IP_ADDRESS_TYPE);
	}

	public String getIpDiscoveryType() {
		return scanInfo.getAsString(IP_DISC_TYPE);
	}

	public String getScanIps() {
		return scanInfo.getAsString(SCAN_IPS);
	}

	public String getIncludeSubNet() {
		return scanInfo.getAsString(INCLUDE_SUBNET);
	}
	
	public String getExcludeSubNet() {
		return scanInfo.getAsString(EXCLUDE_SUBNET);
	}
	
	public String getTcpPorts() {
		return scanInfo.getAsString(TCP_PORTS);
	}
	
	public List<String> getCredIds() {
		return profileInfo.getAsArrayList(CRED_IDS, String.class);
	}
	
	public String getGatewayIp() {
		return profileInfo.getAsString(GATEWAY_IP);
	}
	
	public boolean isVoipDiscoveryEnabled() {
		return discoveryOptions.getAsBoolean(IS_VOIP_DISCOVERY_ENABLED);
	}

	public boolean isL2LinkDiscoveryEnabled() {
		return discoveryOptions.getAsBoolean(IS_L2_LINK_DISCOVERY);
	}
	
	public boolean isBgpDiscoveryEnabled() {
		return discoveryOptions.getAsBoolean(IS_BGP_DISCOVERY_ENABLED) != null ?
				discoveryOptions.getAsBoolean(IS_BGP_DISCOVERY_ENABLED) : Boolean.FALSE;
	}
	
	public boolean isBridgeDiscoveryEnabled() {
		return discoveryOptions.getAsBoolean(IS_BRIDGE_DISCOVERY_ENABLED) != null ?
				discoveryOptions.getAsBoolean(IS_BRIDGE_DISCOVERY_ENABLED) : Boolean.FALSE;
	}
	
	public boolean isOspfDiscoveryEnabled() {
		return discoveryOptions.getAsBoolean(IS_OSPF_DISCOVERY_ENABLED) != null ?
				discoveryOptions.getAsBoolean(IS_OSPF_DISCOVERY_ENABLED) : Boolean.FALSE;
	}
	
	public boolean isLoadbalancerDiscoveryEnabled() {
		return discoveryOptions.getAsBoolean(IS_LOADBALANCER_ENABLED) != null ?
				discoveryOptions.getAsBoolean(IS_LOADBALANCER_ENABLED) : Boolean.FALSE;
	}

	public boolean isSnmpScanAgainstDiscoverHost() {
		if (null != discoveryOptions) {
			return discoveryOptions.getAsBoolean(IS_SNMP_SCAN_AGAINST_DISCOVER_HOST) != null ? 
					discoveryOptions.getAsBoolean(IS_SNMP_SCAN_AGAINST_DISCOVER_HOST) : Boolean.FALSE;
		}
		return Boolean.FALSE;
	}
	
	public boolean isSnmpEnabledDevice() {
		return discoveryOptions.getAsBoolean(IS_SNMP_ENABLED_DEVICE) != null ? 
				discoveryOptions.getAsBoolean(IS_SNMP_ENABLED_DEVICE) : Boolean.FALSE;
	}
	
	public boolean isWmiEnabledDevice() {
		return discoveryOptions.getAsBoolean(IS_WMI_ENABLED_DEVICE) != null ? 
				discoveryOptions.getAsBoolean(IS_WMI_ENABLED_DEVICE) : Boolean.FALSE;
	}
	
	public boolean isSshEnabledDevice() {
		return discoveryOptions.getAsBoolean(IS_SSH_ENABLED_DEVICE) != null ? 
				discoveryOptions.getAsBoolean(IS_SSH_ENABLED_DEVICE) : Boolean.FALSE;
	}

	public int getScanDepth() {
		return scanInfo.getAsInt(SCAN_DEPTH);
	}
	
	public boolean isReverseDnsLookup() {
		return discoveryOptions.getAsBoolean(IS_REVERSE_DNS_LOOKUP);
	}
	
	public boolean dontOwHostnameWithDns() {
		return discoveryOptions.getAsBoolean(DONT_OVERWRITE_HOSTNAME_WITH_DNS);
	}
	
	public List<PrimitiveEntity> getPasswdVaults() {
		return passwdVaults;
	}
}
