Feature: Search recursively on a search_after cursor
  The search_after implementation keeps the initial query alongside the cursor, so a subsequent page
  inherits it and overrides whatever the client resends. A scroll cursor cannot do this - its search
  context is fixed when the scroll is opened - hence these scenarios only run against search_after.

  Background:
    Given the schema is created with the following kind
      | kind                                           | schemaFile |
      | tenant1:search<timestamp>:test-data--Integration:1.0.1    | records_1  |
      | tenant1:search<timestamp>:test-data2--Integration:1.0.2    | records_2  |

  @default
  Scenario Outline: Ingest records for the given kind
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Examples:
      | kind                                          | recordFile  | acl                             |
      | "tenant1:search<timestamp>:test-data--Integration:1.0.1" | "records_1" |  "data.default.viewers@tenant1" |
      | "tenant1:search<timestamp>:test-data2--Integration:1.0.2"  | "records_2" | "data.default.viewers@tenant1"  |

  @default
  Scenario Outline: Subsequent cursor requests can override parameters of the initial request
    When I send <query> with <kind>
    And I limit the count of returned results to <limit>
    And I set the fields I want in response as <returned_fields>
    And I send request to tenant <tenant>
    Then I should get in response <first_count> records along with a cursor
    When I send a subsequent request with the cursor, limit <next_limit> and fields <next_returned_fields>
    And I send request to tenant <tenant>
    Then I should get in response <final_count> records containing only <next_returned_fields>

    Examples:
      | tenant    | kind                            | query | limit | returned_fields | first_count | next_limit | next_returned_fields | final_count |
      | "tenant1" | "tenant1:search<timestamp>:*:*" | None  | 2     | id              | 2           | 1          | kind                 | 1           |
      | "tenant1" | "tenant1:search<timestamp>:*:*" | None  | 4     | id              | 4           | 2          | id,kind              | 2           |
