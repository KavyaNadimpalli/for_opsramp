package com.vistara.gateway.syslog;

import static com.vistara.gateway.syslog.SyslogService.isDebugEnabled;
import static com.vistara.gateway.syslog.config.SyslogConfigCollection.getFacilitySideNote;
import static com.vistara.gateway.syslog.config.SyslogConfigCollection.getSeveritySideNote;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.opsramp.gateway.common.alert.AlertServiceResponse;
import com.opsramp.gateway.common.alert.AlertType;
import com.opsramp.gateway.common.alert.EventCollector;
import com.opsramp.gateway.common.alert.EventSource;
import com.opsramp.gateway.common.alert.GatewayAlertEvent;
import com.opsramp.gateway.common.alert.GatewayEvent;
import com.opsramp.gateway.common.alert.MetricState;
import com.opsramp.gateway.common.alert.PriorityType;
import com.opsramp.gateway.common.core.GenericRequest;
import com.opsramp.gateway.common.publisher.queue.ApplicationPublisher;
import com.opsramp.gateway.common.util.TimeUtils;
import com.vistara.gateway.constants.StringConstants;
import com.vistara.gateway.syslog.config.Profile;
import com.vistara.gateway.syslog.config.ResourceFilter;
import com.vistara.gateway.syslog.config.Rule;
import com.vistara.gateway.syslog.config.SyslogConfigCollection;


/**
 * 
 * @author pavanguruvelli
 *
 */
public class SyslogEventProcessor implements Runnable, StringConstants {
	private static final Logger LOG = LoggerFactory.getLogger(SyslogEventProcessor.class);

	private String ipAddress = null;
	private byte[] byteArray = null;

	public SyslogEventProcessor(String ipAddress, byte[] byteArray) {
		this.ipAddress = ipAddress;
		this.byteArray = byteArray;
	}

	@Override
	public void run() {
		try {

			long startTime = System.currentTimeMillis();
			processSyslogAlert();
			long endTime = System.currentTimeMillis();

			LOG.error("Time : {} s", (endTime-startTime)/1000 );

		} catch (Exception e) {
			LOG.error("Syslog Msg Processing Failed : {}", e.getMessage());
		}
	}

	private void processSyslogAlert() {

		SyslogEvent event = parseEvent(new String(byteArray, 0, byteArray.length, StandardCharsets.UTF_8));

		// create a Matcher Obj
		Matcher m = SyslogConfigCollection.getInstance().getMatcherObject();
		
		// Loop through the profiles alphabetically
		outerloop: for (Profile profile : SyslogConfigCollection.getProfiles()) {

			// check severities
			if (profile.isAllSeverities() || profile.getServerties().contains(event.getSeverity())) {
				
				// check facilities
				if (profile.isAllFacilities() || profile.getFacilites().contains(event.getFacility())) {

					// Loop through filters sequentially
					for (ResourceFilter filter : profile.getRfilters()) {
						if (filter.isStar() || filter.getIps().contains(ipAddress)) {
							boolean ruleMatched = executeRules(filter.getRules(), event, m);
							
							if (ruleMatched) 
								break outerloop;
						}
					}
				} else {
					if (isDebugEnabled())
						LOG.error("Skipped:Facilites. IP:{}, Facility:{}, Profile:{}", ipAddress, event.getFacility(), profile.getProfileName());
					
					break;
				}
			} else {
				if (isDebugEnabled())
					LOG.error("Skipped:Severities. IP : {}, severity: {}, Profile: {}", ipAddress, event.getSeverity(),	profile.getProfileName());
				
				break;
			}
		}
		
		
	}

	private boolean executeRules(List<Rule> rules, SyslogEvent event, Matcher m) {
		boolean noMatch = true;
		GatewayAlertEvent alertEvent = null;

		// Execute the rules sequentially. Drop the Loop if any matches.
		for (Rule rule : rules) {
			
			m.usePattern(rule.getCompiledPattern());
			m.reset(event.getBody());

			if (m.find()) {

				noMatch = false;

				if (EXCLUDE.equalsIgnoreCase(rule.getAction())) {

					if (isDebugEnabled())  // If Rule is Matched and action is Exclude. Break and silently drop the Msg.
						LOG.error("Regex Matched:Excluded. IP : {} Rule : {}", ipAddress, rule.getName());

					break;
				}

				Map<String, String> tokenMap = new HashMap<>();
				for (int idx = 1; idx <= m.groupCount(); idx++) {
					// Key : Group Number, Value : Matched group string
					tokenMap.put(PREFIX + idx + SUFFIX, m.group(idx));
				}

				tokenMap.put(SYSLOG_DESCRIPTION_MACRO, event.getBody());
				tokenMap.put(SYSLOG_TIMESTAMP_MACRO, String.valueOf(System.currentTimeMillis()));

				alertEvent = prepareGatewayAlertEvent(ipAddress, rule, tokenMap, event);
				// Break the Loop upon match
				break;
			}
		}

		try {
			if (null != alertEvent) {
				if (isDebugEnabled()) 
					LOG.error("Syslog Alert Generated. Ip: {}, Alert : {}", ipAddress, alertEvent);

				GatewayEvent gwEvent = new GatewayEvent();
				gwEvent.addGatewayEvent(alertEvent);

				sendGatewayEvents(gwEvent);
			}
		} catch (Exception e) {
			LOG.error("Failed to send Gateway Syslog Event {}", e.getMessage());
		}

		return !noMatch;
	}

