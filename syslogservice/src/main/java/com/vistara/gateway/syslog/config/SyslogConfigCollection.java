
package com.vistara.gateway.syslog.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.opsramp.gateway.common.db.client.DBClient;
import com.opsramp.gateway.common.db.client.DBFactory;
import com.opsramp.gateway.common.error.ProcessingError;
import com.vistara.discovery.syslog.SyslogProfileAction;
import com.vistara.gateway.constants.StringConstants;
import com.vistara.gateway.db.model.sql.SyslogConfPojo;
import com.vistara.gateway.db.repo.SyslogConfRepository;
import com.vistara.gateway.plugin.discovery.util.IpAddressUtils;

public class SyslogConfigCollection implements StringConstants {

	private static Logger log = LoggerFactory.getLogger(SyslogConfigCollection.class);

	private static SyslogConfigCollection instance = null;

	private static List<Profile> profiles = new ArrayList<>();
	private static Map<Integer, String> severitySideNote = new ConcurrentHashMap<>();
	private static Map<Integer, String> facilitySideNote = new ConcurrentHashMap<>();

	public static final Pattern PATTERN = Pattern.compile("\\$\\{(.+?)\\}");
	
	private static ThreadLocal<Matcher> threadLocalMatcher = new ThreadLocal<>();

	// private constructor
	private SyslogConfigCollection() {
	}

	static {
		setSeveritySideNote();
		setFacilitySideNote();
	}

	public static SyslogConfigCollection getInstance() {
		if (instance == null) {
			synchronized (SyslogConfigCollection.class) {
				if (instance == null) {
					instance = new SyslogConfigCollection();
				}
			}
		}
		return instance;
	}

	public void loadConfig() {
		// vprobe must have restarted. Reload the config from DB

		try {
			// clear all the data in collection.
			profiles.clear();
			

			List<SyslogConfPojo> syslogConfigs = DBClient.findAll(SyslogConfPojo.class);
			if (syslogConfigs != null && !syslogConfigs.isEmpty()) {
				Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
				syslogConfigs.forEach(conf -> {
					SyslogConfig config = gson.fromJson(conf.getPayload(), SyslogConfig.class);
					fillCollection(config);
				});

				sortCollection();

				log.error("Reloaded the Configuration from Database and Rebuilded config Collection");

			} else {
				log.error("## SYSLOG ## No Syslog Configuration found !!!");
			}

		} catch (Exception e) {
			log.error("Failed to Load the syslog configuration. Syslog will not work !!");
		}
	}

	private void sortCollection() {
		profiles.sort(Comparator.comparing(Profile::getProfileName, Comparator.nullsLast(Comparator.naturalOrder())));
	}

	public void loadConfig(String syslogJson) {
		// received config from Opsramp Cloud.
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

		// Drop it here if it is invalid configuration.
		try {
			if (syslogJson != null && !syslogJson.isEmpty()) {
				SyslogConfig syslogConf = gson.fromJson(syslogJson, SyslogConfig.class);

				if (syslogConf == null) {
					throw new ProcessingError("Invalid Syslog Configuration");
				}

				SyslogConfRepository repo = (SyslogConfRepository) DBFactory.getInstance().getRepository(SyslogConfPojo.class.getName());
				if (syslogConf.getProfileAction().equals(SyslogProfileAction.ADD)
						|| syslogConf.getProfileAction().equals(SyslogProfileAction.UPDATE)) {

					repo.createOrUpdateConfigByUniqueId(syslogConf.getUniqueId(), syslogJson);
					fillCollection(syslogConf);

				} else if (syslogConf.getProfileAction().equals(SyslogProfileAction.DELETE)) {
					repo.deleteConfigByUniqueId(syslogConf.getUniqueId());

					// remove from collection
					profiles.removeIf(r -> syslogConf.getUniqueId().equalsIgnoreCase(r.getUniqueId()));
				}

				// sort the config alphabetically
				sortCollection();

				log.error("Updated Database and Syslog config Collection");
			}

		} catch (JsonSyntaxException | JsonIOException e) {
			log.error("Invalid Syslog Configuration. Exception : {}", e.getMessage(), e);
		} catch (Exception e) {
			log.error("Failed to load the SyslogConfRepository.", e);
		}
	}

	private void fillCollection(SyslogConfig config) {

		Profile profile = new Profile();

		profile.setProfileName(config.getName());
		profile.setUniqueId(config.getUniqueId());
		profile.setServerties(new HashSet<Integer>(config.getGlobalFilters().getSeverities()));
		profile.setFacilites(new HashSet<Integer>(config.getGlobalFilters().getFacilities()));

		// Processing Resource Specific Rules
		ResourceSpecificRule[] rspecificRules = config.getResourceSpecificRules();

		// Process Rules
		Map<String, Rule> ruledef = new ConcurrentHashMap<>();
		Rule[] rulesArray = config.getRules();
		for (Rule rule : rulesArray) {
			processRule(rule);
			if (null != rule.getCompiledPattern()) {
				ruledef.put(rule.getUniqueId(), rule);
			}
		}
		
		for (ResourceSpecificRule rfilter : rspecificRules) {

			ResourceFilter filter = new ResourceFilter();
			filter.setUniqueId(rfilter.getUniqueId());
			
			for (String ruleid : rfilter.getRuleids()) {
				filter.addRule(ruledef.get(ruleid));
			}

			if (STRING_STAR.equals(rfilter.getIp())) {
				filter.setStar(true);
			} else {
				filter.setStar(false);
				filter.setIps(IpAddressUtils.getIpAddrsStrList(rfilter.getIp()));
			}

			// Add the filter to profile
			profile.addFilter(filter);
		}

		// remove the old object
		profiles.removeIf(p -> profile.getUniqueId().equalsIgnoreCase(p.getUniqueId()));
		// Add the profile to the list.
		profiles.add(profile);
	}

