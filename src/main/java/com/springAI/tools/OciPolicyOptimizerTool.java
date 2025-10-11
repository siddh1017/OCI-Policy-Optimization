package com.springAI.tools;

import com.oracle.bmc.identity.model.Compartment;
import com.springAI.entity.PolicyStatement;
import com.springAI.utilities.OCICache;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;

@Service
public class OciPolicyOptimizerTool {

    Logger logger = Logger.getLogger((OciPolicyOptimizerTool.class).toString());

    private final OCICache ociCache;
    // make it default access (only for testing)
    OciPolicyOptimizerTool(OCICache ociCache) {
        this.ociCache = ociCache;
    }


    /**
     * find compartmentId from give CompartmentName
     *
     * @param name Compartment name
     * @return compartmentId
     */
    public String fetchCompartmentNameById(String name) {
        List<Compartment> compartmentList = ociCache.getCompartments();
        for (Compartment compartment : compartmentList) {
            if (compartment.getName().equals(name)) {
                return compartment.getId();
            }
        }
        return "No such compartment exist in Tenancy";
    }

    /**
     * merge exact same policies
     * @param policies from each compartment
     * @return unique set polices
     */
    public List<PolicyStatement> mergeDuplicates(List<PolicyStatement> policies) {
        Map<String, PolicyStatement> mergedMap = new HashMap<>();
        for (PolicyStatement policy : policies) {
            String key = policy.getVerb() + "|" + policy.getResource() + "|" + policy.getCompartment();
            if (mergedMap.containsKey(key)) {
                mergedMap.get(key).getGroups().addAll(policy.getGroups());
            }
            else {
                mergedMap.put(key, new PolicyStatement(
                        new HashSet<>(policy.getGroups()),
                        policy.getVerb(),
                        policy.getResource(),
                        policy.getCompartment()
                ));
            }
        }
        return new ArrayList<>(mergedMap.values());
    }

    /**
     * build parentMap for compartments
     * like {child1 = [parent1, parent2,....], child2 = [parent3, parent2,....]}
     *
     * @param parentMap map of [child, parent]
     * @return parentMap
     */
    private Map<String, Set<String>> buildParentList(Map<String, String> parentMap) {
        Map<String, Set<String>> result = new HashMap<>();
        for (String child : parentMap.keySet()) {
            Set<String> parents = new LinkedHashSet<>();
            String currentParent = parentMap.get(child);

            // climb up the hierarchy till root
            while (currentParent != null) {
                parents.add(currentParent);
                currentParent = parentMap.get(currentParent);
            }
            result.put(child, parents);
        }
        return result;
    }

    /**
     * for each child compartment policy
     *      iterate through its parent compartment policies
     *          remove redundant policy in child compartment if so
     *
     * @param parentPolicies parent compartment policies
     * @param childPolicies child compartment polices
     * @param parentMap of compartments {child1 = [parent1, parent2,....], child2 = [parent3, parent2,....]}
     * @return optimized policies
     */
    public List<PolicyStatement> helper(List<PolicyStatement> parentPolicies,
                               List<PolicyStatement> childPolicies,
                               Map<String, Set<String>> parentMap) {
        Iterator<PolicyStatement> childIterator = childPolicies.iterator();
        while (childIterator.hasNext()) {
            PolicyStatement childPolicy = childIterator.next();
            for (PolicyStatement parentPolicy : parentPolicies) {
                if (parentPolicy == childPolicy) { // for same compartment
                    continue;
                }
                if (!childPolicy.getResource().equals(parentPolicy.getResource())) {
                    continue;
                }
//                todo: confirm if this is possible
                // case 1: same verb, same compartment
//                if (parentPolicy.getVerbLevel() == childPolicy.getVerbLevel()
//                        && parentPolicy.getCompartment().equals(childPolicy.getCompartment())) {
//                    parentPolicy.getGroups().addAll(childPolicy.getGroups());
//                    childIterator.remove();
//                    break;
//                }

                //  parent has higher privilege
                if (parentPolicy.getVerbLevel() >= childPolicy.getVerbLevel()) {
                    String childCompartment = childPolicy.getCompartment();
                    String parentCompartment = parentPolicy.getCompartment();

                    if (parentMap.get(childCompartment) != null) { // if child compartment is root
                        if (parentMap.get(childCompartment).contains(parentCompartment)
                                || childCompartment.equals(parentPolicy.getCompartment())) {
                            logger.info("parentPolicy: " + parentPolicy);
                            logger.info("childPolicy: " + childPolicy);
                            Set<String> commonGroups = new HashSet<>(childPolicy.getGroups());
                            commonGroups.retainAll(parentPolicy.getGroups());
                            logger.info("commonGroups: " + commonGroups);
                            if (!commonGroups.isEmpty()) {
                                Set<String> leftoverGroups = new HashSet<>(childPolicy.getGroups());
                                leftoverGroups.removeAll(commonGroups);
                                if (leftoverGroups.isEmpty()) {
                                    logger.info("Child policy removed : " + childPolicy);
                                    childIterator.remove();
                                    logger.info("childPolicies: " + childPolicies);
                                    break;
                                } else {
                                    childPolicy.setGroups(leftoverGroups);
                                    logger.info("leftoverGroups for child policy " + leftoverGroups);
                                    logger.info("Child Policy after with leftoverGroups:  " + childPolicy);
                                }
                            }
                        }
                    }
                }
            }
        }
        return childPolicies;
    }

