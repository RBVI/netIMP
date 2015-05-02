package edu.ucsf.rbvi.netIMP.internal.ui;

import java.util.Comparator;

import org.cytoscape.model.CyNetwork;

public class NetworkSorter implements Comparator<CyNetwork> {
	public NetworkSorter() { }

	public int compare(CyNetwork n1, CyNetwork n2) {
		if (n1 == null && n2 == null) return 0;
		if (n1 == null && n2 != null) return -1;
		if (n2 == null && n1 != null) return 1;

		if(n1.getNodeCount() < n2.getNodeCount()) return 1;
		if(n1.getNodeCount() > n2.getNodeCount()) return -1;
		return 0;
	}
}
