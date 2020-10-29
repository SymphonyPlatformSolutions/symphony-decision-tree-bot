package com.symphony.platformsolutions.decisiontree;

import authentication.SymBotRSAAuth;
import clients.SymBotClient;
import com.opencsv.exceptions.CsvException;
import com.sun.net.httpserver.HttpServer;
import com.symphony.platformsolutions.decisiontree.config.DecisionTreeBotConfig;
import com.symphony.platformsolutions.decisiontree.entity.Scenario;
import com.symphony.platformsolutions.decisiontree.entity.ScenarioDatabase;
import com.symphony.platformsolutions.decisiontree.listeners.IMListenerImpl;
import com.symphony.platformsolutions.decisiontree.listeners.RoomListenerImpl;
import com.symphony.platformsolutions.decisiontree.service.ScenarioService;
import configuration.SymConfigLoader;
import model.OutboundMessage;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.core.NoContentException;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

public class DecisionTreeBot {
    private static final Logger LOG = LoggerFactory.getLogger(DecisionTreeBot.class);
    private static SymBotClient botClient;
    private static DecisionTreeBotConfig config;
    private static String adminRoomId = null;
    private static ScenarioDatabase scenarioDatabase;
    private static Map<Long, List<Scenario>> state = new HashMap<>();

    public static void main(String[] args) throws Exception {
        new DecisionTreeBot();
    }

    private DecisionTreeBot() throws Exception {
        BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
        LogManager.getLogManager().reset();

        try {
            config = SymConfigLoader.loadConfig("config.json", DecisionTreeBotConfig.class);
            reloadScenarioDb();

            SymBotRSAAuth botAuth = new SymBotRSAAuth(config);
            botAuth.authenticate();
            botClient = SymBotClient.initBot(config, botAuth);
            botClient.getDatafeedEventsService().addListeners(new RoomListenerImpl(), new IMListenerImpl());

            if (config.getAdminRoomStreamId() != null && !config.getAdminRoomStreamId().isEmpty()) {
                adminRoomId = config.getAdminRoomStreamId();
                LOG.info("Admin Room = {}", adminRoomId);
            } else {
                LOG.info("No admin room defined");
            }
            LOG.info("Bot is ready");

            if (config.isHealthCheckEnabled()) {
                startHealthCheckServer();
            }
        } catch (NoContentException e) {
            LOG.error("Compliance room [{}] does not exist", config.getAdminRoomName());
            System.exit(1);
        } catch (IOException e) {
            LOG.error("Data file does not exist or insufficient permissions: {}", config.getDataFilePath());
            System.exit(1);
        }
    }

    private void startHealthCheckServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/healthz", exchange -> {
                String response = "{ \"status\": \"UP\" }";
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            });
            server.setExecutor(null);
            server.start();
            LOG.info("Health check endpoint is up");
        } catch (IOException e) {
            LOG.error("Unable to start health check", e);
        }
    }

    /* Config */

    public static String getDataFilePath() {
        return config.getDataFilePath();
    }

    public static String getAdminRoomId() {
        return adminRoomId;
    }

    public static boolean isEnquiryEnabled() {
        return config.isEnquiryEnabled();
    }

    public static String getWelcomeMessage() {
        return config.getWelcomeMessage();
    }

    public static String getInvalidChoiceMessage() {
        if (config.getInvalidChoiceMessage() == null) {
            return "Invalid Choice";
        }
        return config.getInvalidChoiceMessage();
    }

    public static String getCompletionMessage() {
        return config.getCompletionMessage();
    }

    /* Scenario Database */

    public static void reloadScenarioDb() throws IOException, CsvException {
        List<String[]> data = ScenarioService.readCsv();
        scenarioDatabase = ScenarioService.loadScenarioDatabase(data);

        LOG.info("Loaded {} scenario paths from data file {}",
            scenarioDatabase.getScenarioPaths().size(), getDataFilePath());
    }

    public static ScenarioDatabase getScenarioDb() {
        return scenarioDatabase;
    }

    /* State Management */

    public static Map<Long, List<Scenario>> getState() {
        return state;
    }

    public static List<Scenario> getState(long userId) {
        return state.get(userId);
    }

    public static void resetState() {
        state = new HashMap<>();
    }

    public static void resetState(long userId) {
        state.remove(userId);
    }

    public static void updateState(long userId, List<Scenario> options) {
        state.put(userId, options);
    }

    public static String cleanMessage(String original) {
        return original.replaceAll("&", "&amp;")
            .replaceAll("<(?!/?a)", "&lt;")
            .replaceAll("<br>", "<br />")
            .replaceAll("\n", "<br />");
    }

    /* Symphony APIs */

    public static void sendMessage(String streamId, String message) {
        botClient.getMessagesClient().sendMessage(streamId, new OutboundMessage(message));
    }

    public static void sendMessage(String streamId, String message, File[] attachments) {
        botClient.getMessagesClient().sendMessage(streamId, new OutboundMessage(message, null, attachments));
    }

    public static String getUserIMStreamId(long userId) {
        return botClient.getStreamsClient().getUserIMStreamId(userId);
    }

    public static byte[] getAttachment(String streamId, String messageId, String attachmentId) {
        return botClient.getMessagesClient().getAttachment(streamId, attachmentId, messageId);
    }
}
