package org.protobj.mock.javafx;

import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.logging.log4j.message.FormattedMessage;

import static com.guangyu.cd003.projects.mock.javafx.JavaFxMockApplication.half_width;

public class LogUI extends HBox {

    static TextArea textArea;

    public LogUI() {
        //左边日志输出
        TextArea textArea = new TextArea();

        textArea.setMaxWidth(half_width - 2);
        textArea.setMinHeight(half_width - 2);
        textArea.setWrapText(true);
        textArea.setMinHeight(JavaFxMockApplication.height - 25);
        textArea.setMaxHeight(JavaFxMockApplication.height - 25);
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setMaxWidth(half_width);
        scrollPane.setMinWidth(half_width);
        scrollPane.setFitToWidth(true);
        scrollPane.setMinHeight(JavaFxMockApplication.height - 25);
        this.getChildren().add(scrollPane);
        LogUI.textArea = textArea;

    }


    public static void log(String content, Object... params) {
        FormattedMessage formattedMessage = new FormattedMessage(content, params);
        String text = "[" +
                DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss") +
                "] " +
                formattedMessage.getFormattedMessage() + "\r\n";
        Platform.runLater(() -> textArea.appendText(text));
    }
}
