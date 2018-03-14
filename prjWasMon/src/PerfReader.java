import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.ini4j.Config;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.AdminClientFactory;
import com.ibm.websphere.management.exception.ConnectorException;
import com.ibm.websphere.pmi.stat.StatDescriptor;
import com.ibm.websphere.pmi.stat.WSStats;

public final class PerfReader extends JFrame implements Runnable {

	private static final long serialVersionUID = 5832510607198769335L;
	
	private static final Logger logger = Logger.getLogger("WasMon");

	/**
	 * ��Ҫ���ص�����ģ��
	 */
	private static String[] modules = new String[] { "jvmRuntimeModule",
			"threadPoolModule", "servletSessionsModule", "connectionPoolModule" };
	
	private static PerfReader self = null;

	/**
	 * ά������dmgr����
	 */
	private static final Map<String, AdminClient> mapConn = new HashMap<String, AdminClient>();

	private PerfReader() {
		self = this;
		setTitle("WebSphere ���ƽ̨");
		Dimension scrSz = Toolkit.getDefaultToolkit().getScreenSize(); // ��ȡ��Ļ�ߴ�
		Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(
				getGraphicsConfiguration()); // ��ȡ�������߶�
		setSize((int) scrSz.getWidth(),
				(int) (scrSz.getHeight() - screenInsets.bottom)); // ���ô�СΪ����
		setResizable(false); // ���岻����С��
		setLocation(0, 0); // ����λ��
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // ���ô����˳���Ϊ

		JSplitPane hSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); // ˮƽ�ָ���
		hSplitPane.setDividerLocation(getSize().width / 8); // �ָ����
		hSplitPane.setBackground(Color.gray); // ���ñ���
		getContentPane().add(hSplitPane, BorderLayout.CENTER); // ��ӷָ���
		hSplitPane.setLeftComponent(new NaviPanel()); // ��ർ����������WebSphere��Ⱥ
		hSplitPane.setRightComponent(new PerfPanel()); // �Ҳ�����ָ���б�
		setVisible(true); // �򿪴���
	}


	/**
	 * dmgr�������öԻ���(�½�/�޸�)
	 * 
	 * @author heqiming
	 * 
	 */
	private static class DlgDmgrConfig extends JDialog {

		private static final long serialVersionUID = 3899811007317902279L;

		private static DlgDmgrConfig self = null; // ��������������
		
		private JControls.CustomizedIPField txtHost = new JControls.CustomizedIPField(); // DMGR����IP��ַ�ı���
		//private CustomizedTextField txtMask = new CustomizedTextField("32"); // ����λ, ������������
		private JControls.CustomizedTextField txtPort = new JControls.CustomizedTextField("8879"); // �˿��ı���
		private JControls.CustomizedComboBox cmbConnTyp = new JControls.CustomizedComboBox(
				new String[] { "SOAP", "JMX", "WSADMIN" }); // ��������
		private JControls.CustomizedButtonGroup btnGrpIfSec = new JControls.CustomizedButtonGroup(
				new String[] { "��", "��" }); // ��ѡ��ť
		private JPasswordField txtUserName = new JPasswordField("wasadmin"); // �û���
		private JPasswordField txtPassword = new JPasswordField("WebJ2ee"); // ����
		private JControls.CustomizedPathField txtTrustStorPath = new JControls.CustomizedPathField(
				"DummyClientTrustFile.jks"); // ����֤��
		private JControls.CustomizedPathField txtKeyStorPath = new JControls.CustomizedPathField(
				"DummyClientKeyFile.jks"); // ����֤��
		private JControls.CustomizedComboBox cmbKeyStorTyp = new JControls.CustomizedComboBox(
				new String[] { "JKS", "JCEKS", "PKCS12" }); // ��Կ����
		private JPasswordField txtTrustStorPass = new JPasswordField("WebAS"); // ֤������
		private JPasswordField txtKeyStorPass = new JPasswordField("WebAS"); // ��Կ������
		private JControls.CustomizedComboBox cmbCategory = new JControls.CustomizedComboBox(
				new String[] { "Ӫҵ��CRMǰ̨", "�ͷ�CRM", "Ӧ��4A", "һ��BOSS", "����ҵ��̽��",
						"�汾����", "������", "�����ն�", "�ն˹���ƽ̨", "�̳�", "��Ƿ",
						"imsportal", "ESOP", "ʡ��SP��", "֧����", "�ƶ�����̨", "������",
						"������", "����" }); // ϵͳ���࣬��ʾΪ����Ŀ¼
		private JLabel txtClusterAlias = new JLabel(""); // ����Ⱥ����,����dmgr����ʱ���ݻ�ȡ��cell���Ƶõ�,���ɲ���
		private JControls.CustomizedJSlider sldReloadInterval = new JControls.CustomizedJSlider(
				JSlider.HORIZONTAL); // ˢ��Ƶ�ʻ�����, 0-���Զ�ˢ��, 1~10���������߳��Զ�ˢ��
		private boolean state = false; // �Ի��򷵻�״̬��
		
		/**
		 * ��ȡdmgr���öԻ�����������
		 * @return
		 */
		private Map<String, String> getDmgrConfigurations() {
			Map<String, String> map = new HashMap<String, String>();
			map.put("host", txtHost.getText());
			map.put("port", txtPort.getText());
			map.put("connTyp", (String) cmbConnTyp.getSelectedItem());
			map.put("ifSec", btnGrpIfSec.getSelectedOption().equals("��") ? "true" : "false");
			map.put("userName", new String(txtUserName.getPassword()));
			map.put("password", new String(txtPassword.getPassword()));
			map.put("trustStorPath", txtTrustStorPath.getText());
			map.put("keyStorPath", txtKeyStorPath.getText());
			map.put("keyStorType", (String) cmbKeyStorTyp.getSelectedItem());
			map.put("trustStorPass", new String(txtTrustStorPass.getPassword()));
			map.put("keyStorPass", new String(txtKeyStorPass.getPassword()));
			map.put("category", (String) cmbCategory.getSelectedItem());
			map.put("clusterAlias", txtClusterAlias.getText());
			map.put("reloadInterval", Integer.toString(sldReloadInterval.getValue()));
			return map;
		}

		/**
		 * ��ȡBasic���
		 * 
		 * @return
		 */
		private JPanel getDmgrConfPanel() {
			GridBagLayout gbl = new GridBagLayout(); // ����GridBag����
			final JPanel jpBasic = new JPanel(gbl);
			jpBasic.setBorder(BorderFactory.createTitledBorder(" ��������"));
			GridBagConstraints gbc = new GridBagConstraints();// GridBag����
			gbc.fill = GridBagConstraints.BOTH;
			gbc.ipadx = 20;
			gbc.ipady = 20;

			// ��һ��
			jpBasic.add(new JControls.CustomizedLabel("����IP:")); // Ԫ��0
			gbc.gridwidth = 1;
			gbc.weightx = 0.1;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(0), gbc);
			jpBasic.add(txtHost); // Ԫ��1
			gbc.gridwidth = 2;
			gbc.weightx = 0.2;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(1), gbc);
			jpBasic.add(new Container()); // Ԫ��2
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(2), gbc);
			jpBasic.add(new JControls.CustomizedLabel("����˿�:")); // Ԫ��3
			gbc.gridwidth = 1;
			gbc.weightx = 0.1;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(3), gbc);
			jpBasic.add(txtPort); // Ԫ��4
			txtPort.setEnabled(false);
			gbc.gridwidth = 2;
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(4), gbc);
			JCheckBox chkEnablePort = new JCheckBox(); // ��ѡ���������ö˿ڱ༭
			chkEnablePort.setSelected(false);
			chkEnablePort.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					txtPort.setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnablePort); // Ԫ��5
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(5), gbc);
			jpBasic.add(new JControls.CustomizedLabel("��������:")); // Ԫ��6
			gbc.gridwidth = 1;
			gbc.weightx = 0.1;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(6), gbc);
			jpBasic.add(cmbConnTyp); // Ԫ��7
			cmbConnTyp.setEnabled(false);
			gbc.gridwidth = 1;
			gbc.weightx = 0.2;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(7), gbc);
			JCheckBox chkEnableConnTyp = new JCheckBox(); // ��ѡ���������ñ༭
			chkEnableConnTyp.setSelected(false);
			chkEnableConnTyp.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					cmbConnTyp.setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnableConnTyp); // Ԫ��8
			gbc.gridwidth = 0;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(8), gbc);
			// �ڶ���
			jpBasic.add(new JControls.CustomizedLabel("��ȫ����:")); // Ԫ��9
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(9), gbc);
			Enumeration<AbstractButton> btnOpts = btnGrpIfSec.getElements();
			jpBasic.add(btnOpts.nextElement()); // Ԫ��10
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			jpBasic.getComponent(10).setEnabled(false);
			gbl.setConstraints(jpBasic.getComponent(10), gbc);
			jpBasic.add(btnOpts.nextElement()); // Ԫ��11
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			jpBasic.getComponent(11).setEnabled(false);
			gbl.setConstraints(jpBasic.getComponent(11), gbc);
			JCheckBox chkEnableIfSec = new JCheckBox(); // ��ѡ���������ñ༭
			chkEnableIfSec.setSelected(false);
			chkEnableIfSec.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					jpBasic.getComponent(10).setEnabled(chk.isSelected());
					jpBasic.getComponent(11).setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnableIfSec); // Ԫ��12
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(12), gbc);
			jpBasic.add(new JControls.CustomizedLabel("dmgr�˺�:")); // Ԫ��13
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(13), gbc);
			jpBasic.add(txtUserName); // Ԫ��14
			txtUserName.setEnabled(false);
			gbc.gridwidth = 2;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(14), gbc);
			JCheckBox chkEnableUserName = new JCheckBox(); // ��ѡ���������ñ༭
			chkEnableUserName.setSelected(false);
			chkEnableUserName.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					txtUserName.setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnableUserName); // Ԫ��15
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(15), gbc);
			jpBasic.add(new JControls.CustomizedLabel("dmgr����:")); // Ԫ��16
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(16), gbc);
			jpBasic.add(txtPassword); // Ԫ��17
			txtPassword.setEnabled(false);
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(17), gbc);
			JCheckBox chkEnablePassword = new JCheckBox(); // ��ѡ���������ñ༭
			chkEnablePassword.setSelected(false);
			chkEnablePassword.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					txtPassword.setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnablePassword); // Ԫ��18
			gbc.gridwidth = 0;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(18), gbc);
			// ������
			jpBasic.add(new JControls.CustomizedLabel("����֤��:")); // Ԫ��19
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(19), gbc);
			jpBasic.add(txtTrustStorPath); // Ԫ��20
			txtTrustStorPath.setEnabled(false);
			gbc.gridwidth = 2;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(20), gbc);
			JButton btnStorPathSelect = new JButton("..."); // Ԫ��21
			btnStorPathSelect.addMouseListener(new MouseListener() {
				@Override
				public void mouseReleased(MouseEvent e) {
				}

				@Override
				public void mousePressed(MouseEvent e) {
					JButton btn = (JButton) e.getComponent();
					if (SwingUtilities.isLeftMouseButton(e) == false
							|| !btn.isEnabled()) {
						return;
					}
					JFileChooser fc = new JFileChooser();
					fc.setDialogType(JFileChooser.OPEN_DIALOG);
					try {
						fc.setCurrentDirectory(new File(getClass()
								.getClassLoader().getResource("").toURI()));
					} catch (URISyntaxException e1) {
						e1.printStackTrace();
					}
					fc.setFileFilter(new FileNameExtensionFilter("֤��/��Կ��",
							"jks"));
					fc.setSelectedFile(new File("*.jks"));
					fc.setMultiSelectionEnabled(false);
					int retVal = fc.showOpenDialog(self);
					if (retVal == JFileChooser.APPROVE_OPTION) {
						txtTrustStorPath.setText(fc.getSelectedFile()
								.getAbsolutePath());
					}
				}

				@Override
				public void mouseExited(MouseEvent e) {
				}

				@Override
				public void mouseEntered(MouseEvent e) {
				}

				@Override
				public void mouseClicked(MouseEvent e) {
				}
			});
			jpBasic.add(btnStorPathSelect);
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(21), gbc);
			jpBasic.add(new JControls.CustomizedLabel("��Կ:")); // Ԫ��22
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(22), gbc);
			jpBasic.add(txtKeyStorPath); // Ԫ��23
			txtKeyStorPath.setEnabled(false);
			gbc.gridwidth = 2;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(23), gbc);
			JButton btnKeyPathSelect = new JButton("..."); // Ԫ��24
			btnKeyPathSelect.addMouseListener(new MouseListener() {
				@Override
				public void mouseReleased(MouseEvent e) {
				}

				@Override
				public void mousePressed(MouseEvent e) {
					JButton btn = (JButton) e.getComponent();
					if (SwingUtilities.isLeftMouseButton(e) == false
							|| !btn.isEnabled()) {
						return;
					}
					JFileChooser fc = new JFileChooser();
					fc.setDialogType(JFileChooser.OPEN_DIALOG);
					try {
						fc.setCurrentDirectory(new File(getClass()
								.getClassLoader().getResource("").toURI()));
					} catch (URISyntaxException e1) {
						e1.printStackTrace();
					}
					fc.setFileFilter(new FileNameExtensionFilter("֤��/��Կ��",
							"jks"));
					fc.setSelectedFile(new File("*.jks"));
					fc.setMultiSelectionEnabled(false);
					int retVal = fc.showOpenDialog(self);
					if (retVal == JFileChooser.APPROVE_OPTION) {
						txtKeyStorPath.setText(fc.getSelectedFile()
								.getAbsolutePath());
					}
				}

				@Override
				public void mouseExited(MouseEvent e) {
				}

				@Override
				public void mouseEntered(MouseEvent e) {
				}

				@Override
				public void mouseClicked(MouseEvent e) {
				}
			});
			jpBasic.add(btnKeyPathSelect);
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(24), gbc);
			jpBasic.add(new JControls.CustomizedLabel("��Կ����:")); // Ԫ��25
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(25), gbc);
			jpBasic.add(cmbKeyStorTyp); // Ԫ��26
			cmbKeyStorTyp.setEnabled(false);
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(26), gbc);
			JCheckBox chkEnableKeyStorTyp = new JCheckBox(); // ��ѡ���������ñ༭
			chkEnableKeyStorTyp.setSelected(false);
			chkEnableKeyStorTyp.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					cmbKeyStorTyp.setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnableKeyStorTyp); // Ԫ��27
			gbc.gridwidth = 0;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(27), gbc);
			// ������
			jpBasic.add(new JControls.CustomizedLabel("֤������:")); // Ԫ��28
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(28), gbc);
			jpBasic.add(txtTrustStorPass); // Ԫ��29
			txtTrustStorPass.setEnabled(false);
			gbc.gridwidth = 2;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(29), gbc);
			JCheckBox chkEnableTrustStorPass = new JCheckBox(); // ��ѡ���������ñ༭
			chkEnableTrustStorPass.setSelected(false);
			chkEnableTrustStorPass.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					txtTrustStorPass.setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnableTrustStorPass); // Ԫ��30
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(30), gbc);
			jpBasic.add(new JControls.CustomizedLabel("��Կ����:")); // Ԫ��31
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(31), gbc);
			jpBasic.add(txtKeyStorPass); // Ԫ��32
			txtKeyStorPass.setEnabled(false);
			gbc.gridwidth = 2;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(32), gbc);
			JCheckBox chkEnableKeyStorPass = new JCheckBox(); // ��ѡ���������ñ༭
			chkEnableKeyStorPass.setSelected(false);
			chkEnableKeyStorPass.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					txtKeyStorPass.setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnableKeyStorPass); // Ԫ��33
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(33), gbc);
			jpBasic.add(new JControls.CustomizedLabel("ϵͳ����:")); // Ԫ��34
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(34), gbc);
			jpBasic.add(cmbCategory); // Ԫ��35
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(35), gbc);
			jpBasic.add(new Container()); // Ԫ��36
			gbc.gridwidth = 0;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(36), gbc);
			// ��6��
			jpBasic.add(new JControls.CustomizedLabel("ˢ��Ƶ��:")); // Ԫ��31
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(37), gbc);
			jpBasic.add(sldReloadInterval); // Ԫ��36
			gbc.gridwidth = 2;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(38), gbc);
			jpBasic.add(new Container()); // Ԫ��44
			gbc.gridwidth = 0;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(39), gbc);
			// ��7��У׼��
			jpBasic.add(new Container()); // Ԫ��37
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(40), gbc);
			jpBasic.add(new Container()); // Ԫ��35
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(41), gbc);
			jpBasic.add(new Container()); // Ԫ��36
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(42), gbc);
			jpBasic.add(new Container()); // Ԫ��37
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(43), gbc);
			jpBasic.add(new Container()); // Ԫ��38
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(44), gbc);
			jpBasic.add(new Container()); // Ԫ��39
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(45), gbc);
			jpBasic.add(new Container()); // Ԫ��40
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(46), gbc);
			jpBasic.add(new Container()); // Ԫ��41
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(47), gbc);
			jpBasic.add(new Container()); // Ԫ��42
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(48), gbc);
			jpBasic.add(new Container()); // Ԫ��43
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(49), gbc);
			jpBasic.add(new Container()); // Ԫ��44
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(50), gbc);

			return jpBasic;
		}

		/**
		 * ��ȡ��ť���
		 * 
		 * @return
		 */
		private JPanel getButtonPanel() {
			GridBagLayout gbl = new GridBagLayout(); // ����GridBag����
			JPanel jpButton = new JPanel(gbl); // ������
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.BOTH;
			gbc.ipadx = 20;
			gbc.ipady = 20;

			jpButton.add(new Container());
			gbc.gridwidth = 1;
			gbc.weightx = 0.8;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpButton.getComponent(0), gbc);
			jpButton.add(txtClusterAlias); // ��Ⱥ����
			gbc.gridwidth = 1;
			gbc.weightx = 0.05;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpButton.getComponent(1), gbc);
			JButton btnApply = new JButton("Ӧ��");
			btnApply.addMouseListener(new MouseListener() {
				@Override
				public void mouseReleased(MouseEvent e) {
				}
				@Override
				public void mousePressed(MouseEvent e) {
					JButton btn = (JButton) e.getComponent();
					if (SwingUtilities.isLeftMouseButton(e) == false
							|| !btn.isEnabled()) {
						return;
					}
					state = false;
					Map<String, String> map = getDmgrConfigurations();
					if (map.get("mask") != null && !map.get("mask").equals("32")) { // ������λ����32,˵��������
						List<String> ipList = IpUtils.parseIpMaskRange(map.get("host"), map.get("mask"));
						for (String ip : ipList) {
							if (!isPortUsing(ip, Integer.parseInt(map.get("port")))) ipList.remove(ip);
							// δ���
						}
					}
					String key = map.get("host") + ":" + map.get("port");
					if (!mapConn.containsKey(key)) { // ����dmgr����ӳ����в�����,�����µ�����,����¼������ӳ���
						AdminClient ac = PerfReader.self.createConnection(
								map.get("host"), map.get("port"),
								map.get("connTyp"), map.get("ifSec"),
								map.get("userName"), map.get("password"),
								map.get("trustStorPath"),
								map.get("keyStorPath"),
								map.get("keyStorType"),
								map.get("trustStorPass"),
								map.get("keyStorPass"));
						if (ac == null) return;
					}
					AdminClient ac = mapConn.get(key); // ��ȡdmgr����
					Map<String, String> mapDmgr = PerfReader.self.qryDmgrInfo(ac); // ��ȡdmgr��Ϣ
					txtClusterAlias.setText(mapDmgr.get("cell").substring(0,
							mapDmgr.get("cell").indexOf("Cell"))); // ����dmgr��������������
				}
				@Override
				public void mouseExited(MouseEvent e) {
				}
				@Override
				public void mouseEntered(MouseEvent e) {
				}
				@Override
				public void mouseClicked(MouseEvent e) {
				}
			});
			jpButton.add(btnApply); // Ӧ��
			gbc.gridwidth = 1;
			gbc.weightx = 0.05;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpButton.getComponent(2), gbc);
			JButton btnConfirm = new JButton("ȷ��");
			btnConfirm.addMouseListener(new MouseListener() {

				@Override
				public void mouseReleased(MouseEvent e) {
				}

				@Override
				public void mousePressed(MouseEvent e) {
					JButton btn = (JButton) e.getComponent();
					if (SwingUtilities.isLeftMouseButton(e) == false
							|| !btn.isEnabled()) {
						return;
					}
					// ���÷���״̬
					state = true;
					// �رնԻ���
					dispose();
				}

				@Override
				public void mouseExited(MouseEvent e) {
				}

				@Override
				public void mouseEntered(MouseEvent e) {
				}

				@Override
				public void mouseClicked(MouseEvent e) {
				}
			});
			jpButton.add(btnConfirm);
			gbc.gridwidth = 1;
			gbc.weightx = 0.05;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpButton.getComponent(3), gbc);
			JButton btnCancel = new JButton("ȡ��");
			btnCancel.addMouseListener(new MouseListener() {
				
				@Override
				public void mouseReleased(MouseEvent e) {
				}
				
				@Override
				public void mousePressed(MouseEvent e) {
					JButton btn = (JButton) e.getComponent();
					if (SwingUtilities.isLeftMouseButton(e) == false
							|| !btn.isEnabled()) {
						return;
					}
					// ���÷���״̬
					state = false;
					// �رնԻ���
					dispose();
				}
				
				@Override
				public void mouseExited(MouseEvent e) {
				}
				
				@Override
				public void mouseEntered(MouseEvent e) {
				}
				
				@Override
				public void mouseClicked(MouseEvent e) {	
				}
			});
			jpButton.add(btnCancel);
			gbc.gridwidth = 0;
			gbc.weightx = 0.05;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpButton.getComponent(4), gbc);
			return jpButton;
		}

		public boolean getReturnStatus() {
			return state;
		}
		
		/**
		 * ����dmgr���ӶԻ���
		 */
		private DlgDmgrConfig(String category) {
			if (category != null && !category.equals("")) { // ��ϵͳ��������ǿ�,��Ҫ����ϵͳ������Ͽ�Ĭ��ֵ
				int cnt = cmbCategory.getItemCount();
				for (int i = 0; i < cnt; i++) {
					if (cmbCategory.getItemAt(i).equals(category)) {
						cmbCategory.setSelectedIndex(i);
						break;
					}
				}
			}
			self = this;
			setTitle("����µ�dmgr����");
			Dimension scrSz = Toolkit.getDefaultToolkit().getScreenSize();
			Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(
					getGraphicsConfiguration());
			setSize(scrSz.width * 3 / 4,
					(scrSz.height - screenInsets.bottom) / 3); // �Ի����С���߶�ȥ���������߶�
			setLocation(scrSz.width / 8,
					(scrSz.height - screenInsets.bottom) / 3);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // �رճ�����ֹ
			setModalityType(ModalityType.APPLICATION_MODAL); // �����ڲ�����
			setModal(true);
			setResizable(false); // ���ɵ�����С

			GridBagLayout gbl = new GridBagLayout(); // ����GridBag����
			JPanel jpMain = new JPanel(gbl); // ������

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.BOTH;
			gbc.ipadx = 20;
			gbc.ipady = 20;

			jpMain.add(getDmgrConfPanel()); // Ԫ�����
			gbc.gridwidth = 0;
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpMain.getComponent(0), gbc);
			jpMain.add(getButtonPanel()); // ��ť���
			gbc.gridwidth = 0;
			gbc.weightx = 1.0;
			gbc.weighty = 0.5;
			gbl.setConstraints(jpMain.getComponent(1), gbc);

			add(jpMain);
			setVisible(true);
			setResizable(false);
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		}
	}

	/**
	 * ��ർ����
	 * 
	 */
	private static class NaviPanel extends JPanel {
		private static final long serialVersionUID = 924411562858994237L;
		private IconNode root; // ���ڵ�
		private static JTree tree = null; // ������
		private static Set<Entry<String, Section>> dmgrs = null; // ���ص���������{[����IP,����������],...}
		private static Map<String, ThrReload> mapReloadThreads = new LinkedHashMap<String, ThrReload>(); // ˢ���߳�ӳ���<����IP, ˢ���߳�>
		private static Connection connDerby = null; // ���ֿܲ�����
		private JPopupMenu popMenu = null; // �����˵�
		
		public static void main(String[] args) {
			initDBConnection();
			initSchema();
			initTables();
			initConstraints();
		}
		
		/**
		 * ����host��ȡdmgr��������
		 * @param host
		 * @return
		 */
		private static Section getCfgByHost(String host) {
			Iterator<Entry<String, Section>> itr = dmgrs.iterator(); // ����dmgr��������
			while (itr.hasNext()) {
				Entry<String, Section> ent = itr.next();
				if (ent.getKey().equals(host)) { // �����뵱ǰ���ڵ�IDƥ��ļ�¼
					return ent.getValue();
				}
			}
			return null;
		}
		
		/**
		 * ��ʼ�����ֿܲ�����
		 */
		private static void initDBConnection() {
			try {
				if (connDerby != null && !connDerby.isClosed()) {
					return;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			String driver = "org.apache.derby.jdbc.ClientDriver";
			String url = "jdbc:derby://localhost:1527/perfDB;create=true";
			try {
				Class.forName(driver);
				connDerby = DriverManager.getConnection(url, "emma", "watson");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * ��ʼ��schema
		 */
		private static void initSchema() {
			ResultSet rs = null;
			boolean bSchema = false;
			try {
				rs = connDerby.getMetaData().getSchemas(); // ��Ԫ���ݻ�ȡ����schema
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				while (rs.next()) { // ��ѯschema
					if (rs.getString(1).toUpperCase().equals("PERF")) {
						bSchema = true;
						break;
					}
				}
				rs.close();
				rs = null;
				if (!bSchema) { // ���Ҳ���schema�򴴽�schema
					Statement stmt = connDerby.createStatement();
					stmt.execute("CREATE SCHEMA PERF AUTHORIZATION emma");
					connDerby.commit();
					stmt.close();
					stmt = null;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * ��ʼ����ṹ
		 */
		private static void initTables() {
			ResultSet rs = null;
			Statement stmt = null;
			try {
				stmt = connDerby.createStatement();
				rs = stmt
						.executeQuery("SELECT 1 FROM sys.systables t1, sys.sysschemas t2 "
								+ "WHERE t1.tablename = 'PERF_WEBSPHERE' AND t1.schemaid = t2.schemaid");
				connDerby.commit();
				if (!rs.next()) {
					stmt.execute("CREATE TABLE PERF.PERF_WEBSPHERE("
							+ "ID BIGINT NOT NULL,"
							+ "CREATE_TIME TIMESTAMP NOT NULL,"
							+ "PERF_DATA CLOB NOT NULL,"
							+ "CONSTRAINT PK_PERF_WEBSPHERE PRIMARY KEY (ID)"
							+ ")");
				}
				rs.close();
				rs = null;
				stmt.close();
				stmt = null;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * ��ʼ��Լ��
		 */
		private static void initConstraints() {
			ResultSet rs = null;
			Statement stmt = null;
			try {
				// �������/Ψһ����
				stmt = connDerby.createStatement();
				rs = stmt
						.executeQuery("SELECT 1 FROM sys.sysconstraints t1, sys.systables t2 "
								+ "WHERE t1.tableid = t2.tableid AND t1.constraintname = 'PK_PERF_WEBSPHERE' "
								+ "AND t1.type = 'P'");
				connDerby.commit();
				if (!rs.next()) {
					stmt.execute("ALTER TABLE perf.perf_websphere ADD CONSTRAINT PK_PERF_WEBSPHERE "
							+ "PRIMARY KEY (ID)");
				}
				rs.close();
				rs = null;
				stmt.close();
				stmt = null;
				// ���ʱ������
				stmt = connDerby.createStatement();
				rs = stmt
						.executeQuery("SELECT * FROM sys.sysconglomerates t1, sys.systables t2 "
								+ "WHERE t1.tableid = t2.tableid AND t2.tablename = 'PERF_WEBSPHERE' AND "
								+ "t1.conglomeratename = 'IDX_PERF_WEBSPHERE_TIME'");
				connDerby.commit();
				if (!rs.next()) {
					stmt.execute("CREATE INDEX IDX_PERF_WEBSPHERE_TIME ON PERF.PERF_WEBSPHERE("
							+ "CREATE_TIME desc)");
				}
				rs.close();
				rs = null;
				stmt.close();
				stmt = null;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * ��ʼ������
		 */
		private static void initSequence() {
			
		}
		
		/**
		 * �����ṹ
		 */
		private static void destroyTable() {
			try {
				Statement stmt = connDerby.createStatement();
				ResultSet rs = stmt
						.executeQuery("SELECT 1 FROM sys.systables t1, sys.sysschemas t2 "
								+ "WHERE t1.tablename = 'PERF_WEBSPHERE' AND t1.schemaid = t2.schemaid AND "
								+ "t2.schemaname = 'PERF'");
				connDerby.commit();
				if (rs.next()) { // ��������ɾ��
					stmt.execute("DROP TABLE PERF.PERF_WEBSPHERE");
				}
				rs.close();
				rs = null;
				stmt.close();
				stmt = null;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * ����schema
		 */
		private static void destroySchema() {
			try {
				Statement stmt = connDerby.createStatement();
				ResultSet rs = stmt
						.executeQuery("SELECT 1 FROM sys.sysschemas WHERE schemaname = 'PERF'");
				connDerby.commit();
				if (rs.next()) {
					stmt.execute("DROP SCHEMA PERF RESTRICT");
				}
				rs.close();
				rs = null;
				stmt.close();
				stmt = null;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		/**
		 * �����ݿ��ȡ���µ���������
		 * @return
		 */
//		private static List<Map<String, String>> getLatestPerfDataFromDB() {
//			try {
//				if (connDerby == null || connDerby.isClosed()) initDBConnection();
//				if (connDerby == null || connDerby.isClosed()) return null;
//			} catch (SQLException e) {
//				e.printStackTrace();
//			}
//			Statement stmt = connDerby.createStatement();
//			ResultSet rs = stmt.executeQuery("SELECT perf_data FROM app.perf_websphere WHERE ");
//			return null;
//		}
		
		private static void writePerfData2DB(List<Map<String, String>> data) {
			if (data == null) return;
			String serializedData = JSON.toJSONString(data, true);
			
		}
		
		/**
		 * ��ȡ���ʼ��AdminClient����
		 * @param host
		 * @param sect
		 * @return
		 */
		private static AdminClient getOrInitDmgrConnections(String host,
				Section sect) {
			if (host == null || sect == null)
				return null; // ���Ҳ�������,���˳�
			if (!mapConn.containsKey(host + ":" + sect.get("port"))) { // ����dmgr����ӳ����в�����,�����µ�����,����¼������ӳ���
				AdminClient ac = PerfReader.self.createConnection(host,
						sect.get("port"), sect.get("connTyp"),
						sect.get("ifSec"), sect.get("userName"),
						sect.get("password"), sect.get("trustStorPath"),
						sect.get("keyStorPath"), sect.get("keyStorType"),
						sect.get("trustStorPass"), sect.get("keyStorPass"));
				if (ac == null)
					return null;
			}
			return mapConn.get(host + ":" + sect.get("port")); // ��ȡdmgr����
		}
		
		/**
		 * ��dmgr��ȡ������������
		 * @param host
		 * @param sect
		 * @return
		 */
		private static List<Map<String, String>> getPerfDataFromDmgr(
				AdminClient ac, String host, Section sect) {
			List<Map<String, Object>> list = PerfReader.self.qrySrvPerf(ac); // ��ѯ��������
			List<Map<String, String>> listFormat = new ArrayList<Map<String, String>>();
			for (Map<String, Object> map : list) {
				String json = JsonUtil.toJson(map);
				Map<String, String> m = PerfReader.self.formatSrvPerf(json,
						MOD_FULL);
				listFormat.add(m);
			}
			return listFormat;
		}
		
		private static class ThrReload implements Runnable {
			private String host = null; // ����IP
			private int interval = 0; // ˢ��ʱ����

			private ThrReload(String para0, int para1) {
				host = para0;
				interval = para1;
			}

			@Override
			public void run() {
				while (true) {
					if (interval != 0) { // ������ʱ����
						// 1.��derby��ѯ��������
						// 2.��δ�ҵ�,��ѯdmgr,ϵ�л�,д��derby
						// 3.
						try {
							Thread.sleep(interval * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}

		}
		
		/**
		 * ������: ����keyֵ����
		 * @author heqiming
		 *
		 */
		private static class SortByKey implements Comparator<Entry<String, Section>> {

			@Override
			public int compare(Entry<String, Section> o1, Entry<String, Section> o2) {
				return o1.getKey().compareTo(o2.getKey());
			}
		}

		/**
		 * �������ļ���������
		 * @throws InvalidFileFormatException
		 * @throws IOException
		 */
		private void loadConfiguration(String fileName) { // ��������
			String folder = getClass().getClassLoader().getResource("").getFile();
			String path = new File(folder).getAbsolutePath() + "\\" + fileName;
			File file = new File(path);
			try {
				if (!file.exists() && !file.createNewFile()) return;
			} catch (IOException e1) {
				logger.severe(e1.getMessage());
			}
			Config cfg = new Config();
			cfg.setMultiSection(false);
			Ini ini = new Ini();
			ini.setConfig(cfg);
			try {
				ini.load(file);
			} catch (InvalidFileFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			List<Entry<String, Section>> listDmgrs = new ArrayList<Entry<String, Section>>(
					ini.entrySet()); // ׼������
			if (listDmgrs.size() >= 2)
				Collections.sort(listDmgrs, new SortByKey());
			if (dmgrs == null) {
				dmgrs = new LinkedHashSet<Entry<String, Section>>();
			}
			dmgrs.addAll(listDmgrs);
		}
		
		/**
		 * �������õ������ļ�
		 * @param fileName
		 * @param mapCnf
		 */
		private void saveDmgrConfigs(String fileName, Map<String, String> mapCnf) {
			String folder = getClass().getClassLoader().getResource("").getFile();
			String path = new File(folder).getAbsolutePath() + "\\" + fileName;
			File file = new File(path);
			try {
				if (!file.exists() && !file.createNewFile()) return;
			} catch (IOException e1) {
				logger.severe(e1.getMessage());
			}
			Config cfg = new Config();
			cfg.setMultiSection(false);
			Ini ini = new Ini();
			ini.setConfig(cfg);
			try {
				ini.load(file); // �����ļ�
			} catch (InvalidFileFormatException e2) {
				logger.severe(e2.getMessage());
			} catch (IOException e2) {
				logger.severe(e2.getMessage());
			}
			String host = mapCnf.get("host");
			ini.put(host, "port", mapCnf.get("port"));
			ini.put(host, "connTyp", mapCnf.get("connTyp"));
			ini.put(host, "ifSec", mapCnf.get("ifSec"));
			ini.put(host, "userName", mapCnf.get("userName"));
			ini.put(host, "password", mapCnf.get("password"));
			ini.put(host, "trustStorPath", mapCnf.get("trustStorPath"));
			ini.put(host, "keyStorPath", mapCnf.get("keyStorPath"));
			ini.put(host, "keyStorType", mapCnf.get("keyStorType"));
			ini.put(host, "trustStorPass", mapCnf.get("trustStorPass"));
			ini.put(host, "keyStorPass", mapCnf.get("keyStorPass"));
			ini.put(host, "category", mapCnf.get("category"));
			ini.put(host, "clusterAlias", mapCnf.get("clusterAlias"));
			ini.put(host, "reloadInterval", mapCnf.get("reloadInterval"));
			try {
				ini.store(file); // �����ļ�
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			ini.clear();
		}
		
		/**
		 * ɾ���������
		 * @param fileName
		 * @param host
		 */
		private void removeDmgrConfigByHost(String fileName, String host) {
			String folder = getClass().getClassLoader().getResource("").getFile();
			String path = new File(folder).getAbsolutePath() + "\\" + fileName;
			File file = new File(path);
			try {
				if (!file.exists() && !file.createNewFile()) return;
			} catch (IOException e1) {
				logger.severe(e1.getMessage());
			}
			Config cfg = new Config();
			cfg.setMultiSection(false);
			Ini ini = new Ini();
			ini.setConfig(cfg);
			try {
				ini.load(file);
			} catch (InvalidFileFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			ini.remove(host);
			try {
				ini.store(file); // �����ļ�
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			ini.clear();
		}
		
		/**
		 * ��ʼ���Ҽ��˵�
		 */
		private void initPopupMenu() {
			JMenuItem addItem = new JMenuItem("��Ӳ��������");
			addItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					IconNode currNode = (IconNode) tree
							.getLastSelectedPathComponent(); // ��ȡ��ǰ�����Ľڵ�
					String currCategory = (String) (currNode.getParent() != null
							&& currNode.getParent().equals(root) ? currNode
							.getText() : null); // ����ǰ�ڵ�ĸ��ڵ���root,��ǰ�ڵ�Ϊϵͳ���ڵ�
					DlgDmgrConfig ddc = new DlgDmgrConfig(currCategory); // ��ʾ���dmgr����Ի���
					if (!ddc.getReturnStatus()) return; // ����������
					Map<String, String> mapCnf = ddc.getDmgrConfigurations(); // ��ȡ����
					String key = mapCnf.get("host") + ":" + mapCnf.get("port");
					if (!mapConn.containsKey(key)) { // ����dmgr����ӳ����в�����,�����µ�����,����¼������ӳ���
						AdminClient ac = PerfReader.self.createConnection(
								mapCnf.get("host"), mapCnf.get("port"),
								mapCnf.get("connTyp"), mapCnf.get("ifSec"),
								mapCnf.get("userName"), mapCnf.get("password"),
								mapCnf.get("trustStorPath"),
								mapCnf.get("keyStorPath"),
								mapCnf.get("keyStorType"),
								mapCnf.get("trustStorPass"),
								mapCnf.get("keyStorPass"));
						if (ac == null) return;
					}
					AdminClient ac = mapConn.get(key); // ��ȡdmgr����
					try { // ���Ự�������˳�
						if (ac.isAlive().getSessionId() == null) return;
					} catch (ConnectorException e2) {
						logger.severe(e2.getMessage());
					}
					if (mapCnf.get("clusterAlias").equals("")) { // ϵͳ������Ŀ¼��ʶ,����������˵��û�а�Ӧ��,ֱ��ȷ����
						Map<String, String> map = PerfReader.self
								.qryDmgrInfo(ac); // ��ȡdmgr��Ϣ
						mapCnf.put(
								"clusterAlias",
								map.get("cell").substring(0,
										map.get("cell").indexOf("Cell")));
					}
					// ׼����tree��ӽڵ�
					String category = mapCnf.get("category"); // ϵͳ���
					String dmgrId = mapCnf.get("clusterAlias") + "_" + mapCnf.get("host"); // �ڽڵ�����ʾ��dmgr��ʶ
					IconNode nodeCat = null; // ����ȡ/������ϵͳ���ڵ�
					int cateNum = root.getChildCount(); // Ŀǰ��������ʾ��ϵͳ�������
					for (int i = 0; i < cateNum; i++) {
						IconNode child = (IconNode) root.getChildAt(i);
						if (child.getText().equals(category)) {
							nodeCat = child; // �ҵ��Ѵ��ڵ�ϵͳ���ڵ�
							break;
						}
					}
					if (nodeCat == null) { // ��δ�ҵ�,���´������ڵ�,����ӵ���
						nodeCat = new IconNode(new ImageIcon(getClass().getResource("Closed_Folder.png")), category);
						root.add(nodeCat);
					}
					int dmgrNum = nodeCat.getChildCount(); // ��ǰϵͳ������Ѿ�չʾ��dmgr�ڵ�����
					for (int i = 0; i < dmgrNum; i++) {
						IconNode child = (IconNode) nodeCat.getChildAt(i);
						if (child.getText().equals(dmgrId)) {
							return; // ����ǰdmgr�ڵ��Ѵ������˳�
						}
					}
					IconNode nodeDmgr = new IconNode(new ImageIcon(getClass().getResource("WebSphere.png")), dmgrId); // ����dmgr����ڵ�
					nodeCat.add(nodeDmgr);
					tree.updateUI(); // ˢ����
					expandAll(new TreePath(root), true); // ȫ��չ��
					// �������ļ����������
					saveDmgrConfigs("dmgr-config.ini", mapCnf); // ���������ļ�
					// ���¼�������dmgrs
					loadConfiguration("dmgr-config.ini"); // ���¼��������ļ�
				}
			});
			JMenuItem modItem = new JMenuItem("�޸�");
			JMenuItem delItem = new JMenuItem("ɾ��");
			delItem.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					// �������ڵ�
					IconNode currNode = (IconNode) tree
							.getLastSelectedPathComponent();
					IconNode parentNode = (IconNode) currNode.getParent();
					parentNode.remove(currNode);
					expandAll(new TreePath(root), true); // ȫ��չ��
					// �����ڵ��޺��ӽڵ�,�򽫸��ڵ����
					if (parentNode.getChildCount() == 0 && !parentNode.equals(root)) {
						root.remove(parentNode);
					}
					// ˢ����
					tree.updateUI();
					// �������ļ���ɾ��
					String dmgrId = (String) currNode.getText();
					String host = dmgrId.split("_")[1];
					removeDmgrConfigByHost("dmgr-config.ini", host);
					// ����dmgrs
					Iterator<Entry<String, Section>> itr = dmgrs.iterator();
					String port = null;
					while (itr.hasNext()) {
						Entry<String, Section> entry = itr.next();
						if (entry.getKey().equals(host)) {
							port = entry.getValue().get("port");
							dmgrs.remove(entry);
							break;
						}
					}
					// ����mapConn
					if (mapConn.containsKey(host + ":" + port)) {
						mapConn.remove(host + ":" + port);
					}
				}
			});
			popMenu = new JPopupMenu();
			popMenu.add(addItem);
			popMenu.add(modItem);
			popMenu.add(delItem);
			popMenu.addSeparator();
			JMenuItem subNetworkItem = new JMenuItem("�������");
			subNetworkItem.setEnabled(false);
			popMenu.add(subNetworkItem);
		}
		
		/**
		 * չ��/����������
		 * @param parent
		 * @param ifExpand
		 */
		private void expandAll(TreePath parent, boolean ifExpand) {
			TreeNode node = (TreeNode) parent.getLastPathComponent();
			for (Enumeration<?> e = node.children(); e.hasMoreElements();) {
				TreeNode n = (TreeNode) e.nextElement();
				TreePath path = parent.pathByAddingChild(n);
				expandAll(path, ifExpand);
			}
			if (ifExpand) {
				tree.expandPath(parent);
			} else {
				tree.collapsePath(parent);
			}
		}

		private NaviPanel() {
			root = new IconNode(new ImageIcon(getClass().getResource(
					"whole_network.png")), "ȫ��");
			tree = new JTree(root); // ������
			tree.setForeground(Color.LIGHT_GRAY); // ǰ��ɫ
			tree.setBackground(Color.LIGHT_GRAY); // ����ɫ
			tree.getSelectionModel().setSelectionMode( // ����������ѡ
					TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
			tree.setCellRenderer(new IconNodeRenderer()); // ������Ⱦ��
			tree.setEditable(false); // �����ɱ༭
			tree.setRootVisible(true); // ���ڵ�ɼ�
			tree.setToggleClickCount(1); // ���õ���1�μ�չ��/����
			loadConfiguration("dmgr-config.ini"); // �������õ�dmgrs
			for (Entry<String, Section> dmgr : dmgrs) { // �������õ���
				String dmgrId = dmgr.getValue().get("clusterAlias") + "_" + dmgr.getKey(); // ��װ���ڵ�ID
				String category = dmgr.getValue().get("category"); // ��ȡϵͳ���
				IconNode nodeCat = null; // ����ȡ/������ϵͳ���ڵ�
				int cateNum = root.getChildCount(); // Ŀǰ��������ʾ��ϵͳ�������
				for (int i = 0; i < cateNum; i++) { // ���ж����ӽڵ�(ϵͳ���)���������ȡ������ƥ�����
					IconNode child = (IconNode) root.getChildAt(i);
					if (child.getText().equals(category)) {
						nodeCat = child; // �ҵ��Ѵ��ڵ�ϵͳ���ڵ�
						break;
					}
				}
				if (nodeCat == null) { // ��δ�ҵ�,���´������ڵ�,����ӵ���
					nodeCat = new IconNode(new ImageIcon(getClass()
							.getResource("Closed_Folder.png")), category);
					root.add(nodeCat);
				}
				int dmgrNum = nodeCat.getChildCount(); // ��ǰϵͳ������Ѿ�չʾ��dmgr�ڵ�����
				for (int i = 0; i < dmgrNum; i++) { // �Ե�ǰϵͳ���������dmgr�ڵ���dmgrIdƥ��,��ƥ�䵽�򲻽��к�������
					IconNode child = (IconNode) nodeCat.getChildAt(i);
					if (child.getText().equals(dmgrId)) {
						return;
					}
				}
				IconNode nodeDmgr = new IconNode(new ImageIcon(getClass()
						.getResource("WebSphere.png")), dmgrId); // ����dmgr����ڵ�
				nodeCat.add(nodeDmgr); // ��ӽڵ�
			}
			initPopupMenu(); // ��ʼ�������˵�
			tree.addMouseListener(new MouseAdapter() { // ��Ӧ��궯��
				public void mousePressed(MouseEvent e) {
					if (e.getComponent().isEnabled() == false) { // JTree״̬�������
						return;
					}
					TreePath path = tree.getPathForLocation(e.getX(), e.getY()); // ��ȡ��ǰ�ڵ�·��
					if (path == null) return;
					tree.setSelectionPath(path); // ѡ�е�ǰ�ڵ�
					IconNode currNode = (IconNode) tree.getLastSelectedPathComponent();
					if (currNode.equals(root)) { // ���ڵ�ֻ����ӳ�Ա
						popMenu.getComponent(0).setEnabled(true);
						popMenu.getComponent(1).setEnabled(false);
						popMenu.getComponent(2).setEnabled(false);
						popMenu.getComponent(3).setEnabled(false);
					} else {
						if (((IconNode) currNode.getParent()).equals(root)) { // �����ڵ�
							popMenu.getComponent(0).setEnabled(true);
							popMenu.getComponent(1).setEnabled(false);
							popMenu.getComponent(2).setEnabled(false);
							popMenu.getComponent(3).setEnabled(false);
						} else {
							if (currNode.isLeaf()) { // Dmgr����ڵ�
								popMenu.getComponent(0).setEnabled(false);
								popMenu.getComponent(1).setEnabled(true);
								popMenu.getComponent(2).setEnabled(true);
								popMenu.getComponent(3).setEnabled(false);
							}
						}
					}

					if (SwingUtilities.isRightMouseButton(e)) popMenu.show(e.getComponent(), e.getX(), e.getY());
					if (SwingUtilities.isLeftMouseButton(e)) { // �������չʾ��ǰ�ڵ��Ӧ��dmgr������������
						if (currNode.equals(root)
								|| currNode.getParent().equals(root))
							return; // ���ڸ��ڵ��ϵͳ���ͽڵ㲻��Ӧ
						String host = ((String) currNode.getText()).split("_")[1]; // ��ǰ���ڵ��ID������IP
						Section sect = null;
						Iterator<Entry<String, Section>> itr = dmgrs.iterator(); // ����dmgr��������
						while (itr.hasNext()) {
							Entry<String, Section> ent = itr.next();
							if (ent.getKey().equals(host)) { // �����뵱ǰ���ڵ�IDƥ��ļ�¼
								sect = ent.getValue();
								break;
							}
						}
						if (sect == null) return; // ���Ҳ�������,���˳�
						String connId = host + ":" + sect.get("port"); // <host:port, conn>
						if (!mapConn.containsKey(connId)) { // ����dmgr����ӳ����в�����,�����µ�����,����¼������ӳ���
							AdminClient ac = PerfReader.self.createConnection(
									host, sect.get("port"),
									sect.get("connTyp"), sect.get("ifSec"),
									sect.get("userName"), sect.get("password"),
									sect.get("trustStorPath"),
									sect.get("keyStorPath"),
									sect.get("keyStorType"),
									sect.get("trustStorPass"),
									sect.get("keyStorPass"));
							if (ac == null) return;
						}
						AdminClient ac = mapConn.get(connId); // ��ȡdmgr����
						List<Map<String, Object>> list = PerfReader.self.qrySrvPerf(ac); // ��ѯ��������
						List<Map<String, String>> listFormat = new ArrayList<Map<String, String>>();
						for (Map<String, Object> map : list) {
							String json = JsonUtil.toJson(map);
							Map<String, String> m = PerfReader.self.formatSrvPerf(json, MOD_FULL);
							listFormat.add(m);
						}
						PerfPanel.self.load(listFormat);
					}
				}
			});

			JScrollPane pane = new JScrollPane(tree);
			add(pane, BorderLayout.NORTH);
//			tree.updateUI();
//			tree.repaint();
//			renderTree(); // ��Ⱦ��
			expandAll(new TreePath(root), true);
			setVisible(true);
			setLayout(new GridLayout(1, 3, 5, 5));
			setBackground(Color.LIGHT_GRAY);
		}
	}

	/**
	 * ����ָ��������
	 * 
	 */
	private static class PerfPanel extends JPanel {
		private static final long serialVersionUID = -1737823149753250108L;
		private String columnNames[] = { "����ָ��" }; // �б���
		private static JTable table = null; // ���������б�
		private static JTable tableJDBC = null; // JDBC�����б�
		private static PerfPanel self = null;
		
		/**
		 * �����Զ���Բ�Ǳ߿�
		 * @author Hugh
		 *
		 */
		public class MyLineBorder extends LineBorder {
			private static final long serialVersionUID = -5675849187436999387L;

			public MyLineBorder(Color color, int thickness,
					boolean roundedCorners) {
				super(color, thickness, roundedCorners);
			}

			public void paintBorder(Component c, Graphics g, int x, int y,
					int width, int height) {

				RenderingHints rh = new RenderingHints(
						RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				Color oldColor = g.getColor();
				Graphics2D g2 = (Graphics2D) g;
				int i;
				g2.setRenderingHints(rh);
				g2.setColor(lineColor);
				for (i = 0; i < thickness; i++) {
					if (!roundedCorners)
						g2.drawRect(x + i, y + i, width - i - i - 1, height - i
								- i - 1);
					else
						g2.drawRoundRect(x + i, y + i, width - i - i - 1,
								height - i - i - 1, 10, 10);
				}
				g2.setColor(oldColor);
			}
		}

		private PerfPanel() {
			/* ���������б� */
			Object data[][] = new Object[1][1]; // ��ʼ״̬
			if (table == null)
				table = new JTable(data, columnNames);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table.setAutoscrolls(true);
			table.setBackground(Color.LIGHT_GRAY);
			Font font = new Font(table.getFont().getName(), table.getFont().getStyle(),
					table.getFont().getSize() - 2); // ����������С
			table.setFont(font);
			table.setRowHeight(table.getRowHeight() - 2); // �����и�
			font = null;
			
			/* ���ݿ����ӳ������б� */
			if (tableJDBC == null)
				tableJDBC = new JTable(data, columnNames);
			tableJDBC.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			tableJDBC.setAutoscrolls(true);
			tableJDBC.setBackground(Color.LIGHT_GRAY);
			font = new Font(tableJDBC.getFont().getName(), tableJDBC.getFont().getStyle(),
					tableJDBC.getFont().getSize() - 2); // ����������С
			tableJDBC.setFont(font);
			tableJDBC.setRowHeight(tableJDBC.getRowHeight() - 2); // �����и�
			
			/* �������·ָ */
			JSplitPane vSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT); // ��ֱ�ָ������Tabҳչ��
			vSplitPane.setDividerLocation(PerfReader.self.getSize().height / 2); // �ָ����
			vSplitPane.setBackground(Color.gray);
			JTabbedPane tabbedUp = new JTabbedPane(); // ��ѡ�
			tabbedUp.addTab("���������б�", new JScrollPane(table));
			JTabbedPane tabbedDn = new JTabbedPane(); // ��ѡ�
			tabbedDn.addTab("���ݿ����ӳ������б�", new JScrollPane(tableJDBC));
			vSplitPane.setLeftComponent(tabbedUp); // ��ѡ�
			vSplitPane.setRightComponent(tabbedDn); // �Ҳ�����ָ���б�
			
			/* ���ָ��ӵ���ǰ������� */
			setVisible(true);
			add(vSplitPane, BorderLayout.NORTH); // ��ӷָ���
			setLayout(new GridLayout(1, 1, 1, 1));
			self = this;
		}
		
		/**
		 * ɾ��table��������,�൱������table
		 */
		private void removeAllColumns() {
			TableColumnModel tcm = table.getColumnModel();
			while (tcm.getColumnCount() > 0) {
				tcm.removeColumn(tcm.getColumn(0));
			}
		}

		/**
		 * ���������ݼ��ص�JTable
		 * @param data
		 * @throws IllegalArgumentException
		 * @throws SecurityException
		 * @throws IllegalAccessException
		 * @throws NoSuchFieldException
		 */
		public void load(List<Map<String, String>> data) {
			if (data == null || data.size() == 0) // ���ݺϷ��Լ��
				return;
			removeAllColumns(); // ���������,��������JTable
			/*  */
			Set<String> setKern = new LinkedHashSet<String>(); // ���Ĳ���
			Set<String> setJDBC = new LinkedHashSet<String>(); // JDBC����
			for (Map<String, String> each : data) { // ���м�¼�ļ�ֵ���ܲ�ͬ
				Set<String> setKernEach = new LinkedHashSet<String>();
				Set<String> setJDBCEach = new LinkedHashSet<String>();
				Set<String> setCol = each.keySet();
				for (String col : setCol) {
					if (col.equals("�ڵ���") || col.equals("��������")) {
						setKernEach.add(col);
						setJDBCEach.add(col);
					} else {
						if (col.indexOf("_jdbc_pool") != -1) setJDBCEach.add(col.substring(0, col.indexOf("_jdbc_pool")));
						else setKernEach.add(col);
					}
				}
				setKern.addAll(setKernEach); // �кϲ�
				setJDBC.addAll(setJDBCEach);
			}

			Object[] colNamesKern = setKern.toArray();
			Object[] colNamesJDBC = setJDBC.toArray();
			Object[][] valuesKern = new Object[data.size()][colNamesKern.length];
			Object[][] valuesJDBC = new Object[data.size()][colNamesJDBC.length];
			for (int row = 0; row < data.size(); row++) {
				for (int col = 0; col < colNamesKern.length; col++) {
					valuesKern[row][col] = data.get(row).get(colNamesKern[col]);
				}
				for (int col = 0; col < colNamesJDBC.length; col++) {
					if (colNamesJDBC[col].equals("�ڵ���") || colNamesJDBC[col].equals("��������"))
						valuesJDBC[row][col] = data.get(row).get(colNamesJDBC[col]);
					else
						valuesJDBC[row][col] = data.get(row).get(colNamesJDBC[col] + "_jdbc_pool");
				}
			}
			DefaultTableModel modelKern = new DefaultTableModel(valuesKern, colNamesKern);
			DefaultTableModel modelJDBC = new DefaultTableModel(valuesJDBC, colNamesJDBC);
			table.setModel(modelKern);
			tableJDBC.setModel(modelJDBC);
			fitTableColumns(table); // �����п�
			fitTableColumns(tableJDBC);
			highLightKernal(); // �����쳣ָ��
			highLightJDBC();
		}
		
		/**
		 * �����쳣ָ��(����ָ��)
		 */
		public void highLightKernal() {
			
			DefaultTableCellRenderer highLight = new DefaultTableCellRenderer() {
				private static final long serialVersionUID = -8848303924674335966L;

				public void setValue(Object value) { // ��дsetValue�������Ӷ����Զ�̬�����е�Ԫ������ɫ
					if (value == null || value.equals("")) { // ����δ��ȡ��
						setForeground(Color.RED);
						MyLineBorder myLineBorder = new MyLineBorder(Color.RED,
								1, true);
						setBorder(myLineBorder);
						setText("�����쳣");
						return;
					}
					int perc = 0;
					try {
						perc = Integer.parseInt(((String) value).replace(" %",
							""));
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (perc > 90) {
						setForeground(Color.RED);
						MyLineBorder myLineBorder = new MyLineBorder(Color.RED,
								1, true);
						setBorder(myLineBorder);

					} else {
						setForeground(Color.BLACK);
					}
					setText(value.toString());
				}
			};
			TableColumn cpuCol = table.getColumn("CPUʹ��");
			cpuCol.setCellRenderer(highLight);

			TableColumn sessCol = table.getColumn("��ʹ����");
			sessCol.setCellRenderer(highLight);

			TableColumn wcCol = table.getColumn("WC������");
			wcCol.setCellRenderer(highLight);
		}
		
		/**
		 * �����쳣ָ��(JDBCָ��)
		 */
		public void highLightJDBC() {
			
			DefaultTableCellRenderer highLight = new DefaultTableCellRenderer() {
				private static final long serialVersionUID = -8848303924674335966L;

				public void setValue(Object value) { // ��дsetValue�������Ӷ����Զ�̬�����е�Ԫ������ɫ
					if (value == null || value.equals("")) { // ����δ��ȡ��
						setForeground(Color.RED);
						MyLineBorder myLineBorder = new MyLineBorder(Color.RED,
								1, true);
						setBorder(myLineBorder);
						setText("�����쳣");
						return;
					}
					int perc = 0;
					try {
						if (((String) value).indexOf("%") > 0) {
							perc = Integer.parseInt(((String) value).split("%")[0]);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (perc > 90) {
						setForeground(Color.RED);
						MyLineBorder myLineBorder = new MyLineBorder(Color.RED,
								1, true);
						setBorder(myLineBorder);
					} else {
						setForeground(Color.BLACK);
					}
					setText(value.toString());
				}
			};
			int colNum = tableJDBC.getColumnCount();
			for (int i = 0; i < colNum; i++) {
				String colName = tableJDBC.getColumnName(i);
				if (!colName.equals("�ڵ���")
						&& colName.equals("��������")) {
					tableJDBC.getColumn(colName).setCellRenderer(highLight);
				}
			}
		}

		/**
		 * �Զ������п�
		 * 
		 * @param myTable
		 */
		public void fitTableColumns(JTable myTable) {
			JTableHeader header = myTable.getTableHeader();
			int rowCount = myTable.getRowCount();
			Enumeration<TableColumn> columns = myTable.getColumnModel()
					.getColumns();
			while (columns.hasMoreElements()) {
				TableColumn column = (TableColumn) columns.nextElement();
				int col = header.getColumnModel().getColumnIndex(
						column.getIdentifier());
				int width = (int) myTable
						.getTableHeader()
						.getDefaultRenderer()
						.getTableCellRendererComponent(myTable,
								column.getIdentifier(), false, false, -1, col)
						.getPreferredSize().getWidth();
				for (int row = 0; row < rowCount; row++) {
					int preferedWidth = (int) myTable
							.getCellRenderer(row, col)
							.getTableCellRendererComponent(myTable,
									myTable.getValueAt(row, col), false, false,
									row, col).getPreferredSize().getWidth();
					width = Math.max(width, preferedWidth);
				}
				header.setResizingColumn(column);
				column.setWidth(width + myTable.getIntercellSpacing().width);
			}
		}
	}
	
	private static void log(Level l, Exception e) {
		logger.logrb(
				l,
				e.getClass().getCanonicalName(),
				e.getStackTrace()[1].getMethodName(),
				e.getStackTrace()[1].getFileName()
						+ "["
						+ Integer.toString(e.getStackTrace()[1]
								.getLineNumber()) + "]", e.getMessage());
	}
	
	/**
	 * �ж϶˿��Ƿ�ռ��
	 * @param host
	 * @param port
	 * @return
	 */
	private static boolean isPortUsing(String host, int port) {
		boolean flag = false;
		try {
			SocketAddress isa = new InetSocketAddress(host, port);
			Socket sock = new Socket();
			sock.connect(isa, 3000);
			InputStream is = sock.getInputStream();
			is.close();
			is = null;
			sock.close();
			sock = null;
			flag = true;
		} catch (UnknownHostException e) {
			log(Level.SEVERE, e);
		} catch (IOException e) {
			log(Level.SEVERE, e);
		}
		return flag;
	}

	/**
	 * ������dmgr����
	 * 1.Ĭ�ϲ��ð�ȫSOAPЭ��
	 * 2.dmgr��¼�û�������Ĭ��
	 * 3.SSL֤�顢��Կ��Ĭ��·���ڵ�ǰ��·����
	 * @param host
	 * @param port
	 * @param connTyp
	 * @param ifSec
	 * @param userName
	 * @param password
	 * @param trustStorPath
	 * @param keyStorPath
	 * @param keyStorType
	 * @param trustStorPass
	 * @param keyStorPass
	 * @return
	 */
	private AdminClient createConnection(String host, String port,
			String connTyp, String ifSec, String userName, String password,
			String trustStorPath, String keyStorPath, String keyStorType,
			String trustStorPass, String keyStorPass) {
		PerfReader.self.setTitle(PerfReader.self.getTitle() + " ����������..."); // ������״̬
		if (!isPortUsing(host, Integer.parseInt(port))) {
			PerfReader.self.setTitle(PerfReader.self.getTitle().replace(" ����������...", "")); // ������״̬
			return null;
		}
		final Properties properties = new Properties();
		properties.setProperty("host", host);
		properties.setProperty("port", port);
		properties.setProperty("type", connTyp == null ? "SOAP" : connTyp);
		properties.setProperty("securityEnabled", ifSec == null ? "true" : ifSec);
		properties.setProperty("username", userName == null ? "wasadmin"
				: userName);
		properties.setProperty("password", password == null ? "WebJ2ee"
				: password);
		if (trustStorPath == null) { // ��֤���ļ�·�����������Ĭ����·���²���
			URL urlCert = getClass().getClassLoader().getResource("DummyClientTrustFile.jks");
			if (urlCert == null) { // �ļ�URL��ȡʧ��
				URL urlFolder = getClass().getClassLoader().getResource("");
				String folder = new File(urlFolder.getFile()).getAbsolutePath();
				logger.severe("��" + folder + "���Ҳ���֤���ļ�.");
			}
			File fCert = new File(urlCert.getFile());
			trustStorPath = fCert.getAbsolutePath();
		}
		properties.setProperty("javax.net.ssl.trustStore", trustStorPath);
		if (keyStorPath == null) { // ��֤���ļ�·�����������Ĭ����·���²���
			URL urlKey = getClass().getClassLoader().getResource("DummyClientKeyFile.jks");
			if (urlKey == null) { // �ļ�URL��ȡʧ��
				URL urlFolder = getClass().getClassLoader().getResource("");
				String folder = new File(urlFolder.getFile()).getAbsolutePath();
				logger.severe("��" + folder + "���Ҳ�����Կ���ļ�.");
			}
			File fKey = new File(urlKey.getFile());
			keyStorPath = fKey.getAbsolutePath();
		}
		properties.setProperty("javax.net.ssl.keyStore", keyStorPath);
		properties.setProperty("javax.net.ssl.keyStoreType",
				keyStorType == null ? "JKS" : keyStorType);
		properties.setProperty("javax.net.ssl.trustStorePassword",
				trustStorPass == null ? "WebAS" : trustStorPass);
		properties.setProperty("javax.net.ssl.keyStorePassword",
				keyStorPass == null ? "WebAS" : keyStorPass);
		try {
			AdminClient ac = AdminClientFactory.createAdminClient(properties); // ��������
			if (ac != null) { // ���ӳɹ������浽����Ӱ���
				mapConn.put(properties.getProperty("host") + ":"
						+ properties.getProperty("port"), ac);
			}
		} catch (ConnectorException e) {
			e.printStackTrace();
		}
		String key = host + ":" + port;
		AdminClient ac = mapConn.get(key);
		if (ac == null) {
			logger.severe("����dmgr����ʧ��.");
		}
		PerfReader.self.setTitle(PerfReader.self.getTitle().replace(" ����������...", "")); // ������״̬
		return ac;
	}
	
	/**
	 * ��ȡdmgr������Ϣ
	 * @param ac
	 * @return
	 */
	private Map<String, String> qryDmgrInfo(AdminClient ac) {
		PerfReader.self.setTitle(PerfReader.self.getTitle() + " ��ȡ��..."); // ������״̬
		Map<String, String> map = new HashMap<String, String>();
		Set<?> set = null;
		try { // ��ѯdmgr���������
			set = ac.queryNames(new ObjectName(
					"WebSphere:type=Server,processType=DeploymentManager,*"),
					null);
		} catch (MalformedObjectNameException e) {
			logger.severe(e.getMessage());
		} catch (ConnectorException e) {
			logger.severe(e.getMessage());
		} catch (NullPointerException e) {
			logger.severe(e.getMessage());
		}
		if (set == null) {
			logger.severe("δ��ѯ��Dmgr���������.");
			return null;
		}
		Iterator<?> itr = set.iterator(); // ��������Server
		if (itr.hasNext()) {
			Object obj = itr.next();
			ObjectName objName = null;
			if (obj instanceof ObjectName)
				objName = (ObjectName) obj; // ��ȡMBean������
			if (objName == null)
				return null;
			map.put("node", objName.getKeyProperty("node"));
			map.put("version", objName.getKeyProperty("version"));
			map.put("cell", objName.getKeyProperty("cell"));
		}
		PerfReader.self.setTitle(PerfReader.self.getTitle().replace(" ��ȡ��...", "")); // ������״̬
		return map;
	}

	/**
	 * ��ѯdmgr�ϲ�������з���ĸ�ģ���������ݱ���
	 * 
	 * @param ac
	 * @throws ConnectorException
	 * @throws MalformedObjectNameException
	 * @throws NullPointerException
	 * @throws InstanceNotFoundException
	 * @throws MBeanException
	 * @throws ReflectionException
	 */
	private List<Map<String, Object>> qrySrvPerf(AdminClient ac) {
		PerfReader.self.setTitle(PerfReader.self.getTitle() + " ��ȡ��..."); // ������״̬
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Set<?> set = null;
		try { // ��ѯ�����ܹܷ���MBean������
			set = ac.queryNames(new ObjectName(
					"WebSphere:type=Server,processType=ManagedProcess" + ",*"),
					null);
		} catch (MalformedObjectNameException e) {
			logger.severe(e.getMessage());
		} catch (ConnectorException e) {
			logger.severe(e.getMessage());
		} catch (NullPointerException e) {
			logger.severe(e.getMessage());
		}
		if (set == null) {
			logger.severe("δ��ѯ���ܹܷ���MBean������.");
			return null;
		}
		Iterator<?> itr = set.iterator(); // ��������Server
		while (itr.hasNext()) {
			Object obj = itr.next();
			ObjectName objName = null;
			if (obj instanceof ObjectName)
				objName = (ObjectName) obj; // ��ȡMBean������
			if (objName == null)
				continue;
			String perfQryStr = "WebSphere:type=Perf,node="
					+ objName.getKeyProperty("node") + ",process="
					+ objName.getKeyProperty("name") + ",*"; // �����ѯ����,׼����ȡserver������MBean
			Set<?> setPerfObjName = null;
			try { // ��ѯ�ܹܷ��������MBean������
				setPerfObjName = ac
						.queryNames(new ObjectName(perfQryStr), null);
			} catch (MalformedObjectNameException e) {
				logger.severe(e.getMessage());
			} catch (ConnectorException e) {
				logger.severe(e.getMessage());
			} catch (NullPointerException e) {
				logger.severe(e.getMessage());
			}
			if (setPerfObjName == null) {
				logger.severe("δ��ѯ���ܹܷ�������MBean������.");
			}
			StatDescriptor[] arrStatDesc = new StatDescriptor[modules.length]; // ����ͳ��������
			for (int i = 0; i < modules.length; i++) { // ��ʼ������ͳ��������
				arrStatDesc[i] = new StatDescriptor(new String[] { modules[i] });
			}
			Object[] objStat = new Object[] { arrStatDesc, new Boolean(true) };
			String[] recursive = {
					"[Lcom.ibm.websphere.pmi.stat.StatDescriptor;",
					"java.lang.Boolean" };
			WSStats[] arrWSStats = null;
			try { // ��ȡ���ܾ���
				arrWSStats = (WSStats[]) ac.invoke(
						(ObjectName) setPerfObjName.iterator().next(),
						"getStatsArray", objStat, recursive);
			} catch (InstanceNotFoundException e) {
				logger.severe(e.getMessage());
			} catch (MBeanException e) {
				logger.severe(e.getMessage());
			} catch (ReflectionException e) {
				logger.severe(e.getMessage());
			} catch (ConnectorException e) {
				logger.severe(e.getMessage());
			}
			Map<String, Object> mapWSStat = new HashMap<String, Object>();
			mapWSStat.put("server_name", objName.getKeyProperty("name")); // ���÷�����
			mapWSStat.put("node_name", objName.getKeyProperty("node")); // ���ýڵ���
			mapWSStat.put("perf_data", arrWSStats); // ������������
			list.add(mapWSStat);
		}
		Collections.sort(list, new SortByNodeSrv()); // ���սڵ���������������
		PerfReader.self.setTitle(PerfReader.self.getTitle().replace(" ��ȡ��...", "")); // ������״̬
		return list;
	}
	
	private static class SortByNodeSrv implements Comparator<Map<String, Object>> {

		@Override
		public int compare(Map<String, Object> o1, Map<String, Object> o2) {
			String s1 = (String) o1.get("node_name") + (String) o1.get("server_name");
			String s2 = (String) o2.get("node_name") + (String) o2.get("server_name");
			return s1.compareTo(s2);
		}		
	}

	private static int MOD_FULL = 0; // ��ȡȫ������
	private static int MOD_COMP = 1; // ��ȡ�������� compact

	/**
	 * ������ΪList<JSONObject>�Ĳ���ת���ַ��� ȱ��: ����ͨ�û�
	 * 
	 * @param listJson
	 * @return
	 */
	private String getStrFromListJSON(Object listJson) {
		if (listJson instanceof List<?>) {
			List<?> list = ((List<?>) listJson);
			if (list == null || list.size() == 0) return null;
			Object json = ((List<?>) listJson).get(0);
			if (json instanceof String) {
				return (String) json;
			}
		} else if (listJson instanceof String) {
			return (String) listJson;
		}
		return null;
	}

	/**
	 * �����ֽ����ɶ��� ��KB�ֽ���ת��Ϊ�ɶ��ĸ�ʽ Convert KiloBytes to Human Readable Format
	 * 
	 * @param kiloBytes
	 * @return
	 */
	private String convKBytes2ReadableFormat(String kiloBytes) {
		if (kiloBytes == null) return "";
		DecimalFormat df = new DecimalFormat("######0.00");
		Double kb = Double.parseDouble(kiloBytes);
		if (kb >= 1024.0 * 1024.0) {
			return df.format(kb / 1024.0 / 1024.0) + " GB";
		} else if (kb >= 1024.0) {
			return df.format(kb / 1024.0) + " MB";
		} else {
			return df.format(kb) + " KB";
		}
	}

	/**
	 * �������������ɶ��� �����ŵ�����ת��Ϊ�ɶ��ĸ�ʽ Convert Seconds Goes by to Human Readable Format
	 * 
	 * @return
	 */
	private String convSeconds2ReadableFormat(String seconds) {
		String result = "";
		if (seconds == null) return result;
		int days = (int) Math.floor(Double.parseDouble(seconds)
				/ (24.0 * 60.0 * 60.0));
		if (days > 0)
			result += " " + days + " ��";
		long leftSeconds = Long.parseLong(seconds) % (24 * 60 * 60);
		int hours = (int) Math.floor((double) leftSeconds / (60.0 * 60.0));
		if (hours > 0)
			result += " " + hours + " Сʱ";
		leftSeconds = leftSeconds % (60 * 60);
		int minutes = (int) Math.floor((double) leftSeconds / 60.0);
		if (minutes > 0)
			result += " " + minutes + " ����";
		leftSeconds = leftSeconds % 60;
		if (leftSeconds > 0)
			result += " " + leftSeconds + " ��";
		return result;
	}

	/**
	 * ��ȡĳServer�ؼ�ָ�겢��ʽ��Ϊָ��Ӱ���
	 * 
	 * @param json
	 * @return
	 */
	private Map<String, String> formatSrvPerf(String json, int mod) {
		System.out.println(json);
		Map<String, String> map = new LinkedHashMap<String, String>();
		if (mod == MOD_COMP) {
			map.put("��������",
					getStrFromListJSON(JSONPath.read(json, "$.server_name")));
			String upperBound = convKBytes2ReadableFormat(getStrFromListJSON(JSONPath
					.read(json, "$..jvmRuntimeModule.HeapSize.upperBound")));
			map.put("���ѷ���", upperBound); // ���ѷ���
			String usedMemory = convKBytes2ReadableFormat(getStrFromListJSON(JSONPath
					.read(json, "$..jvmRuntimeModule.UsedMemory.count")));
			map.put("�����ڴ�", usedMemory);
			String processCpuUsage = getStrFromListJSON(JSONPath.read(json,
					"$..jvmRuntimeModule.ProcessCpuUsage.count")) + " %";
			map.put("CPUʹ��", processCpuUsage);
		}
		if (mod == MOD_FULL) {
			map.put("�ڵ���",
					getStrFromListJSON(JSONPath.read(json, "$.node_name")));
			map.put("��������",
					getStrFromListJSON(JSONPath.read(json, "$.server_name")));
			String upperBoundKBytes = getStrFromListJSON(JSONPath.read(json,
					"$..jvmRuntimeModule.HeapSize.upperBound"));
			String upperBound = convKBytes2ReadableFormat(upperBoundKBytes);
			map.put("���ѷ���", upperBound);
			String usedMemoryKBytes = getStrFromListJSON(JSONPath
					.read(json, "$..jvmRuntimeModule.UsedMemory.count"));
			if (usedMemoryKBytes != null && upperBoundKBytes != null) {
				map.put("��ʹ����",
						Math.round(Double.parseDouble(usedMemoryKBytes)
								/ Double.parseDouble(upperBoundKBytes) * 100)
								+ " %");
			}
			String processCpuUsage = getStrFromListJSON(JSONPath.read(json,
					"$..jvmRuntimeModule.ProcessCpuUsage.count"));
			if (processCpuUsage != null) processCpuUsage += " %";
			else processCpuUsage = "";
			map.put("CPUʹ��", processCpuUsage);
			String uptime = convSeconds2ReadableFormat(getStrFromListJSON(JSONPath
					.read(json, "$..jvmRuntimeModule.UpTime.count")));
			map.put("��������ʱ��", uptime);
			String webcontainerActiveCount = getStrFromListJSON(JSONPath.read(json,
					"$..WebContainer.ActiveCount.value"));
			map.put("WC��߳���", webcontainerActiveCount);
			String webcontainerPoolUpper = getStrFromListJSON(JSONPath.read(json,
					"$..WebContainer.PoolSize.upperBound"));
			map.put("WC������", webcontainerPoolUpper);
			String webcontainerPoolUsed = getStrFromListJSON(JSONPath.read(json,
					"$..WebContainer.PoolSize.value"));
			if (webcontainerPoolUsed != null && webcontainerPoolUpper != null) {
				map.put("WC������",
						Math.round(Double.parseDouble(webcontainerPoolUsed)
								/ Double.parseDouble(webcontainerPoolUpper) * 100)
								+ " %");
			}
			String servletLiveSessions = getStrFromListJSON(JSONPath.read(json,
					"$..servletSessionsModule.LiveCount.value"));
			map.put("���ػỰ��", servletLiveSessions);
			map.putAll(parseWarPerf(json)); // Ӧ�ó���Ự��
			map.putAll(parseJDBCPerf(json)); // JDBC���ӳ�
		}
		return map;
	}
	
	/**
	 * ��ȡӦ�ó���Ựͳ��
	 * @param json
	 * @param map
	 * @return
	 */
	private Map<String, String> parseWarPerf(String json) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		Object obj = JSONPath.read(json, "$perf_data");
		if (obj instanceof JSONObject) {
			JSONObject jsonObj = (JSONObject) obj;
			Set<String> key = jsonObj.keySet();
			Iterator<String> itrKey = key.iterator();
			while (itrKey.hasNext()) {
				String currKey = itrKey.next();
				if (currKey.indexOf(".war") != -1) {
					Object warPerfObj = jsonObj.get(currKey);
					if (warPerfObj instanceof JSONObject) {
						JSONObject warPerfJsonObj = (JSONObject) warPerfObj;
						Object objLiveCount = warPerfJsonObj.get("LiveCount");
						if (objLiveCount instanceof JSONObject) {
							map.put(currKey.split("#")[0] + "�Ự��",
									((JSONObject) objLiveCount).get("value")
											.toString());
						}
					}
				}
			}
		}
		return map;
	}
	
	/**
	 * ��ȡ���ݿ����ӳ�ͳ��
	 * @param json
	 * @param map
	 * @return
	 */
	private Map<String, String> parseJDBCPerf(String json) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		Object obj = JSONPath.read(json, "$perf_data");
		if (obj instanceof JSONObject) {
			JSONObject jsonObj = (JSONObject) obj;
			Set<String> key = jsonObj.keySet();
			Iterator<String> itrKey = key.iterator();
			while (itrKey.hasNext()) {
				String currKey = itrKey.next();
				Object objItem = jsonObj.get(currKey);
				if (objItem instanceof JSONObject) {
					JSONObject item = (JSONObject) objItem;
					if (item.keySet().contains("JDBCTime")) { // ����JDBCTime��˵��ΪJDBC����ָ��
						int jdbcPoolUpperBound = 0; // ���ӳ����ֵ
						int jdbcPoolCurrent = 0; // ���ӳص�ǰ��С(��ǰ�Ѿ����������ݿ�������)
						int jdbcFreeConnections = 0; // ���ӳ��еĿ���������
						Object objPoolSize = item.get("PoolSize");
						if (objPoolSize != null && objPoolSize instanceof JSONObject) {
							JSONObject poolSize = (JSONObject) objPoolSize;
							String sUpperBound = (String) poolSize.get("upperBound");
							if (sUpperBound != null) {
								jdbcPoolUpperBound = Integer.parseInt(sUpperBound);
							}
							String sPoolCurrent = (String) poolSize.get("value");
							if (sPoolCurrent != null) {
								jdbcPoolCurrent = Integer.parseInt(sPoolCurrent);
							}
							if (jdbcPoolUpperBound == 0 && jdbcPoolCurrent == 0) continue; // �����߶�Ϊ0,���˳�
							if (jdbcPoolUpperBound == 0) jdbcPoolUpperBound = jdbcPoolCurrent; // �ش�С�Ե�ǰֵΪ׼
						}
						Object objFreePoolSize = item.get("FreePoolSize");
						if (objFreePoolSize != null && objFreePoolSize instanceof JSONObject) {
							JSONObject freePoolSize = (JSONObject) objFreePoolSize;
							String sFreePoolSize = (String) freePoolSize.get("value");
							if (sFreePoolSize != null) {
								jdbcFreeConnections = Integer.parseInt(sFreePoolSize);
							}
						}
						map.put(currKey + "_jdbc_pool", // ������ӵ�_jdbc_pool�������ֵ�ǰ�������ӳ�ͳ����Ϣ
								Math.round((jdbcPoolCurrent - jdbcFreeConnections)
										* 100.0 / jdbcPoolUpperBound)
										+ "% ("
										+ Integer.toString(jdbcPoolCurrent
												- jdbcFreeConnections)
										+ " / "
										+ jdbcPoolUpperBound + ")");
					}
				}
			}
		}
		return map;
	}

	public static void main(String[] args) throws ConnectorException,
			MalformedObjectNameException, NullPointerException,
			InstanceNotFoundException, MBeanException, ReflectionException {
		@SuppressWarnings("unused")
		PerfReader pr = new PerfReader();
	}

	@Override
	public void run() {

	}

}
