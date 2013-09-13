package org.ziggrid.generator.out.ws;

import java.io.File;

import org.ziggrid.utils.http.GPServletDefn;
import org.ziggrid.utils.http.InlineServer;

public class AnalyticServer extends Thread {
	private InlineServer server;
	private static AnalyticServer main;
	static boolean realtime;
	static String config;
	public static int delay;

	public static void main(String[] argv) {
		main = new AnalyticServer();
		boolean usage = false;
		for (int i=0;i<argv.length && !usage;i++) {
			if ("--realtime".equals(argv[i]))
				realtime = true;
			else if ("--delay".equals(argv[i]) && i+1 < argv.length) {
				delay = Integer.parseInt(argv[i+1]);
				i++;
			} else if ("--vpath".equals(argv[i]) && i+2 < argv.length) {
				main.server.addVirtualDir(argv[i+1], new File(argv[i+2]));
				i+=2;
			} else if (argv[i].startsWith("--"))
				usage = true;
			else if (config == null)
				config = argv[i];
			else
				usage = true;
		}
		if (usage || config == null)
		{
			System.out.println("Usage: Assessment [--realtime] [--vpath from to] config");
			System.exit(1);
		}
		main.run();
	}

	public AnalyticServer() {
		int port = 10095;
		server = new InlineServer(port, AnalyticServlet.class.getName());
	}

	@Override
	public void run() {
		GPServletDefn servlet = server.getBaseServlet();
		servlet.setContextPath("/analytic");
		servlet.setServletPath("/server");
		servlet.addClassDir(new File("."));
		server.run();
	}

	public static AnalyticServer getInstance() {
		return main;
	}
	
	public void close() {
		server.pleaseExit();
	}
}
