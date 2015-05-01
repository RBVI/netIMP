package edu.ucsf.rbvi.netIMP.internal.model;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.cytoscape.model.CyEdge;

public class IMPRestraint {
	List<IMPEdge> restraintEdges;
	List<CyEdge> restraintCyEdges;
	boolean directed = false;
	String restraintType;
	double score;


	public IMPRestraint (List<IMPNode> nodeList, JSONObject model) {
		restraintEdges = new ArrayList<>();

		JSONArray edges = (JSONArray)model.get("edges");
		for (Object edge: edges) {
			restraintEdges.add(new IMPEdge(nodeList, (JSONObject) edge));
		}

		directed = ((Boolean)model.get("directed")).booleanValue();
		score = ((Double)model.get("score")).doubleValue();
		restraintType = (String)model.get("restraint type");

		restraintCyEdges = new ArrayList<>();
	}

	public List<IMPEdge> getEdges() { return restraintEdges; }
	public double getScore() { return score; }
	public boolean isDirected() { return directed; }
	public String getType() { return restraintType; }

	public List<CyEdge> getCyEdges() { return restraintCyEdges; }
	public void setCyEdges(List<CyEdge> cyEdges) { restraintCyEdges = cyEdges; }
	public void addCyEdge(CyEdge cyEdge) { restraintCyEdges.add(cyEdge); }
}
