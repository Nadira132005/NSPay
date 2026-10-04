package com.nspay.config;

import com.nspay.entities.UserModel;
import com.nspay.repository.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import javax.servlet.http.*;

/** Protect legacy endpoints too; the new customer API always derives ownership from the JWT. */
@Component
@ConditionalOnProperty(prefix="app.jwt",name="filter-enabled",havingValue="true",matchIfMissing=true)
public class AccountAccessPolicy implements HandlerInterceptor, WebMvcConfigurer {
    private final UserRepository users;
    private final AccountRepository accounts;
    public AccountAccessPolicy(UserRepository users,AccountRepository accounts) { this.users=users; this.accounts=accounts; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/api/accounts/**", "/accounts/**", "/transaction", "/transactions/**");
    }
    @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())) return true;
        Object raw=request.getAttribute("authenticatedUserId");
        if (!(raw instanceof Number)) { response.sendError(401,"Sign in to continue"); return false; }
        UserModel user=users.findById(((Number)raw).longValue()).orElse(null);
        if (user==null) { response.sendError(401); return false; }
        boolean admin=user.getRole()==com.nspay.enums.UserRole.Admin;
        boolean analyst=user.getRole()==com.nspay.enums.UserRole.Analyst;
        if (admin || (analyst && "GET".equals(request.getMethod()))) return true;
        if (!"GET".equals(request.getMethod())) { response.sendError(403,"Use your customer transfer API; account administration is restricted"); return false; }
        com.nspay.entities.AccountModel own=user.getAccountId()==null ? null : accounts.findById(user.getAccountId()).orElse(null);
        String path=request.getRequestURI().replaceAll("/$", "");
        boolean allowed=own!=null && (path.equals("/api/accounts/"+own.getId()) || path.equals("/accounts/"+own.getId())
            || path.equals("/api/accounts/card-number/"+own.getCardNumber()) || path.equals("/accounts/card-number/"+own.getCardNumber())
            || path.equals("/api/accounts/transactions/"+own.getCardNumber()) || path.equals("/transactions/"+own.getCardNumber()));
        if (!allowed) response.sendError(403,"This account or activity is not available to your user");
        return allowed;
    }
}
