package hello;

import invoker.HelloSidNameInvoker;
import io.protobj.services.annotations.Service;
import io.protobj.services.annotations.Sid;
import io.protobj.services.annotations.Sids;
import io.protobj.services.api.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Service(st = 100)
public interface HelloService {

    @Service(ix = 1)
    Flux<HelloSidNameInvoker.HelloSidNameMessage> hello0(@Sids int[] sids, String name);

    @Service(ix = 2)
    Mono<HelloSidNameInvoker.HelloSidNameMessage> hello(@Sid int sid, String name);

    @Service(ix = 3)
    HelloSidNameInvoker.HelloSidNameMessage helloBlock(@Sid int sid, String name);

    @Service(ix = 4)
    Flux<HelloSidNameInvoker.HelloSidNameMessage> helloStream(@Sid int sid, String name);

    @Service(ix = 5)
    Flux<HelloSidNameInvoker.HelloSidNameMessage> helloChannel(Flux<HelloSidNameInvoker.HelloSidNameMessage> name);


    @Service(ix = 6)
    void helloFireForgot(String name);
}
