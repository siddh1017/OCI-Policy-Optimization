package com.springAI.utilities;

import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.model.Policy;
import com.springAI.cloudService.OCIService;
import com.springAI.entity.PolicyStatement;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OCICache {
    Logger logger = Logger.getLogger(OCICache.class.getName());
    private final Map<String, List<PolicyStatement>> policies = new HashMap<>();
    private final List<Compartment> compartments = new ArrayList<>();
    private final Map<String, String> parentMap = new HashMap<>();
    private final Map<String, List<String>> invalidPolicyMap = new HashMap<>();
    private final OCIService ociService;

    String COMPARTMENT_ID = "ocid1.compartment.oc1..aaaaaaaanja6vwsret3ng5cmvi5yqzrttnfywsovxjb32bpy4gbekj6l54aq";

    public OCICache(OCIService ociService) {
        this.ociService = ociService;
    }

    // Getter methods — expose read-only copies
    public Map<String, List<PolicyStatement>> getPolicies() {
        logger.info("Inside OCICache.getPolicies :: " + policies);
        return Collections.unmodifiableMap(policies);
    }

    public List<Compartment> getCompartments() {
        logger.info("Total compartments: " + compartments.size());
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
    }

    @PostConstruct
    public void init() {
        System.out.println("OCI Cache initialized...");
        refreshOciCache();
    }

    /**
     * converts policy String statement in PolicyStatement Object
     *
     * @param policyText policy statement
     * @return converted PolicyStatement Object
     */
    public PolicyStatement fromStatement(String policyText) {
        logger.info("Parsing Policy : " + policyText);

        // Make it case-insensitive
        String text = policyText.trim().toLowerCase();

        // Regex to extract parts
        Pattern pattern = Pattern.compile(
                "allow\\s+group\\s+([a-z0-9_,\\s-]+)\\s+to\\s+([a-z-]+)\\s+([a-z0-9-_]+)\\s+in\\s+compartment\\s+([a-z0-9-_]+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(policyText);

        if (matcher.find()) {
            // Extract groups, verb, resource, and compartment
            String groupsStr = matcher.group(1).trim();
            String verb = matcher.group(2).trim();
            String resource = matcher.group(3).trim();
            String compartment = matcher.group(4).trim();

            // Split multiple groups by comma
            Set<String> groups = new HashSet<>();
            for (String g : groupsStr.split(",")) {
                groups.add(g.trim());
            }
            return new PolicyStatement(groups, verb, resource, compartment);
        } else {
            logger.warning("Invalid policy statement: " + policyText);
            return null;
        }
    }

    public void refreshOciCache() {
        clearCache();
        logger.info("Fetching all required details");

        List<Compartment> comps = ociService.listCompartments();
        for (Compartment comp : comps) {
            logger.info(comp.getName());
            // add compartment to list
            addCompartment(comp);

            // update Policy Map only if we are in TechCloud Compartment (Access Issue)
            if (comp.getId().equals(COMPARTMENT_ID)) {
                logger.info("fetching policies for compartment :: " + comp.getId());
                List<Policy> policies = ociService.fetchPolicyByCompartmentId(comp.getId());

                List<String> invalidPolicies = new ArrayList<>();
                List<PolicyStatement> policyStatementList = new ArrayList<>();
                for (Policy policy : policies) {
                    List<String> statements = policy.getStatements();
                    for (String statement : statements) {
                        PolicyStatement policyStatement = fromStatement(statement);
                        if (policyStatement != null) {
                            policyStatementList.add(policyStatement);
                        }
                        else {
                            invalidPolicies.add(statement);
                        }
                    }
                }
                addPolicies(comp.getId(), policyStatementList);
                addInvalidPolices(comp.getId(), invalidPolicies);
            }
        }

    }


    public void clearCache() {
        policies.clear();
        compartments.clear();
        parentMap.clear();
    }

    public void addInvalidPolices(String compartmentId, List<String> invalidPolicies) {
        invalidPolicyMap.put(compartmentId, invalidPolicies);
    }

    //  comment it (only for testing)
//    public void setParentMap(Map<String, String> map) {
//        parentMap.clear();
//        parentMap.putAll(map);
//    }
}

