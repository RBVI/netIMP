package edu.ucsf.rbvi.netIMP.internal.ui;

import java.awt.Color;
import java.util.List;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.DiscreteRange;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.LineType;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;

import edu.ucsf.rbvi.netIMP.internal.model.CyIMPManager;

public class CyViewUtils {
	public static VisualStyle createVisualStyle(CyIMPManager manager) {
		VisualMappingManager vmManager = manager.getService(VisualMappingManager.class);
		VisualStyleFactory vsFactory = manager.getService(VisualStyleFactory.class);

		for (VisualStyle style: vmManager.getAllVisualStyles()) {
			if ("IMP Models".equals(style.getTitle()))
				return style;
		}

		VisualStyle newStyle = vsFactory.createVisualStyle("IMP Models");

		VisualMappingFunctionFactory discreteMappingFactory = 
						manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=discrete)");

		VisualMappingFunctionFactory continuousMappingFactory = 
						manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=continuous)");

		VisualMappingFunctionFactory passthroughMappingFactory = 
						manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");

		newStyle.setDefaultValue(NODE_WIDTH, 40.0);
		newStyle.setDefaultValue(NODE_HEIGHT, 40.0);
		newStyle.setDefaultValue(NODE_SIZE, 40.0);
		newStyle.setDefaultValue(NODE_LABEL_COLOR, Color.BLACK);
		newStyle.setDefaultValue(NODE_BORDER_WIDTH, 2.0);
		newStyle.setDefaultValue(NODE_BORDER_PAINT, Color.BLACK);

		// Passthrough mapping for the label
		PassthroughMapping labelMapping = 
					(PassthroughMapping) passthroughMappingFactory.createVisualMappingFunction("label", String.class, NODE_LABEL);
		newStyle.addVisualMappingFunction(labelMapping);

		// Discrete mapper for node shape
		DiscreteMapping shapeMapping = 
					(DiscreteMapping) discreteMappingFactory.createVisualMappingFunction("node type", String.class, NODE_SHAPE);
		shapeMapping.putMapValue("metabolite", NodeShapeVisualProperty.ELLIPSE);
		shapeMapping.putMapValue("enzyme", NodeShapeVisualProperty.HEXAGON);
		newStyle.addVisualMappingFunction(shapeMapping);

		// Discrete mapper for node color
		DiscreteMapping colorMapping = 
					(DiscreteMapping) discreteMappingFactory.createVisualMappingFunction("node type", String.class, NODE_FILL_COLOR);
		colorMapping.putMapValue("metabolite", Color.WHITE);
		colorMapping.putMapValue("enzyme", new Color(153,255,153));
		newStyle.addVisualMappingFunction(colorMapping);

		// Discrete mapper for restraints to turn them off by default
		DiscreteMapping restraintMapping = 
					(DiscreteMapping) discreteMappingFactory.createVisualMappingFunction("isRestraint", Boolean.class, EDGE_VISIBLE);
		restraintMapping.putMapValue(true, false);
		restraintMapping.putMapValue(false, true);
		newStyle.addVisualMappingFunction(restraintMapping);

		// Discrete mapper for directed restraints 
		DiscreteMapping restraintArrowMapping = 
					(DiscreteMapping) discreteMappingFactory.createVisualMappingFunction("directed", Boolean.class, EDGE_TARGET_ARROW_SHAPE);
		restraintArrowMapping.putMapValue(true, ArrowShapeVisualProperty.ARROW);
		restraintArrowMapping.putMapValue(false, ArrowShapeVisualProperty.NONE);
		newStyle.addVisualMappingFunction(restraintArrowMapping);
	
		// Continuous mapper for edge thickness
		int max = manager.getMaxModelCount();
		ContinuousMapping edgeMapping =
				(ContinuousMapping) continuousMappingFactory.createVisualMappingFunction("ModelCount", Integer.class, EDGE_WIDTH);
		edgeMapping.addPoint(1, new BoundaryRangeValues(1, 1, 1));
		edgeMapping.addPoint(max, new BoundaryRangeValues(20, 20, 20));
		newStyle.addVisualMappingFunction(edgeMapping);

		// Finally, set our visual property dependencies
		for (VisualPropertyDependency<?> vpd: newStyle.getAllVisualPropertyDependencies()) {
			if (vpd.getIdString().equals("arrowColorMatchesEdge"))
				vpd.setDependency(true);
		}
		vmManager.addVisualStyle(newStyle);
		vmManager.setCurrentVisualStyle(newStyle);
		return newStyle;
	}

	public static void showEdge(CyIMPManager manager, CyNetwork network, CyEdge edge, boolean show) {
		for (CyNetworkView view: manager.getNetworkViews(network)) {
			View<CyEdge> edgeView = view.getEdgeView(edge);
			if (edgeView == null) continue;
			if (show) {
				edgeView.clearValueLock(EDGE_VISIBLE);
				edgeView.setLockedValue(EDGE_VISIBLE, show);
			} else {
				edgeView.clearValueLock(EDGE_VISIBLE);
				edgeView.setLockedValue(EDGE_VISIBLE, show);
			}
			// view.updateView();
		}
	}

	public static void hideRestraintEdges(CyNetworkView view) {
		CyNetwork net = view.getModel();
		for (View<CyEdge> ev: view.getEdgeViews()) {
			CyEdge edge = ev.getModel();
			if (net.getRow(edge).get("isRestraint", Boolean.class))
				ev.setLockedValue(EDGE_VISIBLE, false);
		}
	}

	public static void setLineType(View<CyEdge> edgeView, VisualProperty<?> ltVp, String type) {
		DiscreteRange<LineType> range = (DiscreteRange<LineType>)ltVp.getRange();
		for (LineType t: range.values()) {
			if (t.getDisplayName().equals(type)) {
				edgeView.setLockedValue(ltVp, t);
				return;
			}
		}
	}

	public static boolean isLayedOut(CyIMPManager manager, CyNetwork network) {
		for (CyNetworkView view: manager.getNetworkViews(network)) {
			for (View<CyNode> nodeView: view.getNodeViews()) {
				if (nodeView.getVisualProperty(NODE_X_LOCATION) != 0.0 ||
				    nodeView.getVisualProperty(NODE_Y_LOCATION) != 0.0)
					return true;
			}
		}
		return false;
	}
}
