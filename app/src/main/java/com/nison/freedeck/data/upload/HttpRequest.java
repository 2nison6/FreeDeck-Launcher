package com.nison.freedeck.data.upload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** The start of an HTTP request: method, path and headers. The body, if any, is left in the stream. */
final class HttpRequest {

    private static final int MAX_HEAD_BYTES = 16 * 1024;

    final String method;
    final String path;
    private final Map<String, String> headers;

    private HttpRequest(String method, String path, Map<String, String> headers) {
        this.method = method;
        this.path = path;
        this.headers = headers;
    }

    /** Reads up to the blank line that ends the headers. @return null if what arrived is not an HTTP request */
    static HttpRequest read(InputStream in) throws IOException {
        ByteArrayOutputStream head = new ByteArrayOutputStream();
        int matched = 0; // how much of "\r\n\r\n" has just been seen
        int b;
        while ((b = in.read()) != -1) {
            head.write(b);
            if (head.size() > MAX_HEAD_BYTES) {
                return null;
            }
            matched = (b == (matched % 2 == 0 ? '\r' : '\n')) ? matched + 1 : (b == '\r' ? 1 : 0);
            if (matched == 4) {
                break;
            }
        }
        if (matched != 4) {
            return null;
        }
        String[] lines = head.toString("ISO-8859-1").split("\r\n");
        String[] first = lines[0].split(" ");
        if (first.length < 2) {
            return null;
        }
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon > 0) {
                headers.put(lines[i].substring(0, colon).trim().toLowerCase(Locale.ROOT),
                        lines[i].substring(colon + 1).trim());
            }
        }
        String path = first[1];
        int query = path.indexOf('?');
        return new HttpRequest(first[0].toUpperCase(Locale.ROOT), query >= 0 ? path.substring(0, query) : path, headers);
    }

    /** The value of a header (name in lower case), or null. */
    String header(String name) {
        return headers.get(name);
    }

    /** The Content-Length, or -1 when there is none or it is not a number. */
    long contentLength() {
        try {
            String value = headers.get("content-length");
            return value == null ? -1 : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
