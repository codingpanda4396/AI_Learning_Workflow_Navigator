package navigator.api.auth;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;

public final class CurrentUserHolder {

    private static final ThreadLocal<CurrentUser> CURRENT = new ThreadLocal<>();

    private CurrentUserHolder() {
    }

    public static void set(CurrentUser user) {
        CURRENT.set(user);
    }

    public static CurrentUser get() {
        return CURRENT.get();
    }

    public static CurrentUser require() {
        CurrentUser user = CURRENT.get();
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED, "authentication required");
        }
        return user;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
