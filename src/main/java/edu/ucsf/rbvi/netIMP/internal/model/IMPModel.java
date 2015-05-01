package edu.ucsf.rbvi.netIMP.internal.model;

import java.awt.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.SavePolicy;

public class IMPModel {
	final CyIMPManager manager;
	List<IMPNode> impNodes;
	List<IMPEdge> impEdges;
	List<IMPRestraint> impRestraints;
	Map<String, Double> impScores;

	Map <CyNode, IMPNode> nodeMap;
	Map <CyEdge, IMPEdge> edgeMap;
	Map <CyEdge, IMPRestraint> restraintMap;

	Color color = Color.GRAY;
	CyNetwork network = null;
	CyNetworkFactory networkFactory = null;
	int modelNumber;

	public IMPModel (CyIMPManager manager, JSONObject model) {
		this.manager = manager;
		impNodes = new ArrayList<>();
		impEdges = new ArrayList<>();
		impRestraints = new ArrayList<>();
		impScores = new HashMap<>();

		nodeMap = new HashMap<>();
		edgeMap = new HashMap<>();
		restraintMap = new HashMap<>();
		modelNumber = 0;

		// The JSON input should have retraints, nodes, edges, and scores

		// Get the nodes first -- we'll use those when we handle both
		// edges and restraints
		JSONArray nodes = (JSONArray)model.get("nodes");
		for (Object node: nodes) {
			impNodes.add(new IMPNode((JSONObject) node));
		}

		JSONArray edges = (JSONArray)model.get("edges");
		for (Object edge: edges) {
			impEdges.add(new IMPEdge(impNodes, (JSONObject) edge));
		}

		JSONArray restraints = (JSONArray)model.get("restraints");
		for (Object restraint: restraints) {
			impRestraints.add(new IMPRestraint(impNodes, (JSONObject) restraint));
		}

		JSONObject inputScores = (JSONObject)model.get("scores");
		for (Object key: inputScores.keySet()) {
			if (key instanceof String) {
				String strKey = (String) key;
				Object value = inputScores.get(strKey);
				if (value instanceof Double)
					impScores.put(strKey, (Double)value);
				else if (value instanceof String) {
					try {
						Double v = Double.valueOf((String)inputScores.get(strKey));
						impScores.put(strKey, v);
					} catch (NumberFormatException nfe) {}
				}
			}
		}
	}

	public Map <String, Double> getScores() { return impScores; }
	public List <IMPNode> getNodes() { return impNodes; }
	public List <IMPEdge> getEdges() { return impEdges; }
	public List <IMPRestraint> getRestraints() { return impRestraints; }

	public Color getColor() { return color; }
	public void setColor(Color color) { this.color = color; }

	public int getModelNumber() { return modelNumber; }
	public void setModelNumber(int modelNumber) { this.modelNumber = modelNumber; }

	public CyNetwork getNetwork() {
		if (network != null)
			return network;

		getServices();

		// 1) Create a new network
		network = networkFactory.createNetwork(SavePolicy.DO_NOT_SAVE);

		// 2) Create our nodes
		for (IMPNode iNode: impNodes) {
			CyNode node = network.addNode();
			iNode.setNode(node);

			for (String attr: iNode.getAttributeKeys()) {
				Object attrValue = iNode.getAttribute(attr);
				if (attr.equals("label")) {
					CyModelUtils.setName(network, node, (String)attrValue);
				}

				Class type = getObjectType(attrValue);
				CyModelUtils.createColumnIfNecessary(network.getDefaultNodeTable(), attr, type, null);
				network.getRow(node).set(attr, attrValue);
			}
			nodeMap.put(node, iNode);
		}
		// 3) Create our edges
		for (IMPEdge iEdge: impEdges) {
			CyNode sourceNode = iEdge.getSourceNode().getNode();
			CyNode targetNode = iEdge.getTargetNode().getNode();
			CyEdge edge = network.addEdge(sourceNode, targetNode, false);
			edgeMap.put(edge, iEdge);
			String edgeName = network.getRow(sourceNode).get(CyNetwork.NAME, String.class) + " (model) "+
			                  network.getRow(targetNode).get(CyNetwork.NAME, String.class);
			CyModelUtils.setName(network, edge, edgeName);
		}
		// 4) Add our restraint edges
		for (IMPRestraint iRestraint: impRestraints) {
		}
		return network;
	}

	private Class getObjectType(Object attrValue) {
		if (attrValue instanceof String) 
			return String.class;
		if (attrValue instanceof Integer) 
			return Integer.class;
		if (attrValue instanceof Long) 
			return Long.class;
		if (attrValue instanceof Double) 
			return Double.class;
		if (attrValue instanceof Boolean) 
			return Boolean.class;
		return String.class;
	}

	private void getServices() {
		if (networkFactory == null) {
			networkFactory = (CyNetworkFactory)manager.getService(CyNetworkFactory.class);
		}
	}
}
