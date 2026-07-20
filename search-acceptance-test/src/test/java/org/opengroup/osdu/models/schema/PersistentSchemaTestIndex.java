// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.models.schema;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.common.BaseSearchSteps;
import org.opengroup.osdu.core.test.client.SchemaClient;
import org.opengroup.osdu.core.test.client.model.schema.SchemaIdentity;
import org.opengroup.osdu.core.test.client.model.schema.SchemaModel;
import org.opengroup.osdu.models.TestIndex;
import org.opengroup.osdu.core.test.util.TestFileUtil;

import java.io.IOException;
import java.io.UncheckedIOException;

@Slf4j
@Getter
public class PersistentSchemaTestIndex extends TestIndex {

    private final SchemaClient schemaClient;
    private final BaseSearchSteps baseSearchSteps;
    private SchemaModel schemaModel;

    public PersistentSchemaTestIndex(SchemaClient schemaClient, BaseSearchSteps baseSearchSteps) {
        super();
        this.schemaClient = schemaClient;
        this.baseSearchSteps = baseSearchSteps;
    }

    @Override
    public void setupSchema() {
        this.schemaModel = readSchemaFromJson();
        SchemaIdentity schemaIdentity = schemaModel.getSchemaInfo().getSchemaIdentity();
        log.debug("Read the schema={}", schemaIdentity);
        schemaIdentity.setAuthority(baseSearchSteps.generateActualNameWithoutTs(schemaIdentity.getAuthority()));
        schemaIdentity.setSource(baseSearchSteps.generateActualName(schemaIdentity.getSource()));
        log.debug("Updated the schema={}", schemaIdentity);
        schemaClient.createIfNotExist(schemaModel);
        log.debug("Finished setting up the schema={}", schemaIdentity);
    }

    @Override
    public void deleteSchema(String kind) {
        // The DELETE API is not supported in the Schema service.
        // In order not to overwhelm a DB with a lots of test schemas
        // the integration tests create/update a schema per schema file if the schema does not exists
        // If a developer updates the schema manually, the developer is supposed to update its version as well
    }

    private SchemaModel readSchemaFromJson() {
        try {
            return TestFileUtil.readTestDataFile(getSchemaFile(), SchemaModel.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected String getSchemaFile() {
        return super.getSchemaFile() + ".json";
    }
}
