package com.studlgu.vkbot.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackLoggingFilterTest {

    @Test
    void logsCallbackRequestBodyAndResponseBody() throws ServletException, IOException {
        Logger logger = (Logger) LoggerFactory.getLogger(CallbackLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/callback");
            request.setContentType(MediaType.APPLICATION_JSON_VALUE);
            request.setContent("{\"type\":\"message_new\"}".getBytes());
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain(new HttpServlet() {
                @Override
                protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                    req.getInputStream().readAllBytes();
                    resp.getWriter().write("ok");
                }
            });

            new CallbackLoggingFilter().doFilter(request, response, chain);

            assertThat(response.getContentAsString()).isEqualTo("ok");
            assertThat(appender.list)
                    .filteredOn(event -> event.getLevel().equals(Level.INFO))
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .anySatisfy(message -> assertThat(message).contains(
                            "Incoming callback request",
                            "{\"type\":\"message_new\"}"
                    ))
                    .anySatisfy(message -> assertThat(message).contains(
                            "Callback response",
                            "ok"
                    ));
        } finally {
            logger.detachAppender(appender);
        }
    }
}
