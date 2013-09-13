package org.ziggrid.driver;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atmosphere.cpr.AsyncSupport;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResponse;
import org.ziggrid.utils.reflection.Reflection;

@SuppressWarnings("serial")
public class UpdateServlet extends HttpServlet {
    private AtmosphereFramework framework;

    public UpdateServlet() {
    	try {
    		framework = new AtmosphereFramework(false, false);
    	} catch (Exception ex) {
    		ex.printStackTrace();
    	}
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
    	super.init(config);
		String asyncSupportClass = config.getServletContext().getInitParameter("org.ziggrid.asyncSupportClass");
		if (asyncSupportClass == null)
			throw new ServletException("Cannot initialize adapter without org.ziggrid.asyncSupportClass property");
		try {
			AsyncSupport<?> support = (AsyncSupport<?>) Reflection.create(getClass().getClassLoader(), asyncSupportClass, framework.getAtmosphereConfig());
			framework.setAsyncSupport(support);
			framework.addAtmosphereHandler("/*", new UpdateHandler(config.getServletContext().getInitParameter("org.ziggrid.couchUrl"), config.getServletContext().getInitParameter("org.ziggrid.bucket")));
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

	@Override
	public void destroy() {
		framework.destroy();
	}
}
