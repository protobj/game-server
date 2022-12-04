package io.protobj.hotswap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.protobj.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static io.protobj.util.NetUtil.getIpAddress;
import static java.net.HttpURLConnection.HTTP_OK;

public class HotSwapManger {
    private static final Logger log = LoggerFactory.getLogger(HotSwapManger.class);


    private HotSwapConfig hotSwapConfig;

    private Map<String, Record> swapRecordMap = new ConcurrentHashMap<>();
    private List<Record> swapSuccessList = new CopyOnWriteArrayList<>();
    private List<Record> swapFailList = new CopyOnWriteArrayList<>();
    private Map<String, Record> addRecordMap = new ConcurrentHashMap<>();
    private List<Record> addSuccessList = new CopyOnWriteArrayList<>();
    private List<Record> addFailList = new CopyOnWriteArrayList<>();

    public HotSwapManger() {
    }


    public void start(HotSwapConfig hotSwapConfig) {
        this.hotSwapConfig = hotSwapConfig;
        try {
            String host = getIpAddress();
            //随机端口
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(host, 0), 1);
            httpServer.createContext("/swap", new HttpHandlerWrapper(this::atHotswapReq));
            httpServer.createContext("/swapInfo", new HttpHandlerWrapper(this::atInfoReq));
            httpServer.createContext("/add", new HttpHandlerWrapper(this::atAddReq));
            httpServer.createContext("/addInfo", new HttpHandlerWrapper(this::atAddReq));
            httpServer.start();
            log.warn("HotSwapManger http service start at %s:%d".formatted(host, httpServer.getAddress().getPort()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public class HttpHandlerWrapper implements HttpHandler {
        HttpHandler httpHandler;

        public HttpHandlerWrapper(HttpHandler httpHandler) {
            this.httpHandler = httpHandler;
        }

        @Override
        public void handle(HttpExchange exchange) {
            try {
                checkReq(exchange);
                httpHandler.handle(exchange);
            } catch (Throwable e) {
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
            List<String>[] lists = hotSwap(hotSwapConfig.getSwapDir());
            StringBuilder result = new StringBuilder("swap success:%n".formatted());
            for (String list : lists[0]) {
                result.append("\t".formatted()).append(list).append("%n".formatted());
            }
            result.append("swap fail:%n".formatted());
            for (String list : lists[1]) {
                result.append("\t".formatted()).append(list).append("%n".formatted());
            }
            exchange.sendResponseHeaders(HTTP_OK, 0);
            exchange.getResponseBody().write(result.toString().getBytes(StandardCharsets.UTF_8));
            exchange.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void atInfoReq(HttpExchange exchange) {
        checkReq(exchange);

    }

    private void atAddReq(HttpExchange httpExchange) {
        try {
            checkReq(httpExchange);
            final long startTime = System.currentTimeMillis();
            List<Path> files = FileUtil.getPaths(path -> path.toFile().getName().endsWith(".java"), hotSwapConfig.getAddDir());
            Map<Class, byte[]> swapMap = getChangeClassMap(files, addRecordMap);
            List<String>[] lists = hotSwap0(swapMap);
            log.error("热替换耗时[{}]毫秒", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String>[] hotSwap(String... dirs) throws Exception {
        final long startTime = System.currentTimeMillis();
        List<Path> files = FileUtil.getPaths(path -> path.toFile().getName().endsWith(".java"), dirs);
        Map<Class, byte[]> swapMap = getChangeClassMap(files, swapRecordMap);
        List<String>[] lists = hotSwap0(swapMap);
        log.error("热替换耗时[{}]毫秒", System.currentTimeMillis() - startTime);
        return lists;
    }

    private List<String>[] hotSwap0(Map<Class, byte[]> swapMap) {
        List<String>[] result = new List[2];
        result[0] = new ArrayList<>();
        result[1] = new ArrayList<>();
        swapMap.forEach((key, value) -> {
            try {
                JavassistUtil.swapClass(key, value);
                result[0].add(key.getName());
                log.warn("替换成功 [{}]", key.getName());
            } catch (Exception e) {
                Record remove = swapRecordMap.remove(key.getName());
                if (remove != null) {
                    swapSuccessList.remove(remove);
                    swapFailList.add(remove);
                }
                result[1].add(key.getName());
                log.warn("替换失败 [" + key.getName() + "]", e);
            }
        });
        return result;
    }

    private Map<Class, byte[]> getChangeClassMap(List<Path> files, Map<String, Record> recordMap) throws Exception {
        Map<Class, byte[]> resultMap = new HashMap<>();
        for (Path path : files) {
            List<String> strings = Files.readAllLines(path);
            final File newFile = path.toFile();
            String className = strings.get(0).replace("package", "").replace(";", "").trim()
                    + "." + newFile.getName().replace(".java", "");
            Record record = recordMap.get(className);
            String fileContent = strings.stream().collect(Collectors.joining());
            if (record != null && record.fileContent().equals(fileContent)) {
                log.error("file [{}] has been hotswap ", path.toFile());
            } else {
                InMemoryJavaCompiler inMemoryJavaCompiler = InMemoryJavaCompiler.newInstance();
                inMemoryJavaCompiler.compile(className, fileContent);
                Map<String, byte[]> customCompiledCode = inMemoryJavaCompiler.getClassLoader().getCustomCompiledCode();
                for (Map.Entry<String, byte[]> entry : customCompiledCode.entrySet()) {
                    final Class<?> key = Class.forName(entry.getKey());
                    resultMap.put(key, entry.getValue());
                }
                Record value = new Record(newFile.getPath(), className, System.currentTimeMillis(), fileContent);
                recordMap.put(className, value);
            }
        }
        return resultMap;
    }

}
