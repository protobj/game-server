package org.protobj.mock.javafx;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.pv.framework.gs.core.util.RandomUtils;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoginUI extends HBox {

    public static final String MOCKLAST_LOGIN_TXT = "mocklastLogin.txt";
    TextField accountText;
    ChoiceBox<MockServerOption> serverChoiceBox;
    ChoiceBox<String> serverIdChoice;

    public LoginUI() {
        List<String> server;
        try {
            String s = FileUtils.readFileToString(new File(MOCKLAST_LOGIN_TXT), StandardCharsets.UTF_8);
            server = JavaFxMockApplication.gson.fromJson(s, new TypeToken<List<String>>() {
            }.getType());
        } catch (IOException e) {
            server = new ArrayList<>();
        }

        serverChoiceBox = new ChoiceBox<>();
        serverChoiceBox.setConverter(new StringConverter<MockServerOption>() {
            @Override
            public String toString(MockServerOption object) {
                return object.getName();
            }

            @Override
            public MockServerOption fromString(String string) {
                Optional<MockServerOption> first = findMockServerOption(string);
                return first.get();
            }
        });
        List<MockServerOption> mockServerOptions = MockServerOption.mockServerOptions;
        serverChoiceBox.getItems().addAll(mockServerOptions);
        this.getChildren().add(serverChoiceBox);
        serverIdChoice = new ChoiceBox<>();
        this.getChildren().add(serverIdChoice);

        serverChoiceBox.setOnAction(event -> {
            MockServerOption value = serverChoiceBox.getValue();
            List<String> serverIds = value.getServerIds();
            serverIdChoice.getItems().clear();
            serverIdChoice.getItems().addAll(serverIds);
            serverIdChoice.setValue(serverIds.get(0));
        });

        if (server.isEmpty()) {
            serverChoiceBox.setValue(serverChoiceBox.getItems().get(0));
            accountText = new TextField("cq11");
        } else {
            serverChoiceBox.setValue(findMockServerOption(server.get(0)).get());
            serverIdChoice.setValue(server.get(1));
            accountText = new TextField(server.get(2));
        }


        this.getChildren().add(accountText);
        Button login = new Button("登录");

        login.setOnMouseClicked(this::onLogin);
        this.getChildren().add(login);
        Button logout = new Button("登出");
        logout.setOnMouseClicked(this::onLogout);
        this.getChildren().add(logout);
    }

    private Optional<MockServerOption> findMockServerOption(String string) {
        return MockServerOption.mockServerOptions.stream().filter(it -> it.getName().equals(string)).findFirst();
    }

    private void onLogout(MouseEvent mouseEvent) {
        JavaFxMockContext.JavaFxConnect javaFxConnect = JavaFxMockApplication.mockContext.javaFxConnect;
        if (javaFxConnect != null) {
            javaFxConnect.close();
        }
    }

    private void onLogin(MouseEvent event) {
        onLogout(event);
        String account = accountText.getText();
        MockServerOption value = serverChoiceBox.getValue();
        String gateway = value.getGateways().get(RandomUtils.nextInt(value.getGateways().size()));
        JavaFxMockApplication.mockContext.login(account, gateway, serverIdChoice.getValue());
        try (FileWriter fileWriter = new FileWriter(MOCKLAST_LOGIN_TXT)) {
            ArrayList<String> strings = Lists.newArrayList(value.getName(), serverIdChoice.getValue(), account);
            String s = JavaFxMockApplication.gson.toJson(strings);
            fileWriter.write(s);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
