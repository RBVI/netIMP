package edu.ucsf.rbvi.netIMP.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.netIMP.internal.model.CyIMPManager;
import edu.ucsf.rbvi.netIMP.internal.model.IMPModel;

public class ModelPanel extends JPanel implements CytoPanelComponent {
  private static final long serialVersionUID = 1L;
	private final CyIMPManager cyIMPManager;
	private CyNetwork network;
	private CyNetworkView networkView;
	private ModelBrowser modelBrowser;
	private CyEventHelper eventHelper;

  // table size parameters
	private static final int graphPicSize = 80;
	private static final int defaultRowHeight = graphPicSize + 8;

	public ModelPanel(CyIMPManager cManager, CyNetwork net) {
		cyIMPManager = cManager;
		if (net == null) {
			this.network = cyIMPManager.getCurrentNetwork();
			this.networkView = cyIMPManager.getCurrentNetworkView();
		} else {
			this.network = net;
			// TODO: get the list of views from the view manager
			this.networkView = cyIMPManager.getCurrentNetworkView();
		}

		setLayout(new BorderLayout());
		modelBrowser = new ModelBrowser(this, cyIMPManager);
		add(modelBrowser, BorderLayout.CENTER);
		this.setSize(this.getMinimumSize());
		eventHelper = (CyEventHelper)cyIMPManager.getService(CyEventHelper.class);

	}

	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.EAST;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	public NetworkImageRenderer getImageRenderer() {
		return modelBrowser.getImageRenderer();
	}

	@Override
	public String getTitle() {
		return "IMP Results";
	}

	public void updateTable() {
		if (modelBrowser != null)
			modelBrowser.updateTable();
	}

	public void updateData() {
		if (modelBrowser != null)
			modelBrowser.updateData();
	}

	public class ModelBrowser extends JPanel implements ListSelectionListener {
		private ModelBrowserTableModel tableModel;
		private	NetworkImageRenderer netImageRenderer;
		private	ModelRenderer modelRenderer;
		private final JTable table;
		private final JScrollPane tableScrollPane;
		private JLabel tableLabel;
		private JSlider slider = null;
		private final ModelPanel modelPanel;
		private final CyIMPManager cyIMPManager;
		private double scoreCutOff = 0;
		private List<Double> scores = null;
		protected final DecimalFormat formatter;
		int minScore = 0;
		int maxScore = 0;

		public ModelBrowser(ModelPanel component, CyIMPManager cyIMPManager) {
			super();

			modelPanel = component;
			this.cyIMPManager = cyIMPManager;
			formatter = new DecimalFormat("0.00");

			setLayout(new BorderLayout());

/*
			// TODO
			scores = new ArrayList<Double>(cyIMPManager.getModelScores());
			Collections.sort(scores);
			minScore = (int)(scores.get(0)*100.0);
			maxScore = (int)(scores.get(scores.size()-1)*100.0);
			scoreCutOff = scores.get(0);

			// Only create the slider panel if we have more than one
			// stress value
			if (minScore < maxScore) {
				JPanel sliderPanel = new JPanel(new BorderLayout());
				JLabel sliderLabel = new JLabel("Score Cutoff");
				sliderPanel.add(sliderLabel, BorderLayout.WEST);

				slider = new JSlider(minScore, maxScore, (int)(score*100.0));
				slider.setLabelTable(generateLabels(stresses));
				slider.setPaintLabels(true);
				slider.addChangeListener(this);
				sliderPanel.add(slider, BorderLayout.CENTER);
			
			// Put a border around our sliderPanel
				Border outer = BorderFactory.createEtchedBorder();
				Border inner = BorderFactory.createEmptyBorder(10,10,10,10);
				Border compound = BorderFactory.createCompoundBorder(outer, inner);
				sliderPanel.setBorder(compound);
				add(sliderPanel, BorderLayout.NORTH);
			}
*/

			// Create a new JPanel for the table
			JPanel tablePanel = new JPanel(new BorderLayout());
			// String stressLabel = formatter.format(scoreCutOff);
			// tableLabel = new JLabel("IMP Results");
			// tablePanel.add(tableLabel, BorderLayout.NORTH);

			tableModel = new ModelBrowserTableModel(cyIMPManager, cyIMPManager.getCurrentNetworkView(), 
			                                        modelPanel, scoreCutOff);
			table = new JTable(tableModel);

			table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			table.setAutoCreateRowSorter(true);
			table.setAutoCreateColumnsFromModel(true);
			table.setIntercellSpacing(new Dimension(0, 4)); // gives a little vertical room between clusters
			table.setFocusable(false); // removes an outline that appears when the user clicks on the images
			table.setRowHeight(defaultRowHeight);

			TableRowSorter rowSorter = new TableRowSorter(tableModel);
			rowSorter.setComparator(0, new NetworkSorter());
			rowSorter.setSortable(1, false);
			table.setRowSorter(rowSorter);

			// Make the headers centered
			JTableHeader tableHeader = table.getTableHeader();
			TableCellRenderer tRenderer = tableHeader.getDefaultRenderer();
			((DefaultTableCellRenderer)tRenderer).setHorizontalAlignment(SwingConstants.CENTER);

			netImageRenderer = new NetworkImageRenderer(cyIMPManager, graphPicSize);
			table.setDefaultRenderer(CyNetwork.class, netImageRenderer);

			// Used for both column 1 and column 2!
			modelRenderer = new ModelRenderer(cyIMPManager);
			table.setDefaultRenderer(IMPModel.class, modelRenderer);

			// Ask to be notified of selection changes.
			ListSelectionModel rowSM = table.getSelectionModel();
			rowSM.addListSelectionListener(this);

			tableScrollPane = new JScrollPane(table);
			//System.out.println("CBP: after creating JScrollPane");
			tableScrollPane.getViewport().setBackground(Color.WHITE);
			tablePanel.add(tableScrollPane, BorderLayout.CENTER);
			add(tablePanel);
		}

