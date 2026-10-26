package com.futuremedia.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CapturedEmail {

    private String name;
    private String email;
    private String company;
    private String source;
}
