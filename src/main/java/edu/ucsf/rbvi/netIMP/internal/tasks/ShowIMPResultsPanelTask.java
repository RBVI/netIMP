package edu.ucsf.rbvi.netIMP.internal.tasks;

import java.util.Properties;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.netIMP.internal.model.CyIMPManager;
import edu.ucsf.rbvi.netIMP.internal.ui.ModelPanel;

public class ShowIMPResultsPanelTask extends AbstractTask {
	final CyIMPManager impManager;
	final boolean showHide;

	/*
	@Tunable(description="File containing PDB data")
	public File pdbFile;
	*/

	/**
	 * Constructor for loading CDD Domain from the CDD website.
	 * @param net CyNetwork to load the domain.
	 * @param manager The CDD Domain manager
	 */
	public ShowIMPResultsPanelTask(CyIMPManager manager, boolean showHide) {
		super();
		this.impManager = manager;
		this.showHide = showHide;
	}

	@ProvidesTitle
	public String getTitle() { return "Show IMP Results Panel"; }
	
	@Override
	public void run(TaskMonitor monitor) throws Exception {
		CySwingApplication swingApplication = impManager.getService(CySwingApplication.class);
		CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.EAST);
		if (showHide) {
			monitor.setTitle("Showing IMP results panel");
			ModelPanel modelPanel = new ModelPanel(impManager, null);
			impManager.registerService(modelPanel, CytoPanelComponent.class, new Properties());
			impManager.setResultsPanel(modelPanel);
			if (cytoPanel.getState() == CytoPanelState.HIDE)
				cytoPanel.setState(CytoPanelState.DOCK);
		} else {
			monitor.setTitle("Hiding IMP results panel");
			ModelPanel modelPanel = impManager.getResultsPanel();
			if (modelPanel != null) {
				impManager.unregisterService(modelPanel, CytoPanelComponent.class);
				impManager.setResultsPanel(null);
			}
		}
	}
}
