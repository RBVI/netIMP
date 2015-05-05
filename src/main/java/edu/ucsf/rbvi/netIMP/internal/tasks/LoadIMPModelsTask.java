package edu.ucsf.rbvi.netIMP.internal.tasks;

import java.io.File;

import org.cytoscape.model.CyNetworkManager;
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

public class LoadIMPModelsTask extends AbstractTask {
	final CyIMPManager impManager;

	@Tunable(description="File containing IMP models", params="input=true")
	public File impFile;

	/**
	 * Constructor for loading CDD Domain from the CDD website.
	 * @param net CyNetwork to load the domain.
	 * @param manager The CDD Domain manager
	 */
	public LoadIMPModelsTask(CyIMPManager manager) {
		super();
		this.impManager = manager;
	}

	@ProvidesTitle
	public String getTitle() { return "Load IMP Models"; }
	
	@Override
	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle("Load IMP Models");

		if (impManager.getModelCount() > 0) {
			monitor.setStatusMessage("Clearing previous IMP model load");
		}

		// Load the models
		int models = impManager.loadIMPModels(impFile);
		monitor.setStatusMessage("Loaded "+models+" IMP models");

		SynchronousTaskManager tm = impManager.getSynchronousTaskManager();

		{
			// Show the Union Network
			TaskIterator ti = new TaskIterator(new ShowUnionNetworkTask(impManager));
			tm.execute(ti);
			impManager.getEventHelper().flushPayloadEvents();
		}

		{
			// Show the results panel
			TaskIterator ti = new TaskIterator(new ShowIMPResultsPanelTask(impManager, true));
			tm.execute(ti);
			impManager.getResultsPanel().updateData();

			// Need to re-apply the visual style for some reason
			VisualMappingManager visualManager = impManager.getService(VisualMappingManager.class);
			VisualStyle style = visualManager.getVisualStyle(impManager.getCurrentNetworkView());
			style.apply(impManager.getCurrentNetworkView());
		}

	}
}
