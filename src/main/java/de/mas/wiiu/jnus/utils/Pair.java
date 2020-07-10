package de.mas.wiiu.jnus.utils;

import lombok.Data;

@Data
public class Pair<T1, T2> {
    public final T1 k;
    public final T2 v;
}
