package io.protobj.services.discovery.scalecube.api;

import io.protobj.services.ServiceEndPoint;
import io.scalecube.net.Address;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.nio.ByteBuffer;

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
    Mono<Address> queryByGid(int st, int gid);

    /**
     * 根据服务器类型和服务器Id查询服务器地址
     *
     * @param st  服务类型
     * @param sid 服务id
     * @return 地址
     */
    Mono<Address> queryBySid(int st, int sid);

    /**
     * 根据服务器类型查询服务器地址
     *
     * @param st 服务类型
     * @return 地址
     */
    Mono<Address> queryBySt(int st);


    <T> Mono<T> requestResponse(int st, long slotKey, ByteBuffer header, ByteBuffer body);

    void requestOne(int st, long slotKey, ByteBuffer header, ByteBuffer body);

    <T> Flux<T> requestStream(int st, long slotKey, ByteBuffer header, ByteBuffer body);

    <T> Flux<T> requestChannel(int st, long slotKey, Flux<Tuple2<ByteBuffer, ByteBuffer>> request);

}
