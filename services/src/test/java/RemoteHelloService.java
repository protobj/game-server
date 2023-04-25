import io.protobj.services.ServiceContext;

import java.util.concurrent.CompletableFuture;

public class RemoteHelloService implements HelloService {

    private int st;

    private ServiceContext serviceContext;

    @Override
    public CompletableFuture<String> hello(int[] sids, String name) {

        return null;
    }

    @Override
    public CompletableFuture<String> hello(int sid, String name) {
        return null;
    }
}
