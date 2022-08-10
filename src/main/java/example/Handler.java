package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.firehose.FirehoseClient;
import software.amazon.awssdk.services.firehose.model.FirehoseException;
import software.amazon.awssdk.services.firehose.model.PutRecordRequest;
import software.amazon.awssdk.services.firehose.model.PutRecordResponse;
import software.amazon.awssdk.services.firehose.model.Record;

// Handler value: example.Handler
public class Handler implements RequestHandler<DynamodbEvent, String>{
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String handleRequest(DynamodbEvent dynamodbEvent, Context context) {
        LambdaLogger logger = context.getLogger();

        String response = "200 OK";

        // log execution details
        logger.log("ENVIRONMENT VARIABLES: " + gson.toJson(System.getenv()));
        logger.log("CONTEXT: " + gson.toJson(context));

        // process event
        logger.log("EVENT: " + gson.toJson(dynamodbEvent.toString()));
        logger.log("EVENT TYPE: " + dynamodbEvent.getClass());

        // Firehose client
        FirehoseClient firehoseClient = FirehoseClient.builder().build();

        logger.log("RECORDS:...");
        for (DynamodbEvent.DynamodbStreamRecord dRecord : dynamodbEvent.getRecords()) {
            try {
                // log record
                logger.log(dRecord.toString());

                // Forward record to Kinesis Firehose
                String deliveryStreamName = "PUT-S3-CwAUa";

                logger.log(String.format("Sending dynamo record with eventId: %s to delivery stream: %s", dRecord.getEventID(), deliveryStreamName));
                SdkBytes sdkBytes = SdkBytes.fromByteArray(dRecord.toString().getBytes());
                Record kRecord = Record.builder()
                        .data(sdkBytes)
                        .build();

                PutRecordRequest kPutRecordRequest = PutRecordRequest.builder()
                        .deliveryStreamName(deliveryStreamName)
                        .record(kRecord)
                        .build();

                PutRecordResponse kRecordPutResponse = firehoseClient.putRecord(kPutRecordRequest);

                logger.log(String.format("Completed sending record with record ID: %s", kRecordPutResponse.recordId()));
            } catch (FirehoseException e) {
                logger.log("Error occurred attempting to put record to kinesis delivery stream.");
                logger.log(e.getLocalizedMessage());

                firehoseClient.close();

                return "500 ERROR";
            }
        }

        firehoseClient.close();

        return response;
    }
}