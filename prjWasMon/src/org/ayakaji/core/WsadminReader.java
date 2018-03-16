package org.ayakaji.core;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class WsadminReader {
//	private static TraceComponent tc = Tr.register(WasxShell.class,
//			"Scripting", "com.ibm.ws.scripting.resources.wscpMessage");
	private ResourceBundle bundle = null;
	private String prompt = null;
	private Thread shutdownHook_Thread = null;

	/**
	 * @param args
	 * @throws MalformedURLException
	 */
	public static void main(String[] args) {
		WsadminReader wr = new WsadminReader();
	}

	private WsadminReader() {
		bundle = ResourceBundle.getBundle(
				"com.ibm.ws.scripting.resources.wscpMessage",
				Locale.getDefault());
		prompt = "¿ØÖÆÌ¨>";
		shutdownHook_Thread = new Thread() {
			public void run() {
//				WasxShell.this.leaving(false);
			}
		};
		Runtime.getRuntime().addShutdownHook(this.shutdownHook_Thread);
	}

}