    @Tool(
            name = "optimizePoliciesInCompartment",
            description = "optimize policy in particular compartment"
    )
    public List<PolicyStatement> optimizePoliciesInCompartment(@ToolParam String compartmentName) {
        logger.info("Optimizing PolicyStatement In Compartment " + compartmentName);

        // get parentMap
        Map<String, String> parentMap = ociCache.getParentMap();
        Map<String, Set<String>> compartmentParentMap = buildParentList(parentMap);
        logger.info("helper Parent Map: " + compartmentParentMap);

        // comment this  (only for testing)
        String compartmentID =  fetchCompartmentNameById(compartmentName);
        logger.info("Optimizing PolicyStatement In CompartmentID " + compartmentID);
        // change passing attribute to Name (only for testing)
        List<PolicyStatement> policyStatementList = new ArrayList<>(ociCache.getPolicies().get(compartmentID));
        logger.info("Policies in Compartment " + compartmentName + " : \n" + policyStatementList);

        // merge duplicate policies
        List<PolicyStatement> mergedPolicies = mergeDuplicates(policyStatementList);
        logger.info("Policies after duplicate optimization: " + mergedPolicies);

        // remove verb and hierarchy based redundant policies
        List<PolicyStatement> optimizedPolicyList = helper(new ArrayList<>(mergedPolicies), new ArrayList<>(mergedPolicies), compartmentParentMap);
        logger.info("Optimized PolicyStatement List for compartment " + compartmentName + " : "  + optimizedPolicyList);
        return optimizedPolicyList;
    }

    /**
     * returns optimized policies in each compartment
     *
     * @param fetchedPolicies map (compartmentID : List<PolicyStatement>)
     * @param parentMap map (childCompartment : parentCompartment)
     * @return optimized map
     */
    public Map<String, List<PolicyStatement>> optimizeAllCompartmentPolicies
            (Map<String, List<PolicyStatement>> fetchedPolicies,
             Map<String, String> parentMap) {
        Map<String, List<PolicyStatement>> optimizedPolicies = new HashMap<>();
        Map<String, Set<String>> compartmentParentMap = buildParentList(parentMap);

        for (Map.Entry<String, List<PolicyStatement>> entry : fetchedPolicies.entrySet()) {
            String compartment = entry.getKey();
            List<PolicyStatement> policyList = new ArrayList<>(entry.getValue());
            // merge duplicate policies
            List<PolicyStatement> mergedPolicies = mergeDuplicates(policyList);

            // remove verb and hierarchy based redundant policies
            List<PolicyStatement> optimizedPolicyList = helper(new ArrayList<>(mergedPolicies), new ArrayList<>(mergedPolicies), compartmentParentMap);

            optimizedPolicies.put(compartment, optimizedPolicyList);
        }
        return optimizedPolicies;
    }

    @Tool(
            name = "optimizeAllPolices",
            description = "returns optimized policies from complete tenancy"
    )
    public Map<String, List<PolicyStatement>> optimizeAllPolicies () {
        // pull required data
        Map<String, List<PolicyStatement>> policyMap = ociCache.getPolicies();

        Map<String, String> parentMap = ociCache.getParentMap();
        Map<String, Set<String>> compartmentParentMap = buildParentList(parentMap);

        // comment (only for testing purpose)
        List<Compartment> compartmentList = ociCache.getCompartments();
        List<String> compartments = new ArrayList<>();
        for (Compartment compartment : compartmentList) {
            compartments.add(compartment.getId());
        }

        // testing list of compartments
//        List<String> compartments = new ArrayList<>();
//        compartments.add("A");
//        compartments.add("root");
//        compartments.add("B");

        // level one optimization (optimize self compartment policies)
        Map<String, List<PolicyStatement>> optimizedPolicyMap =
                optimizeAllCompartmentPolicies(policyMap, parentMap);
        logger.info("Level-1 Optimized policies from complete tenancy: " + optimizedPolicyMap);

        // map to store final output
        Map<String, List<PolicyStatement>> optimizedPolicies = new HashMap<>();

        for (String compartment : compartments) {
            String currentCompartment = compartment;
            List<PolicyStatement> childPolicies = new ArrayList<>(optimizedPolicyMap.get(compartment));
            // loop till we reach root compartment
            while (parentMap.get(compartment) != null) {
                String parentCompartment = parentMap.get(compartment);
                List<PolicyStatement> parentPolicies = optimizedPolicyMap.get(parentCompartment);
                // fetch optimized policy for childCompartment
                List<PolicyStatement> optimizedPolicyList = helper(new ArrayList<>(parentPolicies), new ArrayList<>(childPolicies), compartmentParentMap);
                childPolicies = new ArrayList<>(optimizedPolicyList);
                compartment = parentCompartment;
            }
            optimizedPolicies.put(currentCompartment, childPolicies);
        }
        logger.info("Optimized policies from complete tenancy: " + optimizedPolicies);
        return optimizedPolicies;
    }
}