	private void processRule(Rule rule) {
		try {
			rule.setCompiledPattern(Pattern.compile(rule.getPattern()));
			
			// flags and matched groups to avoid processing overhead.
			setFlags(rule.getMetricName(), rule, "metric");
			setFlags(rule.getComponent(), rule, "component");
			setFlags(rule.getAlertSubject(), rule, "subject");
			setFlags(rule.getAlertDescription(), rule, "description");
		} catch (PatternSyntaxException pse) {
			log.error("Invalid Regex for Rule : {}. Pattern : {},, Msg : {}", rule.getName(), pse.getPattern(),	pse.getMessage());
		} catch (Exception e) {
			// Ignore
		}
	}

	private void setFlags(String inputStr, Rule rule, String key) {
		try {
			Matcher matcher = PATTERN.matcher(inputStr);
			List<String> list = new ArrayList<>();
			while (matcher.find()) {
				list.add(matcher.group());
			}

			if (!list.isEmpty()) {
				switch (key) {
				case "metric":
					rule.setDynamicMetric(true);
					rule.setMetricGroups(list);
					break;

				case "component":
					rule.setDynamicComponent(true);
					rule.setComponentgroups(list);
					break;

				case "subject":
					rule.setDynamicSubject(true);
					rule.setSubjectgroups(list);
					break;

				case "description":
					rule.setDynamicDescription(true);
					rule.setDescriptiongroups(list);
					break;

				default:
					break;
				}
			}
		} catch (Exception e) {
			log.error("Error while processing syslog flags : {}", inputStr);
		}
	}

	private static void setSeveritySideNote() {
		severitySideNote.put(0, " (Emergency: system is unusable)");
		severitySideNote.put(1, " (Alert: action must be taken immediately)");
		severitySideNote.put(2, " (Critical: critical conditions)");
		severitySideNote.put(3, " (Error: error conditions)");
		severitySideNote.put(4, " (Warning: warning conditions)");
		severitySideNote.put(5, " (Notice: normal but significant condition)");
		severitySideNote.put(6, " (Informational: informational messages)");
		severitySideNote.put(7, " (Debug: debug-level messages)");
	}

	private static void setFacilitySideNote() {
		facilitySideNote.put(0, " (kernel messages)");
		facilitySideNote.put(1, " (user-level messages)");
		facilitySideNote.put(2, " (mail system)");
		facilitySideNote.put(3, " (system daemons)");
		facilitySideNote.put(4, " (security/authorization messages)");
		facilitySideNote.put(5, " (messages generated internally by syslogd)");
		facilitySideNote.put(6, " (line printer subsystem)");
		facilitySideNote.put(7, " (network news subsystem)");
		facilitySideNote.put(8, " (UUCP subsystem)");
		facilitySideNote.put(9, " (clock daemon)");
		facilitySideNote.put(10, " (security/authorization messages)");
		facilitySideNote.put(11, " (FTP daemon)");
		facilitySideNote.put(12, " (NTP subsystem)");
		facilitySideNote.put(13, " (log audit)");
		facilitySideNote.put(14, " (log alert)");
		facilitySideNote.put(15, " (clock daemon (note 2))");
		facilitySideNote.put(16, " (local use 0  (local0))");
		facilitySideNote.put(17, " (local use 1  (local1))");
		facilitySideNote.put(18, " (local use 2  (local2))");
		facilitySideNote.put(19, " (local use 3  (local3))");
		facilitySideNote.put(20, " (local use 4  (local4))");
		facilitySideNote.put(21, " (local use 5  (local5))");
		facilitySideNote.put(22, " (local use 6  (local6))");
		facilitySideNote.put(23, " (local use 7  (local7))");
	}

	public static String getSeveritySideNote(int severity) {
		String str = severitySideNote.get(severity);
		return str != null ? str : "";
	}

	public static String getFacilitySideNote(int facility) {
		String str = facilitySideNote.get(facility);
		return str != null ? str : "";
	}
	
	public static List<Profile> getProfiles() {
		return profiles;
	}
	
	public Matcher getMatcherObject() {
		Matcher m = threadLocalMatcher.get();
		if (m == null) {
			m = PATTERN.matcher("");
			threadLocalMatcher.set(m);
		}
		
		return m;
	}
	
	public void remove() { // Just for sonar-lint issue.
		threadLocalMatcher.remove();
	}
	
}
