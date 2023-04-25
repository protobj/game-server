import io.protobj.services.ServiceCache;
import io.protobj.services.ServiceCall;

import java.util.concurrent.CompletableFuture;

public class RemoteHelloService implements HelloService {

    private int st;

    private ServiceCall serviceCall;

    @Override
    public CompletableFuture<String> hello(int[] sids, String name) {
        ServiceCache discovery = serviceCall.discovery(st);
        for (int sid : sids) {
            discovery.find(sid);
        }
        return null;
    }

    @Override
    public CompletableFuture<String> hello(int sid, String name) {
        return null;
    }
}
