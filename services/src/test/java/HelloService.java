import io.protobj.services.annotations.Sid;
import io.protobj.services.annotations.Sids;

import java.util.concurrent.CompletableFuture;

public interface HelloService {

    CompletableFuture<String> hello(@Sids int[] sids, String name);

    CompletableFuture<String> hello(@Sid int sid, String name);

}
