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

import edu.ucsf.rbvi.netIMP.internal.model.CyIMPManager;

public class CyViewUtils {
	public static VisualStyle createVisualStyle(CyIMPManager manager) {
		VisualMappingManager vmManager = manager.getService(VisualMappingManager.class);
		VisualStyleFactory vsFactory = manager.getService(VisualStyleFactory.class);

		VisualStyle currentStyle = vmManager.getCurrentVisualStyle();
		VisualStyle newStyle = vsFactory.createVisualStyle(currentStyle);
		newStyle.setTitle(currentStyle.getTitle()+"-IMP");

		VisualMappingFunctionFactory discreteMappingFactory = 
						manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=discrete)");

		VisualMappingFunctionFactory continuousMappingFactory = 
						manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=continuous)");

		// Discrete mapper for node shape
		DiscreteMapping shapeMapping = 
					(DiscreteMapping) discreteMappingFactory.createVisualMappingFunction("node type", String.class, NODE_SHAPE);
		shapeMapping.putMapValue("metabolite", NodeShapeVisualProperty.ELLIPSE);
		shapeMapping.putMapValue("enzyme", NodeShapeVisualProperty.HEXAGON);
		newStyle.addVisualMappingFunction(shapeMapping);

		// Discrete mapper for node color
		DiscreteMapping colorMapping = 
					(DiscreteMapping) discreteMappingFactory.createVisualMappingFunction("node type", String.class, NODE_PAINT);
		colorMapping.putMapValue("metabolite", Color.BLUE);
		colorMapping.putMapValue("enzyme", Color.GREEN);
	
		// Continuous mapper for edge thickness
		return null;
	}
}
