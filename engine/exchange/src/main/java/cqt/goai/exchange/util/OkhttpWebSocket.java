package cqt.goai.exchange.util;

import okhttp3.*;
import okio.ByteString;
import org.slf4j.Logger;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 推送回调
 * @author GOAi
 */
public class OkhttpWebSocket extends WebSocketListener {

    private static final OkHttpClient OKHTTP_CLIENT = new OkHttpClient();

    /**
     * 连接url
     */
    private final String url;

    /**
     * 收到消息转码方式
     */
    private final Function<ByteString, String> decode;

    /**
     * 连接开始
     */
    private final Runnable open;

    /**
     * 接收消息，转码后，交给谁
     */
    private final Consumer<String> receive;

    /**
     * 断开后回调
     */
    private final Runnable closed;

    /**
     * 日志
     */
    private Logger log;

    /**
     * 连接对象
     */
    private WebSocket webSocket;

    public OkhttpWebSocket(String url, Function<ByteString, String> decode, Runnable open,
                           Consumer<String> receive, Runnable closed, Logger log) {
        this.url = url;
        this.decode = decode;
        this.open = open;
        this.receive = receive;
        this.closed = closed;
        this.log = log;
        this.connect();
    }

    /**
     * 连接
     */
    public void connect() {
        if (null != this.webSocket) {
            this.webSocket.cancel();
            this.webSocket = null;
        }
        OKHTTP_CLIENT.newWebSocket(new Request.Builder()
                .url(this.url)
                .build(), this);
    }

    /**
     * 打开连接回调
     */
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        this.webSocket = webSocket;
        if (null != this.open) {
            this.open.run();
        }
    }

    /**
     * 发送消息
     * @param message 消息
     */
    public void send(String message) {
        if (null != this.webSocket) {
            this.webSocket.send(message);
        }
    }

    /**
     * 收到消息
     */
    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        if (null != this.receive) {
            String message = this.decode.apply(bytes);
            this.receive.accept(message);
        }
    }

    /**
     * 连接正在关闭
     */
    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        super.onClosing(webSocket, code, reason);
        this.log.info("onClosing --> {} {} {}", webSocket, code, reason);
        if (null != this.closed) {
            this.closed.run();
        }
    }

    /**
     * 连接关闭回调
     */
    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        super.onClosed(webSocket, code, reason);
        this.log.info("onClosed --> {} {} {}", webSocket, code, reason);
        if (null != this.closed) {
            this.closed.run();
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
        t.printStackTrace();
        this.log.info("onFailure --> {} {} {}", webSocket, t, response);
        if (null != this.closed) {
            this.closed.run();
        }
    }

    /**
     * 主动断开
     * @param code code
     * @param reason reason
     */
    public void close(int code, String reason) {
        if (null != this.webSocket) {
            this.webSocket.close(code, reason);
        }
    }

}
