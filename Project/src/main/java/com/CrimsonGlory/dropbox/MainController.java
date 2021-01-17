package com.CrimsonGlory.dropbox;

import com.CrimsonGlory.dropbox.Database.FileInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;


@RestController
@Component
public class MainController {
    private final static String BUCKET_NAME = "securedrop-ncali";
    private final static String TABLE_NAME = "FileInfo";

    private final AwsCredentialsProvider awsCredentialsProvider;
    private final DynamoDbClient ddClient;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    public MainController(@Value("${amazon.aws.accesskey}") String accessKey, @Value("${amazon.aws.secretkey}") String secretKey) {
        AwsCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
        awsCredentialsProvider = StaticCredentialsProvider.create(awsCredentials);

        ddClient = DynamoDbClient.builder()
            .region(Region.US_WEST_1)
            .credentialsProvider(awsCredentialsProvider)
            .build();

        s3Client = S3Client.builder()
            .region(Region.US_WEST_1)
            .credentialsProvider(awsCredentialsProvider)
            .build();

        s3Presigner = S3Presigner.builder()
            .region(Region.US_WEST_1)
            .credentialsProvider(awsCredentialsProvider)
            .build();
    }


    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST, consumes = {"multipart/form-data"})
    @CrossOrigin(origins = "*")
    @ResponseBody
    public ObjectNode uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("metadata") MultipartFile metadata, @RequestParam("authtext") MultipartFile authtext){
        ObjectNode response = mapper.createObjectNode();

        try {
            FileInfo info = new FileInfo(file, metadata, authtext);
            shoveIntoDb(info);
            uploadToS3(info);

            response.put("success", true);
            response.put("message", "");
            response.put("id", info.getId());
        } catch (IOException | S3Exception e) {
            response.put("success", false);
            response.put("message", "Unknown exception occured. Contact support.");
        }

        return response;
    }

    private void shoveIntoDb(FileInfo info) {
        PutItemRequest request = PutItemRequest.builder()
                .item(info.getAttributeMap())
                .tableName(TABLE_NAME)
                .build();

        ddClient.putItem(request);
    }

    private void uploadToS3(FileInfo info) {
        PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(info.getId())
                    .build();
        s3Client.putObject(request, RequestBody.fromByteBuffer(info.getFile()));
    }

    @RequestMapping(value = "/metadata/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ObjectNode download(@PathVariable("id") String id){
        Map<String, AttributeValue> attributeMap = new HashMap<>();
        attributeMap.put("id", AttributeValue.builder().s(id).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .attributesToGet("metadata")
                .key(attributeMap)
                .build();

        Map<String, AttributeValue> result = ddClient.getItem(request).item();

        ObjectNode response = mapper.createObjectNode();
        if (result == null) {
            response.put("success", false);
            response.put("message", "ID not found");
        } else {
            byte[] metadata = result.get("metadata").b().asByteArray();
            String base64Meta = Base64.getEncoder().encodeToString(metadata);

            response.put("success", true);
            response.put("message", "");
            response.put("metadata", base64Meta);
        }

        return response;
    }

    /*@RequestMapping(value = "/downloadS3Link", method = RequestMethod.POST)
    public URL downloadLink(@RequestParam("id") String id, @RequestParam("authtext") String authtext){
        GetItemSpec spec = new GetItemSpec().withPrimaryKey("id", id);
        URL url = null;

        try {
            AmazonS3 s3client = AmazonS3ClientBuilder.standard()
                    .withRegion(String.valueOf(Region.US_WEST_1))
                    .withCredentials(new ProfileCredentialsProvider())
                    .build();

            Date expiration = new Date();
            long expTimeMil = expiration.getTime();
            expTimeMil += 1000 * 60 * 60;
            expiration.setTime(expTimeMil);

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest("securedrop-ncali", id)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);
            url = s3client.generatePresignedUrl(request);
        } catch (AmazonServiceException e){
            System.err.println("Link unable to be generated.");
        }

        try{
            Item outcome = table.getItem(spec);
            if(outcome.get("authtext") == authtext)
                return url;
        }catch (Exception e){
            System.err.println("Unable to find file.");
        }
        return null;
    }*/
}
