package com.repl.asset.master.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * KEEPS ams_add_device_master.age CURRENT.
 *
 * Age (Year.Month) grows by itself as months pass, so the stored value is brought up to today
 * when the backend starts and again every night just after midnight. Only rows whose age has
 * changed are written.
 */
@Configuration
@EnableScheduling
public class AgeRefreshJob {

	private static final Logger logger = LoggerFactory.getLogger(AgeRefreshJob.class);

	@Autowired
	private AssetService assetService;

	@EventListener(ApplicationReadyEvent.class)
	public void onStartup() {
		refresh();
	}

	@Scheduled(cron = "0 5 0 * * *")
	public void nightly() {
		refresh();
	}

	private void refresh() {
		try {
			int changed = assetService.refreshAges();
			if (changed > 0) {
				logger.info("Device age brought up to date for {} device(s).", changed);
			}
		} catch (Exception e) {
			logger.error("Could not refresh device ages — has the age column been added to "
					+ "ams_add_device_master?", e);
		}
	}
}
