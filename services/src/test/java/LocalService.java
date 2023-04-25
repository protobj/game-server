import io.protobj.services.annotations.Service;

public class LocalService {


    @Service(st = ServiceType.HELLO)
    private HelloService helloService;
}
