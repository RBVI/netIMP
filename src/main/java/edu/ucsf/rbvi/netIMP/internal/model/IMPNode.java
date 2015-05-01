package edu.ucsf.rbvi.netIMP.internal.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.cytoscape.model.CyNode;

public class IMPNode {
	String nodeType;
	String nodeLabel;
	Map<String, Object> attributes;
	CyNode node;

	public IMPNode (JSONObject model) {
		attributes = new HashMap<>();
		for (Object o: model.keySet()) {
			String key = (String)o;
			attributes.put(key, model.get(key));
		}
		nodeType = (String)attributes.get("node type");
		nodeLabel = (String)attributes.get("label");
		node = null;
	}

	public String getNodeType() {
		return nodeType;
	}

	public String getNodeLabel() {
		return nodeLabel;
	}

	public Object getAttribute(String key) {
		if (attributes.containsKey(key))
			return attributes.get(key);
		return null;
	}

	public Set<String> getAttributeKeys() {
		return attributes.keySet();
	}

	public void setNode(CyNode node) { this.node = node; }
	public CyNode getNode() { return node; }
}
