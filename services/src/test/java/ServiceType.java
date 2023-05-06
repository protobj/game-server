import hello.HelloService;
import hello.RemoteHelloService;
import io.protobj.AServer;
import io.protobj.services.ServiceContext;
import io.protobj.services.ServiceEndPoint;
import io.protobj.services.discovery.scalecube.ScalecubeServiceDiscovery;
import io.protobj.services.transport.rsocket.RSocketServerTransportFactory;
import io.protobj.services.transport.rsocket.RSocketServiceTransport;
import io.scalecube.net.Address;

public interface ServiceType {
    int HELLO = 100;


    public static void main(String[] args) {
        ServiceEndPoint localEndPoint = new ServiceEndPoint();
        localEndPoint.setSid(1);
        localEndPoint.setSt(100);
        ServiceContext context = new ServiceContext.Builder()
                .services(new RemoteHelloService())
                .server(new AServer() {
                    @Override
                    protected void preStart() {

                    }

                    @Override
                    protected void postStart() {

                    }

                    @Override
                    protected void initNet() {

                    }
                })
                .discovery(new ScalecubeServiceDiscovery()
                        .membership(membershipConfig -> membershipConfig.seedMembers(Address.create("localhost", 8888)))
                )
                .localEndPoint(localEndPoint)
                .transport(RSocketServiceTransport::new)
                .startAwait();

        HelloService helloService = context.api(HelloService.class);

        helloService.helloFireForgot("tom");
    }
}
