package io.protobj.services;

import io.protobj.services.annotations.Service;
import io.protobj.services.api.Message;
import io.protobj.services.discovery.api.ServiceDiscovery;
import io.protobj.services.methods.Reflect;
import io.protobj.services.methods.ServiceMethodInvoker;
import io.protobj.services.registry.api.ServiceRegistry;
import io.protobj.services.router.ServiceLookup;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
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

    private final ServiceEndPoint localEndPoint;

    private final ServiceDiscovery serviceDiscovery;


    private final ServiceTransportBootstrap transportBootstrap;
    private final Int2ObjectMap<ServiceLookup> serviceLookupMap = new Int2ObjectOpenHashMap<>();

    private final Int2ObjectMap<ServiceMethodInvoker> invokerMap;

    private final Sinks.One<Void> shutdown = Sinks.one();
    private final Sinks.One<Void> onShutdown = Sinks.one();

    private ServiceContext(Builder builder) {
        this.localEndPoint = builder.localEndPoint;
        this.serviceDiscovery = builder.serviceDiscovery;
        this.transportBootstrap = builder.transportBootstrap;
        this.invokerMap = parse(builder.serviceProviders);
    }

    private Int2ObjectMap<ServiceMethodInvoker> parse(List<Object> serviceProviders) {
        Int2ObjectMap<ServiceMethodInvoker> invokerMap = new Int2ObjectOpenHashMap<>();
        for (Object serviceProvider : serviceProviders) {
            Service service = serviceProvider.getClass().getAnnotation(Service.class);
            int st = service.st();
            Class<? extends ServiceLookup> router =null;
            try {
                ServiceLookup lookup = router.getConstructor().newInstance();
                serviceLookupMap.putIfAbsent(st, lookup);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            for (Method method : serviceProvider.getClass().getDeclaredMethods()) {
                Service methodAnnotation = method.getAnnotation(Service.class);
                int ix = methodAnnotation.ix();
                int id = st + ix;
            }

        }
        return null;
    }


    public Mono<ServiceContext> start() {
        logger.info("[{}][start] Starting", localEndPoint);

        // Create bootstrap scheduler
        Scheduler scheduler = Schedulers.newSingle(toString(), true);

        return transportBootstrap
                .start(this)
                .publishOn(scheduler)
                .flatMap(
                        transportBootstrap -> {
                            final Address serviceAddress = transportBootstrap.transportAddress;
                            localEndPoint.setAddress(serviceAddress);
                    return Mono.just(ServiceContext.this);

//                            return createDiscovery(
//                                    this, new ServiceDiscoveryOptions().serviceEndpoint(serviceEndpoint))
//                                    .publishOn(scheduler)
//                                    .publishOn(scheduler)
//                                    .then(Mono.fromCallable(() -> Injector.inject(this, serviceInstances)))
//                                    .then(serviceDiscovery.start())
//                                    .then(serviceDiscovery.listen())
//                                    .publishOn(scheduler)
//                                    .thenReturn(this);
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
        Service service = serviceInterface.getAnnotation(Service.class);
        int st = service.st();


        int cmd = 100;
        Message.Content content = null;
        Type returnType = null;
        // fireForgot,requestResponse,stream,channel
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

    public static class Builder {
        private ServiceEndPoint localEndPoint;
        private ServiceDiscovery serviceDiscovery;
        private ServiceTransportBootstrap transportBootstrap = new ServiceTransportBootstrap();
        private List<Object> serviceProviders = new ArrayList<>();

        public Mono<ServiceContext> start() {
            return Mono.defer(() -> new ServiceContext(this).start());
        }

        public ServiceContext startAwait() {
            return start().block();
        }

        public Builder services(Object... services) {
            serviceProviders.addAll(Arrays.stream(services).collect(Collectors.toList()));
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

    private static class ServiceTransportBootstrap {

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
    }

}
