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
	 * �Զ����ǩ
	 * 
	 * @author Hugh
	 */
	public static class CustomizedLabel extends JLabel {
		private static final long serialVersionUID = 4326986450195469329L;

		public CustomizedLabel(String title) {
			setText(title); // �����ı�
			setVerticalAlignment(SwingConstants.CENTER); // ��ֱ����
			setHorizontalAlignment(SwingConstants.RIGHT); // ����չʾ
		}
	}

	/**
	 * �Զ����ı���
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
					getFont().getSize() + 3); // �����ԭ������Ŵ�
			setFont(font);
		}
	}

	/**
	 * �Զ���ѡ���
	 * 
	 * @author hugh
	 */
	public static class CustomizedComboBox extends JComboBox {
		private static final long serialVersionUID = 8704402336624915239L;

		public CustomizedComboBox(String[] content) {
			super(content);
			Font font = new Font(getFont().getName(), getFont().getStyle(),
					getFont().getSize() + 3); // �����ԭ������Ŵ�
			setFont(font);
			setSelectedIndex(0); // Ĭ��ѡ���һ����¼
		}
	}

	/**
	 * �Զ��嵥ѡ��ť
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
					radio.setSelected(true); // Ĭ��ѡ���һ��
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
	 * �Զ���·���ı���
	 * 
	 * @author Hugh
	 */
	public static class CustomizedPathField extends JTextField {
		private static final long serialVersionUID = 5256743147024587008L;

		public CustomizedPathField(String fileName) {
			URL url = getClass().getClassLoader().getResource(fileName); // �ӵ�ǰ��·��������
			if (url == null)
				return; // ����ȡ����,�����κθı�
			File file = new File(url.getFile()); // ����url��ȡFile����
			String absPath = file.getAbsolutePath(); // ����File��ȡ�ļ�����·��
			setText(absPath);
			Font font = new Font(getFont().getName(), getFont().getStyle(),
					getFont().getSize() + 3); // ���������ԭ������Ŵ�
			setFont(font);
			setAlignmentX(RIGHT_ALIGNMENT);
		}
	}

	/**
	 * �Զ���IP�ı���(ʹ��JIDE)
	 * 
	 * @author Hugh
	 */
	public static class CustomizedIPField extends IPTextField {
		private static final long serialVersionUID = 7606495762236505574L;

		public CustomizedIPField() {
			Font font = new Font(getFont().getName(), getFont().getStyle(),
					getFont().getSize() + 3); // ���������ԭ������Ŵ�
			setFont(font);
		}
	}

	/**
	 * �Զ��廬����
	 * 
	 * @author Hugh
	 */
	public static class CustomizedJSlider extends JSlider {
		private static final long serialVersionUID = -7414578184047012513L;

		public CustomizedJSlider(int style) {
			super(style);
			Font font = new Font(getFont().getName(), getFont().getStyle(),
					getFont().getSize() + 3); // ���������ԭ������Ŵ�
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
