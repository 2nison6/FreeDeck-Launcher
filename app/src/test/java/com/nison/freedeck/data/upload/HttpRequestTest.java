package com.nison.freedeck.data.upload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HttpRequestTest {

    private static HttpRequest parse(String text) throws IOException {
        return HttpRequest.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.ISO_8859_1)));
    }

    @Test
    public void readsMethodPathAndLength() throws IOException {
        HttpRequest request = parse("POST /u/abc/send HTTP/1.1\r\nHost: tv\r\nContent-Length: 1234\r\n\r\nBODY");
        assertEquals("POST", request.method);
        assertEquals("/u/abc/send", request.path);
        assertEquals(1234, request.contentLength());
    }

    @Test
    public void ignoresTheQueryString() throws IOException {
        assertEquals("/u/abc", parse("GET /u/abc?x=1 HTTP/1.1\r\n\r\n").path);
    }

    @Test
    public void headerNamesAreCaseInsensitive() throws IOException {
        assertEquals(7, parse("POST / HTTP/1.1\r\ncontent-length: 7\r\n\r\n").contentLength());
    }

    @Test
    public void aMissingOrBrokenLengthIsMinusOne() throws IOException {
        assertEquals(-1, parse("GET / HTTP/1.1\r\n\r\n").contentLength());
        assertEquals(-1, parse("POST / HTTP/1.1\r\nContent-Length: lots\r\n\r\n").contentLength());
    }

    @Test
    public void leavesTheBodyInTheStream() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(
                "POST / HTTP/1.1\r\nContent-Length: 4\r\n\r\nBODY".getBytes(StandardCharsets.ISO_8859_1));
        HttpRequest.read(in);
        assertEquals(4, in.available());
        assertEquals('B', in.read());
    }

    @Test
    public void garbageAndTruncatedInputAreRejected() throws IOException {
        assertNull(parse("not http"));
        assertNull(parse("GET / HTTP/1.1\r\nHost: tv"));
        assertNull(parse(""));
    }
}
