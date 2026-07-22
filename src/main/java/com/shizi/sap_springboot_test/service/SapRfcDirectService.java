package com.shizi.sap_springboot_test.service;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoRepository;
import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;
import com.shizi.sap_springboot_test.config.SapJcoProperties;

@Service
public class SapRfcDirectService {

    private static final String FUNCTION_NAME = "STFC_CONNECTION";
    private static final Map<String, Properties> DESTINATIONS = new ConcurrentHashMap<>();

    private static volatile boolean providerRegistered;

    private final SapJcoProperties properties;

    public SapRfcDirectService(SapJcoProperties properties) {
        this.properties = properties;
    }

    public Map<String, String> callStfcConnection(String requtext) {
        List<String> missingKeys = properties.validateForDemo();
        if (!missingKeys.isEmpty()) {
            throw new IllegalStateException("Please fill SAP connection config first: " + String.join(", ", missingKeys));
        }

        try {
            ensureDestinationProviderRegistered();
            DESTINATIONS.put(properties.getDestinationName(), properties.toDestinationProperties());

            JCoDestination destination = JCoDestinationManager.getDestination(properties.getDestinationName());
            destination.ping();

            JCoRepository repository = destination.getRepository();
            JCoFunction function = repository.getFunction(FUNCTION_NAME);
            if (function == null) {
                throw new IllegalStateException("SAP standard RFC not found: " + FUNCTION_NAME);
            }

            function.getImportParameterList().setValue("REQUTEXT", requtext);
            function.execute(destination);

            return Map.of(
                    "ECHOTEXT", function.getExportParameterList().getString("ECHOTEXT"),
                    "RESPTEXT", function.getExportParameterList().getString("RESPTEXT")
            );
        } catch (JCoException ex) {
            throw new IllegalStateException("SAP RFC call failed: " + ex.getMessage(), ex);
        }
    }

    private static synchronized void ensureDestinationProviderRegistered() {
        if (providerRegistered) {
            return;
        }

        Environment.registerDestinationDataProvider(new InMemoryDestinationDataProvider());
        providerRegistered = true;
    }

    private static final class InMemoryDestinationDataProvider implements DestinationDataProvider {

        @Override
        public Properties getDestinationProperties(String destinationName) {
            return DESTINATIONS.get(destinationName);
        }

        @Override
        public void setDestinationDataEventListener(DestinationDataEventListener eventListener) {
            // This demo keeps destination properties in memory and does not fire update events.
        }

        @Override
        public boolean supportsEvents() {
            return false;
        }
    }
}
