package edu.ucsf.rbvi.netIMP.internal.model;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.cytoscape.model.CyEdge;

public class IMPEdge {
	IMPNode source;
	IMPNode target;
	CyEdge edge;

	public IMPEdge (List<IMPNode> nodeList, JSONObject model) {
		String sourceLabel = (String)model.get("source");
		String targetLabel = (String)model.get("target");
		parse(nodeList, sourceLabel, targetLabel);
		edge = null;
	}

	public void parse(List<IMPNode> nodeList, String sourceLabel, String targetLabel) {
		for (IMPNode node: nodeList) {
			String label = node.getNodeLabel();
			if (label.equals(sourceLabel))
				source = node;
			else if (label.equals(targetLabel))
				target = node;
		}
	}

	public IMPNode getSourceNode() {
		return source;
	}

	public IMPNode getTargetNode() {
		return target;
	}

	public void setEdge(CyEdge edge) { this.edge = edge; }
	public CyEdge getEdge() { return edge; }
}
