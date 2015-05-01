package edu.ucsf.rbvi.netIMP.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.netIMP.internal.model.CyIMPManager;

public class ShowIMPResultsPanelTaskFactory extends AbstractTaskFactory {
	final CyIMPManager manager;
	final boolean showHide;

	public ShowIMPResultsPanelTaskFactory(CyIMPManager manager, boolean show) {
		super();
		this.manager = manager;
		this.showHide = show;
	}

	public TaskIterator createTaskIterator() {
		// TODO Auto-generated method stub
		return new TaskIterator(new ShowIMPResultsPanelTask(manager, showHide));
	}

	public boolean isReady() {
		if (manager.getModelCount() == 0) return false;
		if (showHide && manager.getResultsPanel() == null)
			return true;
		else if (!showHide && manager.getResultsPanel() != null)
			return true;
		return false;
	}

}
