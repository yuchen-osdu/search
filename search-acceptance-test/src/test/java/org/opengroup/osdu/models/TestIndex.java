package org.opengroup.osdu.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TestIndex {
    private String kind;
    private String index;
    private String mappingFile;
    private String recordFile;
    private int recordCount;
    private String schemaFile;
    private String[] dataGroup;
    private String[] viewerGroup;
    private String[] ownerGroup;

    public void setupSchema() {
        throw new UnsupportedOperationException("setupSchema must be implemented by subclass");
    }

    public void deleteSchema(String kind) {
        throw new UnsupportedOperationException("deleteSchema must be implemented by subclass");
    }

    protected String getSchemaFile() {
        return String.format("%s.schema", this.schemaFile);
    }
}
