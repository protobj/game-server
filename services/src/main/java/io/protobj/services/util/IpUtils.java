package io.protobj.services.util;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class IpUtils {

    private static List<Inet4Address> getLocalIp4AddressFromNetworkInterface() throws SocketException {
        List<Inet4Address> addresses = new ArrayList<>(1);
        Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface n = e.nextElement();
            if (!isValidInterface(n)) {
                continue;
            }
            Enumeration<InetAddress> ee = n.getInetAddresses();
            while (ee.hasMoreElements()) {
                InetAddress i = ee.nextElement();
                if (isValidAddress(i)) {
                    addresses.add((Inet4Address) i);
                }
            }
        }
        return addresses;
    }

    /**
     * 过滤回环网卡、点对点网卡、非活动网卡、虚拟网卡并要求网卡名字是eth或ens开头
     *
     * @param ni 网卡
     * @return 如果满足要求则true，否则false
     */
    private static boolean isValidInterface(NetworkInterface ni) throws SocketException {
        return !ni.isLoopback() && !ni.isPointToPoint() && ni.isUp() && !ni.isVirtual()
                && (ni.getName().startsWith("eth") || ni.getName().startsWith("ens"));
    }

    /**
     * 判断是否是IPv4，并且内网地址并过滤回环地址.
     */
    private static boolean isValidAddress(InetAddress address) {
        return address instanceof Inet4Address && address.isSiteLocalAddress() && !address.isLoopbackAddress();
    }

    private static Inet4Address getIpBySocket() throws SocketException {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            if (socket.getLocalAddress() instanceof Inet4Address) {
                return (Inet4Address) socket.getLocalAddress();
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private static String getLocalIp4Address() throws SocketException {
        final List<Inet4Address> ipByNi = getLocalIp4AddressFromNetworkInterface();
        if (ipByNi.size() > 1) {
            final Inet4Address ipBySocketOpt = getIpBySocket();
            if (ipBySocketOpt != null) {
                return ipBySocketOpt.getHostAddress();
            } else {
                return ipByNi.get(0).getHostAddress();
            }
        }
        return ipByNi.get(0).getHostAddress();
    }

    private static String getExternHost() throws IOException {
        try (java.util.Scanner s = new java.util.Scanner(new java.net.URL("https://api.ipify.org").openStream(), "UTF-8").useDelimiter("\\A")) {
            return s.next();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getHost(boolean extern) {
        try {
            if (extern) {
                String externHost = getExternHost();
                List<Inet4Address> localIp4AddressFromNetworkInterface = getLocalIp4AddressFromNetworkInterface();
                for (Inet4Address inet4Address : localIp4AddressFromNetworkInterface) {
                    if (inet4Address.getHostAddress().equals(externHost)) {
                        return inet4Address.getHostAddress();
                    }
                }
            }
            return getLocalIp4Address();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
