package de.cweiske.ouya.louyapi;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends NanoHTTPD {

    public HttpServer(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        return newFixedLengthResponse(Response.Status.OK, "text/plain", "hello");
    }
}
