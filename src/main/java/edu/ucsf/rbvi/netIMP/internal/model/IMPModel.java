package edu.ucsf.rbvi.netIMP.internal.model;

import java.awt.Color;

import java.util.ArrayList;
import java.util.Comparator;
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

public class IMPModel implements Comparable<IMPModel> {
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
	public Double getScore(String score) { 
		if (impScores.containsKey(score))
			return impScores.get(score);
		return null; 
	}

	public List <IMPNode> getNodes() { return impNodes; }
	public List <IMPEdge> getEdges() { return impEdges; }
	public List <IMPRestraint> getRestraints() { return impRestraints; }
	public List <CyEdge> getRestraintEdges() { 
		return new ArrayList<>(restraintMap.keySet()); 
	}

	public IMPRestraint getRestraint(CyEdge edge) {
		if (restraintMap.containsKey(edge))
			return restraintMap.get(edge);
		return null;
	}

	public List <CyEdge> getRestraintEdges(String type) {
		List <CyEdge> edges = new ArrayList<>();
		for (CyEdge edge: restraintMap.keySet()) {
			if (type.equals(restraintMap.get(edge).getType()))
				edges.add(edge);
		}
		return edges;
	}

	public double getMaxRestraintScore(String type) {
		double max = -1.0;
		for (IMPRestraint restraint: impRestraints) {
			if (type.equals(restraint.getType())) {
				if (restraint.getScore() > max)
					max = restraint.getScore();
			}
		}
		return max;
	}

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
		CyModelUtils.createColumnIfNecessary(network.getDefaultEdgeTable(), 
		                                     "isRestraint", Boolean.class, null);
		for (IMPEdge iEdge: impEdges) {
			CyEdge edge = addEdge(network, iEdge, "model");
			edgeMap.put(edge, iEdge);
			network.getRow(edge).set("isRestraint", false);
		}

		return network;
	}

	public List<CyEdge> addRestraints(CyNetwork net) {
		List<CyEdge> restraintEdges = new ArrayList<>();

		// Create the restraint columns
		CyModelUtils.createColumnIfNecessary(net.getDefaultEdgeTable(), 
		                                     "restraint type", String.class, null);
		CyModelUtils.createColumnIfNecessary(net.getDefaultEdgeTable(), 
		                                     "score", Double.class, null);
		CyModelUtils.createColumnIfNecessary(net.getDefaultEdgeTable(), 
		                                     "directed", Boolean.class, null);

		// ) Add our restraint edges
		for (IMPRestraint iRestraint: impRestraints) {
			boolean directed = iRestraint.isDirected();
			double score = iRestraint.getScore();
			String type = iRestraint.getType();
			for (IMPEdge iEdge: iRestraint.getEdges()) {
				CyEdge edge = addEdge(net, iEdge, "restraint");
				restraintEdges.add(edge);
				restraintMap.put(edge, iRestraint);
				net.getRow(edge).set("restraint type", type);
				net.getRow(edge).set("isRestraint", true);
				net.getRow(edge).set("directed", directed);
				net.getRow(edge).set("score", score);
			}
		}
		return restraintEdges;
	}

	public boolean isSatisfied(CyEdge edge) {
		double maxScore = getMaxRestraintScore("chemical transformation");
		IMPRestraint restraint = restraintMap.get(edge);
		if (restraint.getType().equals("chemical transformation") &&
		    restraint.getScore() < maxScore)
			return false;
		if (restraint.getScore() < 0.0)
			return false;

		return true;
	}

	public void removeRestraints(CyNetwork network) {
		for (CyEdge edge: network.getEdgeList()) {
			if (network.getRow(edge).get("isRestraint", Boolean.class)) {
			}
		}
	}

	private CyEdge addEdge(CyNetwork net, IMPEdge iEdge, String type) {
		CyNode sourceNode = iEdge.getSourceNode().getNode();
		CyNode targetNode = iEdge.getTargetNode().getNode();
		// See if we need to match names
		if (!net.containsNode(sourceNode)) {
			// Yup
			sourceNode = CyModelUtils.getNodeByName(net, iEdge.getSourceNode().getNodeLabel());
			targetNode = CyModelUtils.getNodeByName(net, iEdge.getTargetNode().getNodeLabel());
		}
		CyEdge edge = net.addEdge(sourceNode, targetNode, false);
		String edgeName = net.getRow(sourceNode).get(CyNetwork.NAME, String.class) + " ("+
		                  type+") "+
		                  net.getRow(targetNode).get(CyNetwork.NAME, String.class);
		CyModelUtils.setName(net, edge, edgeName);
		return edge;
	}

	@Override
	public int compareTo(IMPModel m2) {
		if (m2 == null) return -1;

		Double d1 = getScore("score");
		Double d2 = m2.getScore("score");
		return Double.compare(d2, d1);
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
