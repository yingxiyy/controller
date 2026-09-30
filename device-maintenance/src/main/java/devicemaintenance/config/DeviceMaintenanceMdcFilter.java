package devicemaintenance.config;

import devicemaintenance.utils.DeviceMaintenanceLogContext;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DeviceMaintenanceMdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        DeviceMaintenanceLogContext.clearAll();
        DeviceMaintenanceLogContext.ensureTraceId();
        try {
            filterChain.doFilter(request, response);
        } finally {
            DeviceMaintenanceLogContext.clearAll();
        }
    }
}
