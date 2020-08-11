package de.mas.wiiu.jnus.entities.FST.nodeentry;

import lombok.Data;

@Data
public class Permission {
    private int value;

    public Permission(int value) {
        this.value = value;
    }

}
