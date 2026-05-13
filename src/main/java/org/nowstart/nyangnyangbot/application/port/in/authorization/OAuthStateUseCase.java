package org.nowstart.nyangnyangbot.application.port.in.authorization;

public interface OAuthStateUseCase {

    String generateState();

    boolean matches(String expected, String actual);
}
