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
import java.util.stream.IntStream;

public class IMListenerImpl implements IMListener {
    private static final Logger LOG = LoggerFactory.getLogger(IMListenerImpl.class);
    public static final String COLOR_TEMPLATE = "<span class=\"tempo-bg-color--%s\">~$0</span>";

    public void onIMMessage(InboundMessage inMsg) {
        long userId = inMsg.getUser().getUserId();
        String msg = inMsg.getMessageText().trim();
        String streamId = inMsg.getStream().getStreamId();

        while (processMessage(userId, msg, streamId)) {
            LOG.debug("Processed messages");
        }
    }

    public void onIMCreated(Stream stream) {
        if (DecisionTreeBot.getWelcomeMessage() != null) {
            DecisionTreeBot.sendMessage(stream.getStreamId(), DecisionTreeBot.getWelcomeMessage());
        }
    }

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

        boolean isCustomEnquiry = DecisionTreeBot.getState(userId).stream()
            .anyMatch(scenario -> scenario.getQuestion().equals("Custom Enquiry") && scenario.getAnswer() == null);

        if (isCustomEnquiry) {
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

        if (!msg.matches("^\\d+$")) {
            // String input
            option = previousOptions.stream()
                .filter(o -> o.replaceAll(" ", "")
                    .equalsIgnoreCase(msg.replaceAll(" ", ""))
                ).findFirst().orElse(null);
        } else {
            // Numeric input
            try {
                option = previousOptions.get(Integer.parseInt(msg) - 1);
            } catch (IndexOutOfBoundsException e) {
                option = null;
            }
        }
        if (option == null) {
            DecisionTreeBot.sendMessage(streamId, DecisionTreeBot.getInvalidChoiceMessage());
            return false;
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
        boolean isResult = (stage == DecisionTreeBot.getScenarioDb().getHeaders().length - 1);

        if (isResult) {
            optionsToReturn = Collections.singletonList(options.get(0));
            DecisionTreeBot.resetState(userId);
        }
        sendOptionsMessage(streamId, DecisionTreeBot.getScenarioDb().getHeaders()[stage], optionsToReturn, isResult);
        return false;
    }

    private void sendOptionsMessage(String streamId, String header, List<String> options, boolean isResult) {
        if (!isResult) {
            String optionsML = IntStream.range(0, options.size())
                .mapToObj(i -> String.format("<li>%d: %s</li>", i+1, options.get(i)))
                .collect(Collectors.joining(""));

            DecisionTreeBot.sendMessage(streamId, String.format("%s<ul>%s</ul>", header, optionsML));
        } else {
            String result = options.get(0);

            String resultML = result
                .replaceAll("([Pp]ermitted, [Ww]ith [Cc]onditions)", getTemplate("yellow"))
                .replaceAll("([Nn]ot [Pp]ermitted)", getTemplate("red"))
                .replaceAll("(?<!~([Nn]ot )?)([Pp]ermitted)", getTemplate("green"))
                .replaceAll("~([Pp]ermitted)", "$1")
                .replaceAll("~([Nn]ot)", "$1");

            DecisionTreeBot.sendMessage(streamId, String.format("%s: %s", header, resultML));

            if (DecisionTreeBot.getCompletionMessage() != null) {
                DecisionTreeBot.sendMessage(streamId, DecisionTreeBot.getCompletionMessage());
            }
        }
    }

    private String getTemplate(String color) {
        String template = color.equals("green") ? COLOR_TEMPLATE.replace("~", "") : COLOR_TEMPLATE;
        return String.format(template, color);
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
        if (DecisionTreeBot.getAdminRoomId() != null && DecisionTreeBot.isEnquiryEnabled()) {
            options.add("My enquiry is not listed");
        }
        return options;
    }
}
