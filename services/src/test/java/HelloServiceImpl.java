import java.util.concurrent.CompletableFuture;

public class HelloServiceImpl implements HelloService {

    private HelloService helloService;

    @Override
    public CompletableFuture<String> hello(int[] sids, String name) {
        return null;
    }

    @Override
    public CompletableFuture<String> hello(int gid, String name) {
        return null;
    }
}
