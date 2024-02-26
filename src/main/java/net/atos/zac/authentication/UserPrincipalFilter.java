/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.authentication;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.wildfly.security.http.oidc.AccessToken;
import org.wildfly.security.http.oidc.OidcPrincipal;
import org.wildfly.security.http.oidc.OidcSecurityContext;

import net.atos.zac.zaaksturing.ZaakafhandelParameterService;
import net.atos.zac.zaaksturing.model.ZaakafhandelParameters;

@WebFilter(filterName = "UserPrincipalFilter")
public class UserPrincipalFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(UserPrincipalFilter.class.getName());

    private static final String ROL_DOMEIN_ELK_ZAAKTYPE = "domein_elk_zaaktype";

    private static final String GROUP_MEMBERSHIP_CLAIM_NAME = "group_membership";

    private ZaakafhandelParameterService zaakafhandelParameterService;

    @Inject
    public UserPrincipalFilter(ZaakafhandelParameterService zaakafhandelParameterService) {
        this.zaakafhandelParameterService = zaakafhandelParameterService;
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                         final FilterChain filterChain) throws ServletException, IOException {
        if (servletRequest instanceof HttpServletRequest) {
            final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            final OidcPrincipal principal = (OidcPrincipal) httpServletRequest.getUserPrincipal();

            if (principal != null) {
                HttpSession httpSession = httpServletRequest.getSession(true);
                LoggedInUser loggedInUser = SecurityUtil.getLoggedInUser(httpSession);
                if (loggedInUser != null && !loggedInUser.getId().equals(principal.getName())) {
                    LOG.info(String.format("HTTP session of user '%s' on context path %s is invalidated",
                                           loggedInUser.getId(), httpServletRequest.getServletContext().getContextPath()));
                    httpSession.invalidate();
                    loggedInUser = null;
                    httpSession = httpServletRequest.getSession(true);
                }

                if (loggedInUser == null) {
                    loggedInUser = createLoggedInUser(principal.getOidcSecurityContext());
                    SecurityUtil.setLoggedInUser(httpSession, loggedInUser);
                    LOG.info(String.format("User logged in: '%s' with roles: %s, groups: %s en zaaktypen: %s",
                                           loggedInUser.getId(),
                                           loggedInUser.getRoles(), loggedInUser.getGroupIds(),
                                           loggedInUser.isGeautoriseerdVoorAlleZaaktypen() ? "ELK-ZAAKTYPE" :
                                                   loggedInUser.getGeautoriseerdeZaaktypen()));
                }
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }

    private LoggedInUser createLoggedInUser(final OidcSecurityContext context) {
        final AccessToken accessToken = context.getToken();
        final Set<String> roles = Set.copyOf(accessToken.getRealmAccessClaim().getRoles());
        return new LoggedInUser(accessToken.getPreferredUsername(),
                                accessToken.getGivenName(),
                                accessToken.getFamilyName(),
                                accessToken.getName(),
                                accessToken.getEmail(),
                                roles,
                                Set.copyOf(accessToken.getStringListClaimValue(GROUP_MEMBERSHIP_CLAIM_NAME)),
                                getGeautoriseerdeZaaktypen(roles));
    }

    private Set<String> getGeautoriseerdeZaaktypen(final Set<String> roles) {
        if (roles.contains(ROL_DOMEIN_ELK_ZAAKTYPE)) {
            return null;
        } else {
            return zaakafhandelParameterService.listZaakafhandelParameters().stream()
                                               .filter(zaakafhandelParameters -> zaakafhandelParameters.getDomein() != null &&
                                                                                 roles.contains(zaakafhandelParameters.getDomein()))
                                               .map(ZaakafhandelParameters::getZaaktypeOmschrijving)
                                               .collect(Collectors.toUnmodifiableSet());
        }
    }
}
