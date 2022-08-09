# DynamoDB TTL Stream to S3

### Create DynamoDB Table
1. Create a table called ```test_ttl_table``` e.g,:
```
+-----------+--------------------------------------+
| my_pk     | 24e05f94-1788-11ed-861d-0242ac120002 |
+-----------+--------------------------------------+
| firstname | Bob                                  |
+-----------+--------------------------------------+
| lastname  | Adam                                 |
+-----------+--------------------------------------+
| expdate   | 1660010100                           |
+-----------+--------------------------------------+
```
2. Enable TTL for the table with a `number` attribute named ```expdate```
3. Enable DynamoDB stream for the table

### Create Lambda Function w/ TTL Filter
```ssh
aws lambda create-event-source-mapping \
--event-source-arn '<dynamodb_stream_ARN>' \
--batch-size 10 \
--enabled \
--function-name test_ddb_ttl \
--starting-position LATEST \
--filter-criteria \"{\"Filters\": [{"Pattern": "{\"userIdentity\":{\"type\":[\"Service\"],\"principalId\":[\"dynamodb.amazonaws.com\"]}}\"}]}\"
```

Change ```<dynamodb_stream_ARN>``` with the generated DynamoDB table stream ARN.

If you are adding the filter through the AWS Trigger Configuration page, use below:
```json
{"userIdentity":{"type":["Service"],"principalId":["dynamodb.amazonaws.com"]}}
```

If you receive an error the function is missing Permissions, attach ```AWSLambdaInvocation-DynamoDB``` policy to the lambda generated role.

### Build Java Project
```mvn package```

### Upload JAR File to Lambda
```ssh
aws lambda update-function-code --function-name test_ddb_ttl --zip-file fileb://<lambda_function.jar>
```

Change ```<lambda_function.jar>``` to point to the generated .jar file in the ```/target``` directory.