import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.AdminClientFactory;
import com.ibm.websphere.management.exception.ConnectorException;

public class JApachePerfReader {

	public JApachePerfReader(String url) {
		String html = getURLResponse(url);
		System.out.println(format(html));
	}

	private String getURLResponse(String arg0) {
		URL url = null;
		try {
			url = new URL(arg0);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		try {
			conn.setRequestMethod("GET"); // 设置请求方法
		} catch (ProtocolException e) {
			e.printStackTrace();
			return null;
		}
		conn.setUseCaches(false); // 不使用缓存
		conn.setInstanceFollowRedirects(false); // 不自动跟随跳转
		conn.setConnectTimeout(10000); // 连接超时时间
		conn.setReadTimeout(10000); // 读取流超时时间
		try {
			conn.connect(); // 建立连接
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		int respCode;
		try {
			respCode = conn.getResponseCode(); // 返回码
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		if (respCode != HttpURLConnection.HTTP_OK)
			return null;
		InputStream instream = null;
		try {
			instream = conn.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		InputStreamReader in = new InputStreamReader(instream);
		BufferedReader reader = new BufferedReader(in);
		StringBuffer buff = new StringBuffer();
		String strRet = null;
		if (conn.getContentLength() != -1) {
			char[] cBuf = new char[conn.getContentLength()];
			try {
				if (reader.read(cBuf, 0, cBuf.length) != -1) { // One-time read
					buff.append(String.valueOf(cBuf));
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			strRet = buff.toString();
			buff = null;
		} else {
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					buff.append(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			strRet = buff.toString();
			buff = null;
		}
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		reader = null;
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		in = null;
		try {
			instream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		instream = null;
		conn.disconnect();
		return strRet;
	}

	/**
	 * 中英文对照
	 */
	private static final Map<String, String> mapDict = new HashMap<String, String>() {
		private static final long serialVersionUID = -1439909729385067514L;

		{
			put("Server Version", "Server版本");
			put("Server Built", "Build时间");
			put("Current Time", "主机时间");
			put("Restart Time", "最近一次重起");
			put("Parent Server Generation", "父进程世代编号(SIGHUP重起次数)");
			put("Server uptime", "在线时长");
			put("CPU Usage", "CPU使用率");
		}
	};

	private Map<String, String> format(String html) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		CustomizedHtmlFilter filter = new CustomizedHtmlFilter("dt", "");
		Parser parser = Parser.createParser(html, "UTF-8");
		NodeList nodes = null;
		try {
			nodes = parser.extractAllNodesThatMatch(filter);
		} catch (ParserException e) {
			e.printStackTrace();
		}
		SimpleNodeIterator itr = nodes.elements();
		while (itr.hasMoreNodes()) {
			String txt = itr.nextNode().toPlainTextString();
			Pattern p = Pattern.compile("(.*): (.*)");
			Matcher m = p.matcher(txt);
			if (m.find()) {
				String key = mapDict.get(m.group(1));
				if (key != null)
					map.put(key, m.group(2));
			}
		}
		return map;
	}

	public static void main(String[] args) throws IOException, ParserException {
		AdminClient ac = createConnection();
		Set<?> set = null;
		try {
			set = ac.queryNames(new ObjectName("WebSphere:type=WebServer"
					+ ",*"), null);
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
		} catch (ConnectorException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		Iterator<?> itr = set.iterator();
		while (itr.hasNext()) {
			Object obj = itr.next();
			System.out.println(obj);
		}
		// JApachePerfReader apr = new JApachePerfReader(
		// "http://10.17.249.115/server-status");
		// if (apr != null)
		// apr = null;

	}

	private static AdminClient createConnection() {
		final Properties properties = new Properties();
		properties.setProperty("host", "10.17.249.115");
		properties.setProperty("port", "8879");
		properties.setProperty("type", "SOAP");
		properties.setProperty("securityEnabled", "true");
		properties.setProperty("username", "wasadmin");
		properties.setProperty("password", "WebJ2ee");
		properties.setProperty("javax.net.ssl.trustStore", new File(
				JApachePerfReader.class.getResource("DummyClientTrustFile.jks")
						.getFile()).getAbsolutePath());
		properties.setProperty("javax.net.ssl.keyStore", new File(
				JApachePerfReader.class.getResource("DummyClientKeyFile.jks")
						.getFile()).getAbsolutePath());
		properties.setProperty("javax.net.ssl.keyStoreType", "JKS");
		properties.setProperty("javax.net.ssl.trustStorePassword", "WebAS");
		properties.setProperty("javax.net.ssl.keyStorePassword", "WebAS");
		AdminClient ac = null;
		try {
			ac = AdminClientFactory.createAdminClient(properties);
		} catch (ConnectorException e) {
			e.printStackTrace();
		}
		return ac;
	}

	private static class CustomizedHtmlFilter implements NodeFilter {
		private static final long serialVersionUID = 4873996336114524566L;
		private String tagName = null;
		private String prefix = null;

		private CustomizedHtmlFilter(String tagName, String prefix) {
			this.tagName = tagName;
			this.prefix = prefix;
		}

		@Override
		public boolean accept(Node arg0) {
			if (arg0.getText().equals(tagName)
					&& arg0.toPlainTextString().startsWith(prefix))
				return true;
			return false;
		}
	}

}
