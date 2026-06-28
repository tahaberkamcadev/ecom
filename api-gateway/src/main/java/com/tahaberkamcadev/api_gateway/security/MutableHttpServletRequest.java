package com.tahaberkamcadev.api_gateway.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

public class MutableHttpServletRequest extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders = new HashMap<>();
    private final Set<String> removedHeaders = new HashSet<>();

    public MutableHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    public void putHeader(String name, String value) {
        customHeaders.put(name, value);
    }

    public void removeHeader(String name) {
        removedHeaders.add(normalize(name));
        customHeaders.remove(name);
    }

    @Override
    public String getHeader(String name) {
        if (removedHeaders.contains(normalize(name))) {
            return customHeaders.get(name);
        }
        String value = customHeaders.get(name);
        return value != null ? value : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (removedHeaders.contains(normalize(name)) && !customHeaders.containsKey(name)) {
            return Collections.emptyEnumeration();
        }
        if (customHeaders.containsKey(name)) {
            return Collections.enumeration(List.of(customHeaders.get(name)));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new HashSet<>(customHeaders.keySet());
        Enumeration<String> superNames = super.getHeaderNames();
        while (superNames.hasMoreElements()) {
            String headerName = superNames.nextElement();
            if (!removedHeaders.contains(normalize(headerName))) {
                names.add(headerName);
            }
        }
        return Collections.enumeration(names);
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
