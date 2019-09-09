package com.symphony.platformsolutions.decisiontree.listeners;

import com.symphony.platformsolutions.decisiontree.DecisionTreeBot;
import com.symphony.platformsolutions.decisiontree.service.ScenarioService;
import listeners.RoomListener;
import model.Attachment;
import model.InboundMessage;
import model.Stream;
import model.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoomListenerImpl implements RoomListener {
    private static final Logger LOG = LoggerFactory.getLogger(RoomListenerImpl.class);
    private static final String HELP_TEXT = "Usage:<ul><li><b>/reply @Person message</b>: sends a message to the mentioned person</li><li><b>/download</b>: get the current data file</li><li><b>/upload</b>: send this with a revised data file attached to replace the current data file</li></ul>";

    public void onRoomMessage(InboundMessage inMsg) {
        String streamId = inMsg.getStream().getStreamId();
        if (!streamId.equalsIgnoreCase(DecisionTreeBot.getAdminRoomId())) {
            return;
        }

        String msgML = inMsg.getMessage();
        String msgText = inMsg.getMessageText().trim();
        List<Long> mentions = inMsg.getMentions();

        String command = msgText;
        if (command.contains(" "))
            command = command.substring(0, msgText.indexOf(' '));

        switch (command) {
            case "/reply":
                processReply(msgText, msgML, streamId, mentions);
                break;
            case "/download":
                processDownload(streamId, true);
                break;
            case "/upload":
                processUpload(streamId, inMsg.getMessageId(), inMsg.getAttachments());
                break;
            case "/help":
                processHelp();
                break;
            default:
                if (msgText.startsWith("/"))
                    processHelp();
        }
    }

    private void processReply(String msgText, String msgML, String streamId, List<Long> mentions) {
        if (mentions.size() < 1) {
            DecisionTreeBot.sendMessage(streamId, "Usage: /reply @Person message");
            return;
        }
        String mentionDisplayName = "";
        Pattern pattern = Pattern.compile("<span class=\"entity\" data-entity-id=\"0\">@(.*?)</span>");
        Matcher matcher = pattern.matcher(msgML);
        if (matcher.find())
            mentionDisplayName = matcher.group(1);
        String userImStreamId = DecisionTreeBot.getUserIMStreamId(mentions.get(0));
        String replyText = msgText
            .replaceFirst("/reply @", "")
            .replaceFirst(mentionDisplayName, "").trim();
        String mention = String.format("<mention uid=\"%d\" /> ", mentions.get(0));
        DecisionTreeBot.sendMessage(userImStreamId, mention + DecisionTreeBot.cleanMessage(replyText));
    }

    private void processDownload(String streamId, boolean isAdHoc) {
        File[] files = new File[] { ScenarioService.getDataFile() };
        String prefix = isAdHoc ? "Current" : "Previous";
        String suffix = isAdHoc ? "" : " for your reference";
        DecisionTreeBot.sendMessage(streamId, prefix + " data file attached" + suffix, files);
    }

    private void processUpload(String streamId, String messageId, List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            DecisionTreeBot.sendMessage(streamId, "Please attach the data file when using /upload");
            return;
        }
        String attachmentId = attachments.get(0).getId();
        byte[] dataBytes = DecisionTreeBot.getAttachment(streamId, messageId, attachmentId);

        // Backup existing data
        File existingDataFile = ScenarioService.getDataFile();
        byte[] existingDataBytes = null;

        if (existingDataFile == null)
            return;
        try (FileInputStream inputStream = new FileInputStream(existingDataFile)) {
            long byteLength = existingDataFile.length();
            existingDataBytes = new byte[(int) byteLength];
            LOG.debug("Read bytes {}", inputStream.read(existingDataBytes, 0, (int) byteLength));
        } catch (IOException e) {
            LOG.error("", e);
        }

        processDownload(streamId, false);

        try {
            ScenarioService.saveDataFile(dataBytes);
            DecisionTreeBot.reloadScenarioDb();
            DecisionTreeBot.resetState();

            DecisionTreeBot.sendMessage(streamId, "New data file saved successfully");
        } catch (Exception e1) {
            DecisionTreeBot.sendMessage(streamId, "Error saving data file. Please check file format.");

            // Restore backup if anything goes wrong
            try {
                ScenarioService.saveDataFile(existingDataBytes);
                DecisionTreeBot.reloadScenarioDb();
            } catch (IOException e2) {
                LOG.error("", e2);
            }
        }
    }

    private void processHelp() {
        DecisionTreeBot.sendMessage(DecisionTreeBot.getAdminRoomId(), HELP_TEXT);
    }

    public void onRoomCreated(RoomCreated roomCreated) {}
    public void onRoomDeactivated(RoomDeactivated roomDeactivated) {}
    public void onRoomMemberDemotedFromOwner(RoomMemberDemotedFromOwner roomMemberDemotedFromOwner) {}
    public void onRoomMemberPromotedToOwner(RoomMemberPromotedToOwner roomMemberPromotedToOwner) {}
    public void onRoomReactivated(Stream stream) {}
    public void onRoomUpdated(RoomUpdated roomUpdated) {}
    public void onUserJoinedRoom(UserJoinedRoom userJoinedRoom) {}
    public void onUserLeftRoom(UserLeftRoom userLeftRoom) {}
}
