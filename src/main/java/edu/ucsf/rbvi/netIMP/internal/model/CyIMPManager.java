package edu.ucsf.rbvi.netIMP.internal.model;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.SavePolicy;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;

import edu.ucsf.rbvi.netIMP.internal.ui.ModelPanel;

public class CyIMPManager {

	final CyServiceRegistrar serviceRegistrar;
	CyNetworkFactory networkFactory = null;
	CyNetworkManager networkManager = null;
	SynchronousTaskManager taskManager = null;
	CyEventHelper eventHelper = null;
	File modelFile = null;
	List<IMPModel> impModels;
	ModelPanel resultsPanel = null;

	// A couple of useful services that we want to cache
	CyApplicationManager cyAppManager = null;

	public CyIMPManager(CyServiceRegistrar registrar) {
		this.serviceRegistrar = registrar;
		impModels = new ArrayList<>();
	}

	public int loadIMPModels(File modelFile) throws IOException, FileNotFoundException {
		this.modelFile = modelFile;

		if (networkFactory == null)
			networkFactory = getService(CyNetworkFactory.class);

		FileReader reader = new FileReader(modelFile);

		try {
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);

			if (!jsonObject.containsKey("models"))
				throw new RuntimeException("No models in file");

			JSONArray models = (JSONArray) jsonObject.get("models");
			int modelNumber = 1;
			for (Object model: models) {
				if (model instanceof JSONObject) {
					IMPModel impModel = new IMPModel(this, (JSONObject)model);
					impModel.setModelNumber(modelNumber);
					impModels.add(impModel);
					modelNumber++;
				}
			}

		}
		catch (ParseException pe) {
			System.out.println("Unable to parse "+modelFile+": "+pe);
		}
		return impModels.size();
	}

	public CyNetwork buildUnionNetwork() {
		CyNetworkFactory networkFactory = (CyNetworkFactory)getService(CyNetworkFactory.class);
		CyNetwork union = networkFactory.createNetwork(SavePolicy.DO_NOT_SAVE);

		// For each model, get the network and those nodes and edges to
		// our union network.  If an edge already exists, increment the edge
		// count.
		int edgeCount = 0;
		for (IMPModel model: impModels) {
			Map<CyNode, CyNode> nodeMap = new HashMap<>();
			CyNetwork modelNetwork = model.getNetwork();

			// Copy over all of the nodes
			for (CyNode modelNode: modelNetwork.getNodeList()) {
				String name = modelNetwork.getRow(modelNode).get(CyNetwork.NAME, String.class);
				CyNode unionNode = CyModelUtils.getNodeByName(union, name);
				if (unionNode == null) {
					unionNode = CyModelUtils.copyNode(modelNetwork, modelNode, union);
				}
				nodeMap.put(modelNode, unionNode);
			}

			// Copy over all of the edges
			for (CyEdge modelEdge: modelNetwork.getEdgeList()) {
				String name = modelNetwork.getRow(modelEdge).get(CyNetwork.NAME, String.class);
				CyEdge unionEdge = CyModelUtils.getEdgeByName(union, name);
				if (unionEdge != null) {
					int count = union.getRow(unionEdge).get("ModelCount", Integer.class);
					union.getRow(unionEdge).set("ModelCount", Integer.valueOf(count+1));
					continue;
				}

				if (!nodeMap.containsKey(modelEdge.getSource()))
					continue;
				if (!nodeMap.containsKey(modelEdge.getTarget()))
					continue;

				CyNode source = nodeMap.get(modelEdge.getSource());
				CyNode target = nodeMap.get(modelEdge.getTarget());

				CyEdge newEdge = CyModelUtils.copyEdge(modelNetwork, modelEdge, source, target, false, union);
				CyModelUtils.createColumnIfNecessary(union.getDefaultEdgeTable(), "ModelCount", Integer.class, null);
				union.getRow(newEdge).set("ModelCount", Integer.valueOf(1));
				edgeCount++;
			}
		}
		System.out.println("Created: "+edgeCount+" edges");
		return union;
	}

	public List<IMPModel> getIMPModels() {
		return impModels;
	}

	public List<IMPModel> getIMPModels(double cutoff) {
		return impModels;
	}

	public int getModelCount() {
		return impModels.size();
	}

	public void syncColors() {
	}

	public CyNetwork getCurrentNetwork() {
		if (cyAppManager == null) {
			cyAppManager = getService(CyApplicationManager.class);
		}
		return cyAppManager.getCurrentNetwork();
	}

	public CyNetworkView getCurrentNetworkView() {
		if (cyAppManager == null) {
			cyAppManager = getService(CyApplicationManager.class);
		}
		return cyAppManager.getCurrentNetworkView();
	}

	public <S> S getService(Class<S> serviceClass) {
		return serviceRegistrar.getService(serviceClass);
	}

	public <S> S getService(Class<S> serviceClass, String filter) {
		return serviceRegistrar.getService(serviceClass, filter);
	}

	public void registerService(Object service, Class<?> serviceClass, Properties props) {
		serviceRegistrar.registerService(service, serviceClass, props);
	}

	public void unregisterService(Object service, Class<?> serviceClass) {
		serviceRegistrar.unregisterService(service, serviceClass);
	}

	private void getTaskServices() {
		if (taskManager == null) {
			taskManager = getService(SynchronousTaskManager.class);
		}
		if (eventHelper == null) {
			eventHelper = getService(CyEventHelper.class);
		}
	}

	public ModelPanel getResultsPanel() {
		return resultsPanel;
	}

	public void setResultsPanel(ModelPanel panel) {
		resultsPanel = panel;
	}

	private String chimeraColor(Color color) {
		double r = (double)color.getRed()/255.0;
		double g = (double)color.getGreen()/255.0;
		double b = (double)color.getBlue()/255.0;
		double a = (double)color.getAlpha()/255.0;
		return ""+r+","+g+","+b+","+a;
	}

}
