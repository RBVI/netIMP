package edu.ucsf.rbvi.netIMP.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.netIMP.internal.model.CyIMPManager;

public class ShowUnionNetworkTaskFactory extends AbstractTaskFactory {
	final CyIMPManager manager;

	public ShowUnionNetworkTaskFactory(CyIMPManager manager) {
		this.manager = manager;
	}

	public TaskIterator createTaskIterator() {
		// TODO Auto-generated method stub
		return new TaskIterator(new ShowUnionNetworkTask(manager));
	}

	public boolean isReady() {
		if (manager.getModelCount() == 0) return false;

		return true;
	}
}
