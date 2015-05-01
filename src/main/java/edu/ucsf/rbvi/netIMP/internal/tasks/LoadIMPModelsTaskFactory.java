package edu.ucsf.rbvi.netIMP.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.netIMP.internal.model.CyIMPManager;

public class LoadIMPModelsTaskFactory extends AbstractTaskFactory {
	final CyIMPManager manager;

	public LoadIMPModelsTaskFactory(CyIMPManager manager) {
		this.manager = manager;
	}

	public TaskIterator createTaskIterator() {
		// TODO Auto-generated method stub
		return new TaskIterator(new LoadIMPModelsTask(manager));
	}

}
