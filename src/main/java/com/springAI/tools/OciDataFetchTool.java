package com.springAI.tools;

import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.model.Policy;
import com.springAI.cloudService.OCIService;
import com.springAI.entity.PolicyStatement;
import com.springAI.utilities.OCICache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OciDataFetchTool {
    Logger logger = Logger.getLogger(String.valueOf(OciDataFetchTool.class));
    String COMPARTMENT_ID = "ocid1.compartment.oc1..aaaaaaaanja6vwsret3ng5cmvi5yqzrttnfywsovxjb32bpy4gbekj6l54aq";

    private final OCIService ociService;
    private final OCICache ociCache;

    public OciDataFetchTool(OCIService ociService, OCICache ociCache) {
        this.ociService = ociService;
        this.ociCache = ociCache;
    }

    @Tool (
            name = "fetchCompartmentNameById",
            description = "returns Compartment ID from CompartmentName for other Tool calling"
    )
    public String fetchCompartmentNameById(@ToolParam String name) {
        List<Compartment> compartmentList = ociCache.getCompartments();
        for (Compartment compartment : compartmentList) {
            if (compartment.getName().equals(name)) {
                return compartment.getId();
            }
        }
        return "No such compartment exist in Tenancy";
    }

    @Tool (
            name = "fetchALlCompartments",
            description = "return compartments list in tenancy"
    )
    public List<Compartment> fetchCompartments() {
        logger.info("Fetching all compartments from TOOL");
        return ociCache.getCompartments();
    }

    // todo remove hard-coded id
    @Tool(
            name = "fetchPolicyByCompartment",
            description = "return policy in particular compartment"
    )
    public List<PolicyStatement> fetchPolicyByCompartment() {
        logger.info("Fetching policy by policy from TOOL");
        return ociCache.getPolicies().get(COMPARTMENT_ID);
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

    @Tool(
            name = "refreshOciCache",
            description = "Fetch compartments and policies, then cache them"
    )
    public String refreshOciCache() {
        ociCache.clearCache();
        logger.info("Fetching all required details from TOOL");

        List<Compartment> comps = ociService.listCompartments();
        for (Compartment comp : comps) {
            logger.info(comp.getName());
            // add compartment to list
            ociCache.addCompartment(comp);

            // update Policy Map only if we are in TechCloud Compartment (Access Issue)
            if (comp.getId().equals(COMPARTMENT_ID)) {
                logger.info("Entered in if");

                List<Policy> policies = ociService.fetchPolicyByCompartmentId(comp.getId());

                List<String> invalidPolicies = new ArrayList<>();
                List<PolicyStatement> policyStatementList = new ArrayList<>();
                for (Policy policy : policies) {
                    List<String> statements = policy.getStatements();
                    for (String statement : statements) {
                        PolicyStatement policyStatement = fromStatement(statement);
                        if (policyStatement != null) {
                            policyStatementList.add(policyStatement);
                            logger.info(policyStatement.toString());
                        }
                        else {
                            invalidPolicies.add(statement);
                        }
                    }
                }
                ociCache.addPolicies(comp.getId(), policyStatementList);
                ociCache.addInvalidPolices(comp.getId(), invalidPolicies);
            }
        }
        return "OCI Cache refreshed successfully with " + comps.size() + " compartments.";
    }

}
