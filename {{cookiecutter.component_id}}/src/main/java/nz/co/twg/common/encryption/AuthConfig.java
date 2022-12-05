package nz.co.twg.common.encryption;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** Configuration for properties used for communicating with the IdP for access token retrieval. */
public class AuthConfig {

    @NotNull @NotBlank private String tokenUrl;

    @NotNull @NotBlank private String clientId;

    @NotNull @NotBlank private String clientSecret;

    @NotNull @NotBlank private String scope;

    public AuthConfig() {}

    /** Constructor. */
    public AuthConfig(String tokenUrl, String clientId, String clientSecret, String scope) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
