package com.vistara.gateway.syslog.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
//import com.opsramp.gateway.adapter.ftp.monitor.FtpMonitorDataCollectorWorker;
import com.opsramp.gateway.common.core.GenericRequest;
import com.opsramp.gateway.common.core.GenericResponse;
import com.opsramp.gateway.common.core.MetaData;
import com.opsramp.gateway.common.listener.Processor;
import com.opsramp.gateway.common.util.AppUtils;
import com.opsramp.gateway.monitor.NewGenericMonitorPolicyBuilder;
import com.opsramp.gateway.monitor.snapshot.MonSnapShotWorker;
import com.vistara.mon.msg.model.GatewayMonSnapShotReq;


public class ApplicationProcessor implements Processor {
	private static final Logger LOG = LoggerFactory.getLogger(ApplicationProcessor.class);

	@Override
	public GenericResponse run(String queueName, String requestJson) {
		GenericRequest request = new Gson().fromJson(requestJson, GenericRequest.class);
		try {
			request.setMessage(AppUtils.getPayload(request.getMessage()));
		} catch (Exception e1) {
			LOG.error("Failed to retrieve payload.. {}", e1.getMessage(), e1);
		}
		MetaData metaData = request.getMetaData();
		if (metaData == null) {
			return new GenericResponse(500, "Request Metadata not found");
		}

		if (metaData.getAction() == null || metaData.getAction().isEmpty()) {
			return new GenericResponse(500, "No action to process this request");
		}

		if () {
		}
		return new GenericResponse(GenericResponse.OK);
	}
}
