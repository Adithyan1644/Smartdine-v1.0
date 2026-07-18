package com.smartdine.coreheart;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@Component
public class TenantInterceptor implements HandlerInterceptor {
	
	 @Override
	    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
	        // If the Waiter/Biller sends their ID in the Header, set the TenantContext
	        String tenantId = request.getHeader("X-Restaurant-ID");
	        
	        if (tenantId != null && !tenantId.isEmpty()) {
	            TenantContext.setRestaurantId(UUID.fromString(tenantId));
	        }
	        return true;
	    }

	    @Override
	    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
	        // CRITICAL for Production: Clear the thread so the next request doesn't see old data
	        TenantContext.clear();
	    }

}
