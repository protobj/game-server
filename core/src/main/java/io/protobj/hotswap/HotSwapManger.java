package io.protobj.hotswap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.net.HttpURLConnection.HTTP_OK;

@Slf4j
public class HotSwapManger {

    private int httpPort;

    private String hotswapDir;

    private String addDir;

    public HotSwapManger() {
    }

    public static String getIpAddress() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = allNetInterfaces.nextElement();
                if (!netInterface.isLoopback() && !netInterface.isVirtual() && netInterface.isUp()) {
                    Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        ip = addresses.nextElement();
                        if (ip instanceof Inet4Address) {
                            return ip.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("can not getIp");
    }

    public void start() {
        try {
            String host = getIpAddress();
            int port = Integer.getInteger("io.protobj.hotswap.port", 12500);
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(host, port), 1);
            httpServer.createContext("/hotswap", this::atHotswapReq);
            httpServer.createContext("/info", this::atInfoReq);
            httpServer.createContext("/add", this::atAddReq);
            httpServer.start();
            log.warn("HotSwapManger http service start at %s:%d".formatted(host, port));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void atAddReq(HttpExchange httpExchange) {
        checkReq(httpExchange);

    }

    private void checkReq(HttpExchange httpExchange) {
        log.warn("recv http req remote:%s path:%s".formatted(httpExchange.getRemoteAddress(), httpExchange.getRequestURI().toString()));
        InetSocketAddress remoteAddress = httpExchange.getRemoteAddress();
        InetSocketAddress localAddress = httpExchange.getLocalAddress();
        if (!localAddress.getAddress().equals(remoteAddress.getAddress())) {
            throw new RuntimeException("BadRequest");
        }
    }

    private void atHotswapReq(HttpExchange exchange) {
        try {
            checkReq(exchange);
            List<String>[] lists = hotSwap("D:\\io");
            StringBuilder result = new StringBuilder("hotswap success:%n".formatted());
            for (String list : lists[0]) {
                result.append("\t".formatted()).append(list).append("%n".formatted());
            }
            result.append("hotswap fail:%n".formatted());
            for (String list : lists[1]) {
                result.append("\t".formatted()).append(list).append("%n".formatted());
            }
            exchange.sendResponseHeaders(HTTP_OK, 0);
            exchange.getResponseBody().write(result.toString().getBytes(StandardCharsets.UTF_8));
            exchange.close();
        } catch (Exception e) {
            try {
                e.printStackTrace();
                exchange.sendResponseHeaders(HTTP_OK, 0);
                String message = e.getMessage();
                exchange.getResponseBody().write(message == null ? "null".getBytes() : message.getBytes(StandardCharsets.UTF_8));
                exchange.close();
            } catch (IOException ex) {
                log.error("atHotswapReq error", ex);
            }
        }

    }

    private void atInfoReq(HttpExchange exchange) {
        checkReq(exchange);

    }


    private Map<String, UpdateFile> updateFiles = new ConcurrentHashMap<>();

    public static record UpdateFile(String fileName, long lastModified) {
    }

    public List<String>[] hotSwap(String... dirs) throws Exception {
        final long startTime = System.currentTimeMillis();
        List<Path> files = getPaths(dirs);
        Map<Class, byte[]> swapMap = getChangeClassMap(files);
        List<String>[] lists = hotSwap0(swapMap);
        log.error("热更耗时[{}]毫秒", System.currentTimeMillis() - startTime);
        return lists;
    }

    private List<String>[] hotSwap0(Map<Class, byte[]> swapMap) {
        List<String>[] result = new List[2];
        result[0] = new ArrayList<>();
        result[1] = new ArrayList<>();
        swapMap.forEach((key, value) -> {
            try {
                JavaAssistUtil.swapClass(key, value);
                result[0].add(key.getName());
                log.warn("更新成功 [{}]", key.getName());
            } catch (Exception e) {
                updateFiles.remove(key.getName());
                result[1].add(key.getName());
                log.warn("更新失败 [" + key.getName() + "]", e);
            }
        });
        return result;
    }

    private Map<Class, byte[]> getChangeClassMap(List<Path> files) throws Exception {
        Map<Class, byte[]> swapMap = new HashMap<>();
        for (Path path : files) {
            List<String> strings = Files.readAllLines(path);
            final File newFile = path.toFile();
            String className = strings.get(0).replace("package", "").replace(";", "").trim()
                    + "." + newFile.getName().replace(".java", "");
            final UpdateFile oldFile = updateFiles.get(newFile.getPath());
            if (oldFile != null && newFile.lastModified() == oldFile.lastModified()) {
                log.error("file [{}] has been hotswap ", path.toFile());
            } else {
                InMemoryJavaCompiler inMemoryJavaCompiler = InMemoryJavaCompiler.newInstance();
                inMemoryJavaCompiler.compile(className, strings.stream().collect(Collectors.joining()));
                Map<String, byte[]> customCompiledCode = inMemoryJavaCompiler.getClassLoader().getCustomCompiledCode();
                for (Map.Entry<String, byte[]> entry : customCompiledCode.entrySet()) {
                    final Class<?> key = Class.forName(entry.getKey());
                    swapMap.put(key, entry.getValue());
                }
                updateFiles.put(className, new UpdateFile(newFile.getPath(), newFile.lastModified()));
            }
        }
        return swapMap;
    }

    private List<Path> getPaths(String[] dirs) throws IOException {
        List<Path> files = new ArrayList<>();
        for (String dir : dirs) {
            Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    final File e = file.toFile();
                    if (e.getName().endsWith(".java")) {
                        files.add(file);
                    }
                    return super.visitFile(file, attrs);
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return files;
    }
}
