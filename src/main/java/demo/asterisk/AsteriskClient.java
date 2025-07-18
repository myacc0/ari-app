package demo.asterisk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import demo.asterisk.dto.EventDto;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class AsteriskClient {
    private static final Gson gson = new GsonBuilder().create();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String APP_NAME = "myapp";
    private static final String USERNAME = "demouser";
    private static final String PASSWORD = "1234";
    private static final String WS_ARI_URL = "ws://172.19.236.62:8088/ari";
    private static final String HTTP_ARI_URL = "http://172.19.236.62:8088/ari";

    public static void main(String[] args) throws Exception {
        String wsUrl = String.format("%s/events?api_key=%s:%s&app=%s", WS_ARI_URL, USERNAME, PASSWORD, APP_NAME);
        WebSocketClient client = new WebSocketClient(new URI(wsUrl)) {

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("WebSocket Connected to Asterisk");
            }

            @Override
            public void onMessage(String message) {
                System.out.println("Received1: " + message);

                EventDto msg = gson.fromJson(message, EventDto.class);
                if ("StasisStart".equals(msg.getType())) {
                    String channelId = extractChannelId(msg);

                    try {
                        // Воспроизвести IVR
                        playAudio(channelId, "sound:hello-world");

                        // Начать запись
                        startRecording(channelId, "recording-" + System.currentTimeMillis());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else if ("RecordingFinished".equals(msg.getType())) {
                    String recordingName = extractRecordingName(msg);
                    try {
                        downloadRecording(recordingName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("WebSocket Closed");
            }

            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        };

        client.connect();
    }

    private static String extractChannelId(EventDto event) {
        return event.getChannel().getId();
    }

    private static String extractRecordingName(EventDto event) {
        if (event.getRecording() != null) {
            return event.getRecording().getName();
        }
        return null;
    }

    private static void playAudio(String channelId, String sound) throws Exception {
        System.out.println("Send command to play Audio...");
        String url = HTTP_ARI_URL + "/channels/" + channelId + "/play?media=" + sound;
        post(url, "");
    }

    private static void startRecording(String channelId, String name) throws Exception {
        System.out.println("Start recording...");
        String url = HTTP_ARI_URL + "/channels/" + channelId + "/record";
        String queryParams = "?name=" + name + "&format=wav&maxDurationSeconds=3600";
        post(url + queryParams, "");
    }

    private static void downloadRecording(String recordingName) throws Exception {
        System.out.println("Downloading recording: " + recordingName);

        String url = HTTP_ARI_URL + "/recordings/stored/" + recordingName + "/file";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes()))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        System.out.println("GET " + url + " => " + response.statusCode());

        if (response.statusCode() == 200) {
            byte[] audioBytes = response.body();
            Path targetFile = Paths.get("records/" + recordingName + ".wav");
            Files.write(targetFile, audioBytes);
            System.out.println("Recording saved to " + targetFile.toAbsolutePath());
        } else {
            System.out.println("Failed to download recording. HTTP Status: " + response.statusCode());
        }
    }

    private static void post(String url, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("POST " + url + " => " + response.statusCode());
    }

    private static String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes()))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("GET " + url + " => " + response.statusCode());
        return response.body();
    }

}
