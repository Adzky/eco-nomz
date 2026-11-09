package id.co.economz.eco_nomz.auth;

import id.co.economz.eco_nomz.user.UserRole;

public enum RegistrationType {
    CUSTOMER(UserRole.CUSTOMER),
    RESTAURANT_OWNER(UserRole.RESTAURANT_OWNER);

    private final UserRole userRole;

    RegistrationType(UserRole userRole) {
        this.userRole = userRole;
    }

    public UserRole toUserRole() {
        return userRole;
    }
}
