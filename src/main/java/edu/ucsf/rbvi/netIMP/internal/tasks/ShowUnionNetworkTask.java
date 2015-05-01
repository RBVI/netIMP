package edu.ucsf.rbvi.netIMP.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.netIMP.internal.model.CyIMPManager;
import edu.ucsf.rbvi.netIMP.internal.ui.CyViewUtils;

public class ShowUnionNetworkTask extends AbstractTask {
	final CyIMPManager impManager;

	/**
	 * Constructor for loading CDD Domain from the CDD website.
	 * @param net CyNetwork to load the domain.
	 * @param manager The CDD Domain manager
	 */
	public ShowUnionNetworkTask(CyIMPManager manager) {
		super();
		this.impManager = manager;
	}

	@ProvidesTitle
	public String getTitle() { return "Show Union Network"; }
	
	@Override
	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle("Show Union Network");

		if (impManager.getModelCount() == 0) {
			monitor.setStatusMessage("No models!");
			return;
		}

		// Create the network
		CyNetwork network = impManager.buildUnionNetwork();
		CyNetworkManager networkManager = impManager.getService(CyNetworkManager.class);
		network.getRow(network).set(CyNetwork.NAME, "Union of IMP Models");
		networkManager.addNetwork(network);

		// Create the view
		CyNetworkViewManager viewManager = impManager.getService(CyNetworkViewManager.class);
		CyNetworkViewFactory viewFactory = impManager.getService(CyNetworkViewFactory.class);
		CyNetworkView view = viewFactory.createNetworkView(network);
		VisualMappingManager visualManager = impManager.getService(VisualMappingManager.class);
		VisualStyle style = CyViewUtils.createVisualStyle(impManager);
		visualManager.setVisualStyle(style, view);
		style.apply(view);
		viewManager.addNetworkView(view);

		// Apply a layout
	}
}
