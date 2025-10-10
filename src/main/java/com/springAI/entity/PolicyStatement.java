package com.springAI.entity;

import com.springAI.utilities.VerbLevel;
import org.springframework.stereotype.Component;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Component
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PolicyStatement {
    // allow group <GROUPS> to <VERB> <RESOURCE> in compartment <COMPARTMENT>
    private Set<String> groups;
    private String verb;
    private String resource;
    private String compartment;

    public int getVerbLevel() {
        return VerbLevel.fromString(verb).getLevel();
    }

    @Override
    public String toString() {
        return "Policy{" +
                "groups=" + groups +
                ", verb='" + verb + '\'' +
                ", resource='" + resource + '\'' +
                ", compartment='" + compartment + '\'' +
                '}';
    }
}
