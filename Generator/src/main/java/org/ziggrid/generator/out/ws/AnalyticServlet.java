package org.ziggrid.generator.out.ws;

import java.io.IOException;
import java.lang.reflect.Constructor;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atmosphere.cpr.AsyncSupport;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResponse;

@SuppressWarnings("serial")
public class AnalyticServlet extends HttpServlet {
    private AtmosphereFramework framework;
	private String asyncSupportClass = "org.zinutils.http.ws.AsyncProcessor";

	public AnalyticServlet() {
    	framework = new AtmosphereFramework(false, false);
    }

	@Override
    public void init(ServletConfig config) throws ServletException {
    	super.init(config);
		try {
			Class<?> cls = Class.forName(asyncSupportClass);
			Constructor<?> ctor = cls.getConstructor(AtmosphereConfig.class);
			AsyncSupport<?> support = (AsyncSupport<?>) ctor.newInstance(framework.getAtmosphereConfig());
			framework.setAsyncSupport(support);
			framework.addAtmosphereHandler("/*", new AnalyticHandler());
	    	framework.init(config);
		} catch (Exception ex) {
			throw new ServletException(ex);
		}
    }
    

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handle(req, resp);
	}
    
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handle(req, resp);
	}

	private void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		AtmosphereRequest areq = new AtmosphereRequest.Builder().request(req).build();
		AtmosphereResponse.Builder builder = new AtmosphereResponse.Builder();
		AtmosphereResponse aresp = builder.response(resp).request(areq).build();
		framework.doCometSupport(areq, aresp);
	}
}
