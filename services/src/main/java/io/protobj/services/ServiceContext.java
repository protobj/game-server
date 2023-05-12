package io.protobj.services;

import io.protobj.IServer;
import io.protobj.services.annotations.Service;
import io.protobj.services.api.Message;
import io.protobj.services.discovery.api.ServiceDiscovery;
import io.protobj.services.methods.MethodInvoker;
import io.protobj.services.methods.RpcMethodEnhance;
import io.protobj.services.registry.api.ServiceRegistry;
import io.protobj.services.router.LookupParam;
import io.protobj.services.router.ServiceLookup;
import io.protobj.services.transport.api.ClientChannel;
import io.protobj.services.transport.api.ClientTransport;
import io.protobj.services.transport.api.ServerTransport;
import io.protobj.services.transport.api.ServiceTransport;
import io.scalecube.net.Address;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.scalecube.reactor.RetryNonSerializedEmitFailureHandler.RETRY_NON_SERIALIZED;

public class ServiceContext implements ServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ServiceContext.class);

    private ServiceEndPoint localEndPoint;

    private ServiceDiscovery serviceDiscovery;

    private ServiceTransportBootstrap transportBootstrap;
    private final Int2ObjectMap<ServiceLookup> serviceLookupMap = new Int2ObjectOpenHashMap<>();

    private final IServer server;

    private final Sinks.One<Void> shutdown = Sinks.one();
    private final Sinks.One<Void> onShutdown = Sinks.one();

    private ServiceContext(Builder builder) {
        this.localEndPoint = builder.localEndPoint;
        this.serviceDiscovery = builder.serviceDiscovery;
        this.transportBootstrap = builder.transportBootstrap;
        this.server = builder.server;
    }

    public ServiceContext(IServer server) {
        this.server = server;
    }

    public Mono<ServiceContext> start() {
        logger.info("[{}][start] Starting", localEndPoint);

        // Create bootstrap scheduler
        Scheduler scheduler = Schedulers.newSingle(toString(), true);

        return transportBootstrap
                .start(this)
                .publishOn(scheduler)
                .map(
                        transportBootstrap -> {
                            final Address serviceAddress = transportBootstrap.transportAddress;
                            localEndPoint.setAddress(serviceAddress);
                            serviceDiscovery.start()
                                    .subscribeOn(scheduler)
                                    .subscribe(t -> {
                                        serviceDiscovery.listen()
                                                .subscribeOn(scheduler)
                                                .subscribe(event -> {
                                                    if (event.isAdded() ) {

                                                    }
                                                });
                                    });
                            return ServiceContext.this;
                        })
                .onErrorResume(
                        ex -> Mono.defer(this::shutdown).then(Mono.error(ex)).cast(ServiceContext.class))
                .doOnSuccess(m -> logger.info("[{}][start] Started", localEndPoint))
                .doOnTerminate(scheduler::dispose);
    }

    @Override
    public void register(ServiceEndPoint endPoint) {


    }

    @Override
    public void update(ServiceEndPoint endPoint) {

    }

    @Override
    public void unregister(ServiceEndPoint endPoint) {

    }

    @Override
    public List<ServiceEndPoint> list() {
        return null;
    }

    public <T> T api(Class<T> serviceInterface) {
        try {
            return RpcMethodEnhance.createApi(serviceInterface, this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void fireForget(int st, int cmd, Message.Content content) {
        ServiceLookup lookup = serviceLookupMap.get(st);
        LookupParam lookupParam = null;
        if (content != null) {
            if (content instanceof Message.SidContent) {
                lookupParam = LookupParam.sid(((Message.SidContent) content).sid());
            } else if (content instanceof Message.SlotContent) {
                lookupParam = LookupParam.hash();
            }
        } else {
            lookupParam = LookupParam.min(st);
        }
        ClientChannel channel = lookup.lookup(this, localEndPoint, lookupParam);
        channel.fireAndForget(Message.New(new Message.Header(cmd), content))
                .subscribeOn(localEndPoint.getScheduler())
                .subscribe();
    }

    public <T extends Message.Content> Mono<T> requestResponse(int st, int cmd, Message.Content content, Class<T> ret) {
        return null;
    }

    public <T extends Message.Content> Flux<T> requestStream(int st, int cmd, Message.Content content, Class<T> ret) {
        return null;
    }

    public <T extends Message.Content> Flux<T> requestChannel(int st, int cmd, Flux<Message.Content> content, Class<T> ret) {
        return null;
    }

    public Mono<Void> shutdown() {
        return Mono.defer(
                () -> {
                    shutdown.emitEmpty(RETRY_NON_SERIALIZED);
                    return onShutdown.asMono();
                });
    }

    public ServiceLookup router(int st) {
        return serviceLookupMap.get(st);
    }

    public ServiceEndPoint localEndPoint() {
        return localEndPoint;
    }

    public IServer getServer() {
        return server;
    }

    public ServiceTransportBootstrap getTransportBootstrap() {
        return transportBootstrap;
    }

    public static class Builder {
        private ServiceEndPoint localEndPoint;
        private ServiceDiscovery serviceDiscovery;
        private ServiceTransportBootstrap transportBootstrap = new ServiceTransportBootstrap();
        private IServer server;

        private final List<Object> providers = new ArrayList<>();

        public Mono<ServiceContext> start() {
            return Mono.defer(() -> {
                localEndPoint.setScheduler(Schedulers.fromExecutor(server.getLogicExecutor()));
                for (Object service : providers) {
                    for (Class<?> anInterface : service.getClass().getInterfaces()) {
                        Arrays.stream(anInterface.getDeclaredMethods()).filter(it -> it.isAnnotationPresent(Service.class)).sorted((o1, o2) -> {
                            Service service12 = o1.getAnnotation(Service.class);
                            Service service1 = o2.getAnnotation(Service.class);
                            return service12.ix() - service1.ix();
                        }).forEach(method -> {
                            try {
                                MethodInvoker invoker = RpcMethodEnhance.createInvoker(service, anInterface, method, server);
                                MethodInvoker put = localEndPoint.getInvokerMap().put(invoker.cmd(), invoker);
                                if (put != null) {
                                    throw new RuntimeException(String.format("cmd repeat:%d", invoker.cmd()));
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
                return new ServiceContext(this).start();
            });
        }

        public ServiceContext startAwait() {
            return start().block();
        }

        public Builder server(IServer server) {
            this.server = server;
            return this;
        }

        public Builder services(Object... services) {
            providers.addAll(Arrays.stream(services).collect(Collectors.toList()));

            return this;
        }

        public Builder localEndPoint(ServiceEndPoint localEndPoint) {
            this.localEndPoint = localEndPoint;
            return this;
        }

        public Builder discovery(ServiceDiscovery serviceDiscovery) {
            this.serviceDiscovery = serviceDiscovery;
            return this;
        }

        public Builder transport(Supplier<ServiceTransport> supplier) {
            this.transportBootstrap = new ServiceTransportBootstrap(supplier);
            return this;
        }

    }

    public static class ServiceTransportBootstrap {

        public static final Supplier<ServiceTransport> NULL_SUPPLIER = () -> null;
        public static final ServiceTransportBootstrap NULL_INSTANCE = new ServiceTransportBootstrap();

        private final Supplier<ServiceTransport> transportSupplier;

        private ServiceTransport serviceTransport;

        private ServerTransport serverTransport;

        private ClientTransport clientTransport;

        private Address transportAddress = Address.NULL_ADDRESS;

        public ServiceTransportBootstrap() {
            this(NULL_SUPPLIER);
        }

        public ServiceTransportBootstrap(Supplier<ServiceTransport> transportSupplier) {
            this.transportSupplier = transportSupplier;
        }


        private Mono<ServiceTransportBootstrap> start(ServiceContext serviceContext) {
            if (transportSupplier == NULL_SUPPLIER
                    || (serviceTransport = transportSupplier.get()) == null) {
                return Mono.just(NULL_INSTANCE);
            }

            return serviceTransport
                    .start()
                    .doOnSuccess(transport -> serviceTransport = transport) // reset self
                    .flatMap(
                            transport -> serviceTransport.serverTransport(serviceContext.localEndPoint).bind())
                    .doOnSuccess(transport -> serverTransport = transport)
                    .map(
                            transport -> {
                                this.transportAddress = prepareAddress(serverTransport.address());
                                this.clientTransport = serviceTransport.clientTransport();
                                return this;
                            })
                    .doOnSubscribe(
                            s -> logger.info("[{}][serviceTransport][start] Starting", serviceContext.localEndPoint))
                    .doOnSuccess(
                            transport ->
                                    logger.info(
                                            "[{}][serviceTransport][start] Started, address: {}",
                                            serviceContext.localEndPoint,
                                            this.serverTransport.address()))
                    .doOnError(
                            ex ->
                                    logger.error(
                                            "[{}][serviceTransport][start] Exception occurred: {}",
                                            serviceContext.localEndPoint,
                                            ex.toString()));
        }

        private static Address prepareAddress(Address address) {
            final InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getByName(address.host());
            } catch (UnknownHostException e) {
                throw Exceptions.propagate(e);
            }
            if (inetAddress.isAnyLocalAddress()) {
                return Address.create(Address.getLocalIpAddress().getHostAddress(), address.port());
            } else {
                return Address.create(inetAddress.getHostAddress(), address.port());
            }
        }

        private Mono<Void> shutdown() {
            return Mono.defer(
                    () ->
                            Flux.concatDelayError(
                                            Optional.ofNullable(serverTransport)
                                                    .map(ServerTransport::stop)
                                                    .orElse(Mono.empty()),
                                            Optional.ofNullable(serviceTransport)
                                                    .map(ServiceTransport::stop)
                                                    .orElse(Mono.empty()))
                                    .then());
        }

        public ClientTransport getClientTransport() {
            return clientTransport;
        }
    }

}
