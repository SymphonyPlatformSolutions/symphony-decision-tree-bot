package com.symphony.platformsolutions.decisiontree.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.symphony.platformsolutions.decisiontree.DecisionTreeBot;
import com.symphony.platformsolutions.decisiontree.entity.Scenario;
import com.symphony.platformsolutions.decisiontree.entity.ScenarioDatabase;
import com.symphony.platformsolutions.decisiontree.entity.ScenarioPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ScenarioService {
    private static final Logger LOG = LoggerFactory.getLogger(ScenarioService.class);

    public static List<String[]> readCsv() throws IOException, CsvException {
        String path = DecisionTreeBot.getDataFilePath();

        if (!(new File(path)).exists()) {
            LOG.error("File does not exist: {}", path);
            System.exit(1);
        }

        InputStreamReader reader = new InputStreamReader(new FileInputStream(path), Charset.forName("windows-1252"));
        CSVReader csvReader = new CSVReader(reader);
        List<String[]> list = csvReader.readAll();

        if (list.isEmpty()) {
            reader.close();
            csvReader.close();
            LOG.info("Cannot read");

            reader = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8);
            csvReader = new CSVReader(reader);
            list = csvReader.readAll();

            LOG.info("Read {} lines", list.size());
        }

        reader.close();
        csvReader.close();
        return list;
    }

    public static ScenarioDatabase loadScenarioDatabase(List<String[]> data) {
        String[] headers = data.get(0);
        return new ScenarioDatabase(
            headers,
            data.stream()
                .filter(entry -> !entry[0].equals(headers[0]))
                .map(entry -> {
                    ScenarioPath scenarioPath = new ScenarioPath();
                    for (int i=0; i<entry.length; i++) {
                        scenarioPath.addScenario(new Scenario(headers[i], entry[i]));
                    }
                    return scenarioPath;
                })
                .collect(Collectors.toCollection(LinkedList::new))
        );
    }

    public static File getDataFile() {
        String path = DecisionTreeBot.getDataFilePath();
        try {
            if ((new File(path)).exists())
                return new File(path);
            else
                return new File(ClassLoader.getSystemResource(path).toURI());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static void saveDataFile(byte[] fileBytes) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(DecisionTreeBot.getDataFilePath())) {
            fos.write(fileBytes);
        }
    }
}
