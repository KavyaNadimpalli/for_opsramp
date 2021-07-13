package com.vistara.gateway.syslog.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Profile {

	private String uniqueId;
	private String profileName;
	private boolean isAllSeverities;
	private Set<Integer> serverties = null;
	private boolean isAllFacilities;
	private Set<Integer> facilites = null;
	private List<ResourceFilter> rfilters = new ArrayList<>();

	public Set<Integer> getServerties() {
		return serverties;
	}

	public void setServerties(Set<Integer> serverties) {
		if(serverties.size() == 8)
			setAllSeverities(true);
			
		this.serverties = serverties;
	}
	
	public void setFacilites(Set<Integer> facilites) {
		if(facilites.size() == 24)
			setAllFacilities(true);
		
		this.facilites = facilites;
	}
	
	public boolean isAllSeverities() {
		return isAllSeverities;
	}
	
	public void setAllSeverities(boolean isAllSeverities) {
		this.isAllSeverities = isAllSeverities;
	}
	
	public boolean isAllFacilities() {
		return isAllFacilities;
	}
	
	public void setAllFacilities(boolean isAllFacilities) {
		this.isAllFacilities = isAllFacilities;
	}
	
	public Set<Integer> getFacilites() {
		return facilites;
	}

	public void addFilter(ResourceFilter filter) {
		rfilters.add(filter);
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public List<ResourceFilter> getRfilters() {
		return rfilters;
	}

	public void setRfilters(List<ResourceFilter> rfilters) {
		this.rfilters = rfilters;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

}
