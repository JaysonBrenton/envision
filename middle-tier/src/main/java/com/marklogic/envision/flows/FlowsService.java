package com.marklogic.envision.flows;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.dataServices.Flows;
import com.marklogic.envision.deploy.DeployService;
import com.marklogic.envision.pojo.StatusMessage;
import com.marklogic.hub.FlowManager;
import com.marklogic.hub.MappingManager;
import com.marklogic.hub.flow.Flow;
import com.marklogic.hub.flow.impl.FlowRunnerImpl;
import com.marklogic.hub.mapping.Mapping;
import com.marklogic.hub.step.impl.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FlowsService {
	final private ObjectMapper mapper = new ObjectMapper();
	final private FlowManager flowManager;
	final private MappingManager mappingManager;
	final private DeployService deployService;
	final private FlowRunnerImpl flowRunner;
	final private SimpMessagingTemplate template;

	@Autowired
	FlowsService(FlowManager flowManager, MappingManager mappingManager, DeployService deployService, FlowRunnerImpl flowRunner, SimpMessagingTemplate template) {
		this.flowManager = flowManager;
		this.mappingManager = mappingManager;
		this.deployService = deployService;
		this.flowRunner = flowRunner;
		this.template = template;
		this.flowRunner.onStatusChanged((jobId, step, jobStatus, percentComplete, successfulEvents, failedEvents, message) -> {
			StatusMessage msg = StatusMessage.newStatus(jobId)
				.withMessage(message + ":: " + jobStatus)
				.withPercentComplete(percentComplete);
			this.template.convertAndSend("/topic/status", msg);
		});
	}

	public JsonNode getFlow(DatabaseClient client, String flowName) {
		List<String> flowNames = new ArrayList<>();
		flowNames.add(flowName);
		JsonNode flows = Flows.on(client).getFlows(mapper.valueToTree(flowNames));
		JsonNode flow =  flows.get(0);
		if (flow != null) {
			return flow;
		}

		// create the flow if it doesn't exist
		Flow newFlow = flowManager.createFlow(flowName);
		flowManager.saveFlow(newFlow);
		deployService.loadFlow(newFlow);
		return mapper.valueToTree(newFlow);
	}

	public void createFlow(JsonNode flowJson) {
		Flow flow = flowManager.createFlowFromJSON(flowJson);
		flowManager.saveFlow(flow);
		deployService.loadFlow(flow);
	}

	public String getMapping(String mapName) {
		return mappingManager.getMappingAsJSON(mapName, -1, true);
	}

	public void addMapping(JsonNode mappingJson) throws IOException {
		Mapping mapping = mappingManager.createMappingFromJSON(mappingJson);
		mappingManager.saveMapping(mapping);
		deployService.loadMapping(mapping);
	}

	public JsonNode getFlows(DatabaseClient client) {
		List<String> flowNames = flowManager.getFlows().stream().map(flow -> flow.getName()).collect(Collectors.toList());
		return Flows.on(client).getFlows(mapper.valueToTree(flowNames));
	}

	public void createStep(String flowName, JsonNode stepJson) {
		String stepType = stepJson.get("stepDefinitionType").asText();
		String stepName = stepJson.get("name").asText();
		String entityName = stepJson.get("options").get("targetEntity").asText();
		Flow flow = flowManager.getFlow(flowName);
		Step step = Step.deserialize(stepJson);
		Map<String, Step> steps = flow.getSteps();

		final String[] existingIndex = {String.valueOf(steps.size() + 1)};
		// see if the step already exists. happens when updating
		steps.forEach((idx, step1) -> {
			if (step1.getName().equals(step.getName())) {
				existingIndex[0] = idx;
			}
		});
		steps.put(existingIndex[0], step);
		flowManager.saveFlow(flow);

		if (stepType.equals("MAPPING")) {
			String mappingName = flowName + "-" + stepName;
			try {
				mappingManager.getMapping(mappingName);
			}
			catch(Exception e) {
				Mapping mapping = mappingManager.createMapping(flowName + "-" + stepName, entityName);
				mappingManager.saveMapping(mapping);
				deployService.loadMapping(mapping);
			}
		}
		deployService.loadFlow(flow);
	}

	public void deleteStep(String flowName, String stepName) {
		Flow flow = flowManager.getFlow(flowName);
		String stepKey = getStepKeyByName(flow, stepName);
		if (stepKey != null) {
			Step step = flow.getStep(stepKey);

			String mappingName = step.getMappingName();
			if (mappingName != null) {
				int mappingVersion = getMappingVersion(step);

				// delete from disk
				mappingManager.deleteMapping(mappingName);

				// delete from MarkLogic
				deployService.deleteMapping(mappingName, mappingVersion);
			}
			Map<String, Step> steps = flow.getSteps();
			steps.remove(stepKey);

			Map<String, Step> newStepMap = new LinkedHashMap<>();
			Step[] stepValues = steps.values().toArray(new Step[0]);
			for (int i = 0; i < stepValues.length; i++) {
				newStepMap.put(String.valueOf(i + 1), stepValues[i]);
			}
			flow.setSteps(newStepMap);
			// save to disk
			flowManager.saveFlow(flow);
			// save to MarkLogic
			deployService.loadFlow(flow);
		}
	}

	public void runSteps(String flowName, JsonNode steps) {
		try {
			ObjectReader reader = mapper.readerFor(new TypeReference<List<String>>() {});
			List<String> stepsList = reader.readValue(steps);
			flowRunner.runFlow(flowName, stepsList);
		}
		catch(Exception e) {
			throw new RuntimeException("invalid steps");
		}
	}

	private String getStepKeyByName(Flow flow, String stepName) {
		Map<String, Step> steps = flow.getSteps();
		Iterator<Map.Entry<String, Step> > it = steps.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, Step> entry = it.next();
			if (entry.getValue().getName().equals(stepName)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public int getMappingVersion(Step step) {
		Map<String, Object> options = step.getOptions();
		if (options != null) {
			Object obj = options.get("mapping");
			if (obj != null && obj instanceof ObjectNode) {
				ObjectNode mapping = (ObjectNode)obj;
				if (mapping.has("version")) {
					return mapping.get("name").asInt();
				}
			}
		}

		return 1;
	}
}
