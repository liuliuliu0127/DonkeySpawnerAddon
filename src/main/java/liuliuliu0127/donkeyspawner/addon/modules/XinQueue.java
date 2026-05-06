package liuliuliu0127.donkeyspawner.addon.modules;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class XinQueue extends Module {
    private final JsonObject questions;

    public XinQueue() {
        super(DonkeySpawnerAddon.CATEGORY, "xin-queue", "automatically answer questions when queueing in2b2t.xin");
        this.questions = loadQuestions();
    }

    private JsonObject loadQuestions() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("questions.json")),
                StandardCharsets.UTF_8))) {
            String content = reader.lines().collect(Collectors.joining("\n"));
            return JsonParser.parseString(content).getAsJsonObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new JsonObject(); // 加载失败时返回空对象，避免崩溃
        }
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (mc.getConnection() == null) return;  // Mojang mapping: getConnection()

        String message = event.getMessage().getString();
        if (!message.contains("丨")) return;

        String[] parts = message.split("丨");
        if (parts.length != 2) return;

        String question = parts[0].replaceAll("<[^>]*>", "").trim();
        String options = parts[1].trim();

        if (!questions.has(question)) return;

        Pattern pattern = Pattern.compile(questions.get(question).getAsString());
        Matcher matcher = pattern.matcher(options);

        if (!matcher.find()) return;

        String answer = matcher.group(1);
        // Mojang mapping: 发送聊天消息使用 mc.player.connection.sendChat()
        mc.player.connection.sendChat(answer);
    }
}