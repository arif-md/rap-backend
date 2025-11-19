package x.y.z.backend.config;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
        // Check if the parameter type is assignable from CurrentUser (interface or implementation)
        return CurrentUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter arg0, ModelAndViewContainer arg1, NativeWebRequest arg2,
            WebDataBinderFactory arg3) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        
        if ("anonymousUser".equals(principal)) {
            throw new IllegalStateException("Anonymous user not allowed");
        }
        
        // Handle UserPrincipal from JWT authentication
        if (principal instanceof CurrentUser) {
            return (CurrentUser) principal;
        }
        
        // If principal is not CurrentUser, throw exception
        throw new IllegalStateException(
            "Principal is not an instance of CurrentUser. Found: " + principal.getClass().getName()
        );
    }
}
