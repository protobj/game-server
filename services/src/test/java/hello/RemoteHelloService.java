package hello;

import io.protobj.services.ServiceContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class RemoteHelloService implements HelloService {

    private int st;

    private ServiceContext serviceContext;

    @Override
    public Flux<String> hello(int[] sids, String name) {

        return null;
    }

    @Override
    public Mono<String> hello(int sid, String name) {
        return null;
    }

    @Override
    public String helloBlock(int sid, String name) {
        return "Hello " + name;
    }

    @Override
    public Flux<String> helloStream(int sid, String name) {
        return null;
    }

    @Override
    public Flux<String> helloChannel(int sid, Flux<String> name) {
        return null;
    }
}
