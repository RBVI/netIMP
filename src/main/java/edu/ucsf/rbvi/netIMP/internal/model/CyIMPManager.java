package edu.ucsf.rbvi.netIMP.internal.model;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
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
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;

import edu.ucsf.rbvi.netIMP.internal.ui.ModelPanel;
import edu.ucsf.rbvi.netIMP.internal.ui.CyViewUtils;

public class CyIMPManager {

	final CyServiceRegistrar serviceRegistrar;
	CyNetworkFactory networkFactory = null;
	CyNetworkManager networkManager = null;
	SynchronousTaskManager taskManager = null;
	CyEventHelper eventHelper = null;
	File modelFile = null;
	List<IMPModel> impModels;
	Map<IMPRestraint, List<Double>> restraintMap;
	ModelPanel resultsPanel = null;
	int maxModelCount = 0;
	CyNetwork unionNetwork = null;

	// A couple of useful services that we want to cache
	CyApplicationManager cyAppManager = null;
	CyNetworkViewManager viewManager = null;

	public CyIMPManager(CyServiceRegistrar registrar) {
		this.serviceRegistrar = registrar;
		impModels = new ArrayList<>();
		restraintMap = new HashMap<>();
	}

	public void reset() {
		impModels = new ArrayList<>();
		restraintMap = new HashMap<>();
		unionNetwork = null;
		resultsPanel = null;
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
					for (IMPRestraint restraint: impModel.getRestraints()) {
						if (!restraintMap.containsKey(restraint)) {
							restraintMap.put(restraint, new ArrayList<Double>());
						}
						restraintMap.get(restraint).add(restraint.getScore());
					}
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
		// unionNetwork = networkFactory.createNetwork(SavePolicy.DO_NOT_SAVE);
		unionNetwork = networkFactory.createNetwork();

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
				CyNode unionNode = CyModelUtils.getNodeByName(unionNetwork, name);
				if (unionNode == null) {
					unionNode = CyModelUtils.copyNode(modelNetwork, modelNode, unionNetwork);
				}
				nodeMap.put(modelNode, unionNode);
			}

			// Copy over all of the edges
			for (CyEdge modelEdge: modelNetwork.getEdgeList()) {
				String name = modelNetwork.getRow(modelEdge).get(CyNetwork.NAME, String.class);
				CyEdge unionEdge = CyModelUtils.getEdgeByName(unionNetwork, name);
				if (unionEdge != null) {
					int count = unionNetwork.getRow(unionEdge).get("ModelCount", Integer.class)+1;
					if ((count) > maxModelCount)
						maxModelCount = count;
					unionNetwork.getRow(unionEdge).set("ModelCount", Integer.valueOf(count));
					continue;
				}

				if (!nodeMap.containsKey(modelEdge.getSource()))
					continue;
				if (!nodeMap.containsKey(modelEdge.getTarget()))
					continue;

				CyNode source = nodeMap.get(modelEdge.getSource());
				CyNode target = nodeMap.get(modelEdge.getTarget());

				CyEdge newEdge = 
							CyModelUtils.copyEdge(modelNetwork, modelEdge, source, target, false, unionNetwork);
				CyModelUtils.createColumnIfNecessary(unionNetwork.getDefaultEdgeTable(), 
				                                     "ModelCount", Integer.class, null);
				unionNetwork.getRow(newEdge).set("ModelCount", Integer.valueOf(1));
				edgeCount++;
			}

			// Now add the restraint edges for this model
			model.addRestraints(unionNetwork);
		}

		System.out.println("Created: "+edgeCount+" edges");
		return unionNetwork;
	}

	public void updateUnionNetwork(double cutoff) {
		if (unionNetwork == null) return;

		Map<CyEdge, Integer> edgeMap = new HashMap<>();
		for (IMPModel model: getIMPModels(cutoff)) {
			CyNetwork modelNetwork = model.getNetwork();
			for (CyEdge modelEdge: modelNetwork.getEdgeList()) {
				String name = modelNetwork.getRow(modelEdge).get(CyNetwork.NAME, String.class);
				CyEdge unionEdge = CyModelUtils.getEdgeByName(unionNetwork, name);
				if (!edgeMap.containsKey(unionEdge))
					edgeMap.put(unionEdge,1);
				else
					edgeMap.put(unionEdge,edgeMap.get(unionEdge)+1);
			}
		}
		for (CyEdge edge: unionNetwork.getEdgeList()) {
			if (edgeMap.containsKey(edge)) {
				unionNetwork.getRow(edge).set("ModelCount", edgeMap.get(edge));
				CyViewUtils.showEdge(this, unionNetwork, edge, true);
			} else {
				CyViewUtils.showEdge(this, unionNetwork, edge, false);
			}
		}
	}

	public void deleteUnionNetwork() {
		if (networkManager == null)
			networkManager = (CyNetworkManager)getService(CyNetworkManager.class);

		if (unionNetwork != null)
			networkManager.destroyNetwork(unionNetwork);
	}

	public CyNetwork getUnionNetwork() {
		return unionNetwork;
	}

	public CyNetworkView getUnionNetworkView() {
		if (unionNetwork == null) return null;

		for (CyNetworkView view: getNetworkViews(unionNetwork)) {
			return view;
		}
		return null;
	}

	public List<IMPModel> getIMPModels() {
		return impModels;
	}

	public List<IMPModel> getIMPModels(double cutoff) {
		List<IMPModel> models = new ArrayList<>();
		for (IMPModel model: impModels) {
			if (model.getScore("score") >= cutoff)
				models.add(model);
		}
		return models;
	}

	public List<Double> getModelScores() {
		List<Double> scores = new ArrayList<>();
		for (IMPModel model: impModels) {
			scores.add(model.getScore("score"));
		}
		return scores;
	}

	public int getModelCount() {
		return impModels.size();
	}

	public int getMaxModelCount() {
		return maxModelCount;
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

	public CyEventHelper getEventHelper() {
		getTaskServices();
		return eventHelper;
	}

	public SynchronousTaskManager getSynchronousTaskManager() {
		getTaskServices();
		return taskManager;
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

	public Collection<CyNetworkView> getNetworkViews(CyNetwork network) {
		if (viewManager == null)
			viewManager = getService(CyNetworkViewManager.class);
		return viewManager.getNetworkViews(network);
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
