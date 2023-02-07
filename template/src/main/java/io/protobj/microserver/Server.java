package io.protobj.microserver;

import io.protobj.AServer;
import io.protobj.Configuration;

public abstract class Server extends AServer {


    public Server(Configuration configuration, ThreadGroup threadGroup) {
        super(configuration, threadGroup);
    }
}
