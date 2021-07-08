Feature: Split Parquet File into JSON rows

  Scenario: Split parquet files
    Given we listen on the output topic
    And we publish the following messages as a parquet file to the input topic
      | asset_id | measure       | timestamp | value |
      | 11       | "temperature" | 13        | 4.1   |
      | 12       | "load"        | 14        | 2.3   |
    Then we expect to find the following json messages in the output topic
      | value                                                                     |
      | {"asset_id": 11, "measure": "temperature", "timestamp": 13, "value": 4.1} |
      | {"asset_id": 12, "measure": "load", "timestamp": 14, "value": 2.3}        |
