package com.vistara.gateway.syslog.bootstrap;

import java.io.FileReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.opsramp.gateway.common.configuration.AppConfiguration;
import com.opsramp.gateway.common.configuration.ApplicationConfig;
import com.opsramp.gateway.common.configuration.ApplicationInfo;
import com.opsramp.gateway.common.core.ApplicationInitiator;
import com.opsramp.gateway.common.listener.Processor;


public class ApplicationMain extends ApplicationInitiator {
	static Logger logger = LoggerFactory.getLogger(ApplicationMain.class.getName());

	public static void main(String[] args) throws Exception {
		Processor applicationProcessor = new ApplicationProcessor();
		try {
			ApplicationInfo info = null;
			info = new Gson().fromJson(new FileReader("/opt/app/info.json"), ApplicationInfo.class);

			ApplicationConfig config = null;
			config = new Gson().fromJson(new FileReader("/opt/app/config/config.json"), ApplicationConfig.class);

			String[] dependencies = new Gson().fromJson(new FileReader("/opt/app/config/dependencies.json"),
					String[].class);

			AppConfiguration appConfiguration = new AppConfiguration.Builder(info, config).setDependency(dependencies)
					.setProcessor(applicationProcessor).build();

			register(appConfiguration);
		} catch (Exception e) {
			logger.error("Failed to register service", e);
			System.exit(1);
		}
	}
}
