package org.opengroup.osdu.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Setup {
    private String tenantId;
    private String kind;
    private String index;
    private String viewerGroup;
    private String ownerGroup;
    private String mappingFile;
    private String recordFile;
    private String schemaFile;
}
