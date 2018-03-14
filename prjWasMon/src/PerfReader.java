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
	 * 需要加载的性能模块
	 */
	private static String[] modules = new String[] { "jvmRuntimeModule",
			"threadPoolModule", "servletSessionsModule", "connectionPoolModule" };
	
	private static PerfReader self = null;

	/**
	 * 维护所有dmgr连接
	 */
	private static final Map<String, AdminClient> mapConn = new HashMap<String, AdminClient>();

	private PerfReader() {
		self = this;
		setTitle("WebSphere 监控平台");
		Dimension scrSz = Toolkit.getDefaultToolkit().getScreenSize(); // 获取屏幕尺寸
		Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(
				getGraphicsConfiguration()); // 获取任务栏高度
		setSize((int) scrSz.getWidth(),
				(int) (scrSz.getHeight() - screenInsets.bottom)); // 设置大小为满屏
		setResizable(false); // 窗体不可最小化
		setLocation(0, 0); // 窗体位置
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 设置窗体退出行为

		JSplitPane hSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); // 水平分割栏
		hSplitPane.setDividerLocation(getSize().width / 8); // 分割比例
		hSplitPane.setBackground(Color.gray); // 设置背景
		getContentPane().add(hSplitPane, BorderLayout.CENTER); // 添加分割栏
		hSplitPane.setLeftComponent(new NaviPanel()); // 左侧导航栏，所有WebSphere集群
		hSplitPane.setRightComponent(new PerfPanel()); // 右侧性能指标列表
		setVisible(true); // 打开窗体
	}


	/**
	 * dmgr服务配置对话框(新建/修改)
	 * 
	 * @author heqiming
	 * 
	 */
	private static class DlgDmgrConfig extends JDialog {

		private static final long serialVersionUID = 3899811007317902279L;

		private static DlgDmgrConfig self = null; // 对自身对象的引用
		
		private JControls.CustomizedIPField txtHost = new JControls.CustomizedIPField(); // DMGR服务IP地址文本框
		//private CustomizedTextField txtMask = new CustomizedTextField("32"); // 掩码位, 用以批量加载
		private JControls.CustomizedTextField txtPort = new JControls.CustomizedTextField("8879"); // 端口文本框
		private JControls.CustomizedComboBox cmbConnTyp = new JControls.CustomizedComboBox(
				new String[] { "SOAP", "JMX", "WSADMIN" }); // 连接类型
		private JControls.CustomizedButtonGroup btnGrpIfSec = new JControls.CustomizedButtonGroup(
				new String[] { "是", "否" }); // 单选按钮
		private JPasswordField txtUserName = new JPasswordField("wasadmin"); // 用户名
		private JPasswordField txtPassword = new JPasswordField("WebJ2ee"); // 密码
		private JControls.CustomizedPathField txtTrustStorPath = new JControls.CustomizedPathField(
				"DummyClientTrustFile.jks"); // 信任证书
		private JControls.CustomizedPathField txtKeyStorPath = new JControls.CustomizedPathField(
				"DummyClientKeyFile.jks"); // 信任证书
		private JControls.CustomizedComboBox cmbKeyStorTyp = new JControls.CustomizedComboBox(
				new String[] { "JKS", "JCEKS", "PKCS12" }); // 密钥类型
		private JPasswordField txtTrustStorPass = new JPasswordField("WebAS"); // 证书密码
		private JPasswordField txtKeyStorPass = new JPasswordField("WebAS"); // 密钥库密码
		private JControls.CustomizedComboBox cmbCategory = new JControls.CustomizedComboBox(
				new String[] { "营业厅CRM前台", "客服CRM", "应用4A", "一级BOSS", "主动业务探测",
						"版本测试", "便利店", "自助终端", "终端管理平台", "商城", "清欠",
						"imsportal", "ESOP", "省内SP类", "支付宝", "移动工作台", "局数据",
						"第三代", "其他" }); // 系统归类，显示为二级目录
		private JLabel txtClusterAlias = new JLabel(""); // 服务集群别名,建立dmgr连接时根据获取的cell名称得到,不可操作
		private JControls.CustomizedJSlider sldReloadInterval = new JControls.CustomizedJSlider(
				JSlider.HORIZONTAL); // 刷新频率滑动条, 0-不自动刷新, 1~10建立独立线程自动刷新
		private boolean state = false; // 对话框返回状态码
		
		/**
		 * 获取dmgr配置对话框配置数据
		 * @return
		 */
		private Map<String, String> getDmgrConfigurations() {
			Map<String, String> map = new HashMap<String, String>();
			map.put("host", txtHost.getText());
			map.put("port", txtPort.getText());
			map.put("connTyp", (String) cmbConnTyp.getSelectedItem());
			map.put("ifSec", btnGrpIfSec.getSelectedOption().equals("是") ? "true" : "false");
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
		 * 获取Basic面板
		 * 
		 * @return
		 */
		private JPanel getDmgrConfPanel() {
			GridBagLayout gbl = new GridBagLayout(); // 创建GridBag布局
			final JPanel jpBasic = new JPanel(gbl);
			jpBasic.setBorder(BorderFactory.createTitledBorder(" 连接配置"));
			GridBagConstraints gbc = new GridBagConstraints();// GridBag布局
			gbc.fill = GridBagConstraints.BOTH;
			gbc.ipadx = 20;
			gbc.ipady = 20;

			// 第一行
			jpBasic.add(new JControls.CustomizedLabel("主机IP:")); // 元素0
			gbc.gridwidth = 1;
			gbc.weightx = 0.1;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(0), gbc);
			jpBasic.add(txtHost); // 元素1
			gbc.gridwidth = 2;
			gbc.weightx = 0.2;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(1), gbc);
			jpBasic.add(new Container()); // 元素2
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(2), gbc);
			jpBasic.add(new JControls.CustomizedLabel("服务端口:")); // 元素3
			gbc.gridwidth = 1;
			gbc.weightx = 0.1;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(3), gbc);
			jpBasic.add(txtPort); // 元素4
			txtPort.setEnabled(false);
			gbc.gridwidth = 2;
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(4), gbc);
			JCheckBox chkEnablePort = new JCheckBox(); // 复选框，用于启用端口编辑
			chkEnablePort.setSelected(false);
			chkEnablePort.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					txtPort.setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnablePort); // 元素5
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(5), gbc);
			jpBasic.add(new JControls.CustomizedLabel("连接类型:")); // 元素6
			gbc.gridwidth = 1;
			gbc.weightx = 0.1;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(6), gbc);
			jpBasic.add(cmbConnTyp); // 元素7
			cmbConnTyp.setEnabled(false);
			gbc.gridwidth = 1;
			gbc.weightx = 0.2;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(7), gbc);
			JCheckBox chkEnableConnTyp = new JCheckBox(); // 复选框，用于启用编辑
			chkEnableConnTyp.setSelected(false);
			chkEnableConnTyp.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					cmbConnTyp.setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnableConnTyp); // 元素8
			gbc.gridwidth = 0;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(8), gbc);
			// 第二行
			jpBasic.add(new JControls.CustomizedLabel("安全连接:")); // 元素9
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(9), gbc);
			Enumeration<AbstractButton> btnOpts = btnGrpIfSec.getElements();
			jpBasic.add(btnOpts.nextElement()); // 元素10
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			jpBasic.getComponent(10).setEnabled(false);
			gbl.setConstraints(jpBasic.getComponent(10), gbc);
			jpBasic.add(btnOpts.nextElement()); // 元素11
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			jpBasic.getComponent(11).setEnabled(false);
			gbl.setConstraints(jpBasic.getComponent(11), gbc);
			JCheckBox chkEnableIfSec = new JCheckBox(); // 复选框，用于启用编辑
			chkEnableIfSec.setSelected(false);
			chkEnableIfSec.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					jpBasic.getComponent(10).setEnabled(chk.isSelected());
					jpBasic.getComponent(11).setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnableIfSec); // 元素12
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(12), gbc);
			jpBasic.add(new JControls.CustomizedLabel("dmgr账号:")); // 元素13
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(13), gbc);
			jpBasic.add(txtUserName); // 元素14
			txtUserName.setEnabled(false);
			gbc.gridwidth = 2;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(14), gbc);
			JCheckBox chkEnableUserName = new JCheckBox(); // 复选框，用于启用编辑
			chkEnableUserName.setSelected(false);
			chkEnableUserName.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					txtUserName.setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnableUserName); // 元素15
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(15), gbc);
			jpBasic.add(new JControls.CustomizedLabel("dmgr密码:")); // 元素16
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(16), gbc);
			jpBasic.add(txtPassword); // 元素17
			txtPassword.setEnabled(false);
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(17), gbc);
			JCheckBox chkEnablePassword = new JCheckBox(); // 复选框，用于启用编辑
			chkEnablePassword.setSelected(false);
			chkEnablePassword.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					txtPassword.setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnablePassword); // 元素18
			gbc.gridwidth = 0;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(18), gbc);
			// 第三行
			jpBasic.add(new JControls.CustomizedLabel("信任证书:")); // 元素19
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(19), gbc);
			jpBasic.add(txtTrustStorPath); // 元素20
			txtTrustStorPath.setEnabled(false);
			gbc.gridwidth = 2;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(20), gbc);
			JButton btnStorPathSelect = new JButton("..."); // 元素21
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
					fc.setFileFilter(new FileNameExtensionFilter("证书/密钥库",
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
			jpBasic.add(new JControls.CustomizedLabel("密钥:")); // 元素22
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(22), gbc);
			jpBasic.add(txtKeyStorPath); // 元素23
			txtKeyStorPath.setEnabled(false);
			gbc.gridwidth = 2;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(23), gbc);
			JButton btnKeyPathSelect = new JButton("..."); // 元素24
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
					fc.setFileFilter(new FileNameExtensionFilter("证书/密钥库",
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
			jpBasic.add(new JControls.CustomizedLabel("密钥类型:")); // 元素25
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(25), gbc);
			jpBasic.add(cmbKeyStorTyp); // 元素26
			cmbKeyStorTyp.setEnabled(false);
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(26), gbc);
			JCheckBox chkEnableKeyStorTyp = new JCheckBox(); // 复选框，用于启用编辑
			chkEnableKeyStorTyp.setSelected(false);
			chkEnableKeyStorTyp.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					cmbKeyStorTyp.setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnableKeyStorTyp); // 元素27
			gbc.gridwidth = 0;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(27), gbc);
			// 第四行
			jpBasic.add(new JControls.CustomizedLabel("证书密码:")); // 元素28
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(28), gbc);
			jpBasic.add(txtTrustStorPass); // 元素29
			txtTrustStorPass.setEnabled(false);
			gbc.gridwidth = 2;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(29), gbc);
			JCheckBox chkEnableTrustStorPass = new JCheckBox(); // 复选框，用于启用编辑
			chkEnableTrustStorPass.setSelected(false);
			chkEnableTrustStorPass.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					txtTrustStorPass.setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnableTrustStorPass); // 元素30
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(30), gbc);
			jpBasic.add(new JControls.CustomizedLabel("密钥密码:")); // 元素31
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(31), gbc);
			jpBasic.add(txtKeyStorPass); // 元素32
			txtKeyStorPass.setEnabled(false);
			gbc.gridwidth = 2;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(32), gbc);
			JCheckBox chkEnableKeyStorPass = new JCheckBox(); // 复选框，用于启用编辑
			chkEnableKeyStorPass.setSelected(false);
			chkEnableKeyStorPass.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox chk = (JCheckBox) e.getSource();
					txtKeyStorPass.setEnabled(chk.isSelected());
				}
			});
			jpBasic.add(chkEnableKeyStorPass); // 元素33
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(33), gbc);
			jpBasic.add(new JControls.CustomizedLabel("系统归类:")); // 元素34
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(34), gbc);
			jpBasic.add(cmbCategory); // 元素35
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(35), gbc);
			jpBasic.add(new Container()); // 元素36
			gbc.gridwidth = 0;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(36), gbc);
			// 第6行
			jpBasic.add(new JControls.CustomizedLabel("刷新频率:")); // 元素31
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(37), gbc);
			jpBasic.add(sldReloadInterval); // 元素36
			gbc.gridwidth = 2;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(38), gbc);
			jpBasic.add(new Container()); // 元素44
			gbc.gridwidth = 0;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(39), gbc);
			// 第7行校准行
			jpBasic.add(new Container()); // 元素37
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(40), gbc);
			jpBasic.add(new Container()); // 元素35
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(41), gbc);
			jpBasic.add(new Container()); // 元素36
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(42), gbc);
			jpBasic.add(new Container()); // 元素37
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(43), gbc);
			jpBasic.add(new Container()); // 元素38
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(44), gbc);
			jpBasic.add(new Container()); // 元素39
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(45), gbc);
			jpBasic.add(new Container()); // 元素40
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(46), gbc);
			jpBasic.add(new Container()); // 元素41
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(47), gbc);
			jpBasic.add(new Container()); // 元素42
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(48), gbc);
			jpBasic.add(new Container()); // 元素43
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(49), gbc);
			jpBasic.add(new Container()); // 元素44
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpBasic.getComponent(50), gbc);

			return jpBasic;
		}

		/**
		 * 获取按钮面板
		 * 
		 * @return
		 */
		private JPanel getButtonPanel() {
			GridBagLayout gbl = new GridBagLayout(); // 创建GridBag布局
			JPanel jpButton = new JPanel(gbl); // 主容器
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.BOTH;
			gbc.ipadx = 20;
			gbc.ipady = 20;

			jpButton.add(new Container());
			gbc.gridwidth = 1;
			gbc.weightx = 0.8;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpButton.getComponent(0), gbc);
			jpButton.add(txtClusterAlias); // 集群别名
			gbc.gridwidth = 1;
			gbc.weightx = 0.05;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpButton.getComponent(1), gbc);
			JButton btnApply = new JButton("应用");
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
					if (map.get("mask") != null && !map.get("mask").equals("32")) { // 若掩码位不是32,说明是网段
						List<String> ipList = IpUtils.parseIpMaskRange(map.get("host"), map.get("mask"));
						for (String ip : ipList) {
							if (!isPortUsing(ip, Integer.parseInt(map.get("port")))) ipList.remove(ip);
							// 未完成
						}
					}
					String key = map.get("host") + ":" + map.get("port");
					if (!mapConn.containsKey(key)) { // 若在dmgr连接映射表中不存在,则建立新的连接,并记录到连接映射表
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
					AdminClient ac = mapConn.get(key); // 获取dmgr连接
					Map<String, String> mapDmgr = PerfReader.self.qryDmgrInfo(ac); // 获取dmgr信息
					txtClusterAlias.setText(mapDmgr.get("cell").substring(0,
							mapDmgr.get("cell").indexOf("Cell"))); // 设置dmgr服务所在主机名
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
			jpButton.add(btnApply); // 应用
			gbc.gridwidth = 1;
			gbc.weightx = 0.05;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpButton.getComponent(2), gbc);
			JButton btnConfirm = new JButton("确定");
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
					// 设置返回状态
					state = true;
					// 关闭对话框
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
			JButton btnCancel = new JButton("取消");
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
					// 设置返回状态
					state = false;
					// 关闭对话框
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
		 * 配置dmgr连接对话框
		 */
		private DlgDmgrConfig(String category) {
			if (category != null && !category.equals("")) { // 若系统分类参数非空,需要设置系统分类组合框默认值
				int cnt = cmbCategory.getItemCount();
				for (int i = 0; i < cnt; i++) {
					if (cmbCategory.getItemAt(i).equals(category)) {
						cmbCategory.setSelectedIndex(i);
						break;
					}
				}
			}
			self = this;
			setTitle("添加新的dmgr连接");
			Dimension scrSz = Toolkit.getDefaultToolkit().getScreenSize();
			Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(
					getGraphicsConfiguration());
			setSize(scrSz.width * 3 / 4,
					(scrSz.height - screenInsets.bottom) / 3); // 对话框大小，高度去掉任务栏高度
			setLocation(scrSz.width / 8,
					(scrSz.height - screenInsets.bottom) / 3);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // 关闭程序终止
			setModalityType(ModalityType.APPLICATION_MODAL); // 父窗口不可用
			setModal(true);
			setResizable(false); // 不可调整大小

			GridBagLayout gbl = new GridBagLayout(); // 创建GridBag布局
			JPanel jpMain = new JPanel(gbl); // 主容器

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.BOTH;
			gbc.ipadx = 20;
			gbc.ipady = 20;

			jpMain.add(getDmgrConfPanel()); // 元素面板
			gbc.gridwidth = 0;
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(jpMain.getComponent(0), gbc);
			jpMain.add(getButtonPanel()); // 按钮面板
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
	 * 左侧导航树
	 * 
	 */
	private static class NaviPanel extends JPanel {
		private static final long serialVersionUID = 924411562858994237L;
		private IconNode root; // 根节点
		private static JTree tree = null; // 导航树
		private static Set<Entry<String, Section>> dmgrs = null; // 加载的配置数据{[主机IP,主机配置项],...}
		private static Map<String, ThrReload> mapReloadThreads = new LinkedHashMap<String, ThrReload>(); // 刷新线程映射表<主机IP, 刷新线程>
		private static Connection connDerby = null; // 性能仓库连接
		private JPopupMenu popMenu = null; // 弹出菜单
		
		public static void main(String[] args) {
			initDBConnection();
			initSchema();
			initTables();
			initConstraints();
		}
		
		/**
		 * 根据host获取dmgr连接配置
		 * @param host
		 * @return
		 */
		private static Section getCfgByHost(String host) {
			Iterator<Entry<String, Section>> itr = dmgrs.iterator(); // 遍历dmgr配置数据
			while (itr.hasNext()) {
				Entry<String, Section> ent = itr.next();
				if (ent.getKey().equals(host)) { // 检索与当前树节点ID匹配的记录
					return ent.getValue();
				}
			}
			return null;
		}
		
		/**
		 * 初始化性能仓库连接
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
		 * 初始化schema
		 */
		private static void initSchema() {
			ResultSet rs = null;
			boolean bSchema = false;
			try {
				rs = connDerby.getMetaData().getSchemas(); // 从元数据获取所有schema
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				while (rs.next()) { // 查询schema
					if (rs.getString(1).toUpperCase().equals("PERF")) {
						bSchema = true;
						break;
					}
				}
				rs.close();
				rs = null;
				if (!bSchema) { // 若找不到schema则创建schema
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
		 * 初始化表结构
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
		 * 初始化约束
		 */
		private static void initConstraints() {
			ResultSet rs = null;
			Statement stmt = null;
			try {
				// 添加主键/唯一索引
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
				// 添加时间索引
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
		 * 初始化序列
		 */
		private static void initSequence() {
			
		}
		
		/**
		 * 清理表结构
		 */
		private static void destroyTable() {
			try {
				Statement stmt = connDerby.createStatement();
				ResultSet rs = stmt
						.executeQuery("SELECT 1 FROM sys.systables t1, sys.sysschemas t2 "
								+ "WHERE t1.tablename = 'PERF_WEBSPHERE' AND t1.schemaid = t2.schemaid AND "
								+ "t2.schemaname = 'PERF'");
				connDerby.commit();
				if (rs.next()) { // 若存在则删表
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
		 * 清理schema
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
		 * 从数据库获取最新的性能数据
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
		 * 获取或初始化AdminClient连接
		 * @param host
		 * @param sect
		 * @return
		 */
		private static AdminClient getOrInitDmgrConnections(String host,
				Section sect) {
			if (host == null || sect == null)
				return null; // 若找不到配置,则退出
			if (!mapConn.containsKey(host + ":" + sect.get("port"))) { // 若在dmgr连接映射表中不存在,则建立新的连接,并记录到连接映射表
				AdminClient ac = PerfReader.self.createConnection(host,
						sect.get("port"), sect.get("connTyp"),
						sect.get("ifSec"), sect.get("userName"),
						sect.get("password"), sect.get("trustStorPath"),
						sect.get("keyStorPath"), sect.get("keyStorType"),
						sect.get("trustStorPass"), sect.get("keyStorPass"));
				if (ac == null)
					return null;
			}
			return mapConn.get(host + ":" + sect.get("port")); // 获取dmgr连接
		}
		
		/**
		 * 从dmgr获取最新性能数据
		 * @param host
		 * @param sect
		 * @return
		 */
		private static List<Map<String, String>> getPerfDataFromDmgr(
				AdminClient ac, String host, Section sect) {
			List<Map<String, Object>> list = PerfReader.self.qrySrvPerf(ac); // 查询性能数据
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
			private String host = null; // 主机IP
			private int interval = 0; // 刷新时间间隔

			private ThrReload(String para0, int para1) {
				host = para0;
				interval = para1;
			}

			@Override
			public void run() {
				while (true) {
					if (interval != 0) { // 启动定时任务
						// 1.从derby查询最新数据
						// 2.若未找到,查询dmgr,系列化,写入derby
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
		 * 排序器: 按照key值排序
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
		 * 从配置文件加载配置
		 * @throws InvalidFileFormatException
		 * @throws IOException
		 */
		private void loadConfiguration(String fileName) { // 加载配置
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
					ini.entrySet()); // 准备排序
			if (listDmgrs.size() >= 2)
				Collections.sort(listDmgrs, new SortByKey());
			if (dmgrs == null) {
				dmgrs = new LinkedHashSet<Entry<String, Section>>();
			}
			dmgrs.addAll(listDmgrs);
		}
		
		/**
		 * 保存配置到配置文件
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
				ini.load(file); // 加载文件
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
				ini.store(file); // 保存文件
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			ini.clear();
		}
		
		/**
		 * 删除相关配置
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
				ini.store(file); // 保存文件
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			ini.clear();
		}
		
		/**
		 * 初始化右键菜单
		 */
		private void initPopupMenu() {
			JMenuItem addItem = new JMenuItem("添加部署管理器");
			addItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					IconNode currNode = (IconNode) tree
							.getLastSelectedPathComponent(); // 获取当前操作的节点
					String currCategory = (String) (currNode.getParent() != null
							&& currNode.getParent().equals(root) ? currNode
							.getText() : null); // 若当前节点的父节点是root,则当前节点为系统类别节点
					DlgDmgrConfig ddc = new DlgDmgrConfig(currCategory); // 显示添加dmgr服务对话框
					if (!ddc.getReturnStatus()) return; // 若放弃保存
					Map<String, String> mapCnf = ddc.getDmgrConfigurations(); // 获取配置
					String key = mapCnf.get("host") + ":" + mapCnf.get("port");
					if (!mapConn.containsKey(key)) { // 若在dmgr连接映射表中不存在,则建立新的连接,并记录到连接映射表
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
					AdminClient ac = mapConn.get(key); // 获取dmgr连接
					try { // 若会话不存在退出
						if (ac.isAlive().getSessionId() == null) return;
					} catch (ConnectorException e2) {
						logger.severe(e2.getMessage());
					}
					if (mapCnf.get("clusterAlias").equals("")) { // 系统类别二级目录标识,若不存在则说明没有按应用,直接确定了
						Map<String, String> map = PerfReader.self
								.qryDmgrInfo(ac); // 获取dmgr信息
						mapCnf.put(
								"clusterAlias",
								map.get("cell").substring(0,
										map.get("cell").indexOf("Cell")));
					}
					// 准备向tree添加节点
					String category = mapCnf.get("category"); // 系统类别
					String dmgrId = mapCnf.get("clusterAlias") + "_" + mapCnf.get("host"); // 在节点上显示的dmgr标识
					IconNode nodeCat = null; // 待获取/创建的系统类别节点
					int cateNum = root.getChildCount(); // 目前在树中显示的系统类别数量
					for (int i = 0; i < cateNum; i++) {
						IconNode child = (IconNode) root.getChildAt(i);
						if (child.getText().equals(category)) {
							nodeCat = child; // 找到已存在的系统类别节点
							break;
						}
					}
					if (nodeCat == null) { // 若未找到,则新创建类别节点,并添加到树
						nodeCat = new IconNode(new ImageIcon(getClass().getResource("Closed_Folder.png")), category);
						root.add(nodeCat);
					}
					int dmgrNum = nodeCat.getChildCount(); // 当前系统类别中已经展示的dmgr节点数量
					for (int i = 0; i < dmgrNum; i++) {
						IconNode child = (IconNode) nodeCat.getChildAt(i);
						if (child.getText().equals(dmgrId)) {
							return; // 若当前dmgr节点已存在则退出
						}
					}
					IconNode nodeDmgr = new IconNode(new ImageIcon(getClass().getResource("WebSphere.png")), dmgrId); // 创建dmgr服务节点
					nodeCat.add(nodeDmgr);
					tree.updateUI(); // 刷新树
					expandAll(new TreePath(root), true); // 全部展开
					// 在配置文件中添加配置
					saveDmgrConfigs("dmgr-config.ini", mapCnf); // 保存配置文件
					// 重新加载配置dmgrs
					loadConfiguration("dmgr-config.ini"); // 重新加载配置文件
				}
			});
			JMenuItem modItem = new JMenuItem("修改");
			JMenuItem delItem = new JMenuItem("删除");
			delItem.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					// 清理树节点
					IconNode currNode = (IconNode) tree
							.getLastSelectedPathComponent();
					IconNode parentNode = (IconNode) currNode.getParent();
					parentNode.remove(currNode);
					expandAll(new TreePath(root), true); // 全部展开
					// 若父节点无孩子节点,则将父节点清除
					if (parentNode.getChildCount() == 0 && !parentNode.equals(root)) {
						root.remove(parentNode);
					}
					// 刷新树
					tree.updateUI();
					// 从配置文件中删除
					String dmgrId = (String) currNode.getText();
					String host = dmgrId.split("_")[1];
					removeDmgrConfigByHost("dmgr-config.ini", host);
					// 清理dmgrs
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
					// 清理mapConn
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
			JMenuItem subNetworkItem = new JMenuItem("添加子网");
			subNetworkItem.setEnabled(false);
			popMenu.add(subNetworkItem);
		}
		
		/**
		 * 展开/收缩整棵树
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
					"whole_network.png")), "全网");
			tree = new JTree(root); // 创建树
			tree.setForeground(Color.LIGHT_GRAY); // 前景色
			tree.setBackground(Color.LIGHT_GRAY); // 背景色
			tree.getSelectionModel().setSelectionMode( // 允许不连续多选
					TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
			tree.setCellRenderer(new IconNodeRenderer()); // 设置渲染器
			tree.setEditable(false); // 树不可编辑
			tree.setRootVisible(true); // 根节点可见
			tree.setToggleClickCount(1); // 设置单击1次即展开/收缩
			loadConfiguration("dmgr-config.ini"); // 加载配置到dmgrs
			for (Entry<String, Section> dmgr : dmgrs) { // 加载配置到树
				String dmgrId = dmgr.getValue().get("clusterAlias") + "_" + dmgr.getKey(); // 组装树节点ID
				String category = dmgr.getValue().get("category"); // 获取系统类别
				IconNode nodeCat = null; // 待获取/创建的系统类别节点
				int cateNum = root.getChildCount(); // 目前在树中显示的系统类别数量
				for (int i = 0; i < cateNum; i++) { // 所有二级子节点(系统类别)中搜索与读取的配置匹配的项
					IconNode child = (IconNode) root.getChildAt(i);
					if (child.getText().equals(category)) {
						nodeCat = child; // 找到已存在的系统类别节点
						break;
					}
				}
				if (nodeCat == null) { // 若未找到,则新创建类别节点,并添加到树
					nodeCat = new IconNode(new ImageIcon(getClass()
							.getResource("Closed_Folder.png")), category);
					root.add(nodeCat);
				}
				int dmgrNum = nodeCat.getChildCount(); // 当前系统类别中已经展示的dmgr节点数量
				for (int i = 0; i < dmgrNum; i++) { // 对当前系统类别中所有dmgr节点与dmgrId匹配,若匹配到则不进行后续操作
					IconNode child = (IconNode) nodeCat.getChildAt(i);
					if (child.getText().equals(dmgrId)) {
						return;
					}
				}
				IconNode nodeDmgr = new IconNode(new ImageIcon(getClass()
						.getResource("WebSphere.png")), dmgrId); // 创建dmgr服务节点
				nodeCat.add(nodeDmgr); // 添加节点
			}
			initPopupMenu(); // 初始化弹出菜单
			tree.addMouseListener(new MouseAdapter() { // 响应鼠标动作
				public void mousePressed(MouseEvent e) {
					if (e.getComponent().isEnabled() == false) { // JTree状态必须可用
						return;
					}
					TreePath path = tree.getPathForLocation(e.getX(), e.getY()); // 获取当前节点路径
					if (path == null) return;
					tree.setSelectionPath(path); // 选中当前节点
					IconNode currNode = (IconNode) tree.getLastSelectedPathComponent();
					if (currNode.equals(root)) { // 根节点只能添加成员
						popMenu.getComponent(0).setEnabled(true);
						popMenu.getComponent(1).setEnabled(false);
						popMenu.getComponent(2).setEnabled(false);
						popMenu.getComponent(3).setEnabled(false);
					} else {
						if (((IconNode) currNode.getParent()).equals(root)) { // 二级节点
							popMenu.getComponent(0).setEnabled(true);
							popMenu.getComponent(1).setEnabled(false);
							popMenu.getComponent(2).setEnabled(false);
							popMenu.getComponent(3).setEnabled(false);
						} else {
							if (currNode.isLeaf()) { // Dmgr服务节点
								popMenu.getComponent(0).setEnabled(false);
								popMenu.getComponent(1).setEnabled(true);
								popMenu.getComponent(2).setEnabled(true);
								popMenu.getComponent(3).setEnabled(false);
							}
						}
					}

					if (SwingUtilities.isRightMouseButton(e)) popMenu.show(e.getComponent(), e.getX(), e.getY());
					if (SwingUtilities.isLeftMouseButton(e)) { // 左键用以展示当前节点对应的dmgr服务性能数据
						if (currNode.equals(root)
								|| currNode.getParent().equals(root))
							return; // 对于根节点和系统类型节点不响应
						String host = ((String) currNode.getText()).split("_")[1]; // 当前树节点的ID即主机IP
						Section sect = null;
						Iterator<Entry<String, Section>> itr = dmgrs.iterator(); // 遍历dmgr配置数据
						while (itr.hasNext()) {
							Entry<String, Section> ent = itr.next();
							if (ent.getKey().equals(host)) { // 检索与当前树节点ID匹配的记录
								sect = ent.getValue();
								break;
							}
						}
						if (sect == null) return; // 若找不到配置,则退出
						String connId = host + ":" + sect.get("port"); // <host:port, conn>
						if (!mapConn.containsKey(connId)) { // 若在dmgr连接映射表中不存在,则建立新的连接,并记录到连接映射表
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
						AdminClient ac = mapConn.get(connId); // 获取dmgr连接
						List<Map<String, Object>> list = PerfReader.self.qrySrvPerf(ac); // 查询性能数据
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
//			renderTree(); // 渲染树
			expandAll(new TreePath(root), true);
			setVisible(true);
			setLayout(new GridLayout(1, 3, 5, 5));
			setBackground(Color.LIGHT_GRAY);
		}
	}

	/**
	 * 性能指标控制面板
	 * 
	 */
	private static class PerfPanel extends JPanel {
		private static final long serialVersionUID = -1737823149753250108L;
		private String columnNames[] = { "性能指标" }; // 列标题
		private static JTable table = null; // 核心性能列表
		private static JTable tableJDBC = null; // JDBC性能列表
		private static PerfPanel self = null;
		
		/**
		 * 勾勒自定义圆角边框
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
			/* 核心性能列表 */
			Object data[][] = new Object[1][1]; // 初始状态
			if (table == null)
				table = new JTable(data, columnNames);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table.setAutoscrolls(true);
			table.setBackground(Color.LIGHT_GRAY);
			Font font = new Font(table.getFont().getName(), table.getFont().getStyle(),
					table.getFont().getSize() - 2); // 设置字体缩小
			table.setFont(font);
			table.setRowHeight(table.getRowHeight() - 2); // 调整行高
			font = null;
			
			/* 数据库连接池性能列表 */
			if (tableJDBC == null)
				tableJDBC = new JTable(data, columnNames);
			tableJDBC.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			tableJDBC.setAutoscrolls(true);
			tableJDBC.setBackground(Color.LIGHT_GRAY);
			font = new Font(tableJDBC.getFont().getName(), tableJDBC.getFont().getStyle(),
					tableJDBC.getFont().getSize() - 2); // 设置字体缩小
			tableJDBC.setFont(font);
			tableJDBC.setRowHeight(tableJDBC.getRowHeight() - 2); // 调整行高
			
			/* 设置上下分割窗 */
			JSplitPane vSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT); // 垂直分割，分两个Tab页展现
			vSplitPane.setDividerLocation(PerfReader.self.getSize().height / 2); // 分割比例
			vSplitPane.setBackground(Color.gray);
			JTabbedPane tabbedUp = new JTabbedPane(); // 上选项卡
			tabbedUp.addTab("核心性能列表", new JScrollPane(table));
			JTabbedPane tabbedDn = new JTabbedPane(); // 下选项卡
			tabbedDn.addTab("数据库连接池性能列表", new JScrollPane(tableJDBC));
			vSplitPane.setLeftComponent(tabbedUp); // 上选项卡
			vSplitPane.setRightComponent(tabbedDn); // 右侧性能指标列表
			
			/* 将分割窗添加到当前控制面板 */
			setVisible(true);
			add(vSplitPane, BorderLayout.NORTH); // 添加分割栏
			setLayout(new GridLayout(1, 1, 1, 1));
			self = this;
		}
		
		/**
		 * 删除table的所有列,相当于重置table
		 */
		private void removeAllColumns() {
			TableColumnModel tcm = table.getColumnModel();
			while (tcm.getColumnCount() > 0) {
				tcm.removeColumn(tcm.getColumn(0));
			}
		}

		/**
		 * 将性能数据加载到JTable
		 * @param data
		 * @throws IllegalArgumentException
		 * @throws SecurityException
		 * @throws IllegalAccessException
		 * @throws NoSuchFieldException
		 */
		public void load(List<Map<String, String>> data) {
			if (data == null || data.size() == 0) // 数据合法性检查
				return;
			removeAllColumns(); // 清除所有列,用于重置JTable
			/*  */
			Set<String> setKern = new LinkedHashSet<String>(); // 核心部分
			Set<String> setJDBC = new LinkedHashSet<String>(); // JDBC部分
			for (Map<String, String> each : data) { // 所有记录的键值可能不同
				Set<String> setKernEach = new LinkedHashSet<String>();
				Set<String> setJDBCEach = new LinkedHashSet<String>();
				Set<String> setCol = each.keySet();
				for (String col : setCol) {
					if (col.equals("节点名") || col.equals("服务名称")) {
						setKernEach.add(col);
						setJDBCEach.add(col);
					} else {
						if (col.indexOf("_jdbc_pool") != -1) setJDBCEach.add(col.substring(0, col.indexOf("_jdbc_pool")));
						else setKernEach.add(col);
					}
				}
				setKern.addAll(setKernEach); // 列合并
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
					if (colNamesJDBC[col].equals("节点名") || colNamesJDBC[col].equals("服务名称"))
						valuesJDBC[row][col] = data.get(row).get(colNamesJDBC[col]);
					else
						valuesJDBC[row][col] = data.get(row).get(colNamesJDBC[col] + "_jdbc_pool");
				}
			}
			DefaultTableModel modelKern = new DefaultTableModel(valuesKern, colNamesKern);
			DefaultTableModel modelJDBC = new DefaultTableModel(valuesJDBC, colNamesJDBC);
			table.setModel(modelKern);
			tableJDBC.setModel(modelJDBC);
			fitTableColumns(table); // 调整列宽
			fitTableColumns(tableJDBC);
			highLightKernal(); // 高亮异常指标
			highLightJDBC();
		}
		
		/**
		 * 高亮异常指标(核心指标)
		 */
		public void highLightKernal() {
			
			DefaultTableCellRenderer highLight = new DefaultTableCellRenderer() {
				private static final long serialVersionUID = -8848303924674335966L;

				public void setValue(Object value) { // 重写setValue方法，从而可以动态设置列单元字体颜色
					if (value == null || value.equals("")) { // 数据未读取到
						setForeground(Color.RED);
						MyLineBorder myLineBorder = new MyLineBorder(Color.RED,
								1, true);
						setBorder(myLineBorder);
						setText("数据异常");
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
			TableColumn cpuCol = table.getColumn("CPU使用");
			cpuCol.setCellRenderer(highLight);

			TableColumn sessCol = table.getColumn("堆使用率");
			sessCol.setCellRenderer(highLight);

			TableColumn wcCol = table.getColumn("WC池已用");
			wcCol.setCellRenderer(highLight);
		}
		
		/**
		 * 高亮异常指标(JDBC指标)
		 */
		public void highLightJDBC() {
			
			DefaultTableCellRenderer highLight = new DefaultTableCellRenderer() {
				private static final long serialVersionUID = -8848303924674335966L;

				public void setValue(Object value) { // 重写setValue方法，从而可以动态设置列单元字体颜色
					if (value == null || value.equals("")) { // 数据未读取到
						setForeground(Color.RED);
						MyLineBorder myLineBorder = new MyLineBorder(Color.RED,
								1, true);
						setBorder(myLineBorder);
						setText("数据异常");
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
				if (!colName.equals("节点名")
						&& colName.equals("服务名称")) {
					tableJDBC.getColumn(colName).setCellRenderer(highLight);
				}
			}
		}

		/**
		 * 自动调节列宽
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
	 * 判断端口是否占用
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
	 * 连接至dmgr服务
	 * 1.默认采用安全SOAP协议
	 * 2.dmgr登录用户名密码默认
	 * 3.SSL证书、密钥库默认路径在当前类路径下
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
		PerfReader.self.setTitle(PerfReader.self.getTitle() + " 建立连接中..."); // 置运行状态
		if (!isPortUsing(host, Integer.parseInt(port))) {
			PerfReader.self.setTitle(PerfReader.self.getTitle().replace(" 建立连接中...", "")); // 置运行状态
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
		if (trustStorPath == null) { // 若证书文件路径不存在则从默认类路径下查找
			URL urlCert = getClass().getClassLoader().getResource("DummyClientTrustFile.jks");
			if (urlCert == null) { // 文件URL获取失败
				URL urlFolder = getClass().getClassLoader().getResource("");
				String folder = new File(urlFolder.getFile()).getAbsolutePath();
				logger.severe("在" + folder + "下找不到证书文件.");
			}
			File fCert = new File(urlCert.getFile());
			trustStorPath = fCert.getAbsolutePath();
		}
		properties.setProperty("javax.net.ssl.trustStore", trustStorPath);
		if (keyStorPath == null) { // 若证书文件路径不存在则从默认类路径下查找
			URL urlKey = getClass().getClassLoader().getResource("DummyClientKeyFile.jks");
			if (urlKey == null) { // 文件URL获取失败
				URL urlFolder = getClass().getClassLoader().getResource("");
				String folder = new File(urlFolder.getFile()).getAbsolutePath();
				logger.severe("在" + folder + "下找不到密钥库文件.");
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
			AdminClient ac = AdminClientFactory.createAdminClient(properties); // 建立连接
			if (ac != null) { // 连接成功，保存到连接影射表
				mapConn.put(properties.getProperty("host") + ":"
						+ properties.getProperty("port"), ac);
			}
		} catch (ConnectorException e) {
			e.printStackTrace();
		}
		String key = host + ":" + port;
		AdminClient ac = mapConn.get(key);
		if (ac == null) {
			logger.severe("创建dmgr连接失败.");
		}
		PerfReader.self.setTitle(PerfReader.self.getTitle().replace(" 建立连接中...", "")); // 置运行状态
		return ac;
	}
	
	/**
	 * 获取dmgr服务信息
	 * @param ac
	 * @return
	 */
	private Map<String, String> qryDmgrInfo(AdminClient ac) {
		PerfReader.self.setTitle(PerfReader.self.getTitle() + " 获取中..."); // 置运行状态
		Map<String, String> map = new HashMap<String, String>();
		Set<?> set = null;
		try { // 查询dmgr服务对象名
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
			logger.severe("未查询到Dmgr服务对象名.");
			return null;
		}
		Iterator<?> itr = set.iterator(); // 遍历所有Server
		if (itr.hasNext()) {
			Object obj = itr.next();
			ObjectName objName = null;
			if (obj instanceof ObjectName)
				objName = (ObjectName) obj; // 获取MBean对象名
			if (objName == null)
				return null;
			map.put("node", objName.getKeyProperty("node"));
			map.put("version", objName.getKeyProperty("version"));
			map.put("cell", objName.getKeyProperty("cell"));
		}
		PerfReader.self.setTitle(PerfReader.self.getTitle().replace(" 获取中...", "")); // 置运行状态
		return map;
	}

	/**
	 * 查询dmgr上部署的所有服务的各模块性能数据报文
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
		PerfReader.self.setTitle(PerfReader.self.getTitle() + " 获取中..."); // 置运行状态
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Set<?> set = null;
		try { // 查询所有受管服务MBean对象名
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
			logger.severe("未查询到受管服务MBean对象名.");
			return null;
		}
		Iterator<?> itr = set.iterator(); // 遍历所有Server
		while (itr.hasNext()) {
			Object obj = itr.next();
			ObjectName objName = null;
			if (obj instanceof ObjectName)
				objName = (ObjectName) obj; // 获取MBean对象名
			if (objName == null)
				continue;
			String perfQryStr = "WebSphere:type=Perf,node="
					+ objName.getKeyProperty("node") + ",process="
					+ objName.getKeyProperty("name") + ",*"; // 构造查询条件,准备获取server的性能MBean
			Set<?> setPerfObjName = null;
			try { // 查询受管服务的性能MBean对象名
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
				logger.severe("未查询到受管服务性能MBean对象名.");
			}
			StatDescriptor[] arrStatDesc = new StatDescriptor[modules.length]; // 数据统计描述符
			for (int i = 0; i < modules.length; i++) { // 初始化数据统计描述符
				arrStatDesc[i] = new StatDescriptor(new String[] { modules[i] });
			}
			Object[] objStat = new Object[] { arrStatDesc, new Boolean(true) };
			String[] recursive = {
					"[Lcom.ibm.websphere.pmi.stat.StatDescriptor;",
					"java.lang.Boolean" };
			WSStats[] arrWSStats = null;
			try { // 读取性能矩阵
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
			mapWSStat.put("server_name", objName.getKeyProperty("name")); // 设置服务名
			mapWSStat.put("node_name", objName.getKeyProperty("node")); // 设置节点名
			mapWSStat.put("perf_data", arrWSStats); // 设置性能数据
			list.add(mapWSStat);
		}
		Collections.sort(list, new SortByNodeSrv()); // 按照节点名、服务名排序
		PerfReader.self.setTitle(PerfReader.self.getTitle().replace(" 获取中...", "")); // 置运行状态
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

	private static int MOD_FULL = 0; // 抽取全量数据
	private static int MOD_COMP = 1; // 抽取精简数据 compact

	/**
	 * 将类型为List<JSONObject>的参数转换字符串 缺陷: 不能通用化
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
	 * 提升字节数可读性 将KB字节数转换为可读的格式 Convert KiloBytes to Human Readable Format
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
	 * 提升流逝秒数可读性 将流逝的秒数转换为可读的格式 Convert Seconds Goes by to Human Readable Format
	 * 
	 * @return
	 */
	private String convSeconds2ReadableFormat(String seconds) {
		String result = "";
		if (seconds == null) return result;
		int days = (int) Math.floor(Double.parseDouble(seconds)
				/ (24.0 * 60.0 * 60.0));
		if (days > 0)
			result += " " + days + " 天";
		long leftSeconds = Long.parseLong(seconds) % (24 * 60 * 60);
		int hours = (int) Math.floor((double) leftSeconds / (60.0 * 60.0));
		if (hours > 0)
			result += " " + hours + " 小时";
		leftSeconds = leftSeconds % (60 * 60);
		int minutes = (int) Math.floor((double) leftSeconds / 60.0);
		if (minutes > 0)
			result += " " + minutes + " 分钟";
		leftSeconds = leftSeconds % 60;
		if (leftSeconds > 0)
			result += " " + leftSeconds + " 秒";
		return result;
	}

	/**
	 * 提取某Server关键指标并格式化为指标影射表
	 * 
	 * @param json
	 * @return
	 */
	private Map<String, String> formatSrvPerf(String json, int mod) {
		System.out.println(json);
		Map<String, String> map = new LinkedHashMap<String, String>();
		if (mod == MOD_COMP) {
			map.put("服务名称",
					getStrFromListJSON(JSONPath.read(json, "$.server_name")));
			String upperBound = convKBytes2ReadableFormat(getStrFromListJSON(JSONPath
					.read(json, "$..jvmRuntimeModule.HeapSize.upperBound")));
			map.put("最大堆分配", upperBound); // 最大堆分配
			String usedMemory = convKBytes2ReadableFormat(getStrFromListJSON(JSONPath
					.read(json, "$..jvmRuntimeModule.UsedMemory.count")));
			map.put("已用内存", usedMemory);
			String processCpuUsage = getStrFromListJSON(JSONPath.read(json,
					"$..jvmRuntimeModule.ProcessCpuUsage.count")) + " %";
			map.put("CPU使用", processCpuUsage);
		}
		if (mod == MOD_FULL) {
			map.put("节点名",
					getStrFromListJSON(JSONPath.read(json, "$.node_name")));
			map.put("服务名称",
					getStrFromListJSON(JSONPath.read(json, "$.server_name")));
			String upperBoundKBytes = getStrFromListJSON(JSONPath.read(json,
					"$..jvmRuntimeModule.HeapSize.upperBound"));
			String upperBound = convKBytes2ReadableFormat(upperBoundKBytes);
			map.put("最大堆分配", upperBound);
			String usedMemoryKBytes = getStrFromListJSON(JSONPath
					.read(json, "$..jvmRuntimeModule.UsedMemory.count"));
			if (usedMemoryKBytes != null && upperBoundKBytes != null) {
				map.put("堆使用率",
						Math.round(Double.parseDouble(usedMemoryKBytes)
								/ Double.parseDouble(upperBoundKBytes) * 100)
								+ " %");
			}
			String processCpuUsage = getStrFromListJSON(JSONPath.read(json,
					"$..jvmRuntimeModule.ProcessCpuUsage.count"));
			if (processCpuUsage != null) processCpuUsage += " %";
			else processCpuUsage = "";
			map.put("CPU使用", processCpuUsage);
			String uptime = convSeconds2ReadableFormat(getStrFromListJSON(JSONPath
					.read(json, "$..jvmRuntimeModule.UpTime.count")));
			map.put("服务运行时长", uptime);
			String webcontainerActiveCount = getStrFromListJSON(JSONPath.read(json,
					"$..WebContainer.ActiveCount.value"));
			map.put("WC活动线程数", webcontainerActiveCount);
			String webcontainerPoolUpper = getStrFromListJSON(JSONPath.read(json,
					"$..WebContainer.PoolSize.upperBound"));
			map.put("WC池上限", webcontainerPoolUpper);
			String webcontainerPoolUsed = getStrFromListJSON(JSONPath.read(json,
					"$..WebContainer.PoolSize.value"));
			if (webcontainerPoolUsed != null && webcontainerPoolUpper != null) {
				map.put("WC池已用",
						Math.round(Double.parseDouble(webcontainerPoolUsed)
								/ Double.parseDouble(webcontainerPoolUpper) * 100)
								+ " %");
			}
			String servletLiveSessions = getStrFromListJSON(JSONPath.read(json,
					"$..servletSessionsModule.LiveCount.value"));
			map.put("本地会话数", servletLiveSessions);
			map.putAll(parseWarPerf(json)); // 应用程序会话数
			map.putAll(parseJDBCPerf(json)); // JDBC连接池
		}
		return map;
	}
	
	/**
	 * 获取应用程序会话统计
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
							map.put(currKey.split("#")[0] + "会话数",
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
	 * 获取数据库连接池统计
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
					if (item.keySet().contains("JDBCTime")) { // 包含JDBCTime项说明为JDBC性能指标
						int jdbcPoolUpperBound = 0; // 连接池最大阀值
						int jdbcPoolCurrent = 0; // 连接池当前大小(当前已经建立的数据库连接数)
						int jdbcFreeConnections = 0; // 连接池中的空闲连接数
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
							if (jdbcPoolUpperBound == 0 && jdbcPoolCurrent == 0) continue; // 若两者都为0,则退出
							if (jdbcPoolUpperBound == 0) jdbcPoolUpperBound = jdbcPoolCurrent; // 池大小以当前值为准
						}
						Object objFreePoolSize = item.get("FreePoolSize");
						if (objFreePoolSize != null && objFreePoolSize instanceof JSONObject) {
							JSONObject freePoolSize = (JSONObject) objFreePoolSize;
							String sFreePoolSize = (String) freePoolSize.get("value");
							if (sFreePoolSize != null) {
								jdbcFreeConnections = Integer.parseInt(sFreePoolSize);
							}
						}
						map.put(currKey + "_jdbc_pool", // 后面添加的_jdbc_pool用于区分当前列是连接池统计信息
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
