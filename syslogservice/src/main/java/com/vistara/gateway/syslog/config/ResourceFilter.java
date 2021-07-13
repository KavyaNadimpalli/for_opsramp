package com.vistara.gateway.syslog.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ResourceFilter {

	private String uniqueId;
	private boolean isStar;
	private Set<String> ips;
	private List<Rule> rules = new ArrayList<>();

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public boolean isStar() {
		return isStar;
	}

	public void setStar(boolean isStar) {
		this.isStar = isStar;
	}

	public Set<String> getIps() {
		return ips;
	}

	public void setIps(Set<String> ips) {
		this.ips = ips;
	}

	public List<Rule> getRules() {
		return rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	public void addRule(Rule rule) {
		rules.add(rule);
	}

}