	private GatewayAlertEvent prepareGatewayAlertEvent(String ipAddress, Rule rule, Map<String, String> tokenMap,
			SyslogEvent event) {

		GatewayAlertEvent gatewayEvent = new GatewayAlertEvent();
		try {
			gatewayEvent.setResUUID("");
			gatewayEvent.setEvtSource(EventSource.MONITORING);
			gatewayEvent.setEvtCollector(EventCollector.GATEWAY);
			gatewayEvent.setGenTimeInSec(TimeUtils.getSystemTimeInGMT().getTime() / 1000);
			gatewayEvent.setResName(ipAddress);
			gatewayEvent.setResIPaddr(ipAddress);
			String gatewayUUID= System.getenv("GATEWAY_UUID");
			gatewayEvent.setCollectorUUID(gatewayUUID);
			gatewayEvent.setMetricCurState(MetricState.getEnumVal(rule.getAlertSeverity()));

			if (rule.isDynamicMetric()) {
				gatewayEvent.setMetricName(replaceTokens(rule.getMetricGroups(), rule.getMetricName(), tokenMap));
			} else {
				gatewayEvent.setMetricName(rule.getMetricName());
			}

			if (rule.isDynamicComponent()) {
				gatewayEvent.setEvtUID(replaceTokens(rule.getComponentgroups(), rule.getComponent(), tokenMap));
			} else {
				gatewayEvent.setEvtUID(rule.getComponent());
			}

			if (rule.isDynamicSubject()) {
				gatewayEvent.setSubject(replaceTokens(rule.getSubjectgroups(), rule.getAlertSubject(), tokenMap));
			} else {
				gatewayEvent.setSubject(rule.getAlertSubject());
			}

			if (rule.isDynamicDescription()) {
				gatewayEvent.setBody(setAdditionalInfo(replaceTokens(rule.getDescriptiongroups(), rule.getAlertDescription(), tokenMap), event));
			} else {
				gatewayEvent.setBody(setAdditionalInfo(rule.getAlertDescription(), event));
			}
			return gatewayEvent;
		} catch (Exception e) {
			LOG.error("Failed while preparing Alert : {}", e.getMessage(), e);
		}
		return null;
	}

	private static String replaceTokens(List<String> matchedGroupList, String source,
			Map<String, String> replacementMap) {

		if (null != matchedGroupList && !matchedGroupList.isEmpty()) {
			for (String searchString : matchedGroupList) {
				source = StringUtils.replace(source, searchString, replacementMap.get(searchString));
			}
		}
		return source;
	}

	private static String setAdditionalInfo(String str, SyslogEvent event) {

		StringBuilder sb = new StringBuilder(str != null ? str : "");
		sb.append(NEW_LINE).append(NEW_LINE).append(ADDITIONAL_INFO).append(NEW_LINE)
		.append(PRIORITY).append(event.getPriority()).append(NEW_LINE)
		.append(SEVERITY).append(event.getSeverity()).append(getSeveritySideNote(event.getSeverity()))
		.append(NEW_LINE)
		.append(FACILITY).append(event.getFacility()).append(getFacilitySideNote(event.getFacility()))
		.append(NEW_LINE);

		return sb.toString();
	}

	protected static SyslogEvent parseEvent(String message) {
		SyslogEvent event = new SyslogEvent();
		if (message.charAt(0) == '<') {
			int i = message.indexOf('>');

			if (i <= 4 && i > -1) {
				String priorityStr = message.substring(1, i);

				int priority = 0;
				try {
					priority = Integer.parseInt(priorityStr);
					int facility = priority >> 3;
				int severity = priority - (facility << 3);
				message = message.substring(i + 1);

				event.setPriority(priority);
				event.setFacility(facility);
				event.setSeverity(severity);
				event.setBody(message);

				} catch (Exception e) {
					LOG.error("UnsupportedFormatException : This Syslog Format is not Supported.");
				}
			}
		}
		return event;
	}
	
	public static void sendGatewayEvents(GatewayEvent gwEvent ) throws Exception {
		

		if (gwEvent.getEvtList() != null && !gwEvent.getEvtList().isEmpty()) {
			Gson gson = new Gson();
			String alertJsonData = gson.toJson(gwEvent);
			AlertServiceResponse alertServiceResponse = new AlertServiceResponse();
			alertServiceResponse.setAlerts(alertJsonData);
			alertServiceResponse.setAlertType(AlertType.GATEWAY_EVENT);
			alertServiceResponse.setPriorityType(PriorityType.LOW);
			String alertServiceJson = gson.toJson(alertServiceResponse);
			GenericRequest request = new GenericRequest();
			request.setMessage(alertServiceJson);
			ApplicationPublisher.publish("alertservice", gson.toJson(request));
		}
	}
}
