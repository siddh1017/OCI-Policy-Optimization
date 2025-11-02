package com.springAI.cloudService;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.model.Policy;
import com.oracle.bmc.identity.requests.ListCompartmentsRequest;
import com.oracle.bmc.identity.requests.ListPoliciesRequest;
import com.oracle.bmc.identity.responses.ListCompartmentsResponse;
import com.oracle.bmc.identity.responses.ListPoliciesResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


@Service
public class OCIService {
    Logger logger = Logger.getLogger(String.valueOf(OCIService.class));

    private final IdentityClient identityClient;
    private final ConfigFileAuthenticationDetailsProvider provider;

    OCIService(IdentityClient identityClient, ConfigFileAuthenticationDetailsProvider provider) {
        this.identityClient = identityClient;
        this.provider = provider;
    }

    /**
     * return policies based on compartmentID
     *
     * @param compartmentId compartment ID
     * @return list of polices
     */
    public List<Policy> fetchPolicyByCompartmentId(String compartmentId) {
        logger.info("Fetching policy by compartment id: " + compartmentId);
        ListPoliciesRequest listPoliciesRequest = ListPoliciesRequest.builder()
                .compartmentId(compartmentId)
                .build();

        ListPoliciesResponse listPoliciesResponse = identityClient.listPolicies(listPoliciesRequest);

        List<Policy> policies = listPoliciesResponse.getItems();
        for (Policy policy : policies) {
            String policyName = policy.getName();
            List<String> statements = policy.getStatements();
            logger.info("Policy Name: " + policyName);
            logger.info("Statements: " + statements);
        }
        return listPoliciesResponse.getItems();
    }


    public List<Compartment> listCompartments() {
        logger.info("Fetching compartments");
        ListCompartmentsRequest listCompartmentsRequest = ListCompartmentsRequest.builder()
                .compartmentId(provider.getTenantId())
                .compartmentIdInSubtree(true)
                .accessLevel(ListCompartmentsRequest.AccessLevel.Any)
                .build();


        List<Compartment> compartmentList = new ArrayList<>();
        String nextPage = null;
        do {
            ListCompartmentsResponse response = identityClient.listCompartments(
                    listCompartmentsRequest.toBuilder().page(nextPage).build()
            );
            List<Compartment> items = response.getItems();
            compartmentList.addAll(items);
            nextPage = response.getOpcNextPage();
        } while (nextPage != null);

        logger.info("Listing compartments :: " + compartmentList);
        return compartmentList;
    }
}
