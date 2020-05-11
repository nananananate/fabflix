import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Servlet Filter implementation class LoginFilter
 */
@WebFilter(filterName = "LoginFilter", urlPatterns = "/*")
public class LoginFilter implements Filter {

    private final ArrayList<String> allowedURIs = new ArrayList<>();
    private final ArrayList<String> protectedURIs = new ArrayList<>();

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        System.out.println("LoginFilter: " + httpRequest.getRequestURI());

        // Check if this URL is allowed to access without logging in
        if (this.isUrlAllowedWithoutLogin(httpRequest.getRequestURI()) && !this.requiresEmployee(httpRequest.getRequestURI())) {
            // Keep default action: pass along the filter chain
            System.out.println("filter not needed for this page");
            chain.doFilter(request, response);
            return;
        }

        // Redirect to login page if the "user" attribute doesn't exist in session
        if (httpRequest.getSession().getAttribute("user") == null) {
            System.out.println("Login filter activated, redirection sent");
            httpResponse.sendRedirect("login.html");
            return;
        } else if(this.requiresEmployee(httpRequest.getRequestURI())) {
            if (! httpRequest.getSession().getAttribute("role").equals("employee")) {
                System.out.println("Dashboard filter activated, redirection sent");
                httpResponse.sendRedirect("employeeAccess.html");
                return;
            } else {
                System.out.println("***************** BALONEY ************");
                chain.doFilter(request, response);
                return;
            }
        } else {
            System.out.println("***************** MACARONI ************");
            chain.doFilter(request, response);
            return;
        }
    }

    private boolean isUrlAllowedWithoutLogin(String requestURI) {
        /*
         Setup your own rules here to allow accessing some resources without logging in
         Always allow your own login related requests(html, js, servlet, etc..)
         You might also want to allow some CSS files, etc..
         */
        return allowedURIs.stream().anyMatch(requestURI.toLowerCase()::endsWith);
    }

    private boolean requiresEmployee(String requestURI){
        return protectedURIs.stream().anyMatch(requestURI.toLowerCase()::endsWith);
    }

    public void init(FilterConfig fConfig) {
        allowedURIs.add("login.html");
        allowedURIs.add("login.js");
        allowedURIs.add("api/login");
        protectedURIs.add("_dashboard.html");
        protectedURIs.add("dashboard.js");
    }

    public void destroy() {
        // ignored.
    }

}
