package io.protobj.cluster.api;

import io.protobj.services.ServiceEndPoint;
import io.protobj.services.annotations.Service;
import io.scalecube.net.Address;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.nio.ByteBuffer;

@Service(100)
public interface ClusterService {

    Mono<Void> register(ServiceEndPoint endPoint);


    <T> Flux<T> updateClusterState();

    /**
     * 根据服务器类型和服务器组Id查询服务器地址
     *
     * @param st  服务类型
     * @param gid 服务组id
     * @return 地址
     */
    @Service(1)
    Mono<Address> queryByGid(int st, int gid);

    /**
     * 根据服务器类型和服务器Id查询服务器地址
     *
     * @param st  服务类型
     * @param sid 服务id
     * @return 地址
     */
    @Service(2)
    Mono<Address> queryBySid(int st, int sid);

    /**
     * 根据服务器类型查询服务器地址
     *
     * @param st 服务类型
     * @return 地址
     */
    @Service(3)
    Mono<Address> queryBySt(int st);


    @Service(1)
    <T> Mono<T> requestResponse(int st, long slotKey, ByteBuffer header, ByteBuffer body);

    @Service(2)
    void requestOne(int st, long slotKey, ByteBuffer header, ByteBuffer body);

    @Service(3)
    <T> Flux<T> requestStream(int st, long slotKey, ByteBuffer header, ByteBuffer body);

    @Service(4)
    <T> Flux<T> requestChannel(int st, long slotKey, Flux<Tuple2<ByteBuffer, ByteBuffer>> request);

}
