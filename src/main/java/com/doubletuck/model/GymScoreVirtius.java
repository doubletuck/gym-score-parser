package com.doubletuck.model;

import java.time.LocalDateTime;

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
    private String sessionId;
    private String meetName;
    private LocalDateTime meetDate;
    private boolean isWag = true;
    private ExportStatus exportStatus = ExportStatus.NOT_PROCESSED;
    private String exportFileName;
    private String exportMessage;

    public enum ExportStatus {
        NOT_PROCESSED,
        ERROR,
        EXPORTED
    }
}
