package hello;

import invoker.HelloSidNameInvoker;
import io.protobj.IServer;
import io.protobj.SessionManager;
import io.protobj.enhance.EnhanceClassCache;
import io.protobj.event.EventBus;
import io.protobj.hotswap.HotSwapManger;
import io.protobj.redisaccessor.RedisAccessor;
import io.protobj.resource.ResourceManager;
import io.protobj.scheduler.SchedulerService;
import io.protobj.services.ServiceContext;
import io.protobj.services.annotations.Service;
import io.protobj.services.methods.RpcMethodEnhance;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.concurrent.Executor;

public class RemoteHelloService implements HelloService {

    private int st;

    private ServiceContext serviceContext;


    @Override
    public Mono<HelloSidNameInvoker.HelloSidNameMessage> hello(int sid, String name) {
        return null;
    }

    @Override
    public HelloSidNameInvoker.HelloSidNameMessage helloBlock(int sid, String name) {
        return new HelloSidNameInvoker.HelloSidNameMessage();
    }

    @Override
    public Flux<HelloSidNameInvoker.HelloSidNameMessage> helloStream(int sid, String name) {
        return null;
    }

    @Override
    public Flux<HelloSidNameInvoker.HelloSidNameMessage> helloChannel(Flux<HelloSidNameInvoker.HelloSidNameMessage> name) {
        return null;
    }

    @Override
    public void helloFireForgot(String name) {
        System.err.println("hello " + name);
    }

    @Override
    public Flux<HelloSidNameInvoker.HelloSidNameMessage> hello0(int[] sids, String name) {
        return null;
    }


    public static void main(String[] args) throws Exception {
        IServer server = new IServer() {
            @Override
            public ThreadGroup threadGroup() {
                return null;
            }

            @Override
            public EnhanceClassCache getEnhanceClassCache() {
                return new EnhanceClassCache();
            }

            @Override
            public EventBus getEventBus() {
                return null;
            }

            @Override
            public ResourceManager getResourceManager() {
                return null;
            }

            @Override
            public RedisAccessor getRedisAccessor() {
                return null;
            }

            @Override
            public HotSwapManger getHotSwapManger() {
                return null;
            }

            @Override
            public Executor getManageExecutor() {
                return null;
            }

            @Override
            public Executor getLogicExecutor() {
                return null;
            }

            @Override
            public SchedulerService schedulerService() {
                return null;
            }

            @Override
            public SessionManager sessionManager() {
                return null;
            }

            @Override
            public <T> T getBeanByType(Class<T> clazz) {
                return null;
            }
        };
        RemoteHelloService remoteHelloService = new RemoteHelloService();
        for (Class<?> anInterface : remoteHelloService.getClass().getInterfaces()) {
            Arrays.stream(anInterface.getDeclaredMethods()).filter(it -> it.isAnnotationPresent(Service.class)).sorted((o1, o2) -> {
                Service service = o1.getAnnotation(Service.class);
                Service service1 = o2.getAnnotation(Service.class);
                return service.ix() - service1.ix();
            }).forEach(method -> {
                try {
                    RpcMethodEnhance.createInvoker(remoteHelloService, anInterface, method, server);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        ServiceContext context = new ServiceContext(server);
        HelloService helloService = RpcMethodEnhance.createApi(HelloService.class, context);
        System.err.println();
        RpcMethodEnhance.deleteTempDir();
    }
}
