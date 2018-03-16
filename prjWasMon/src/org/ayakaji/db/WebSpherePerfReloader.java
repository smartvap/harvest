package org.ayakaji.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ayakaji.core.JsonUtil;
import org.ayakaji.core.PerfReader;
import org.ini4j.Profile.Section;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.exception.ConnectorException;

public class WebSpherePerfReloader implements Runnable {

	private AdminClient ac = null; // dmgr连接客户端
	private String connId = null; // 格式:<ip>:<port>连接ID，用于重建dmgr连接
	private String dmgrId = null; // 格式:<hostname>:<ip>树结构节点名，用于和当前选择的树节点匹配，若匹配成功，则需要将数据定时刷新到面板
	
	/**
	 * 初始化
	 * @param ac
	 * @param connId
	 * @param dmgrId
	 */
	private WebSpherePerfReloader(AdminClient ac, String connId, String dmgrId) {
		this.ac = ac;
		this.connId = connId;
		this.dmgrId = dmgrId;
	}
	
	/**
	 * 检查Dmgr连接是否有效
	 * 
	 * @return
	 */
	private boolean ifDmgrConnAvailable(AdminClient ac) {
		if (ac != null) {
			String sessId = null;
			try {
				sessId = ac.isAlive().getSessionId();
			} catch (ConnectorException e) {
				e.printStackTrace();
			}
			if (sessId != null && !sessId.equals(""))
				return true;
		}
		return false;
	}

	/**
	 * 创建/重建DMGR连接
	 * @param connId
	 * @return
	 */
	private AdminClient rebuildDmgrConnection(String connId) {
		if (connId == null || connId.equals(""))
			return null;
		if (ac != null)
			ac = null;
		if (!PerfReader.mapConn.containsKey(connId)) { // 若在dmgr连接映射表中不存在,则建立新的连接,并记录到连接映射表
			String host = connId.split("_")[1];
			Section sect = PerfReader.NaviPanel.getCfgByHost(host);
			if (sect == null) return null;
			ac = PerfReader.self.createConnection(host,
					sect.get("port"), sect.get("connTyp"), sect.get("ifSec"),
					sect.get("userName"), sect.get("password"),
					sect.get("trustStorPath"), sect.get("keyStorPath"),
					sect.get("keyStorType"), sect.get("trustStorPass"),
					sect.get("keyStorPass"));
			if (ac == null)	return null;
		}
		return ac;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

	/**
	 * 从Dmgr端加载数据
	 */
	private void loadPerfFromDmgr() {
		// 1.从AdminClient读取数据
		if (!ifDmgrConnAvailable(ac)) rebuildDmgrConnection(connId);
		List<Map<String, Object>> list = PerfReader.self.qrySrvPerf(ac);
		List<Map<String, String>> listFormat = new ArrayList<Map<String, String>>();
		for (Map<String, Object> map : list) {
			String json = JsonUtil.toJson(map);
			Map<String, String> m = PerfReader.self.formatSrvPerf(json,
					PerfReader.MOD_FULL);
			listFormat.add(m);
		}
		// 2.系列化
		String serial = JsonUtil.toJson(listFormat);
		// 3.入库
		// 4.输出到面板
		PerfReader.PerfPanel.self.load(listFormat);
	}

	/**
	 * 从性能仓库读取数据
	 */
	private void loadPerfFromWarehouse() {
		// 1.读取数据
		// 2.反系列化
		// 3.输出到面板
	}

	/**
	 * 是否已加载最新数据
	 */
	private void ifLoadedLatestPerfData() {
	}

	@Override
	public void run() {

	}

}
