package org.ayakaji.core;
import java.awt.Font;
import java.io.File;
import java.net.URL;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.jidesoft.field.IPTextField;

public class JControls {

	/**
	 * 自定义标签
	 * 
	 * @author Hugh
	 */
	public static class CustomizedLabel extends JLabel {
		private static final long serialVersionUID = 4326986450195469329L;

		public CustomizedLabel(String title) {
			setText(title); // 设置文本
			setVerticalAlignment(SwingConstants.CENTER); // 垂直居中
			setHorizontalAlignment(SwingConstants.RIGHT); // 靠右展示
		}
	}

	/**
	 * 自定义文本框
	 * 
	 * @author Hugh
	 */
	public static class CustomizedTextField extends JTextField {
		private static final long serialVersionUID = -5853388661248106217L;

		public CustomizedTextField(String text) {
			setText(text);
			setAlignmentX(CENTER_ALIGNMENT);
			setAlignmentY(LEFT_ALIGNMENT);
			Font font = new Font(getFont().getName(), getFont().getStyle(),
					getFont().getSize() + 3); // 字体较原有字体放大
			setFont(font);
		}
	}

	/**
	 * 自定义选择框
	 * 
	 * @author hugh
	 */
	public static class CustomizedComboBox extends JComboBox {
		private static final long serialVersionUID = 8704402336624915239L;

		public CustomizedComboBox(String[] content) {
			super(content);
			Font font = new Font(getFont().getName(), getFont().getStyle(),
					getFont().getSize() + 3); // 字体较原有字体放大
			setFont(font);
			setSelectedIndex(0); // 默认选择第一条记录
		}
	}

	/**
	 * 自定义单选按钮
	 * 
	 * @author Hugh
	 */
	public static class CustomizedButtonGroup extends ButtonGroup {
		private static final long serialVersionUID = -7567378104850647317L;

		public CustomizedButtonGroup(String[] options) {
			for (String option : options) {
				JRadioButton radio = new JRadioButton(option);
				add(radio);
				if (option.equals(options[0]))
					radio.setSelected(true); // 默认选择第一项
			}
		}

		public String getSelectedOption() {
			Enumeration<AbstractButton> enumBtns = getElements();
			while (enumBtns.hasMoreElements()) {
				AbstractButton absBtn = enumBtns.nextElement();
				if (absBtn.isSelected())
					return absBtn.getText();
			}
			return null;
		}
	}

	/**
	 * 自定义路径文本框
	 * 
	 * @author Hugh
	 */
	public static class CustomizedPathField extends JTextField {
		private static final long serialVersionUID = 5256743147024587008L;

		public CustomizedPathField(String fileName) {
			URL url = getClass().getClassLoader().getResource(fileName); // 从当前类路径下搜索
			if (url == null)
				return; // 若获取不到,则不做任何改变
			File file = new File(url.getFile()); // 根据url获取File对象
			String absPath = file.getAbsolutePath(); // 根据File获取文件绝对路径
			setText(absPath);
			Font font = new Font(getFont().getName(), getFont().getStyle(),
					getFont().getSize() + 3); // 设置字体较原有字体放大
			setFont(font);
			setAlignmentX(RIGHT_ALIGNMENT);
		}
	}

	/**
	 * 自定义IP文本框(使用JIDE)
	 * 
	 * @author Hugh
	 */
	public static class CustomizedIPField extends IPTextField {
		private static final long serialVersionUID = 7606495762236505574L;

		public CustomizedIPField() {
			Font font = new Font(getFont().getName(), getFont().getStyle(),
					getFont().getSize() + 3); // 设置字体较原有字体放大
			setFont(font);
		}
	}

	/**
	 * 自定义滑动条
	 * 
	 * @author Hugh
	 */
	public static class CustomizedJSlider extends JSlider {
		private static final long serialVersionUID = -7414578184047012513L;

		public CustomizedJSlider(int style) {
			super(style);
			Font font = new Font(getFont().getName(), getFont().getStyle(),
					getFont().getSize() + 3); // 设置字体较原有字体放大
			setFont(font);
			setMinimum(0);
			setMaximum(10);
			setValue(0);
			setPaintTicks(true);
			setPaintLabels(true);
			setMajorTickSpacing(1);
			setMinorTickSpacing(1);
			setSnapToTicks(true);
		}
	}
}
