package org.protobj.mock.javafx;

import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.pv.common.utilities.common.StringUtil;
import com.pv.framework.gs.core.module.msgproc.IRqstMsg;
import com.pv.framework.gs.core.module.msgproc.NullRqstMsg;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class RqstDetailUI {
    public int rqstCode;
    public Class rqstClass;
    public String rqstDesc;

    private Node pane;

    Object result;

    public Node newPane() {
        try {
            result = rqstClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (pane != null) {
            return pane;
        }
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setMaxWidth(JavaFxMockApplication.half_width);
        scrollPane.setMaxHeight(JavaFxMockApplication.half_height);
        GridPane pane = new GridPane();
        int i = 0;
        i = fillPane(pane, result, rqstClass, i, 0);
        if (i == 0) {
            pane.add(new Text("无参数"), 0, 0);
            i++;
        }
        Button button = new Button("确定");
        button.setOnMouseClicked(event -> {
            if (rqstClass == NullRqstMsg.class) {
                JavaFxMockApplication.mockContext.javaFxConnect.send(rqstCode);
            } else {
                JavaFxMockApplication.mockContext.javaFxConnect.send(rqstCode, result);
            }
        });
        pane.add(button, 0, i);
        scrollPane.setContent(pane);
        return scrollPane;
    }

    private int fillPane(GridPane pane, Object source, Class field, int i, int layer) {
        StringBuilder prefix = new StringBuilder();
        for (int i1 = 0; i1 < layer; i1++) {
            prefix.append("\t");
        }
        for (Field declaredField : field.getDeclaredFields()) {
            declaredField.setAccessible(true);
            Protobuf annotation = declaredField.getAnnotation(Protobuf.class);
            if (annotation == null) {
                continue;
            }
            String description = annotation.description();
            Type type = declaredField.getGenericType();
            if (type instanceof Class) {
                Text desc = new Text(prefix + description + "=" + ((Class<?>) type).getSimpleName() + ":" + declaredField.getName());
                pane.add(desc, 0, i);
            } else {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Class clz = (Class) parameterizedType.getRawType();
                StringBuilder typeDesc = new StringBuilder(clz.getSimpleName() + "<");
                for (Type actualTypeArgument : parameterizedType.getActualTypeArguments()) {
                    typeDesc.append(((Class) actualTypeArgument).getSimpleName()).append(",");
                }
                typeDesc.replace(typeDesc.length() - 1, typeDesc.length(), ">");
                Text desc = new Text(prefix + description + "=" + typeDesc + ":" + declaredField.getName());
                pane.add(desc, 0, i);
            }

            if (type instanceof Class && IRqstMsg.class.isAssignableFrom((Class<?>) type)) {
                Class clz = (Class) type;
                i++;
                try {
                    Object o = clz.newInstance();
                    declaredField.set(source, o);
                    i = fillPane(pane, o, clz, i, layer + 1);
                } catch (Exception e) {
                    LogUI.log(ExceptionUtils.getStackTrace(e));
                }
                continue;
            }
            TextField input = new TextField();
            input.setOnMouseExited(event -> {
                String text = input.getText();
                if (StringUtil.isEmpty(text)) {
                    return;
                }
                Object value;
                if (type == String.class) {
                    value = text;
                } else {
                    value = JavaFxMockApplication.gson.fromJson(text, type);
                }
                try {
                    declaredField.set(source, value);
                } catch (IllegalAccessException e) {
                    LogUI.log(ExceptionUtils.getStackTrace(e));
                }
            });
            pane.add(input, 1, i);
            i++;
        }
        return i;
    }
}
