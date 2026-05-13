package com.studlgu.vkbot.controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class CallbackLoggingFilter extends OncePerRequestFilter {

    private static final int REQUEST_CACHE_LIMIT = 64 * 1024;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/callback".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            log.info("Incoming callback request method={} uri={} body={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    body(wrappedRequest.getContentAsByteArray(), request.getCharacterEncoding()));
            log.info("Callback response status={} body={}",
                    wrappedResponse.getStatus(),
                    body(wrappedResponse.getContentAsByteArray(), wrappedResponse.getCharacterEncoding()));
            wrappedResponse.copyBodyToResponse();
        }
    }

    private String body(byte[] bytes, String characterEncoding) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        Charset charset = characterEncoding == null
                ? StandardCharsets.UTF_8
                : Charset.forName(characterEncoding);
        return new String(bytes, charset);
    }
}
