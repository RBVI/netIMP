package edu.ucsf.rbvi.netIMP.internal.ui;

import java.awt.Color;
import java.util.List;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
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
	
		// Continuous mapper for edge thickness
		int max = manager.getMaxModelCount();
		ContinuousMapping edgeMapping =
				(ContinuousMapping) continuousMappingFactory.createVisualMappingFunction("ModelCount", Integer.class, EDGE_WIDTH);
		edgeMapping.addPoint(1, new BoundaryRangeValues(1, 1, 1));
		edgeMapping.addPoint(max, new BoundaryRangeValues(20, 20, 20));
		newStyle.addVisualMappingFunction(edgeMapping);
		vmManager.addVisualStyle(newStyle);
		vmManager.setCurrentVisualStyle(newStyle);
		return newStyle;
	}
}
