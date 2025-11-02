package com.springAI.entity;

import com.oracle.bmc.identity.model.Compartment;
import lombok.Data;

@Data
public class CompartmentDTO {
    private String id;
    private String name;
    private String description;
    private String lifecycleState;


    public CompartmentDTO(Compartment c) {
        this.id = c.getId();
        this.name = c.getName();
        this.description = c.getDescription();
        this.lifecycleState = (c.getLifecycleState() != null) ? c.getLifecycleState().toString() : null;
    }
}

