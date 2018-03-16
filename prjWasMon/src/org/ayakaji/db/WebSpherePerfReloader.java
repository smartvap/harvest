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

	private AdminClient ac = null; // dmgr���ӿͻ���
	private String connId = null; // ��ʽ:<ip>:<port>����ID�������ؽ�dmgr����
	private String dmgrId = null; // ��ʽ:<hostname>:<ip>���ṹ�ڵ��������ں͵�ǰѡ������ڵ�ƥ�䣬��ƥ��ɹ�������Ҫ�����ݶ�ʱˢ�µ����
	
	/**
	 * ��ʼ��
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
	 * ���Dmgr�����Ƿ���Ч
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
	 * ����/�ؽ�DMGR����
	 * @param connId
	 * @return
	 */
	private AdminClient rebuildDmgrConnection(String connId) {
		if (connId == null || connId.equals(""))
			return null;
		if (ac != null)
			ac = null;
		if (!PerfReader.mapConn.containsKey(connId)) { // ����dmgr����ӳ����в�����,�����µ�����,����¼������ӳ���
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
	 * ��Dmgr�˼�������
	 */
	private void loadPerfFromDmgr() {
		// 1.��AdminClient��ȡ����
		if (!ifDmgrConnAvailable(ac)) rebuildDmgrConnection(connId);
		List<Map<String, Object>> list = PerfReader.self.qrySrvPerf(ac);
		List<Map<String, String>> listFormat = new ArrayList<Map<String, String>>();
		for (Map<String, Object> map : list) {
			String json = JsonUtil.toJson(map);
			Map<String, String> m = PerfReader.self.formatSrvPerf(json,
					PerfReader.MOD_FULL);
			listFormat.add(m);
		}
		// 2.ϵ�л�
		String serial = JsonUtil.toJson(listFormat);
		// 3.���
		// 4.��������
		PerfReader.PerfPanel.self.load(listFormat);
	}

	/**
	 * �����ֿܲ��ȡ����
	 */
	private void loadPerfFromWarehouse() {
		// 1.��ȡ����
		// 2.��ϵ�л�
		// 3.��������
	}

	/**
	 * �Ƿ��Ѽ�����������
	 */
	private void ifLoadedLatestPerfData() {
	}

	@Override
	public void run() {

	}

}
