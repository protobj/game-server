package org.protobj.mock.javafx;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pv.framework.gs.core.module.annotation.CliMsgMethod;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JavaFxMockApplication extends Application {
    public static final int width = 1600;
    public static final int height = 900;

    public static final int half_width = width / 2;
    public static final int half_height = height / 2;
    static Table<String, Integer, RqstDetailUI> rqstMap;

    static JavaFxMockContext mockContext;
    public static Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public void init() {
        mockContext = new JavaFxMockContext();
        rqstMap = HashBasedTable.create();
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .forPackages("com.guangyu" + ".")
                .addScanners(Scanners.MethodsAnnotated)
                .addScanners(Scanners.TypesAnnotated)
        );
        Collection<Method> methodsAnnotatedWith = reflections.getMethodsAnnotatedWith(CliMsgMethod.class);
        //请求方法
        methodsAnnotatedWith = methodsAnnotatedWith.stream().filter(t -> t.getAnnotation(CliMsgMethod.class) != null).sorted(Comparator.comparing(t -> {
            return t.getAnnotation(CliMsgMethod.class).value();
        })).collect(Collectors.toList());
        for (Method method : methodsAnnotatedWith) {
            CliMsgMethod annotation = method.getAnnotation(CliMsgMethod.class);
            RqstDetailUI rqstDetailUI = new RqstDetailUI();
            rqstDetailUI.rqstCode = annotation.value();
            rqstDetailUI.rqstDesc = annotation.desc4Cli();
            rqstDetailUI.rqstClass = annotation.rqstType();
            rqstDetailUI.newPane();
            String name =method.getDeclaringClass().getName();
            String modules = name.split("module")[1];
            String substring = modules.substring(1);
            String module = substring.substring(0, substring.indexOf("."));
            rqstMap.put(module, rqstDetailUI.rqstCode, rqstDetailUI);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Group group = new Group();
        //登录页面
        group.getChildren().add(new LoginUI());
        HBox hBox = new HBox();
        hBox.setLayoutY(25);
        hBox.getChildren().add(new LogUI());

        VBox vBox = new VBox();
        //右边请求消息
        ChoiceBox<String> choiceBox = new ChoiceBox<>();
        choiceBox.setLayoutX(half_width);
        choiceBox.setMaxWidth(half_width);
        FlowPane rqstBox = new FlowPane();
        rqstBox.setLayoutX(half_width);
        rqstBox.setMaxWidth(half_width);
        rqstBox.setHgap(10); //horizontal gap in pixels => that's what you are asking for
        rqstBox.setVgap(10); //vertical gap in pixels
        rqstBox.setPadding(new Insets(10, 10, 10, 10)); //
        choiceBox.setOnAction(event -> {
            if (event.isConsumed()) {
                return;
            }
            if (vBox.getChildren().size() == 3) {
                vBox.getChildren().remove(vBox.getChildren().size() - 1);
            }
            String value = choiceBox.getValue();
            Map<Integer, RqstDetailUI> row = rqstMap.row(value);
            rqstBox.getChildren().remove(0, rqstBox.getChildren().size());
            for (RqstDetailUI rqstDetailUI : row.values()) {
                Button child = new Button(rqstDetailUI.rqstCode + ":" + rqstDetailUI.rqstDesc);
                rqstBox.getChildren().add(child);//, j % 2, j / 2 + 1);
                child.setOnMouseClicked(event1 -> {
                    if (vBox.getChildren().size() == 3) {
                        vBox.getChildren().remove(vBox.getChildren().size() - 1);
                    }
                    vBox.getChildren().add(rqstDetailUI.newPane());
                });
            }
        });
        Set<String> integers = rqstMap.rowKeySet();
        for (String integer : integers) {
            choiceBox.getItems().add(String.valueOf(integer));
        }

        vBox.getChildren().add(choiceBox);
        vBox.getChildren().add(rqstBox);

        hBox.getChildren().add(vBox);

        group.getChildren().add(hBox);


        Scene scene = new Scene(group, width, height);
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    public static void main(String[] args) {
        System.setProperty("cq.mock", "true");
        launch();
    }
}
