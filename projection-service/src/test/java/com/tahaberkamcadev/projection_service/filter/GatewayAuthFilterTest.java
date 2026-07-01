package com.tahaberkamcadev.projection_service.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.FilterChain;

class GatewayAuthFilterTest {

    private GatewayAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GatewayAuthFilter();
        ReflectionTestUtils.setField(filter, "gatewaySecret", "test-secret");
    }

    @Test
    void rejectsRequestWithoutGatewaySecret() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/catalog/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void allowsRequestWithValidGatewaySecret() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/catalog/products");
        request.addHeader("X-Gateway-Secret", "test-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
    }
}
