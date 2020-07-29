package com.marklogic.envision.deploy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.appdeployer.AppDeployer;
import com.marklogic.appdeployer.command.Command;
import com.marklogic.appdeployer.impl.SimpleAppDeployer;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.ext.util.DefaultDocumentPermissionsParser;
import com.marklogic.client.ext.util.DocumentPermissionsParser;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.envision.commands.DeployEnvisionModulesCommand;
import com.marklogic.hub.HubConfig;
import com.marklogic.hub.deploy.commands.LoadUserArtifactsCommand;
import com.marklogic.hub.flow.Flow;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.hub.mapping.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeployService {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private DocumentPermissionsParser documentPermissionsParser = new DefaultDocumentPermissionsParser();

	final private HubConfigImpl hubConfig;
	final private LoadUserArtifactsCommand loadUserArtifactsCommand;

	private ObjectMapper objectMapper = new ObjectMapper();

	DatabaseClient stagingClient;
	DatabaseClient finalClient;

	Path entitiesPath;
	Path mappingsPath;
	Path stepDefPath;
	Path flowPath;

	JSONDocumentManager finalDocMgr;
	JSONDocumentManager stagingDocMgr;

	@Autowired
	DeployService(HubConfigImpl hubConfig, LoadUserArtifactsCommand loadUserArtifactsCommand) {
		this.hubConfig = hubConfig;
		this.loadUserArtifactsCommand = loadUserArtifactsCommand;
	}

	private void init() {
		if (this.stagingClient != null) {
			return;
		}
		this.stagingClient = hubConfig.newStagingClient();
		this.finalClient = hubConfig.newFinalClient();
		this.entitiesPath = hubConfig.getHubEntitiesDir();
		this.mappingsPath = hubConfig.getHubMappingsDir();
		this.stepDefPath = hubConfig.getStepDefinitionsDir();
		this.flowPath = hubConfig.getFlowsDir();
		this.finalDocMgr = finalClient.newJSONDocumentManager();
		this.stagingDocMgr = stagingClient.newJSONDocumentManager();
	}

	public boolean needsInstall() {
		return hubConfig.getAppConfig().newModulesDatabaseClient().newDocumentManager().exists("/envision/search/findEntities.sjs") == null;
	}

	@Async
	public void deployUserArtifactsAsync() {
		deployUserArtifacts();
	}

	public void deployUserArtifacts() {
		List<Command> commands = new ArrayList<>();

		loadUserArtifactsCommand.setHubConfig(hubConfig);
		loadUserArtifactsCommand.setForceLoad(false);

		// Generating function metadata xslt causes running existing mapping (xslts) step to fail with undefined function
		// for any mappings that use these functions. So, we have to generate function metadata xslt only when 'forceLoad'
		// is set to true which would ensure that mappings are inserted and compiled to xslts as well.
		// Added as a temporary fix for DHFPROD-3606.
//		if(forceLoad){
//			commands.add(generateFunctionMetadataCommand);
//		}
		commands.add(loadUserArtifactsCommand);

		SimpleAppDeployer deployer = new SimpleAppDeployer(((HubConfigImpl)hubConfig).getManageClient(), ((HubConfigImpl)hubConfig).getAdminManager());
		deployer.setCommands(commands);
		deployer.deploy(hubConfig.getAppConfig());
	}

	public void deploy() {
		try {
			AppDeployer appDeployer = new SimpleAppDeployer(hubConfig.getManageClient(), hubConfig.getAdminManager(), new DeployEnvisionModulesCommand(hubConfig));
			appDeployer.deploy(hubConfig.getAppConfig());
		}
		catch (Error error) {
			error.printStackTrace();
		}
	}

	public void loadMapping(Mapping mapping) {
		init();
		String uri = getMappingUri(mapping.getName(), mapping.getVersion());
		DocumentMetadataHandle meta = buildMetadata("http://marklogic.com/data-hub/mappings", hubConfig.getModulePermissions());
		StringHandle handle = new StringHandle(mapping.serialize());
		this.stagingDocMgr.write(uri, meta, handle);
		this.finalDocMgr.write(uri, meta, handle);
	}

	public void deleteMapping(String mappingName, int version) {
		init();
		String uri = getMappingUri(mappingName, version);
		this.stagingDocMgr.delete(uri);
		this.finalDocMgr.delete(uri);
	}

	private String getMappingUri(String mappingName, int version) {
		return "/mappings/" + mappingName + "/" + mappingName + "-" + version + ".mapping.json";
	}

	public void loadFlow(Flow flow) {
		init();
		String uri = "/flows/" + flow.getName() + ".flow.json";
		DocumentMetadataHandle meta = buildMetadata("http://marklogic.com/data-hub/flow", hubConfig.getModulePermissions());
		try {
			StringHandle handle = new StringHandle(objectMapper.writeValueAsString(flow));
			this.stagingDocMgr.write(uri, meta, handle);
			this.finalDocMgr.write(uri, meta, handle);
		}
		catch(JsonProcessingException e) {
			throw new RuntimeException("Invalid Flow Json");
		}
	}

	protected DocumentMetadataHandle buildMetadataForEntityModels(HubConfig config) {
		String permissions = config.getEntityModelPermissions();
		if (permissions == null || permissions.trim().length() < 1) {
			if (logger.isInfoEnabled()) {
				logger.info("Entity model permissions were not set, so using module permissions; consider setting mlEntityModelPermissions " +
					"in case you want entity models to have custom permissions.");
			}
			permissions = config.getModulePermissions();
		}
		return buildMetadata("http://marklogic.com/entity-services/models", permissions);
	}

	private DocumentMetadataHandle buildMetadata(String collection, String permissions) {
		DocumentMetadataHandle meta = new DocumentMetadataHandle();
		meta.getCollections().add(collection);
		documentPermissionsParser.parsePermissions(permissions, meta.getPermissions());
		return meta;
	}
}
