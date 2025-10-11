//package com.springAI;
//
//import com.springAI.entity.PolicyStatement;
//import com.springAI.tools.OciPolicyOptimizerTool;
//import com.springAI.utilities.OCICache;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.*;
//
//@SpringBootTest
//public class OciPolicyOptimizerToolMockTest {
//
//    @Test
//    void testOptimizePoliciesWithMockData() {
//
//        // ====== Step 1: Mock OCICache and Compartment Hierarchy ======
//        OCICache mockCache = new OCICache();
//
//        // parent map (child -> parent)
//        Map<String, String> parentMap = new HashMap<>();
//        parentMap.put("A", "root");
//        parentMap.put("B", "A");
//
//        // ====== Step 2: Mock Policies ======
//
//        // ---- Root compartment policies ----
//        List<PolicyStatement> rootPolicies = List.of(
//                // duplicate exact
//                new PolicyStatement(Set.of("groupAdmins"), "manage", "instance", "root"),
//                new PolicyStatement(Set.of("groupAdmins"), "manage", "instance", "root"),
//
//                // same resource, different group (can merge)
//                new PolicyStatement(Set.of("groupDev"), "manage", "instance", "root"),
//
//                // unique
//                new PolicyStatement(Set.of("groupFinance"), "read", "bucket", "root"),
//                new PolicyStatement(Set.of("groupFinance"), "read", "bucket", "A"),
//                new PolicyStatement(Set.of("groupFinance"), "manage", "bucket", "B")
//        );
//
//        // ---- Compartment A policies ----
//        List<PolicyStatement> compartmentA = List.of(
//                // duplicate exact
//                new PolicyStatement(Set.of("groupDev"), "read", "instance", "A"),
//                new PolicyStatement(Set.of("groupDev"), "read", "instance", "A"),
//
//                // redundant with parent (root has manage instance)
//                new PolicyStatement(Set.of("groupAdmins","groupLinux"), "read", "instance", "A"),
//                new PolicyStatement(Set.of("groupAdmins"), "manage", "instance", "A"),
//
//                // different resource (not redundant)
//                new PolicyStatement(Set.of("groupOps"), "manage", "database", "A")
//        );
//
//        // ---- Compartment B policies ----
//        List<PolicyStatement> compartmentB = List.of(
//                // duplicate exact
//                new PolicyStatement(Set.of("groupOps"), "read", "instance", "B"),
//                new PolicyStatement(Set.of("groupOps"), "read", "instance", "B"),
//
//                // redundant: parent A already has manage database
//                new PolicyStatement(Set.of("groupOps"), "read", "database", "B"),
//
//                // higher privilege redundancy (manage > read)
//                new PolicyStatement(Set.of("groupAdmins"), "manage", "instance", "B"),
//
//                // unique
//                new PolicyStatement(Set.of("groupFinance"), "read", "bucket", "B")
//        );
//
//        // ====== Step 3: Put all policies into map ======
//        mockCache.addPolicies("root", new ArrayList<>(rootPolicies));
//        mockCache.addPolicies("A", new ArrayList<>(compartmentA));
//        mockCache.addPolicies("B", new ArrayList<>(compartmentB));
//
//        // ====== Step 4: Inject into mock cache ======
//        mockCache.setParentMap(parentMap);
//
//        System.out.println("Sample Policies :: " + mockCache.getPolicies());
//        System.out.println("Sample ParentMap :: " + mockCache.getParentMap());
//
//        // ====== Step 5: Run optimizer ======
//        OciPolicyOptimizerTool optimizer = new OciPolicyOptimizerTool(mockCache);
//
//        System.out.println("===== Optimizing Compartment Root =====");
//        var optimizedRoot = optimizer.optimizePoliciesInCompartment("root");
//        optimizedRoot.forEach(System.out::println);
//
//        System.out.println("===== Optimizing Compartment A =====");
//        var optimizedA = optimizer.optimizePoliciesInCompartment("A");
//        optimizedA.forEach(System.out::println);
//
//        System.out.println("\n===== Optimizing Compartment B =====");
//        var optimizedB = optimizer.optimizePoliciesInCompartment("B");
//        optimizedB.forEach(System.out::println);
//
//        System.out.println("\n===== Optimizing All Compartment Policies =====");
//        var allOptimized = optimizer.optimizeAllPolicies();
//        allOptimized.forEach((comp, list) -> {
//            System.out.println("Compartment: " + comp);
//            list.forEach(System.out::println);
//            System.out.println("---------------");
//        });
//    }
//}
