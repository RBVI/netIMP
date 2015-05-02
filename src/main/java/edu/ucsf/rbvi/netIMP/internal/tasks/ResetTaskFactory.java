package edu.ucsf.rbvi.netIMP.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.netIMP.internal.model.CyIMPManager;

public class ResetTaskFactory extends AbstractTaskFactory {
	final CyIMPManager manager;

	public ResetTaskFactory(CyIMPManager manager) {
		this.manager = manager;
	}

	public TaskIterator createTaskIterator() {
		// TODO Auto-generated method stub
		return new TaskIterator(new ResetTask(manager));
	}

	public boolean isReady() {
		if (manager.getCurrentNetwork() != null &&
				manager.getModelCount() > 0)
			return true;
		return false;
	}
}
