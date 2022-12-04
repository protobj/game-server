package io.protobj.network;

import io.protobj.event.Response;
import lombok.Data;

import java.util.List;

@Data(staticConstructor = "valueOf")
public class MessageListResponse implements Response {

    private List<Object> messageList;

}
