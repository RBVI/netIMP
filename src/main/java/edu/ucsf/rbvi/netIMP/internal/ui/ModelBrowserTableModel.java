package edu.ucsf.rbvi.netIMP.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_PAINT;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_VISIBLE;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_WIDTH;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_FILL_COLOR;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.netIMP.internal.model.CyIMPManager;
import edu.ucsf.rbvi.netIMP.internal.model.CyModelUtils;
import edu.ucsf.rbvi.netIMP.internal.model.IMPModel;

public class ModelBrowserTableModel extends DefaultTableModel {
	private final CyIMPManager cyIMPManager;
	private CyNetwork network = null;
	private CyNetworkView networkView;
	private final ModelPanel networkBrowser;
	private Object[][] data;
	private List<CyNetwork> modelNetworks;
	private List<Color> componentColors;
	// private final String[] columnNames = { "Model", "Scores", "Show" };
	private final String[] columnNames = { "Model", "Scores" };

	public ModelBrowserTableModel(CyIMPManager cyIMPManager, 
	                              CyNetworkView networkView, 
	                              ModelPanel networkBrowser, 
																double cutoff) {
		super(1,2);
		this.cyIMPManager = cyIMPManager;
		this.networkView = networkView;
		if (networkView != null)
			this.network = networkView.getModel();
		this.networkBrowser = networkBrowser;

		updateData(cutoff);
	}

