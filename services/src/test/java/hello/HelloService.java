package hello;

import io.protobj.services.annotations.Service;
import io.protobj.services.annotations.Sid;
import io.protobj.services.annotations.Sids;
import io.protobj.services.api.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

public interface HelloService {

    Flux<Message.Content> hello0(@Sids int[] sids, String name);

    Mono<String> hello(@Sid int sid, String name);

    String helloBlock(@Sid int sid, String name);

    Flux<String> helloStream(@Sid int sid, String name);

    Flux<String> helloChannel(@Sid int sid, Flux<String> name);
}
