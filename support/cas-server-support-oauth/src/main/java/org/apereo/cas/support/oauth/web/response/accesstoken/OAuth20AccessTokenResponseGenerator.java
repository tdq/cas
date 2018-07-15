package org.apereo.cas.support.oauth.web.response.accesstoken;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.OAuth20ResponseTypes;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.ticket.accesstoken.AccessToken;
import org.apereo.cas.ticket.refreshtoken.RefreshToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This is {@link OAuth20AccessTokenResponseGenerator}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Slf4j
public class OAuth20AccessTokenResponseGenerator implements AccessTokenResponseGenerator {
    private static final int DEVICE_REQUEST_INTERVAL = 15;

    private static final JsonFactory JSON_FACTORY = new JsonFactory(new ObjectMapper().findAndRegisterModules());

    /**
     * The Resource loader.
     */
    @Autowired
    protected ResourceLoader resourceLoader;

    /**
     * CAS settings.
     */
    @Autowired
    protected CasConfigurationProperties casProperties;

    @Override
    @SneakyThrows
    public void generate(final HttpServletRequest request,
                         final HttpServletResponse response,
                         final OAuthRegisteredService registeredService,
                         final Service service,
                         final OAuth20TokenGeneratedResult result,
                         final long accessTokenTimeout,
                         final OAuth20ResponseTypes responseType,
                         final CasConfigurationProperties casProperties) {

        if (OAuth20ResponseTypes.DEVICE_CODE == responseType) {
            generateResponseForDeviceToken(request, response, registeredService, service, result, casProperties);
        } else {
            generateResponseForAccessToken(request, response, registeredService, service, result, accessTokenTimeout, responseType);
        }
    }

    @SneakyThrows
    private void generateResponseForDeviceToken(final HttpServletRequest request,
                                                final HttpServletResponse response,
                                                final OAuthRegisteredService registeredService,
                                                final Service service,
                                                final OAuth20TokenGeneratedResult result,
                                                final CasConfigurationProperties casProperties) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try (val jsonGenerator = getResponseJsonGenerator(response)) {
            jsonGenerator.writeStartObject();
            val uri = casProperties.getServer().getPrefix()
                .concat(OAuth20Constants.BASE_OAUTH20_URL)
                .concat("/")
                .concat(OAuth20Constants.DEVICE_AUTHZ_URL);
            jsonGenerator.writeStringField(OAuth20Constants.DEVICE_VERIFICATION_URI, uri);
            jsonGenerator.writeStringField(OAuth20Constants.DEVICE_USER_CODE, result.getUserCode().get());
            jsonGenerator.writeStringField(OAuth20Constants.DEVICE_CODE, result.getDeviceCode().get());
            jsonGenerator.writeNumberField(OAuth20Constants.DEVICE_INTERVAL, DEVICE_REQUEST_INTERVAL);
            jsonGenerator.writeEndObject();
        }
    }

    private void generateResponseForAccessToken(final HttpServletRequest request, final HttpServletResponse response,
                                                final OAuthRegisteredService registeredService, final Service service,
                                                final OAuth20TokenGeneratedResult result, final long timeout,
                                                final OAuth20ResponseTypes responseType) throws Exception {
        val accessToken = result.getAccessToken().get();
        val refreshToken = result.getRefreshToken().get();
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try (val jsonGenerator = getResponseJsonGenerator(response)) {
            jsonGenerator.writeStartObject();
            generateJsonInternal(request, response, jsonGenerator, accessToken,
                refreshToken, timeout, service, registeredService, responseType);
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Gets response json generator.
     *
     * @param response the response
     * @return the response json generator
     * @throws IOException the io exception
     */
    protected JsonGenerator getResponseJsonGenerator(final HttpServletResponse response) throws IOException {
        return JSON_FACTORY.createGenerator(response.getWriter());
    }

    /**
     * Generate internal.
     *
     * @param request           the request
     * @param response          the response
     * @param jsonGenerator     the json generator
     * @param accessTokenId     the access token id
     * @param refreshTokenId    the refresh token id
     * @param timeout           the timeout
     * @param service           the service
     * @param registeredService the registered service
     * @param responseType      the response type
     * @throws Exception the exception
     */
    protected void generateJsonInternal(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final JsonGenerator jsonGenerator,
                                        final AccessToken accessTokenId,
                                        final RefreshToken refreshTokenId,
                                        final long timeout,
                                        final Service service,
                                        final OAuthRegisteredService registeredService,
                                        final OAuth20ResponseTypes responseType) throws Exception {
        jsonGenerator.writeStringField(OAuth20Constants.ACCESS_TOKEN, accessTokenId.getId());
        jsonGenerator.writeStringField(OAuth20Constants.TOKEN_TYPE, OAuth20Constants.TOKEN_TYPE_BEARER);
        jsonGenerator.writeNumberField(OAuth20Constants.EXPIRES_IN, timeout);
        if (refreshTokenId != null) {
            jsonGenerator.writeStringField(OAuth20Constants.REFRESH_TOKEN, refreshTokenId.getId());
        }
    }
}
