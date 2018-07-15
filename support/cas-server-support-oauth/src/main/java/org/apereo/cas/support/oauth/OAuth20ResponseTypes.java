package org.apereo.cas.support.oauth;

import lombok.Getter;

/**
 * The OAuth response types (on the authorize request).
 *
 * @author Jerome Leleu
 * @since 5.0.0
 */
@Getter
public enum OAuth20ResponseTypes {

    /**
     * For authorization response type.
     */
    CODE("code"),
    /**
     * For implicit response type.
     */
    TOKEN("token"),
    /**
     * For device_code response type.
     */
    DEVICE_CODE("device_code"),
    /**
     * For implicit response type.
     */
    IDTOKEN_TOKEN("id_token token");

    private final String type;

    OAuth20ResponseTypes(final String type) {
        this.type = type;
    }
}
