package org.rainy.log.websocket;

import lombok.extern.slf4j.Slf4j;
import org.rainy.log.listener.FileContent;
import org.rainy.log.listener.FileListener;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * <p>
 *
 * </p>
 *
 * @author zhangyu
 */
@Slf4j
@Component
@ServerEndpoint(value = "/watch")
public class WatchLogEndpoint {

    private static final Map<Session, FileListener> connections = new ConcurrentHashMap<>();
    private static final Executor executor = new ThreadPoolExecutor(10, 50, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(30));

    private Session session;

    @OnOpen
    public void open(Session session) {
        this.session = session;
        Map<String, String> parameters = session.getPathParameters();
        initParameters(parameters);
        log.info("received a connection, parameters: {}.", parameters);
        long lines = Long.parseLong(parameters.get("lines"));

        FileListener listener = new FileListener(lines, -1L, this::output);
        connections.put(session, listener);

        executor.execute(listener);
        log.info("started watch log file.");
    }

    @OnMessage
    public void message(String message) {
        log.info("receive message: {}.", message);
    }

    @OnError
    public void error(Throwable throwable) {
        throwable.printStackTrace();
    }

    @OnClose
    public void close() {
        FileListener listener = connections.remove(this.session);
        listener.close();
        log.info("closed connection.");
    }

    private void output(FileContent fileContent) {
        for (Session session : connections.keySet()) {
            try {
                session.getBasicRemote().sendText(fileContent.getContent());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initParameters(Map<String, String> parameters) {
        parameters.put("lines", "50");
    }

}
