package com.symphony.platformsolutions.decisiontree.listeners;

import com.symphony.platformsolutions.decisiontree.DecisionTreeBot;
import com.symphony.platformsolutions.decisiontree.entity.Scenario;
import com.symphony.platformsolutions.decisiontree.entity.ScenarioPath;
import listeners.IMListener;
import model.InboundMessage;
import model.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

public class IMListenerImpl implements IMListener {
    private static final Logger LOG = LoggerFactory.getLogger(IMListenerImpl.class);

    public void onIMMessage(InboundMessage inMsg) {
        long userId = inMsg.getUser().getUserId();
        String msg = inMsg.getMessageText().trim();
        String streamId = inMsg.getStream().getStreamId();

        while (processMessage(userId, msg, streamId)) {
            LOG.debug("Processed messages");
        }
    }

    public void onIMCreated(Stream stream) {}

    private boolean processMessage(long userId, String msg, String streamId) {
        if (msg.equalsIgnoreCase("/reset")) {
            DecisionTreeBot.resetState(userId);
        }

        if (!DecisionTreeBot.getState().containsKey(userId)) {
            DecisionTreeBot.updateState(userId, new LinkedList<>());
            List<String> options = getOptions(0, null);
            int stage = DecisionTreeBot.getState(userId).size();
            sendOptionsMessage(streamId, DecisionTreeBot.getScenarioDb().getHeaders()[stage], options, false);
            return false;
        }

        if (DecisionTreeBot.getState(userId).stream()
            .anyMatch(scenario -> scenario.getQuestion().equals("Custom Enquiry") && scenario.getAnswer() == null)
        ) {
            DecisionTreeBot.getState(userId).stream()
                .filter(scenario -> scenario.getQuestion().equals("Custom Enquiry"))
                .findFirst().orElse(new Scenario()).setAnswer(msg);

            String userState = DecisionTreeBot.getState(userId).stream()
                .map(scenario -> String.format("<li>%s: %s</li>", scenario.getQuestion(), scenario.getAnswer()))
                .collect(Collectors.joining(""));
            String enquiryML = String.format("User Enquiry received from <mention uid=\"%d\"/>:<ul>%s</ul>",
                userId, userState);

            DecisionTreeBot.sendMessage(DecisionTreeBot.getAdminRoomId(), enquiryML);
            DecisionTreeBot.resetState(userId);
            DecisionTreeBot.sendMessage(streamId, "Your enquiry has been submitted. You will be contacted shortly.");
            return false;
        }

        int stage = DecisionTreeBot.getState(userId).size();
        List<String> previousOptions = getOptions(stage, DecisionTreeBot.getState(userId));
        String option;

        if (previousOptions.size() < 10) {
            if (!msg.matches("^\\d+$")) {
                DecisionTreeBot.sendMessage(streamId, "Invalid choice");
                return false;
            }
            int choice = Integer.parseInt(msg) - 1;
            if (choice > previousOptions.size() - 1) {
                DecisionTreeBot.sendMessage(streamId, "Invalid choice");
                return false;
            }
            option = previousOptions.get(choice);
        } else {
            if (msg.equalsIgnoreCase("/list")) {
                DecisionTreeBot.sendMessage(streamId, "Options: " + String.join(", ", previousOptions));
                return false;
            }
            Optional<String> choice = previousOptions.stream()
                .filter(o -> o.replaceAll(" ", "")
                    .equalsIgnoreCase(msg.replaceAll(" ", ""))
                ).findFirst();
            if (!choice.isPresent()) {
                DecisionTreeBot.sendMessage(streamId, "Invalid choice");
                return false;

            } else {
                option = choice.get();
            }
        }

        String previousHeader = DecisionTreeBot.getScenarioDb().getHeaders()[stage];

        if (option.equals("My enquiry is not listed") && DecisionTreeBot.getAdminRoomId() != null) {
            DecisionTreeBot.sendMessage(streamId, "Please enter your enquiry:");
            DecisionTreeBot.getState(userId).add(new Scenario("Custom Enquiry", null));
            return false;
        } else if (option.equals("Back to Main")) {
            DecisionTreeBot.resetState(userId);
            return true;
        }

        DecisionTreeBot.getState(userId).add(new Scenario(previousHeader, option));

        List<String> options;
        while (true) {
            stage = DecisionTreeBot.getState(userId).size();
            options = getOptions(stage, DecisionTreeBot.getState(userId));
            if (!options.get(0).equals("N/A") || stage == DecisionTreeBot.getScenarioDb().getHeaders().length - 1) {
                break;
            }
            DecisionTreeBot.getState(userId).add(new Scenario(DecisionTreeBot.getScenarioDb().getHeaders()[stage], options.get(0)));
        }

        List<String> optionsToReturn = options;
        if (stage == DecisionTreeBot.getScenarioDb().getHeaders().length - 1) {
            optionsToReturn = Collections.singletonList(options.get(0));
            DecisionTreeBot.resetState(userId);
            sendOptionsMessage(streamId, DecisionTreeBot.getScenarioDb().getHeaders()[stage], optionsToReturn, true);
        } else {
            sendOptionsMessage(streamId, DecisionTreeBot.getScenarioDb().getHeaders()[stage], optionsToReturn, false);
        }
        return false;
    }

    private void sendOptionsMessage(String streamId, String header, List<String> options, boolean isResult) {
        StringBuilder replyML = new StringBuilder();

        if (!isResult) {
            int optionIndex = 1;
            replyML.append(header);

            if (options.size() < 10) {
                replyML.append("<ul>");
                for (String option : options) {
                    replyML.append(String.format("<li>%d: %s</li>", optionIndex++, option));
                }
                replyML.append("</ul>");
            } else {
                replyML.append(String.format(" (%d options available. reply with /list to get all options.)",
                    options.size()));
            }
        } else {
            String colour;
            switch (options.get(0)) {
                case "Permitted": colour = "green"; break;
                case "Not permitted": colour = "red"; break;
                default: colour = "yellow";
            }
            replyML.append(String.format("%s: <span class=\"tempo-bg-color--%s\">%s</span>",
                header, colour, options.get(0)));
        }
        DecisionTreeBot.sendMessage(streamId, replyML.toString());
    }

    private List<String> getOptions(int stage, List<Scenario> previousOptions) {
        String header = DecisionTreeBot.getScenarioDb().getHeaders()[stage];
        List<String> options;

        if (stage == 0) {
            options = DecisionTreeBot.getScenarioDb().getScenarioPaths().stream().distinct()
                .map(ScenarioPath::getScenarios)
                .flatMap(Collection::stream)
                .filter(scenario -> scenario.getQuestion().equals(header))
                .map(Scenario::getAnswer)
                .distinct()
                .collect(Collectors.toCollection(LinkedList::new));
        } else {
            options = DecisionTreeBot.getScenarioDb().getScenarioPaths().stream()
                .filter(scenarioPath -> scenarioPath.getScenarios().containsAll(previousOptions))
                .map(ScenarioPath::getScenarios)
                .flatMap(Collection::stream)
                .filter(scenario -> scenario.getQuestion().equals(header))
                .map(Scenario::getAnswer)
                .distinct()
                .collect(Collectors.toCollection(LinkedList::new));
            options.add("Back to Main");
        }
        if (DecisionTreeBot.getAdminRoomId() != null) {
            options.add("My enquiry is not listed");
        }
        return options;
    }
}
