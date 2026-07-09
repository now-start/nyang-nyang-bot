package org.nowstart.nyangnyangbot.adapter.in.web;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class WebPermissionModelAdviceTest {

    private final WebPermissionModelAdvice advice = new WebPermissionModelAdvice();

    @Test
    void isAdmin_ShouldReturnTrue_WhenAuthenticationHasAdminRole() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        then(advice.isAdmin(authentication)).isTrue();
    }

    @Test
    void isAdmin_ShouldReturnFalse_WhenAuthenticationDoesNotHaveAdminRole() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "user1",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        then(advice.isAdmin(authentication)).isFalse();
    }

    @Test
    void currentUserId_ShouldReturnNull_WhenAuthenticationIsAnonymous() {
        var authentication = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );

        then(advice.currentUserId(authentication)).isNull();
    }

    @Test
    void currentUserId_ShouldReturnAuthenticationName_WhenAuthenticationIsUser() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "user1",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        then(advice.currentUserId(authentication)).isEqualTo("user1");
    }
}
