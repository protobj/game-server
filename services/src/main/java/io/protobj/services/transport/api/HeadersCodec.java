package io.protobj.services.transport.api;

import io.protobj.services.api.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface HeadersCodec {

    void encode(OutputStream stream, Message.Header header) throws IOException;

    Message.Header decode(InputStream stream) throws IOException;
}
