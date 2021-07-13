
package com.vistara.gateway.syslog.config;

import java.util.List;
import java.util.regex.Pattern;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Rule {

	@SerializedName("name")
	@Expose
	private String name;
	@SerializedName("uniqueId")
	@Expose
	private String uniqueId;
	@SerializedName("action")
	@Expose
	private String action;
	@SerializedName("pattern")
	@Expose
	private String pattern;
	@SerializedName("metricName")
	@Expose
	private String metricName;
	@SerializedName("component")
	@Expose
	private String component;
	@SerializedName("alertSeverity")
	@Expose
	private String alertSeverity;
	@SerializedName("alertSubject")
	@Expose
	private String alertSubject;
	@SerializedName("alertDescription")
	@Expose
	private String alertDescription;

	private Pattern compiledPattern = null;
	
	private boolean isDynamicMetric;
	private boolean isDynamicComponent;
	private boolean isDynamicSubject;
	private boolean isDynamicDescription;

	private List<String> metricGroups = null;
	private List<String> componentgroups = null;
	private List<String> subjectgroups = null;
	private List<String> descriptiongroups = null;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public String getAlertSeverity() {
		return alertSeverity;
	}

	public void setAlertSeverity(String alertSeverity) {
		this.alertSeverity = alertSeverity;
	}

	public String getAlertSubject() {
		return alertSubject;
	}

	public void setAlertSubject(String alertSubject) {
		this.alertSubject = alertSubject;
	}

	public String getAlertDescription() {
		return alertDescription;
	}

	public void setAlertDescription(String alertDescription) {
		this.alertDescription = alertDescription;
	}

	public Pattern getCompiledPattern() {
		return compiledPattern;
	}

	public void setCompiledPattern(Pattern compiledPattern) {
		this.compiledPattern = compiledPattern;
	}
	
	public boolean isDynamicMetric() {
		return isDynamicMetric;
	}

	public void setDynamicMetric(boolean isDynamicMetric) {
		this.isDynamicMetric = isDynamicMetric;
	}

	public boolean isDynamicComponent() {
		return isDynamicComponent;
	}

	public void setDynamicComponent(boolean isDynamicComponent) {
		this.isDynamicComponent = isDynamicComponent;
	}

	public boolean isDynamicSubject() {
		return isDynamicSubject;
	}

	public void setDynamicSubject(boolean isDynamicSubject) {
		this.isDynamicSubject = isDynamicSubject;
	}

	public boolean isDynamicDescription() {
		return isDynamicDescription;
	}

	public void setDynamicDescription(boolean isDynamicDescription) {
		this.isDynamicDescription = isDynamicDescription;
	}

	public List<String> getMetricGroups() {
		return metricGroups;
	}

	public void setMetricGroups(List<String> metricGroups) {
		this.metricGroups = metricGroups;
	}

	public List<String> getComponentgroups() {
		return componentgroups;
	}

	public void setComponentgroups(List<String> componentgroups) {
		this.componentgroups = componentgroups;
	}

	public List<String> getSubjectgroups() {
		return subjectgroups;
	}

	public void setSubjectgroups(List<String> subjectgroups) {
		this.subjectgroups = subjectgroups;
	}

	public List<String> getDescriptiongroups() {
		return descriptiongroups;
	}

	public void setDescriptiongroups(List<String> descriptiongroups) {
		this.descriptiongroups = descriptiongroups;
	}
}
