package hello;

import io.protobj.services.ServiceContext;
import io.protobj.services.ServiceEndPoint;
import io.protobj.services.router.ServiceLookup;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

public class LocalHelloService implements HelloService {

    private ServiceContext context;

    @Override
    public Flux<String> hello(int[] sids, String name) {
        Flux<String> flux = Sinks.many().multicast().<String>onBackpressureBuffer().asFlux();
        for (int sid : sids) {
            ServiceLookup lookup = context.router(ServiceType.HELLO);
            Mono<ServiceEndPoint> endPointMono = lookup.lookupBySid(context.localEndPoint(), sid);

        }
        return flux;
    }

    @Override
    public Mono<String> hello(int sid, String name) {
        return null;
    }

    @Override
    public String helloBlock(int sid, String name) {
        return null;
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
