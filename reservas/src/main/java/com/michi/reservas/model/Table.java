package com.michi.reservas.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(value= "tables")
public class Table {
    @Id
    private String id;

    private int capacity;
    private Location location;
}
