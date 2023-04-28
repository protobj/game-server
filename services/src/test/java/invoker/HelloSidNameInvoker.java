package invoker;

import hello.HelloService;
import io.protobj.services.api.Message;
import io.protobj.services.methods.CommunicationMode;
import io.protobj.services.methods.MethodInvoker;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;

public class HelloSidNameInvoker implements MethodInvoker {

    private HelloService helloService;

    public HelloSidNameInvoker(HelloService helloService) {
        this.helloService = helloService;
    }

    @Override
    public int cmd() {
        return 102;
    }


    @Override
    public CommunicationMode mode() {
        return CommunicationMode.REQUEST_RESPONSE;
    }

    @Override
    public Type parameterType() {
        return HelloSidNameMessage.class;
    }


    public static class HelloSidNameMessage implements Message.Content {
        public int sid;
        public String name;
    }

    @Override
    public Mono<Message.Content> invokeOne(Message.Content content) {
        HelloSidNameMessage message0 = (HelloSidNameMessage) content;
        return helloService.hello(message0.sid, message0.name).cast(Message.Content.class);
    }


    @Override
    public void invoke(Message.Content content) {
        HelloSidNameMessage message0 = (HelloSidNameMessage) content;
        helloService.hello(message0.sid, message0.name).cast(Message.Content.class);
    }

    @Override
    public Flux<Message.Content> invokeMany(Message.Content content) {
        HelloSidNameMessage message0 = (HelloSidNameMessage) content;
        return helloService.hello0(null, message0.name).cast(Message.Content.class);
    }

    @Override
    public Flux<Message.Content> invokeBidirectional(Flux<Message.Content> content) {
        return helloService.helloChannel(content.cast(HelloSidNameMessage.class)).cast(Message.Content.class);
    }
}
