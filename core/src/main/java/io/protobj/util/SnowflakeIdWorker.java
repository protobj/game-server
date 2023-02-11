package io.protobj.util;

/**
 * 原来41位时间戳可支持69年不重复，改为40位35年不重复
 * 原来workerBit和dataCenterBit分别为5，现在合并起来为11位，支持2048台游戏服务器
 * 序列号不变 12位 每毫秒可产生4096个id
 * <p>
 * 目前是一个服务器一个SnowflakeIdWorker实例
 * 如果id不足，可以考虑一张表一个SnowflakeIdWorker实例
 */
public class SnowflakeIdWorker implements IdGenerator {
    private final long maxServerIdShift;
    private final long timestampLeftShift;
    private final long sequenceMask;
    private final long serverId;
    private long sequence = 0L;
    /**`
     * 上次生成ID的时间截
     */
    private long lastTimestamp = -1L;

    public SnowflakeIdWorker(long serverId) {
        this(serverId, 11, 12);
    }

    public SnowflakeIdWorker(long serverId, long serverIdBits, long sequenceBits) {
        long maxServerId = ~(-1L << serverIdBits);
        if (serverId > maxServerId || serverId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxServerId));
        }
        this.serverId = serverId;
        maxServerIdShift = sequenceBits;
        timestampLeftShift = sequenceBits + serverIdBits;
        sequenceMask = ~(-1L << sequenceBits);
    }

    /**
     * 获得下一个ID (该方法是必须在单线程中调用)
     *
     * @return SnowflakeId
     */
    private long nextId() {
        long timestamp = timeGen();
        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }
        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // 毫秒内序列溢出
            if (sequence == 0) {
                //阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        // 时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }
        // 上次生成ID的时间截
        lastTimestamp = timestamp;
//        开始时间截 (2021-11-08)
        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - 1636369959844L) << timestampLeftShift) //
                | (serverId << maxServerIdShift) //
                | sequence;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 返回以毫秒为单位的当前时间
     *
     * @return 当前时间(毫秒)
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }


    public static int getServerId(long snowflakeId) {
        return (int) (snowflakeId << 41 >>> 53);
    }

    @Override
    public long generateId() {
        return nextId();
    }
}
