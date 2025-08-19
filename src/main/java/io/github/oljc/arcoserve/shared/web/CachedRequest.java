package io.github.oljc.arcoserve.shared.web;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CachedRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    public CachedRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.body = request.getInputStream().readAllBytes();
    }

    public byte[] body() {
        return this.body;
    }

    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream bais = new ByteArrayInputStream(this.body);
        return new ServletInputStream() {
            @Override
            public int read() {
                return bais.read();
            }
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }
            @Override
            public boolean isReady() {
                return true;
            }
            @Override
            public void setReadListener(ReadListener readListener) {
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        String enc = getCharacterEncoding();
        Charset cs;
        if (enc == null || "utf-8".equalsIgnoreCase(enc) || "utf8".equalsIgnoreCase(enc)) {
            cs = StandardCharsets.UTF_8;
        } else {
            cs = Charset.forName(enc);
        }
        return new BufferedReader(new InputStreamReader(getInputStream(), cs));
    }

    @Override
    public int getContentLength() {
        return this.body.length;
    }

    @Override
    public long getContentLengthLong() {
        return this.body.length;
    }
}
