package edu.ucsf.rbvi.netIMP.internal.tasks;

import java.io.File;

import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.netIMP.internal.model.CyIMPManager;
import edu.ucsf.rbvi.netIMP.internal.model.CyModelUtils;

public class ResetTask extends AbstractTask {
	final CyIMPManager impManager;

	/**
	 * Constructor for loading CDD Domain from the CDD website.
	 * @param net CyNetwork to load the domain.
	 * @param manager The CDD Domain manager
	 */
	public ResetTask(CyIMPManager manager) {
		super();
		this.impManager = manager;
	}

	@ProvidesTitle
	public String getTitle() { return "Reset"; }
	
	@Override
	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle("Resetting");

		monitor.setStatusMessage("Hiding results panel");
		SynchronousTaskManager tm = 
					(SynchronousTaskManager)impManager.getService(SynchronousTaskManager.class);
		TaskIterator ti = new TaskIterator(new ShowIMPResultsPanelTask(impManager, false));
		tm.execute(ti);

		// Remove the network
		monitor.setStatusMessage("Deleting network");
		impManager.deleteUnionNetwork();

		if (impManager.getModelCount() > 0) {
			monitor.setStatusMessage("Clearing previous IMP model load");
			impManager.reset();
		}
	}
}
