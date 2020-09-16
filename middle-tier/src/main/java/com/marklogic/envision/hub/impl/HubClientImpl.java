package com.marklogic.envision.hub.impl;

import com.marklogic.appdeployer.AppConfig;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.ext.DatabaseClientConfig;
import com.marklogic.client.ext.SecurityContextType;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.hub.DatabaseKind;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.mgmt.ManageClient;

import java.util.Map;

public class HubClientImpl implements HubClient {

	private final String username;
	private final DatabaseClient stagingClient;
	private final DatabaseClient finalClient;
	private final DatabaseClient jobsClient;
	private final DatabaseClient modulesClient;
	private final DatabaseClient stagingSchemasClient;
	private final DatabaseClient finalSchemasClient;
	private final Map<DatabaseKind, String> databaseNames;
	private final ManageClient manageClient;
	private final HubConfigImpl hubConfig;

	public HubClientImpl(HubConfigImpl hubConfig, Map<DatabaseKind, String> databaseNames) {
		username = hubConfig.getMlUsername();
		stagingClient = hubConfig.newStagingClient(null);
		finalClient = newFinalClient(hubConfig);
		jobsClient = hubConfig.newJobDbClient();
		modulesClient = hubConfig.newModulesDbClient();
		stagingSchemasClient = hubConfig.newStagingClient(hubConfig.getDbName(DatabaseKind.STAGING_SCHEMAS));
		finalSchemasClient = hubConfig.newStagingClient(hubConfig.getDbName(DatabaseKind.FINAL_SCHEMAS));
		this.databaseNames = databaseNames;
		this.manageClient = hubConfig.getManageClient();
		this.hubConfig = hubConfig;
	}

	private DatabaseClient newFinalClient(HubConfigImpl hubConfig) {
		AppConfig appConfig = hubConfig.getAppConfig();
		DatabaseClientConfig config = new DatabaseClientConfig(appConfig.getHost(), hubConfig.getPort(DatabaseKind.FINAL), hubConfig.getMlUsername(), hubConfig.getMlPassword());
		config.setSecurityContextType(SecurityContextType.valueOf(hubConfig.getAuthMethod(DatabaseKind.FINAL).toUpperCase()));
		config.setSslHostnameVerifier(hubConfig.getSslHostnameVerifier(DatabaseKind.FINAL));
		config.setSslContext(hubConfig.getSslContext(DatabaseKind.FINAL));
		config.setCertFile(hubConfig.getCertFile(DatabaseKind.FINAL));
		config.setCertPassword(hubConfig.getCertPassword(DatabaseKind.FINAL));
		config.setExternalName(hubConfig.getExternalName(DatabaseKind.FINAL));
		config.setTrustManager(hubConfig.getTrustManager(DatabaseKind.FINAL));
		if (hubConfig.getIsHostLoadBalancer()) {
			config.setConnectionType(DatabaseClient.ConnectionType.GATEWAY);
		}
		return appConfig.getConfiguredDatabaseClientFactory().newDatabaseClient(config);
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getDbName(DatabaseKind kind) {
		return databaseNames.get(kind);
	}

	@Override
	public DatabaseClient getStagingClient() {
		return stagingClient;
	}

	@Override
	public DatabaseClient getFinalClient() {
		return finalClient;
	}

	@Override
	public DatabaseClient getJobsClient() {
		return jobsClient;
	}

	@Override
	public DatabaseClient getModulesClient() {
		return modulesClient;
	}

	@Override
	public DatabaseClient getStagingSchemasClient() {
		return stagingSchemasClient;
	}

	@Override
	public DatabaseClient getFinalSchemasClient() {
		return finalSchemasClient;
	}

	@Override
	public ManageClient getManageClient() {
		return manageClient;
	}

	@Override
	public HubConfigImpl getHubConfig() { return hubConfig; }
}