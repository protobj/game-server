package io.protobj.services.transport.api;

import io.protobj.services.api.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

public interface ContentCodec {

    void encode(OutputStream stream, Message.Content value) throws IOException;

    Message.Content decode(InputStream stream, Type type) throws IOException;
}
