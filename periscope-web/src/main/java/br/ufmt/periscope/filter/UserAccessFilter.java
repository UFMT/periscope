package br.ufmt.periscope.filter;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import br.ufmt.periscope.managedbean.SessionBean;

@WebFilter(urlPatterns = { "/pages/*", "*.jsf" })
public class UserAccessFilter implements Filter {
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		HttpSession session = req.getSession(false);
		String pageRequested = req.getRequestURI().toString();
		String prefix = req.getContextPath();

		//System.out.println(prefix);
		//System.out.println(pageRequested);
		if (pageRequested.endsWith("login.jsf")
				|| pageRequested.endsWith("js.jsf")
				|| pageRequested.endsWith("css.jsf")) {
			//System.out.println("Página de login");
			chain.doFilter(request, response);
			return;
		}
		if(session == null){
			//System.out.println("Sessio nulo ? oO");
			resp.sendRedirect(prefix + "/login.jsf");
			return;
		}
		SessionBean sessionBean = (SessionBean) session.getAttribute("sessionBean");
		if (sessionBean == null) {
			resp.sendRedirect(prefix + "/login.jsf");
			return;
		}
		//System.out.println("session não nulo");		
		if (!sessionBean.isLoggedIn()) {
			//System.out.println("Não tem usuário logado.");
			resp.sendRedirect(prefix + "/login.jsf");
		} else {
			//System.out.println("Normal");
			chain.doFilter(request, response);

		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {

	}

	@Override
	public void destroy() {

	}

}
