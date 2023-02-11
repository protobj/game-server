package org.protobj.mock;

import com.guangyu.cd003.projects.common.msg.RespRawDataType;
import org.apache.commons.io.FileUtils;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created on 2021/5/21.
 *
 * @author chen qiang
 */
public class HandlerCreator {

    public static void main(String[] args) throws IOException {
        createHandler();
    }

    private static void createMockData() throws IOException {
        List<Class<?>> classes = getRespClasses();
        StringBuilder header = new StringBuilder();
        StringBuilder content = new StringBuilder();
        String basePackage = "com.guangyu.cd003.projects.mock.resphandler";
        String fileName = "D:\\slg_02\\mock\\src\\main\\java\\com\\guangyu\\cd003\\projects\\mock\\resphandler\\RespData.java";
        File file = new File(fileName);
        if (file.exists()) {
            System.out.println("文件已存在 " + fileName);
            return;
        }
        header.append("package ").append(basePackage).append(";\n\n");
        for (Class<?> aClass : classes) {
            header.append("import ").append(aClass.getName()).append(";\n");
            content.append("\t//").append(aClass.getAnnotation(RespRawDataType.class).description()).append("\n");
            content.append("\tpublic ").append(aClass.getSimpleName()).append(" ").append(firstToLowerCase(aClass.getSimpleName())).append(";\n\n");
        }
        header.append("\n");
        header.append("public class RespData {\n\n");
        header.append(content);
        header.append("}");
        file.createNewFile();
        FileUtils.write(file, header, StandardCharsets.UTF_8);
    }

    static String firstToLowerCase(String value) {
        return value.substring(0, 1).toLowerCase() + value.substring(1);
    }

    private static void createHandler() {
        List<Class<?>> classes = getRespClasses();
        String basePackage = "com.guangyu.cd003.projects.mock.module";
        String filePath = "D:\\slg_02\\mock\\src\\main\\java\\com\\guangyu\\cd003\\projects\\mock\\module\\";
        //生成结果处理器
        for (Class<?> aClass : classes) {
            String s = aClass.getPackage().toString();
            String[] split = s.split("\\.");
            String packagePrefix = split[split.length - 2];
            if (packagePrefix.endsWith("msg")) {
                packagePrefix = split[split.length - 3];
            }
            String fileName = filePath + "\\" + packagePrefix + "\\" + aClass.getSimpleName() + "Handler.java";
            File file = new File(fileName);
            if (file.exists()) {
                System.out.println("文件已存在 " + fileName);
                continue;
            }
            RespRawDataType annotation = aClass.getAnnotation(RespRawDataType.class);

            StringBuilder content = new StringBuilder();
            content.append("package ").append(basePackage + "." + packagePrefix).append(";\n\n");
            content.append("import ").append(aClass.getName()).append(";\n");
            content.append("import com.guangyu.cd003.projects.mock.net.MockConnect;\n");
            content.append("import com.guangyu.cd003.projects.mock.RespHandler;\n\n");
            content.append("public class ").append(aClass.getSimpleName()).append("Handler").append(" implements RespHandler<").append(aClass.getSimpleName()).append("> {\n\n");
            content.append("\t@Override\n");
            content.append("\tpublic void handle(MockConnect connect, ").append(aClass.getSimpleName()).append(" respMsg, int cmd) {\n");
            content.append("\t\tconnect.LAST_RECV_MSGS.put(subCmd(), respMsg)").append(";").append("\n");
            content.append("\n");
            content.append("\t}\n");
            content.append("\n");
            content.append("\t@Override\n");
            content.append("\tpublic int subCmd() {\n");
            content.append("\t\treturn ").append(annotation.value()).append(";\n");
            content.append("\t}\n");
            content.append("}\n");
            try {
                File parentFile = file.getParentFile();
                if (!parentFile.exists()) {
                    parentFile.mkdir();
                }
                file.createNewFile();
                FileUtils.write(file, content, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static List<Class<?>> getRespClasses() {
        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackages("com.guangyu" + ".").addScanners(Scanners.MethodsAnnotated).addScanners(Scanners.TypesAnnotated));
        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(RespRawDataType.class);
        List<Class<?>> classes = typesAnnotatedWith.stream().filter(it -> it.getAnnotation(RespRawDataType.class).mainCmd()).collect(Collectors.toList());
        classes.sort((o1, o2) -> {
            RespRawDataType annotation1 = o1.getAnnotation(RespRawDataType.class);
            RespRawDataType annotation2 = o2.getAnnotation(RespRawDataType.class);
            return annotation1.value() - annotation2.value();
        });
        return classes;
    }
}
