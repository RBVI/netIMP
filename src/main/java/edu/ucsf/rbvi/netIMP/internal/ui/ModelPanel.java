package edu.ucsf.rbvi.netIMP.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
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
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.RenderingEngineManager;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;
import org.cytoscape.view.presentation.property.values.LineType;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
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

	private Map<IMPModel, RestraintListener> listenerMap;

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
		listenerMap = new HashMap<>();
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

	public ItemListener getItemListener(IMPModel model) {
		if (!listenerMap.containsKey(model))
			listenerMap.put(model, new RestraintListener(this, model));
		return listenerMap.get(model);
	}

	public class ModelBrowser extends JPanel implements ListSelectionListener, ChangeListener {
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

			// Add a slider for the score cutoff
			scores = cyIMPManager.getModelScores();
			Collections.sort(scores);
			// minScore = (int)(scores.get(0)*100.0);
			// maxScore = (int)(scores.get(scores.size()-1)*100.0);
			
			minScore = (int)(scores.get(0)-0.5);
			maxScore = (int)(scores.get(scores.size()-1)+0.5);
			scoreCutOff = (double)minScore;

			// Only create the slider panel if we have more than one
			// score
			if (minScore < maxScore) {
				JPanel sliderPanel = new JPanel(new BorderLayout());
				JLabel sliderLabel = new JLabel("Score Cutoff");
				sliderPanel.add(sliderLabel, BorderLayout.WEST);

				slider = new JSlider(minScore*100, maxScore*100, (int)(scoreCutOff)*100);
				slider.setLabelTable(generateLabels(minScore, maxScore));
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

			// Create a new JPanel for the table
			JPanel tablePanel = new JPanel(new BorderLayout());
			String cutoffLabel = formatter.format(scoreCutOff);
			tableLabel = new JLabel("<html><h3>&nbsp;&nbsp;Model Results with scores better than "+
			                   cutoffLabel+"</h3></html>");
			tablePanel.add(tableLabel, BorderLayout.NORTH);

			tableModel = new ModelBrowserTableModel(cyIMPManager, cyIMPManager.getCurrentNetworkView(), 
			                                        modelPanel, scoreCutOff);
			table = new JTable(tableModel);

			table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			table.setAutoCreateColumnsFromModel(true);
			table.setIntercellSpacing(new Dimension(0, 8)); // gives a little vertical room between clusters
			table.setFocusable(true); // removes an outline that appears when the user clicks on the images
			table.setRowHeight(defaultRowHeight);

			// Make the headers centered
			JTableHeader tableHeader = table.getTableHeader();
			TableCellRenderer tRenderer = tableHeader.getDefaultRenderer();
			((DefaultTableCellRenderer)tRenderer).setHorizontalAlignment(SwingConstants.CENTER);

			netImageRenderer = new NetworkImageRenderer(cyIMPManager, graphPicSize);
			table.setDefaultRenderer(CyNetwork.class, netImageRenderer);

			modelRenderer = new ModelRenderer(cyIMPManager);
			table.setDefaultRenderer(IMPModel.class, modelRenderer);
	
			table.setDefaultEditor(IMPModel.class, modelRenderer);

			// Ask to be notified of selection changes.
			ListSelectionModel rowSM = table.getSelectionModel();
			rowSM.addListSelectionListener(this);

			TableRowSorter<ModelBrowserTableModel> rowSorter = new TableRowSorter<>(tableModel);
			table.setRowSorter(rowSorter);
			rowSorter.setSortable(0, false);
			rowSorter.setSortable(1, true);
			table.setAutoCreateRowSorter(true);

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
			// int[] rows = table.getSelectedRows(); // Get all of the selected rows
			if (network == null)
				network = cyIMPManager.getCurrentNetwork();
			if (network == null) return;

			if (networkView == null)
				networkView = cyIMPManager.getCurrentNetworkView();
			if (networkView == null) return;

			// Clear the pathway colors
			VisualStyle style = cyIMPManager.getService(VisualMappingManager.class).getVisualStyle(networkView);
			networkView.clearVisualProperties();
			style.apply(networkView);

			for (int viewRow = 0; viewRow < tableModel.getRowCount(); viewRow++) {
				int modelRow = table.convertRowIndexToModel(viewRow);
				// System.out.println("ViewRow = "+viewRow+" modelRow = "+modelRow);
				if (!table.isRowSelected(viewRow)) {
					// Hide the restraint edges
					continue;
				}

				int modelColumn = table.convertColumnIndexToModel(1);
				Color modelColor = ((IMPModel)table.getValueAt(modelRow,modelColumn)).getColor();

				// Style the appropriate nodes in the network
				for (CyNode node: tableModel.selectNodesFromRow(network, modelRow)) {
					View<CyNode> nodeView = networkView.getNodeView(node);
					nodeView.setVisualProperty(NODE_BORDER_PAINT, modelColor);
					nodeView.setVisualProperty(NODE_BORDER_WIDTH, 10.0);
				}

				// Style our edges
				for (CyEdge edge: tableModel.selectEdgesFromRow(network, modelRow)) {
					View<CyEdge> edgeView = networkView.getEdgeView(edge);
					edgeView.setVisualProperty(EDGE_STROKE_UNSELECTED_PAINT, modelColor);
				}
			}

			networkView.updateView();
		}

		public void stateChanged(ChangeEvent e) {
			if (e.getSource() != slider || slider.getValueIsAdjusting()) return;

			int cutoff = slider.getValue();
			scoreCutOff = ((double)cutoff)/100.0;

			String cutoffLabel = formatter.format(scoreCutOff);
			tableLabel.setText("<html><h3>&nbsp;&nbsp;Model Results with scores better than "+
			                   cutoffLabel+"</h3></html>");

			// Create the new tableModel
			// tableModel.updateData(scoreCutOff);

			tableModel = new ModelBrowserTableModel(cyIMPManager, cyIMPManager.getCurrentNetworkView(), 
			                                        modelPanel, scoreCutOff);
			table.setModel(tableModel);

			// If we're showing the union network, update it
			cyIMPManager.updateUnionNetwork(scoreCutOff);
			updateTable();
		}

		public JTable getTable() { return table; }

		public void updateTable() {
			tableModel.fireTableDataChanged();
			tableModel.fireTableStructureChanged();
			tableScrollPane.getViewport().revalidate();
			table.doLayout();
			// ((TableRowSorter)table.getRowSorter()).sort();
		}

		public void updateData() {
			tableModel.updateData(scoreCutOff);
		}

		public Dictionary<Integer, JComponent> generateLabels(int minScore, int maxScore) {
			Dictionary<Integer, JComponent> table = new Hashtable<>();
			int range = (maxScore-minScore)*100;
			int increment = range/10;
			for (int i = 0; i < 11; i++) {
				int value = ((minScore*100)+(i*increment));
				String label = formatter.format(((double)value)/100.0);
				JLabel jLabel = new JLabel(label); // May have to use a text formatter
				jLabel.setFont(new Font("SansSerif", Font.BOLD, 8));
				table.put(value, jLabel);
			}
			return table;
		}
	}

	private class RestraintListener implements ItemListener {
		IMPModel model;
		ModelPanel panel;
		VisualLexicon lexicon = null;

		public RestraintListener(ModelPanel panel, IMPModel model) {
			this.model = model;
			this.panel = panel;
			if (networkView != null) {
				getVisualLexicon();
				/*
				String rendererID = networkView.getRendererId();
				RenderingEngineManager rem = cyIMPManager.getService(RenderingEngineManager.class);
				for (RenderingEngine<?> re: rem.getRenderingEngines(networkView)) {
					if (re.getRendererId().equals(rendererID)) {
						lexicon = re.getVisualLexicon();
						break;
					}
				}
				*/
			}
		}

		private void getVisualLexicon() {
			RenderingEngineManager rem = cyIMPManager.getService(RenderingEngineManager.class);
			for (RenderingEngine<?> re: rem.getRenderingEngines(networkView)) {
				lexicon = re.getVisualLexicon();
				if (lexicon == null)
					System.out.println("Visual Lexicon for "+networkView+" and "+re+" is null!");
				return;
			}
		}

		public void itemStateChanged(ItemEvent e) {
			JCheckBox cb = (JCheckBox)e.getItemSelectable();
			String restraint = cb.getActionCommand();
			Color modelColor = model.getColor();
			if (lexicon == null) getVisualLexicon();
			VisualProperty<?> ltVp = lexicon.lookup(CyEdge.class, "EDGE_LINE_TYPE");
			VisualProperty<?> targetArrowPaint = lexicon.lookup(CyEdge.class, "EDGE_TARGET_ARROW_UNSELECTED_PAINT");
			// System.out.println("Add restraint edges for "+restraint+" on model "+model.getModelNumber());
			// Unchecked?
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				// Hide edges
				List<CyEdge> edges = model.getRestraintEdges(restraint);
				for (CyEdge edge: edges)
					CyViewUtils.showEdge(cyIMPManager, network, edge, false);
			} else {
				// Show edges
				List<CyEdge> edges = model.getRestraintEdges(restraint);
				for (CyEdge edge: edges) {
					boolean isSatisfied = model.isSatisfied(edge);
					CyViewUtils.showEdge(cyIMPManager, network, edge, true);
					// Style it
					if (networkView != null) {
						View<CyEdge> edgeView = networkView.getEdgeView(edge);
						if (isSatisfied) {
							edgeView.setLockedValue(EDGE_STROKE_UNSELECTED_PAINT, modelColor);
							edgeView.setLockedValue(targetArrowPaint, modelColor);
							edgeView.setLockedValue(EDGE_WIDTH, 3.0);
							CyViewUtils.setLineType(edgeView, ltVp, "Contiguous Arrow");
						} else {
							edgeView.setLockedValue(EDGE_STROKE_UNSELECTED_PAINT, Color.RED);
							edgeView.setLockedValue(targetArrowPaint, Color.RED);
							edgeView.setLockedValue(EDGE_WIDTH, 3.0);
							CyViewUtils.setLineType(edgeView, ltVp, "Vertical Slash");
						}
					}
				}
			}
		}
	}
}
