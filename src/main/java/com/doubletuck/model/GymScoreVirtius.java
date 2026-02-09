package com.doubletuck.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class GymScoreVirtius {

    private String scoreUrl;
    private String meetName;
    private String meetDate;
    private boolean isWag = true;

}