		public NetworkImageRenderer getImageRenderer() {
			return netImageRenderer;
		}

		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting()) return;
			int[] rows = table.getSelectedRows(); // Get all of the selected rows
			if (network == null)
				network = cyIMPManager.getCurrentNetwork();

			if (networkView == null)
				networkView = cyIMPManager.getCurrentNetworkView();

			// Clear the current selection
			for (CyNode node: network.getNodeList())
				network.getRow(node).set(CyNetwork.SELECTED, false);

			// Clear the current selection
			for (CyEdge edge: network.getEdgeList())
				network.getRow(edge).set(CyNetwork.SELECTED, false);

			// Clear the pathway colors

			for (int viewRow: rows) {
				int modelRow = table.convertRowIndexToModel(viewRow);

				// Select the appropriate nodes in the network
				for (CyNode node: tableModel.selectNodesFromRow(network, modelRow)) {
					network.getRow(node).set(CyNetwork.SELECTED, true);
				}

				// Select our edges
				for (CyEdge edge: tableModel.selectEdgesFromRow(network, modelRow)) {
					network.getRow(edge).set(CyNetwork.SELECTED, true);
				}

				// Style our edges
			}

			networkView.updateView();
		}

/*
		public void stateChanged(ChangeEvent e) {
			if (e.getSource() != slider) return;
			int cutoff = slider.getValue();
			double dCutOff = ((double)cutoff)/100.0;
			for (int i = 0; i < scores.size(); i++) {
				double v = scores.get(i);
				if (dCutoff > v) continue;
				if (dCutoff == v) break;
				double vLow = scores.get(i-1);
				if ((dCutoff - vLow) < (v - dCutoff)) {
					dCutoff = vLow;
					break;
				} else {
					dCutoff = v;
					break;
				}
			}
			scoreCutOff = dCutoff;

			String cutoffLabel = formatter.format(scoreCutOff);
			tableLabel.setText("<html><h3>&nbsp;&nbsp;Model Results with scores better than "+
			                   cutoffLabel+"</h3></html>");

			// Create the new tableModel
			// tableModel.updateData(scoreCutOff);

			tableModel = new ModelBrowserTableModel(cyIMPManager, cyIMPManager.getCurrentNetworkView(), 
			                                          modelPanel, scoreCutOff);
			table.setModel(tableModel);
			updateTable();
		}
*/

		public JTable getTable() { return table; }

		public void updateTable() {
			tableModel.fireTableDataChanged();
			tableModel.fireTableStructureChanged();
			tableScrollPane.getViewport().revalidate();
			table.doLayout();
			((TableRowSorter)table.getRowSorter()).sort();
		}

		public void updateData() {
			tableModel.updateData(scoreCutOff);
		}

		public Dictionary<Integer, JComponent> generateLabels(List<Double> models) {
			Dictionary<Integer, JComponent> table = new Hashtable<>();
			for (Double score: scores) {
				int value = (int)(score.doubleValue()*100.0+0.5);
				if (value%5 != 0) continue;
				String label = formatter.format(score);
				JLabel jLabel = new JLabel(label); // May have to use a text formatter
				jLabel.setFont(new Font("SansSerif", Font.BOLD, 8));
				table.put(value, jLabel);
			}
			return table;
		}
	}

	private class NetworkSorter implements Comparator<CyNetwork> {
		public NetworkSorter() { }

		public int compare(CyNetwork n1, CyNetwork n2) {
			if (n1 == null && n2 == null) return 0;
			if (n1 == null && n2 != null) return -1;
			if (n2 == null && n1 != null) return 1;

			if(n1.getNodeCount() < n2.getNodeCount()) return 1;
			if(n1.getNodeCount() > n2.getNodeCount()) return -1;
			return 0;
		}
	}
}
