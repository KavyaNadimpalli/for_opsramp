
package com.vistara.gateway.syslog.config;

import java.util.Set;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class GlobalFilters {

	@SerializedName("severity")
	@Expose
	private Set<Integer> severities = null;
	@SerializedName("facility")
	@Expose
	private Set<Integer> facilities = null;

	public Set<Integer> getSeverities() {
		return severities;
	}

	public void setSeverities(Set<Integer> severities) {
		this.severities = severities;
	}

	public Set<Integer> getFacilities() {
		return facilities;
	}

	public void setFacilities(Set<Integer> facilities) {
		this.facilities = facilities;
	}
}
