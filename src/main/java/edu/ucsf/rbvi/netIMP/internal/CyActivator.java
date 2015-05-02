package edu.ucsf.rbvi.netIMP.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
// Commented out until 3.2 is released
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.INSERT_SEPARATOR_BEFORE;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsf.rbvi.netIMP.internal.model.CyIMPManager;
import edu.ucsf.rbvi.netIMP.internal.tasks.LoadIMPModelsTaskFactory;
import edu.ucsf.rbvi.netIMP.internal.tasks.ResetTaskFactory;
import edu.ucsf.rbvi.netIMP.internal.tasks.ShowIMPResultsPanelTaskFactory;
import edu.ucsf.rbvi.netIMP.internal.tasks.ShowUnionNetworkTaskFactory;

public class CyActivator extends AbstractCyActivator {
	private static Logger logger = LoggerFactory
			.getLogger(edu.ucsf.rbvi.netIMP.internal.CyActivator.class);

	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {

		// See if we have a graphics console or not
		boolean haveGUI = true;
		CySwingApplication cySwingApplication = null;
		ServiceReference ref = bc.getServiceReference(CySwingApplication.class.getName());

		if (ref == null) {
			haveGUI = false;
			// Issue error and return
		} else {
			cySwingApplication = getService(bc, CySwingApplication.class);
		}

		try {
			CyServiceRegistrar registrar = getService(bc, CyServiceRegistrar.class);
			CyIMPManager impManager = new CyIMPManager(registrar);

			{
				LoadIMPModelsTaskFactory loadModels = 
					new LoadIMPModelsTaskFactory(impManager);
				Properties loadProps = new Properties();
				loadProps.setProperty(PREFERRED_MENU, "Apps.NetIMP");
				loadProps.setProperty(TITLE, "Open IMP results");
				loadProps.setProperty(MENU_GRAVITY, "1.0");
				registerService(bc, loadModels, TaskFactory.class, loadProps);
			}
			
			{
				ShowIMPResultsPanelTaskFactory showResults = 
					new ShowIMPResultsPanelTaskFactory(impManager, true);
				Properties showProps = new Properties();
				showProps.setProperty(PREFERRED_MENU, "Apps.NetIMP");
				showProps.setProperty(TITLE, "Show IMP results panel");
				showProps.setProperty(MENU_GRAVITY, "2.0");
				registerService(bc, showResults, TaskFactory.class, showProps);
			}
			
			{
				ShowIMPResultsPanelTaskFactory hideResults = 
					new ShowIMPResultsPanelTaskFactory(impManager, false);
				Properties hideProps = new Properties();
				hideProps.setProperty(PREFERRED_MENU, "Apps.NetIMP");
				hideProps.setProperty(TITLE, "Hide IMP results panel");
				hideProps.setProperty(MENU_GRAVITY, "3.0");
				registerService(bc, hideResults, TaskFactory.class, hideProps);
			}

			{
				ShowUnionNetworkTaskFactory showUnion = 
					new ShowUnionNetworkTaskFactory(impManager);
				Properties showProps = new Properties();
				showProps.setProperty(PREFERRED_MENU, "Apps.NetIMP");
				showProps.setProperty(TITLE, "Show Union Network");
				showProps.setProperty(MENU_GRAVITY, "5.0");
				registerService(bc, showUnion, TaskFactory.class, showProps);
			}
			
			{
				Properties associateProps = new Properties();
				associateProps.setProperty(PREFERRED_MENU, "Apps.NetIMP");
				associateProps.setProperty(TITLE, "Associate results with network");
				associateProps.setProperty(MENU_GRAVITY, "6.0");
			}
			
			{
				ResetTaskFactory reset = 
					new ResetTaskFactory(impManager);
				Properties resetProps = new Properties();
				resetProps.setProperty(PREFERRED_MENU, "Apps.NetIMP");
				resetProps.setProperty(TITLE, "Reset");
				resetProps.setProperty(MENU_GRAVITY, "10.0");
				registerService(bc, reset, TaskFactory.class, resetProps);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
