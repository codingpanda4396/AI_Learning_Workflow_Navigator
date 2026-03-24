package navigator.api.auth;

public final class AuthCookieSupport {

    public static final String COOKIE_NAME = "lumina_session";
    public static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 7;

    private AuthCookieSupport() {
    }
}
