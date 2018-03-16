package org.ayakaji.core;
import java.awt.Color;
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

public class IconNodeRenderer extends DefaultTreeCellRenderer {
	private static final long serialVersionUID = 4323253048797579622L;

	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
				row, hasFocus);
		Icon icon = ((IconNode) value).getIcon();
		String txt = ((IconNode) value).getText();
		if (expanded && !txt.equals("È«Íø"))
			icon = new ImageIcon(getClass().getResource("Opened_Folder.png"));
		setIcon(icon);
		setText(txt);
		setBackground(Color.LIGHT_GRAY); // ±³¾°É«
		setBackgroundNonSelectionColor(Color.LIGHT_GRAY);
		return this;
	}
}

class IconNode extends DefaultMutableTreeNode {
	private static final long serialVersionUID = 7717348849668491435L;
	protected Icon icon;
	protected String txt;

	public IconNode(String txt) {
		super();
		this.txt = txt;
	}

	public IconNode(Icon icon, String txt) {
		super();
		this.icon = icon;
		this.txt = txt;
	}

	public void setIcon(Icon icon) {
		this.icon = icon;
	}

	public Icon getIcon() {
		return icon;
	}

	public void setText(String txt) {
		this.txt = txt;
	}

	public String getText() {
		return txt;
	}
}