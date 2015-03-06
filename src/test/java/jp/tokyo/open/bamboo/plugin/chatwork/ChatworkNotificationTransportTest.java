package jp.tokyo.open.bamboo.plugin.chatwork;

import static org.junit.Assert.*;

import java.net.URI;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Test;

public class ChatworkNotificationTransportTest {

    @Test
    public void canReplaceRoomId() {
        assertEquals("https://api.chatwork.com/v1/rooms/12345/messages",ChatworkNotificationTransport.getChatworkApiURL("12345"));
    }
}
