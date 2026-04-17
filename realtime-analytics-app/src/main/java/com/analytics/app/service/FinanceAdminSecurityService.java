package com.analytics.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FinanceAdminSecurityService {

    @Value("${analytics.finance.admin-api-key:}")
    private String adminApiKey;

    public void validateAdminKey(String providedKey) {
        if (adminApiKey == null || adminApiKey.isBlank()) {
            return;
        }

        if (providedKey == null || !adminApiKey.equals(providedKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing finance admin key");
        }
    }
}
