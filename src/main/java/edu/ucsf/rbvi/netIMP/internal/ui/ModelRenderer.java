package edu.ucsf.rbvi.netIMP.internal.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import java.text.DecimalFormat;

import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

import edu.ucsf.rbvi.netIMP.internal.model.CyIMPManager;
import edu.ucsf.rbvi.netIMP.internal.model.IMPModel;

public class ModelRenderer implements TableCellRenderer {
	static final long serialVersionUID = 1L;
	final CyIMPManager impManager;
	protected final DecimalFormat formatter;

	public ModelRenderer(final CyIMPManager impManager) {
		this.impManager = impManager;
		formatter = new DecimalFormat("0.00");
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object data,
	                                               boolean isSelected, boolean hasFocus,
																								 int viewRow, int viewColumn) {
		int modelRow = table.convertRowIndexToModel(viewRow);
		int modelColumn = table.convertColumnIndexToModel(viewColumn);

		IMPModel model = (IMPModel) data;
		JPanel l = new JPanel();
		l.setLayout(new BoxLayout(l, BoxLayout.PAGE_AXIS));
		if (modelColumn == 1) {
			for (String s: model.getScores().keySet()) {
				JPanel p = new JPanel();
				p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
				Double value = model.getScores().get(s);
				String strValue = formatter.format(value);
				JLabel label = new JLabel("<html>"+s+": <b>"+strValue+"</b></html>");
				JCheckBox jcb = new JCheckBox();
				jcb.setActionCommand(s);
				jcb.setAlignmentX(Component.LEFT_ALIGNMENT);

				p.add(jcb);
				p.add(label);
				p.setPreferredSize(new Dimension(150,20));
				p.setAlignmentX(Component.LEFT_ALIGNMENT);
				l.add(p);
			}
			l.setAlignmentX(Component.LEFT_ALIGNMENT);
			l.setPreferredSize(new Dimension(150, 100));
		}

		l.setOpaque(true);
		l.revalidate();
		return l;

	}

}

