package io.protobj.hotswap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.protobj.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.protobj.util.NetUtil.getIpAddress;
import static java.net.HttpURLConnection.HTTP_OK;

public class HotSwapManger {
    private static final Logger log = LoggerFactory.getLogger(HotSwapManger.class);


    private HotSwapConfig hotSwapConfig;

    private final Map<String, Record> swapRecordMap = new HashMap<>();
    private final List<String> swapLogs = new ArrayList<>();
    private final List<String> addLogs = new ArrayList<>();
    private final Set<String> initScriptClass = new HashSet<>();

    private final InMemoryJavaCompiler javaCompiler = InMemoryJavaCompiler.newInstance();

    public HotSwapManger() {
    }


    public void start(HotSwapConfig hotSwapConfig) {
        this.hotSwapConfig = hotSwapConfig;
        try {
            String host = getIpAddress();
            //随机端口
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(host, hotSwapConfig.getHttpPort()), 1);
            httpServer.createContext("/swap", new HttpHandlerWrapper(this::atSwapReq));
            httpServer.createContext("/swapLog", new HttpHandlerWrapper(this::atWrapLogReq));
            httpServer.createContext("/add", new HttpHandlerWrapper(this::atAddReq));
            httpServer.createContext("/addLog", new HttpHandlerWrapper(this::atAddLogReq));
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

    private void atSwapReq(HttpExchange exchange) {
        try {
            String result = hotSwap(hotSwapConfig.getSwapDir());
            exchange.sendResponseHeaders(HTTP_OK, 0);
            exchange.getResponseBody().write(result.getBytes(StandardCharsets.UTF_8));
            swapLogs.add(result);
            exchange.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void atWrapLogReq(HttpExchange exchange) {
        checkReq(exchange);
        try {
            String result = String.join("", swapLogs);
            exchange.sendResponseHeaders(HTTP_OK, 0);
            exchange.getResponseBody().write(result.getBytes(StandardCharsets.UTF_8));
            swapLogs.add(result);
            exchange.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void atAddReq(HttpExchange httpExchange) {
        try {
            checkReq(httpExchange);
            final long startTime = System.currentTimeMillis();
            List<Path> files = FileUtil.getPaths(path -> path.toFile().getName().endsWith(".java"), hotSwapConfig.getAddDir());
            String s;
            StringBuilder sb = new StringBuilder();
            javaCompiler.getSourceCodes().clear();
            for (Path path : files) {
                List<String> strings = Files.readAllLines(path);
                String className = strings.stream()
                        .filter(str -> str.startsWith("package"))
                        .map(str -> str.replace("package", ""))
                        .map(str -> str.substring(0, str.indexOf(";")).trim())
                        .map(str -> str + "." + path.toFile().getName().replace(".java", ""))
                        .findFirst().get();
                String fileContent = String.join("%n".formatted(), strings);
                sb.append(s = "add file:[%s]%n".formatted(path.toString()));
                log.warn(s);
                sb.append(s = "fileContent:%n[%s]%n".formatted(fileContent));
                log.warn(s);
                javaCompiler.addSource(className, fileContent);
            }
            javaCompiler.compileAll();
            for (String className : javaCompiler.getClassLoader().getCustomCompiledCode().keySet()) {
                try {
                    Class<?> loadClass = javaCompiler.getClassLoader().loadClass(className);
                    sb.append(s = "add class [%s]%n".formatted(className));
                    log.warn(s);
                    Method initScript = loadClass.getMethod("initScript");
                    if (!initScriptClass.contains(className)) {
                        Object instance = loadClass.getConstructor().newInstance();
                        initScript.invoke(instance);
                        sb.append(s = "class [%s]initScript %n".formatted(className));
                        log.warn(s);
                    }
                } catch (Exception e) {
                    if (!(e instanceof NoSuchMethodException)) {
                        StringWriter out = new StringWriter();
                        e.printStackTrace(new PrintWriter(out));
                        s = out.toString();
                        sb.append(s).append("%n".formatted());
                        log.error(s);
                    }
                }
            }
            String result = sb.toString();
            httpExchange.sendResponseHeaders(HTTP_OK, 0);
            httpExchange.getResponseBody().write(result.getBytes(StandardCharsets.UTF_8));
            swapLogs.add(result);
            httpExchange.close();

            log.error("热增加耗时[{}]毫秒", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void atAddLogReq(HttpExchange exchange) {
        checkReq(exchange);
        try {
            String result = String.join("", addLogs);
            exchange.sendResponseHeaders(HTTP_OK, 0);
            exchange.getResponseBody().write(result.getBytes(StandardCharsets.UTF_8));
            swapLogs.add(result);
            exchange.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String hotSwap(String... dirs) throws Exception {
        final long startTime = System.currentTimeMillis();
        List<Path> files = FileUtil.getPaths(path -> path.toFile().getName().endsWith(".java"), dirs);
        Map<String, Record> changeRecordMap = getChangeRecordMap(files, swapRecordMap::get);
        String result = hotSwap0(changeRecordMap);
        log.error("热替换耗时[{}]毫秒", System.currentTimeMillis() - startTime);
        return result;
    }

    private String hotSwap0(Map<String, Record> swapMap) {
        StringBuilder result = new StringBuilder();

        swapMap.forEach((key, value) -> {
            String s;
            log.warn(s = "start wrap [%s]%n".formatted(key));
            result.append(s);

            Record oldRecord = swapRecordMap.get(key);
            Map<String, Record.ClassRecord> classes = value.getClasses();
            List<String> failClass = new ArrayList<>();
            for (Map.Entry<String, Record.ClassRecord> classRecordEntry : classes.entrySet()) {
                Record.ClassRecord oldClassRecord = null;
                String className = classRecordEntry.getKey();
                if (oldRecord != null && (oldClassRecord = oldRecord.getClasses().get(classRecordEntry.getKey())) != null
                        && Arrays.equals(oldClassRecord.compiledBytes(), classRecordEntry.getValue().compiledBytes())) {
                    result.append(s = "[%s] not change %n".formatted(className));
                    log.warn(s);
                } else {
                    try {
                        Class<?> aClass = javaCompiler.getClassLoader().loadClass(classRecordEntry.getValue().clazzName());
                        JavassistUtil.swapClass(aClass, classRecordEntry.getValue().compiledBytes());
                        result.append(s = "wrap success [%s]%n".formatted(className));
                        log.warn(s);
                    } catch (Exception e) {
                        result.append(s = "wrap fail [%s]%n".formatted(className));
                        log.error(s);
                        StringWriter out = new StringWriter();
                        e.printStackTrace(new PrintWriter(out));
                        result.append(out);
                        log.error(out.toString());
                        failClass.add(className);
                    }
                }
            }
            for (String aClass : failClass) {
                classes.remove(aClass);
            }
            value.setUpdateTime(System.currentTimeMillis());
            swapRecordMap.put(key, value);
            result.append(s = "wrap complete [%s] [%s]".formatted(key, LocalDateTime.now().toString()));
            log.warn(s);
        });
        return result.toString();
    }

    private Map<String, Record> getChangeRecordMap(List<Path> files, Function<String, Record> getRecord) throws Exception {
        Map<String, Record> newRecordMap = new ConcurrentHashMap<>();
        for (Path path : files) {
            CompletableFuture.runAsync(() -> {
                try {
                    List<String> strings = Files.readAllLines(path);
                    final File newFile = path.toFile();
                    Record record = getRecord.apply(path.toString());
                    if (record != null && record.getLastModified() == newFile.lastModified()) {
                        log.error("file [{}] has been hotswap ", path.toFile());
                    } else {
                        String className = strings.stream()
                                .filter(str -> str.startsWith("package"))
                                .map(str -> str.replace("package", ""))
                                .map(str -> str.substring(0, str.indexOf(";")).trim())
                                .map(str -> str + "." + newFile.getName().replace(".java", ""))
                                .findFirst().get();
                        String fileContent = strings.stream().collect(Collectors.joining("%n".formatted()));
                        InMemoryJavaCompiler inMemoryJavaCompiler = InMemoryJavaCompiler.newInstance();
                        inMemoryJavaCompiler.compile(className, fileContent);
                        Map<String, byte[]> customCompiledCode = inMemoryJavaCompiler.getClassLoader().getCustomCompiledCode();
                        Record newRecord = new Record();
                        newRecord.setFileContent(fileContent);
                        newRecord.setFilePath(newFile.getPath());
                        newRecord.setLastModified(newFile.lastModified());
                        Map<String, Record.ClassRecord> classes = new HashMap<>();
                        for (Map.Entry<String, byte[]> entry : customCompiledCode.entrySet()) {
                            classes.put(entry.getKey(), new Record.ClassRecord(entry.getKey(), entry.getValue()));
                        }
                        newRecord.setClasses(classes);
                        newRecordMap.put(newRecord.getFilePath(), newRecord);
                    }
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }).join();
        }
        return newRecordMap;
    }

}
