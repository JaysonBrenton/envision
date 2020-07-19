package com.marklogic.envision.entities;

import com.marklogic.hub.EntityManager;
import com.marklogic.hub.entity.HubEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class EntityManagerService {

//	private static final String UI_LAYOUT_FILE = "entities.layout.json";
//	private static final String PLUGINS_DIR = "plugins";
//	private static final String ENTITIES_DIR = "entities";
//	public static final String ENTITY_FILE_EXTENSION = ".entity.json";

	@Autowired
	EntityManager em;

//	@Autowired
//	HubConfigImpl hubConfig;
//
//	@Autowired
//	Scaffolding scaffolding;
//
//	@Autowired
//	private LegacyFlowManagerService legacyFlowManagerService;
//
//	@Autowired
//	private DataHubService dataHubService;
//
//	@Autowired
//	private MappingManagerService mappingManagerService;

//	public List<EntityModel> getLegacyEntities() throws IOException {
//		List<EntityModel> entities = new ArrayList<>();
//		Path entitiesDir = hubConfig.getHubProject().getLegacyHubEntitiesDir();
//		List<String> entityNames = FileUtil.listDirectFolders(entitiesDir.toFile());
//		for (String entityName : entityNames) {
//			EntityModel entityModel = new EntityModel();
//			InfoType infoType = new InfoType();
//			infoType.setTitle(entityName);
//			entityModel.setInfo(infoType);
//			entityModel.inputFlows = legacyFlowManagerService.getFlows(entityName, FlowType.INPUT);
//			entityModel.harmonizeFlows = legacyFlowManagerService.getFlows(entityName, FlowType.HARMONIZE);
//			entities.add(entityModel);
//		}
//		return entities;
//	}

	public List<HubEntity> getEntities() throws IOException {
		return em.getEntities();
	}

//	public EntityModel createEntity(EntityModel newEntity) throws IOException {
//		scaffolding.createEntity(newEntity.getName());
//		return getEntity(newEntity.getName());
//	}
//
//	public EntityModel saveEntity(EntityModel entity) throws IOException {
//		JsonNode node = entity.toJson();
//		String fullpath = entity.getFilename();
//
//		HubEntity hubEntity = HubEntity.fromJson(fullpath, node);
//
//
//		if (fullpath == null) {
//			String entityFileName = entity.getName() + ENTITY_FILE_EXTENSION;
//
//			File entityFile = hubConfig.getHubEntitiesDir().resolve(entityFileName).toFile();
//			String canonicalFileName = Paths.get(entityFile.getCanonicalPath()).getFileName().toString();
//
//			if (entityFile.exists() && !entityFileName.equals(canonicalFileName)) {
//				// Possibly a case insensitive file-system
//				throw new DataHubProjectException("An entity with this name already exists.");
//			}
//
//			em.saveEntity(hubEntity, false);
//		}
//		else {
//			HubEntity renamedEntity = em.saveEntity(hubEntity, true);
//			entity.setFilename(renamedEntity.getFilename());
//		}
//		return entity;
//	}
//
//	public void deleteEntity(String entity) throws IOException {
//		String entityFileName = entity + ENTITY_FILE_EXTENSION;
//		File entitiesFile = hubConfig.getHubEntitiesDir().resolve(entityFileName).toFile();
//		if (entitiesFile.exists()) {
//			em.deleteEntity(entity);
//			dataHubService.deleteDocument("/entities/" + entityFileName, DatabaseKind.STAGING);
//			dataHubService.deleteDocument("/entities/" + entityFileName, DatabaseKind.FINAL);
//		}
//	}
//
//	public void deploySearchOptions() {
//		em.deployQueryOptions();
//	}
//
//	public void saveDbIndexes() {
//		em.saveDbIndexes();
//	}
//
//	public void savePii() {
//		em.savePii();
//	}
//
//	public void saveAllUiData(List<EntityModel> entities) throws IOException {
//		ObjectNode uiData;
//		JsonNode json = getUiRawData();
//		if (json != null) {
//			uiData = (ObjectNode) json;
//		}
//		else {
//			uiData = JsonNodeFactory.instance.objectNode();
//		}
//
//		Path dir = hubConfig.getUserConfigDir();
//		if (!dir.toFile().exists()) {
//			dir.toFile().mkdirs();
//		}
//		File file = Paths.get(dir.toString(), UI_LAYOUT_FILE).toFile();
//
//		ObjectNode cUiData = uiData;
//		entities.forEach((entity) -> {
//			JsonNode node = entity.getHubUi().toJson();
//			cUiData.set(entity.getInfo().getTitle(), node);
//		});
//
//		ObjectMapper objectMapper = new ObjectMapper();
//		FileOutputStream fileOutputStream = new FileOutputStream(file);
//		objectMapper.writerWithDefaultPrettyPrinter().writeValue(fileOutputStream, uiData);
//		fileOutputStream.flush();
//		fileOutputStream.close();
//	}
//
//	public void saveEntityUiData(EntityModel entity) throws IOException {
//
//		ObjectNode uiData;
//		JsonNode json = getUiRawData();
//		if (json != null) {
//			uiData = (ObjectNode) json;
//		}
//		else {
//			uiData = JsonNodeFactory.instance.objectNode();
//		}
//
//		Path dir = hubConfig.getUserConfigDir();
//		if (!dir.toFile().exists()) {
//			dir.toFile().mkdirs();
//		}
//		File file = Paths.get(dir.toString(), UI_LAYOUT_FILE).toFile();
//
//		JsonNode node = entity.getHubUi().toJson();
//		uiData.set(entity.getInfo().getTitle(), node);
//
//		ObjectMapper objectMapper = new ObjectMapper();
//		FileOutputStream fileOutputStream = new FileOutputStream(file);
//		objectMapper.writerWithDefaultPrettyPrinter().writeValue(fileOutputStream, uiData);
//		fileOutputStream.flush();
//		fileOutputStream.close();
//	}

	public HubEntity getEntity(String entityName) throws IOException {
		return getEntity(entityName, Boolean.FALSE);
	}

	public HubEntity getEntity(String entityName, Boolean extendSubEntities) throws IOException {
		HubEntity hubEntity = em.getEntityFromProject(entityName, extendSubEntities);
		return hubEntity;
//		if (hubEntity == null) {
//			throw new DataHubProjectException("Entity not found in project: " + entityName);
//		}
//		EntityModel em  = EntityModel.fromJson(hubEntity.getFilename(),hubEntity.toJson());
//		return em;
	}

//	public FlowModel getFlow(String entityName, FlowType flowType, String flowName) throws IOException {
//		EntityModel entity = getEntity(entityName);
//
//		List<FlowModel> flows;
//		if (flowType.equals(FlowType.INPUT)) {
//			flows = entity.inputFlows;
//		}
//		else {
//			flows = entity.harmonizeFlows;
//		}
//		for (FlowModel flow : flows) {
//			if (flow.flowName != null && flow.flowName.equals(flowName)) {
//				return flow;
//			}
//		}
//		throw new DataHubProjectException("Flow not found: " + entityName + " / " + flowName);
//	}
//
//	public FlowModel createFlow(String entityName, FlowType flowType, FlowModel newFlow) throws IOException {
//		newFlow.entityName = entityName;
//		if(newFlow.mappingName != null) {
//			try {
//				String mappingName = mappingManagerService.getMapping(newFlow.mappingName, false).getVersionedName();
//				newFlow.mappingName = mappingName;
//			} catch (DataHubProjectException e) {
//				throw new DataHubProjectException("Mapping not found in project: " + newFlow.mappingName);
//			}
//		}
//		scaffolding.createLegacyFlow(entityName, newFlow.flowName, flowType, newFlow.codeFormat, newFlow.dataFormat, newFlow.useEsModel, newFlow.mappingName);
//		return getFlow(entityName, flowType, newFlow.flowName);
//	}
//
//	public void deleteFlow(String entityName, String flowName, FlowType flowType) throws IOException {
//		Path flowDir = scaffolding.getLegacyFlowDir(entityName, flowName, flowType);
//		FileUtils.deleteDirectory(flowDir.toFile());
//	}
//
//	public JsonNode validatePlugin(
//		HubConfig config,
//		String entityName,
//		String flowName,
//		PluginModel plugin
//	) throws IOException {
//		JsonNode result = null;
//		String type;
//		if (plugin.pluginType.endsWith("sjs")) {
//			type = "javascript";
//		}
//		else {
//			type = "xquery";
//		}
//		EntitiesValidator validator = EntitiesValidator.create(config.newStagingClient());
//		return validator.validate(entityName, flowName, plugin.fileContents.replaceAll("\\.(sjs|xqy)", ""), type, plugin.fileContents);
//	}
//
//	public void saveFlowPlugin(
//		PluginModel plugin
//	) throws IOException {
//		String pluginContent = plugin.fileContents;
//		Files.write(Paths.get(plugin.pluginPath), pluginContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
//
//	}
//	private JsonNode getUiRawData() {
//		JsonNode json = null;
//		Path dir = hubConfig.getUserConfigDir();
//		File file = Paths.get(dir.toString(), UI_LAYOUT_FILE).toFile();
//		if (file.exists()) {
//			try {
//				ObjectMapper objectMapper = new ObjectMapper();
//				json = objectMapper.readTree(file);
//			}
//			catch(IOException e) {
//				e.printStackTrace();
//			}
//		}
//		return json;
//	}
//
//	private Map<String, HubUIData> getUiData() throws IOException {
//		HashMap<String, HubUIData> uiDataList = new HashMap<>();
//
//		JsonNode json = getUiRawData();
//		if (json != null) {
//			Iterator<String> fieldItr = json.fieldNames();
//			while (fieldItr.hasNext()) {
//				String key = fieldItr.next();
//				JsonNode uiNode = json.get(key);
//				if (uiNode != null) {
//					HubUIData uiData = HubUIData.fromJson(uiNode);
//					uiDataList.put(key, uiData);
//				}
//			}
//		}
//
//		return uiDataList;
//	}
}
