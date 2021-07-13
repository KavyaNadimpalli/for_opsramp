
package com.vistara.gateway.syslog.config;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResourceSpecificRule {

	@SerializedName("uniqueId")
	@Expose
	private String uniqueId;
	@SerializedName("ip")
	@Expose
	private String ip;
	@SerializedName("ruleids")
	@Expose
	private List<String> ruleids = new ArrayList<>();

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public List<String> getRuleids() {
		return ruleids;
	}

	public void setRuleids(List<String> ruleids) {
		this.ruleids = ruleids;
	}

}
