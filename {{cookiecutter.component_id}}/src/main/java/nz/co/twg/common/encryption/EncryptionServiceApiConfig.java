package nz.co.twg.common.encryption;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** Configuration for the Encryption proxy service. */
public class EncryptionServiceApiConfig {

    @NotNull @NotBlank private String baseUrl;

    @Valid @NotNull private AuthConfig auth;

    @Min(0)
    @Max(Integer.MAX_VALUE)
    private int cachedAccessTokenPreemptiveExpirySeconds;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public AuthConfig getAuth() {
        return auth;
    }

    public void setAuth(AuthConfig auth) {
        this.auth = auth;
    }

    public int getCachedAccessTokenPreemptiveExpirySeconds() {
        return cachedAccessTokenPreemptiveExpirySeconds;
    }

    public void setCachedAccessTokenPreemptiveExpirySeconds(
            int cachedAccessTokenPreemptiveExpirySeconds) {
        this.cachedAccessTokenPreemptiveExpirySeconds = cachedAccessTokenPreemptiveExpirySeconds;
    }
}
