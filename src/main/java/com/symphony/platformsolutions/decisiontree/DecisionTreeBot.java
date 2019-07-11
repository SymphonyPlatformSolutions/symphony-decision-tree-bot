package com.symphony.platformsolutions.decisiontree;

import authentication.SymBotRSAAuth;
import clients.SymBotClient;
import com.symphony.platformsolutions.decisiontree.config.DecisionTreeBotConfig;
import com.symphony.platformsolutions.decisiontree.entity.Scenario;
import com.symphony.platformsolutions.decisiontree.entity.ScenarioDatabase;
import com.symphony.platformsolutions.decisiontree.listeners.IMListenerImpl;
import com.symphony.platformsolutions.decisiontree.listeners.RoomListenerImpl;
import com.symphony.platformsolutions.decisiontree.service.ScenarioService;
import com.symphony.platformsolutions.decisiontree.web.WebService;
import configuration.SymConfigLoader;
import model.OutboundMessage;
import model.RoomInfo;
import model.RoomSearchQuery;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.core.NoContentException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DecisionTreeBot {
    private static final Logger LOG = LoggerFactory.getLogger(DecisionTreeBot.class);
    private static SymBotClient botClient;
    private static DecisionTreeBotConfig config;
    private static String adminRoomId = null;
    private static ScenarioDatabase scenarioDatabase;
    private static Map<Long, List<Scenario>> state = new HashMap<>();

    public static void main(String[] args) {
        new DecisionTreeBot();
    }

    private DecisionTreeBot() {
        BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);

        try {
            config = SymConfigLoader.loadConfig("config.json", DecisionTreeBotConfig.class);
            reloadScenarioDb();

            SymBotRSAAuth botAuth = new SymBotRSAAuth(config);
            botAuth.authenticate();
            botClient = SymBotClient.initBot(config, botAuth);
            botClient.getDatafeedEventsService().addListeners(new RoomListenerImpl(), new IMListenerImpl());

            if (config.getAdminRoomName() != null && !config.getAdminRoomName().isEmpty()) {
                RoomSearchQuery query = new RoomSearchQuery();
                query.setQuery(config.getAdminRoomName());
                List<RoomInfo> rooms = botClient.getStreamsClient().searchRooms(query, 0, 1).getRooms();
                if (rooms.isEmpty()) {
                    throw new NoContentException("");
                }
                adminRoomId = rooms.get(0).getRoomSystemInfo().getId();
                LOG.info("Admin Room = {} ({})", rooms.get(0).getRoomAttributes().getName(), adminRoomId);
            } else {
                LOG.info("No admin room defined");
            }

            botClient.getPresenceClient().setPresence("Available");
            LOG.info("Bot is ready");

            startServer();
        } catch (NoContentException e) {
            LOG.error("Compliance room [{}] does not exist", config.getAdminRoomName());
            System.exit(1);
        } catch (IOException | URISyntaxException e) {
            LOG.error("Data file does not exist or insufficient permissions: {}", config.getDataFilePath());
            System.exit(1);
        }
    }

    private void startServer() {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        Server jettyServer = new Server(8080);
        jettyServer.setHandler(context);

        ServletHolder jerseyServlet = context.addServlet(ServletContainer.class, "/*");
        jerseyServlet.setInitOrder(0);

        jerseyServlet.setInitParameter("jersey.config.server.provider.classnames", WebService.class.getCanonicalName());

        try {
            jettyServer.start();
            jettyServer.join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jettyServer.destroy();
        }
    }

    public static void reloadScenarioDb() throws IOException, URISyntaxException {
        List<String[]> data = ScenarioService.readCsv();
        scenarioDatabase = ScenarioService.loadScenarioDatabase(data);

        LOG.info("Loaded {} scenario paths from data file {}",
            scenarioDatabase.getScenarioPaths().size(), getDataFilePath());
    }

    public static String getDataFilePath() {
        return config.getDataFilePath();
    }

    public static ScenarioDatabase getScenarioDb() {
        return scenarioDatabase;
    }

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

    public static String getAdminRoomId() {
        return adminRoomId;
    }

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
