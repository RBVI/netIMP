package edu.ucsf.rbvi.netIMP.internal.model;

import java.util.List;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.SavePolicy;
import org.cytoscape.model.subnetwork.CyRootNetwork;

public class CyModelUtils {
	public static void createColumnIfNecessary(CyTable table, String attr, Class type, Class listType) {
		if (table.getColumn(attr) != null) return;

		if (type.equals(List.class)) {
			table.createListColumn(attr, listType, false);
		} else {
			table.createColumn(attr, type, false);
		}
	}

	public static Class getObjectType(Object attrValue) {
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
		if (attrValue instanceof List) 
			return List.class;
		return String.class;
	}

	public static Class getListObjectType(Object attrValue) {
		if (!(attrValue instanceof List))
			return null;
		List l = (List)attrValue;
		if (l.size() == 0) 
			return null;
		return getObjectType(l.get(0));
	}

	public static boolean nodeExists(CyNetwork network, String name) {
		for (CyNode node: network.getNodeList()) {
			if (name.equals(network.getRow(node).get(CyNetwork.NAME, String.class)))
				return true;
		}
		return false;
	}

	public static CyNode getNodeByName(CyNetwork network, String name) {
		for (CyNode node: network.getNodeList()) {
			if (name.equals(network.getRow(node).get(CyNetwork.NAME, String.class)))
				return node;
		}
		return null;
	}

	public static boolean edgeExists(CyNetwork network, String name) {
		for (CyEdge edge: network.getEdgeList()) {
			if (name.equals(network.getRow(edge).get(CyNetwork.NAME, String.class)))
				return true;
		}
		return false;
	}

	public static CyEdge getEdgeByName(CyNetwork network, String name) {
		for (CyEdge edge: network.getEdgeList()) {
			if (name.equals(network.getRow(edge).get(CyNetwork.NAME, String.class)))
				return edge;
		}
		return null;
	}

	public static CyEdge copyEdge(CyNetwork fromNetwork, CyEdge edge, 
	                            CyNode source, CyNode target, boolean directed, CyNetwork toNetwork) {
		CyEdge toEdge = toNetwork.addEdge(source, target, directed);
		for (String column: CyTableUtil.getColumnNames(fromNetwork.getDefaultEdgeTable())) {
			Class listType = null;
			Class type = fromNetwork.getDefaultEdgeTable().getColumn(column).getType();
			if (type == List.class)
				listType = fromNetwork.getDefaultEdgeTable().getColumn(column).getListElementType();
			createColumnIfNecessary(toNetwork.getDefaultEdgeTable(), column, type, listType);
			if (type == List.class)
				toNetwork.getRow(toEdge).set(column, fromNetwork.getRow(edge).getList(column, listType));
			else
				toNetwork.getRow(toEdge).set(column, fromNetwork.getRow(edge).get(column, type));
		}
		return toEdge;
	}

	public static CyNode copyNode(CyNetwork fromNetwork, CyNode node, CyNetwork toNetwork) {
		CyNode toNode = toNetwork.addNode();
		for (String column: CyTableUtil.getColumnNames(fromNetwork.getDefaultNodeTable())) {
			Class listType = null;
			Class type = fromNetwork.getDefaultNodeTable().getColumn(column).getType();
			if (type == List.class)
				listType = fromNetwork.getDefaultNodeTable().getColumn(column).getListElementType();
			createColumnIfNecessary(toNetwork.getDefaultNodeTable(), column, type, listType);
			if (type == List.class)
				toNetwork.getRow(toNode).set(column, fromNetwork.getRow(node).getList(column, listType));
			else
				toNetwork.getRow(toNode).set(column, fromNetwork.getRow(node).get(column, type));
		}
		return toNode;
	}

	public static void setName(CyNetwork network, CyIdentifiable id, String name) {
		network.getRow(id).set(CyNetwork.NAME, name);
		network.getRow(id).set(CyRootNetwork.SHARED_NAME, name);
	}
}
