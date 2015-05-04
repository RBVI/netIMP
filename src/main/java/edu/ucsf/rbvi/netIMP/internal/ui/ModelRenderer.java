package edu.ucsf.rbvi.netIMP.internal.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import java.text.DecimalFormat;

import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.UIManager;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

import edu.ucsf.rbvi.netIMP.internal.model.CyIMPManager;
import edu.ucsf.rbvi.netIMP.internal.model.IMPModel;

public class ModelRenderer extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {
	static final long serialVersionUID = 1L;
	final CyIMPManager impManager;
	protected final DecimalFormat formatter;
	IMPModel impModel = null;

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

		impModel = (IMPModel) data;
		if (impModel == null) return new JPanel();

		// System.out.println("Rendering model "+impModel.getModelNumber()+" with color "+impModel.getColor());
		JPanel l = new JPanel();
		l.setLayout(new BoxLayout(l, BoxLayout.PAGE_AXIS));
		Color background;
		if (viewRow % 2 == 0) {
			background = UIManager.getColor("Table.background");
		} else {
			background = UIManager.getColor("Table.alternateRowColor");
		}
		if (modelColumn == 1) {
			// Start by showing the summary score (in the appropriate model color)
			showSummary(impModel, l, background);
			for (String s: impModel.getScores().keySet()) {
				if (s.equals("score")) continue;
				JPanel p = new JPanel();
				p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
				Double value = impModel.getScores().get(s);
				String strValue = formatter.format(value);
				JLabel label = new JLabel("<html>"+s+": <b>"+strValue+"</b></html>");
				if (impManager.getCurrentNetwork() != null) {
					JCheckBox jcb = new JCheckBox();
					if (s.endsWith(" score"))
						s = s.substring(0, s.length()-6);
					jcb.setActionCommand(s);
					jcb.addItemListener(impManager.getResultsPanel().getItemListener(impModel));
					jcb.setAlignmentX(Component.LEFT_ALIGNMENT);
					p.add(jcb);
				} else {
					label.setAlignmentX(Component.LEFT_ALIGNMENT);
				}

				p.add(label);
				p.setPreferredSize(new Dimension(150,20));
				p.setAlignmentX(Component.LEFT_ALIGNMENT);
				p.setBackground(background);
				l.add(p);
			}
			l.setAlignmentX(Component.LEFT_ALIGNMENT);
			l.setPreferredSize(new Dimension(150, 100));
		}

		l.setOpaque(true);
		l.revalidate();
		return l;

	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value,
	                                             boolean isSelected,
																							 int viewRow, int viewColumn) {
		return getTableCellRendererComponent(table, value, isSelected, true, viewRow, viewColumn);
	}

	@Override
	public Object getCellEditorValue() {
		return impModel;
	}

	private void showSummary(IMPModel model, JPanel topPanel, Color background) {
		Color modelColor = model.getColor();
		Double summaryScore = model.getScores().get("score");
		String strValue = formatter.format(summaryScore);
		JPanel panel = new JPanel();
		JLabel label = new JLabel();
		label.setForeground(modelColor);
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		label.setText("Score: "+strValue);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setBackground(background);
		panel.add(label);
		topPanel.add(panel);

	}

}

