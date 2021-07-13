package com.vistara.gateway.syslog.config;


import java.util.List;

import com.opsramp.gateway.monitor.model.IPAddress;
import com.vistara.gateway.constants.StringConstants;
//import com.vistara.gateway.model.IPAddress;

/**
 * @author yesu.srungavruksham
 *
 */
public interface IpAddressExpander extends StringConstants {

	public boolean isThisIpVersion(String ipDetailToken);
	
	public List<IPAddress> expandIpAddresses(String ipDetailToken);
	
	public List<String> expandIpAddressesStrList(String ipDetailToken);

	public enum IpRangeType {
		
		HIPEN("-"),
		STAR("*"),
		SUBNET("/"),
		DEFAULT(null);
		
		private String typeChar;
		
		private IpRangeType(String typeChar) {
			this.typeChar = typeChar;
		}
		
		public String getTypeChar() {
			return typeChar;
		}
	}
}
