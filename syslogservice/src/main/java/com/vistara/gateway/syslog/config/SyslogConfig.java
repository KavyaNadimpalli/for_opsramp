
package com.vistara.gateway.syslog.config;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vistara.discovery.syslog.SyslogProfileAction;

public class SyslogConfig {

	@Expose
	@SerializedName("clientId")
	private long clientId;
	@Expose
	@SerializedName("partnerId")
	private long partnerId;
	@Expose
	@SerializedName("profileId")
	private long profileId;
	@Expose
	@SerializedName("name")
	private String name;
	@Expose
	@SerializedName("uniqueId")
	private String uniqueId;
	@Expose
	@SerializedName("profileAction")
	private SyslogProfileAction profileAction;
	@Expose
	@SerializedName("version")
	private int version;
	@Expose
	@SerializedName("globalFilters")
	private GlobalFilters globalFilters;
	@Expose
	@SerializedName("resourceSpecificRules")
	private ResourceSpecificRule[] resourceSpecificRules = null;
	@Expose
	@SerializedName("rules")
	private Rule[] rules = null;
	@Expose
	@SerializedName("createdTime")
	private long createdTime;
	@Expose
	@SerializedName("updatedTime")
	private long lastUpdatedTime;

	public long getClientId() {
		return clientId;
	}

	public void setClientId(long clientId) {
		this.clientId = clientId;
	}

	public long getPartnerId() {
		return partnerId;
	}

	public void setPartnerId(long partnerId) {
		this.partnerId = partnerId;
	}

	public long getProfileId() {
		return profileId;
	}

	public void setProfileId(long profileId) {
		this.profileId = profileId;
	}

	public SyslogProfileAction getProfileAction() {
		return profileAction;
	}

	public void setProfileAction(SyslogProfileAction profileAction) {
		this.profileAction = profileAction;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public GlobalFilters getGlobalFilters() {
		return globalFilters;
	}

	public void setGlobalFilters(GlobalFilters globalFilters) {
		this.globalFilters = globalFilters;
	}

	public ResourceSpecificRule[] getResourceSpecificRules() {
		return resourceSpecificRules;
	}

	public void setResourceSpecificRules(ResourceSpecificRule[] resourceSpecificRules) {
		this.resourceSpecificRules = resourceSpecificRules;
	}

	public Rule[] getRules() {
		return rules;
	}

	public void setRules(Rule[] rules) {
		this.rules = rules;
	}

	public long getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}

	public long getLastUpdatedTime() {
		return lastUpdatedTime;
	}

	public void setLastUpdatedTime(long lastUpdatedTime) {
		this.lastUpdatedTime = lastUpdatedTime;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
