package app.coveredbridge.web.filter;

import java.io.IOException;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

public class AuthFilter implements Filter {

  /* (non-Javadoc)
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {
    // nothing to destroy
  }

  /* (non-Javadoc)
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
   * javax.servlet.ServletResponse, javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest req, ServletResponse res,
                       FilterChain chain) throws IOException, ServletException {
    HttpServletResponse response = (HttpServletResponse) res;
    Subject subject = SecurityUtils.getSubject();
    if (subject != null) {
      if (subject.isAuthenticated()) {
        // Logged-in user found, so just continue request.
        chain.doFilter(req, res);
      } else {
        response.sendRedirect("/home/login");
      }
    } else {
      response.sendRedirect("/home/login");
    }
  }

  /* (non-Javadoc)
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  @Override
  public void init(FilterConfig arg0) throws ServletException {
    // nothing to init
  }

}