	public void updateData(double cutoff) {
		// First, reset all of the RIN nodes to grey
		if (networkView != null) {
			for (View<CyNode> nv: networkView.getNodeViews())
				nv.setVisualProperty(NODE_FILL_COLOR, Color.GRAY);
		}

		// Now handle all of our component networks
		List<IMPModel> impModels = cyIMPManager.getIMPModels(cutoff);
		Collections.sort(impModels);

		componentColors = generateColors(impModels.size());

		this.data = new Object[impModels.size()][columnNames.length];

		for (int i = 0; i < impModels.size(); i++) {
			IMPModel model = impModels.get(i);
			// Default grey
			Color color = new Color(192,192,192,128);
			CyNetwork modelNetwork = model.getNetwork();
			if (modelNetwork.getNodeCount() > 2)
				model.setColor(componentColors.get(i));
			else
				model.setColor(color);

			setValueAt(modelNetwork, i, 0);
			setValueAt(model, i, 1);
		}
		setDataVector(this.data, columnNames);
		fireTableDataChanged();
		if (networkView != null)
			networkView.updateView();
		networkBrowser.updateTable();
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	@Override
	public int getColumnCount() {
		if (columnNames == null) return 3;
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		if (data == null) return 1;
		return data.length;
	}

	@Override
	public Object getValueAt(int row, int col) {
		return data[row][col];
	}

	@Override
	public void setValueAt(Object object, int row, int col) {
		data[row][col] = object;
		fireTableCellUpdated(row, col);
	}

	@Override
	public Class<?> getColumnClass(int c) {
		if (c == 0) 
			return CyNetwork.class;
		return IMPModel.class;
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		if (column == 1) return true;
		return false;
	}

	public List<CyNode> selectNodesFromRow(CyNetwork network, int modelRow) {
		CyNetwork net = (CyNetwork)getValueAt(modelRow, 0);
		IMPModel impModel = (IMPModel)getValueAt(modelRow, 1);

		List<CyNode> nodeList = new ArrayList<>();
		for (CyNode modelNode: net.getNodeList()) {
			String name = net.getRow(modelNode).get(CyNetwork.NAME, String.class);
			CyNode unionNode = CyModelUtils.getNodeByName(network, name);
			nodeList.add(unionNode);
		}
		return nodeList;
	}

	public List<CyEdge> selectEdgesFromRow(CyNetwork network, int modelRow) {
		CyNetwork net = (CyNetwork)getValueAt(modelRow, 0);
		IMPModel impModel = (IMPModel)getValueAt(modelRow, 1);

		List<CyEdge> edgeList = new ArrayList<>();
		for (CyEdge modelEdge: net.getEdgeList()) {
			String name = net.getRow(modelEdge).get(CyNetwork.NAME, String.class);
			CyEdge unionEdge = CyModelUtils.getEdgeByName(network, name);
			edgeList.add(unionEdge);
		}
		return edgeList;
	}

/*
	public List<CyEdge> selectEdgesFromRow(int modelRow) {
		CyNetwork net = (CyNetwork)getValueAt(modelRow, 0);
		List<CyEdge> edgeList = new ArrayList<>();

		for (CyEdge cEdge: net.getEdgeList()) {
			CyNode source = cEdge.getSource();
			CyNode target = cEdge.getTarget();
			Integer sourceResId = new Integer(net.getRow(source).get("ResidueNumber", Integer.class));
			Integer targetResId = new Integer(net.getRow(target).get("ResidueNumber", Integer.class));
			int count = net.getRow(cEdge).get("PathwayCount", Integer.class);
			if (residueMap.containsKey(sourceResId) && residueMap.containsKey(targetResId)) {
				CyNode sourceNode = residueMap.get(sourceResId);
				CyNode targetNode = residueMap.get(targetResId);
				edgeList.add(addEdgeIfNecessary(sourceNode, targetNode, count));
			}
		}
		return edgeList;
	}
*/

	public void styleEdges(List<CyEdge> edges, int modelRow) {
		Color color = (Color) getValueAt(modelRow, 1);

		int max = -1;

		// First pass -- calculate min/max
		for (CyEdge edge: edges) {
			int width = network.getRow(edge).get("PathwayCount", Integer.class);
			if (width > max)
				max = width;
		}

		double factor = 1.0;
		if (max > 50)
			factor = 5.0;
		else if (max > 40)
			factor = 4.0;
		else if (max > 30)
			factor = 3.0;
		else if (max > 20)
			factor = 2.0;

		for (CyEdge edge: edges) {
			int width = network.getRow(edge).get("PathwayCount", Integer.class);
			View<CyEdge> edgeView = networkView.getEdgeView(edge);
			if (edgeView == null) continue;

			// We need to use a locked property because RINalyzer has a discrete mapping
			// for edge color
			edgeView.setLockedValue(EDGE_PAINT, color);
			edgeView.setLockedValue(EDGE_WIDTH, (double)width/factor);
		}
	}

	private CyEdge addEdgeIfNecessary(CyNode source, CyNode target, int count) {
		CyTable edgeTable = network.getDefaultEdgeTable();
		if (edgeTable.getColumn("PathwayCount") == null)
			edgeTable.createColumn("PathwayCount", Integer.class, false);

		if (edgeTable.getColumn(CyEdge.INTERACTION) == null)
			edgeTable.createColumn(CyEdge.INTERACTION, String.class, false);

		CyEdge pathwayEdge = null;
		if (network.containsEdge(source, target)) {
			for (CyEdge edge: network.getConnectingEdgeList(source, target, CyEdge.Type.DIRECTED)) {
				if (network.getRow(edge).get(CyEdge.INTERACTION, String.class).equals("ContactPathway")) {
					pathwayEdge = edge;
					networkView.getEdgeView(pathwayEdge).clearValueLock(EDGE_VISIBLE);
					break;
				}
			}
		}
		if (pathwayEdge == null) {
			pathwayEdge = network.addEdge(source, target, true);
			network.getRow(pathwayEdge).set(CyEdge.INTERACTION, "ContactPathway");
		}
		network.getRow(pathwayEdge).set("PathwayCount", count);
		return pathwayEdge;
	}

	private List<Color> generateColors(int number) {
		int[][] colorArray = new int[][] {
						{0,0,153,255}, // Dark Blue
						{153,0,0,255}, // Dark Red
						{0,153,0,255}, // Dark Green
						{255,153,0,255}, // Orange
						{0,255,0,255}, // Green
						{0,255,255,255}, // Cyan
						{255,0,255,255}, // Magenta
						{0,153,153,255}, // Dark Cyan
						{153,0,153,255}, // Dark Magenta
						{0,153,255,255}, // Light blue
						{255,102,153,255}, // Light red
						{0,255,153,255}, // Light green
			};
		List<Color> colors = new ArrayList<>();
		for (int i = 0; i < number; i++) {
			if (i < colorArray.length)
				colors.add(new Color(colorArray[i][0], colorArray[i][1], 
				                     colorArray[i][2], colorArray[i][3]));
			else
				colors.add(new Color(192,192,192,128));
		}
		return colors;
	}

}
