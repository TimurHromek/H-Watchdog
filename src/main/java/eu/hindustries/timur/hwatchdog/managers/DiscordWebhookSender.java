package eu.hindustries.timur.hwatchdog.managers;

import eu.hindustries.timur.hwatchdog.HWatchdogPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhookSender {

    private final HWatchdogPlugin plugin;

    public DiscordWebhookSender(HWatchdogPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendPublicCommandAlert(Player op, String command, String reason) {
        String url = plugin.getConfigManager().getPublicWebhookUrl();
        if (!isUrlValid(url)) return;

        String mention = plugin.getConfigManager().getMentionRoleId();
        String content = (mention != null && !mention.isEmpty()) ? mention : "";

        String payload;
        if (plugin.getConfigManager().useEmbeds()) {
            payload = buildCommandEmbedPayload(content, op, command, reason);
        } else {
            String message = "⚠️ OP " + op.getName() + " used " + command + " in world: " + op.getWorld().getName() + ".";
            if (reason != null) message += "\n📝 Reason: " + reason;
            payload = buildSimplePayload(content, message);
        }
        send(url, payload);
    }

    public void sendAutoDemoteAlert(String opName) {
        String url = plugin.getConfigManager().getPublicWebhookUrl();
        if (!isUrlValid(url)) return;

        String payload;
        if (plugin.getConfigManager().useEmbeds()) {
            payload = buildSimpleEmbedPayload("OP Auto-Demoted", "❌ " + opName + " was automatically demoted due to excessive use of risky commands.", 0xFF0000);
        } else {
            payload = buildSimplePayload("", "❌ [H-Watchdog] OP " + opName + " auto-demoted.");
        }
        send(url, payload);
    }

    public void sendReportAlert(String reporterName, String targetOpName, String reason) {
        String url = plugin.getConfigManager().getReportWebhookUrl();
        if (!isUrlValid(url)) return;

        String payload;
        if (plugin.getConfigManager().useEmbeds()) {
            payload = buildReportEmbedPayload(reporterName, targetOpName, reason);
        } else {
            String message = "📩 Player " + reporterName + " reported OP " + targetOpName + ": \"" + reason + "\"";
            payload = buildSimplePayload("", message);
        }
        send(url, payload);
    }

    private void send(String urlString, String jsonPayload) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "H-Watchdog-Plugin");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    plugin.getLogger().warning("Failed to send Discord webhook to " + urlString + ". Response code: " + responseCode);
                }
                connection.disconnect();
            } catch (IOException e) {
                plugin.getLogger().severe("Error sending Discord webhook: " + e.getMessage());
            }
        });
    }

    // --- Payload Builders ---

    private String buildSimplePayload(String content, String message) {
        return "{\"content\":\"" + content + " " + escapeJson(message) + "\"}";
    }

    private String buildSimpleEmbedPayload(String title, String description, int color) {
        return "{\"embeds\":[{\"title\":\"" + escapeJson(title) + "\",\"description\":\"" + escapeJson(description) + "\",\"color\":" + color + ",\"footer\":{\"text\":\"H-Watchdog\"},\"timestamp\":\"" + java.time.Instant.now().toString() + "\"}]}";
    }

    private String buildCommandEmbedPayload(String content, Player op, String command, String reason) {
        StringBuilder embed = new StringBuilder("{\"content\":\"" + content + "\",\"embeds\":[{\"title\":\"OP Command Used\",\"color\":16753920,\"fields\":[")
                .append("{\"name\":\"Operator\",\"value\":\"").append(escapeJson(op.getName())).append("\",\"inline\":true},")
                .append("{\"name\":\"World\",\"value\":\"").append(escapeJson(op.getWorld().getName())).append("\",\"inline\":true},")
                .append("{\"name\":\"Command Used\",\"value\":\"`").append(escapeJson(command)).append("`\",\"inline\":false}");
        if (reason != null) {
            embed.append(",{\"name\":\"Reason Provided\",\"value\":\"").append(escapeJson(reason)).append("\",\"inline\":false}");
        }
        embed.append("],\"footer\":{\"text\":\"H-Watchdog\"},\"timestamp\":\"").append(java.time.Instant.now().toString()).append("\"}]}");
        return embed.toString();
    }

    private String buildReportEmbedPayload(String reporterName, String targetOpName, String reason) {
        return "{\"embeds\":[{\"title\":\"Player Report\",\"color\":3447003,\"fields\":[" +
                "{\"name\":\"Reported OP\",\"value\":\"" + escapeJson(targetOpName) + "\",\"inline\":true}," +
                "{\"name\":\"Reported By\",\"value\":\"" + escapeJson(reporterName) + "\",\"inline\":true}," +
                "{\"name\":\"Reason\",\"value\":\"" + escapeJson(reason) + "\",\"inline\":false}" +
                "],\"footer\":{\"text\":\"H-Watchdog | Player Reports\"},\"timestamp\":\"" + java.time.Instant.now().toString() + "\"}]}";
    }

    private boolean isUrlValid(String url) {
        return plugin.getConfigManager().isDiscordEnabled() && url != null && !url.isEmpty() && !url.contains("YOUR_");
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}