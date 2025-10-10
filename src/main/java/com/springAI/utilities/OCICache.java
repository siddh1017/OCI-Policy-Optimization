package com.springAI.utilities;

import com.oracle.bmc.identity.model.Compartment;
import com.springAI.entity.PolicyStatement;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.logging.Logger;

@Service
public class OCICache {
    Logger logger = Logger.getLogger(OCICache.class.getName());
    private final Map<String, List<PolicyStatement>> policies = new HashMap<>();
    private final List<Compartment> compartments = new ArrayList<>();
    private final Map<String, String> parentMap = new HashMap<>();
    private final Map<String, List<String>> invalidPolicyMap = new HashMap<>();

    // Getter methods — expose read-only copies
    public Map<String, List<PolicyStatement>> getPolicies() {
        logger.info("Inside OCICache.getPolicies :: " + policies);
        return Collections.unmodifiableMap(policies);
    }

    public List<Compartment> getCompartments() {
        return Collections.unmodifiableList(compartments);
    }

    public Map<String, String> getParentMap() {
        return Collections.unmodifiableMap(parentMap);
    }

    // Utility methods for updates
    public void addCompartment(Compartment comp) {
        compartments.add(comp);
        parentMap.put(comp.getId(), comp.getCompartmentId());
        logger.info("Added compartment " + compartments);
    }

    public void addPolicies(String compartmentId, List<PolicyStatement> policyStatementList) {
        policies.put(compartmentId, policyStatementList);
        logger.info("adding Policies :: " + policies);
    }

    @PostConstruct
    public void init() {
        System.out.println("OCI Cache initialized...");
    }

    public void clearCache() {
        policies.clear();
        compartments.clear();
        parentMap.clear();
    }

    public void addInvalidPolices(String compartmentId, List<String> invalidPolicies) {
        invalidPolicyMap.put(compartmentId, invalidPolicies);
    }
}

