package com.springAI.tools;

import com.oracle.bmc.identity.model.Compartment;
import com.springAI.cloudService.OCIService;
import com.springAI.entity.CompartmentDTO;
import com.springAI.entity.PolicyStatement;
import com.springAI.utilities.OCICache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OciDataFetchTool {
    Logger logger = Logger.getLogger(String.valueOf(OciDataFetchTool.class));

    private final OCICache ociCache;

    public OciDataFetchTool(OCIService ociService, OCICache ociCache) {
        this.ociCache = ociCache;
    }

    /**
     * fetch compartmentID from compartment Name
     *
     * @param name compartment Name
     * @return compartment ID
     */
    public String fetchCompartmentNameById(String name) {
        List<Compartment> compartmentList = ociCache.getCompartments();
        for (Compartment compartment : compartmentList) {
            if (compartment.getName().equalsIgnoreCase(name)) {
                return compartment.getId();
            }
        }
        return null;
    }

    @Tool(
            name = "fetchAllCompartments",
            description = "Returns a list of all compartments in the tenancy"
    )
    public List<CompartmentDTO> fetchCompartments() {
        logger.info("Fetching all compartments from TOOL");
        List<CompartmentDTO> compartmentDTOList = ociCache.getCompartments()
                .stream()
                .map(CompartmentDTO::new)
                .collect(Collectors.toList());
        logger.info("Fetched " + compartmentDTOList + " compartments");
        return compartmentDTOList;
    }

    @Tool(
            name = "fetchPolicyByCompartment",
            description = "return policy in particular compartmentId, " +
                    "if null ask user to give exact name of compartment"
    )
    public List<PolicyStatement> fetchPolicyByCompartment(@ToolParam String CompartmentName) {
        logger.info("Fetching policy from TOOL for compartment :: " + CompartmentName);
        String compartmentId = fetchCompartmentNameById(CompartmentName);
        if (compartmentId != null) {
            logger.info("Fetching policy from TOOL for compartment :: " + compartmentId);
            return ociCache.getPolicies().get(compartmentId);
        }
        return null;
    }
}
