package br.ufmt.periscope.filter;

import java.io.IOException;

import javax.enterprise.inject.Instance;
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

import br.ufmt.periscope.model.User;
import br.ufmt.periscope.qualifier.LoggedUser;

@WebFilter(urlPatterns = { "/pages/*", "*.jsf" })
public class UserAccessFilter implements Filter {
	
	private @Inject @LoggedUser Instance<User> currentUser;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		HttpSession session = req.getSession(false);
		String pageRequested = req.getRequestURI().toString();
		String prefix = req.getContextPath();

		if (pageRequested.endsWith("login.jsf")
				|| pageRequested.endsWith("js.jsf")
				|| pageRequested.endsWith("css.jsf")) {
			chain.doFilter(request, response);
			return;
		}
		if(session == null){
			resp.sendRedirect(prefix + "/login.jsf");
			return;
		}
		if( currentUser == null){
			resp.sendRedirect(prefix + "/login.jsf");
			return;
		}else{			
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
