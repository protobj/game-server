import io.protobj.services.annotations.Service;
import io.protobj.services.annotations.Sid;
import io.protobj.services.annotations.Sids;

import java.util.concurrent.CompletableFuture;

@Service(st = ServiceType.HELLO, ix = 0)
public interface HelloService {

    CompletableFuture<String> hello(@Sids int[] sids, String name);

    CompletableFuture<String> hello(@Sid int sid, String name);

}
