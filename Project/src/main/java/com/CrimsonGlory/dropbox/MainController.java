package com.CrimsonGlory.dropbox;

import com.CrimsonGlory.dropbox.Database.FileInfo;
import com.CrimsonGlory.dropbox.Database.FileRepository;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.xspec.M;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;


@RestController
@Component
public class MainController {

    private DynamoDBMapper dynamoDBMapper;
    private String awsBucket = "securedrop-ncali";

    @Value("${amazon.dynamodb.endpoint}")
    private String amazonDynamoDBEndpoint;

    @Value("${amazon.aws.accesskey}")
    private String amazonAWSAccessKey;

    @Value("${amazon.aws.secretkey}")
    private String amazonAWSSecretKey;

    @Bean
    public AmazonDynamoDB amazonDynamoDB(){
        AmazonDynamoDB amazonDynamoDB = new AmazonDynamoDBClient(amazonAWSCredentials());

        if(!StringUtils.isNullOrEmpty(amazonDynamoDBEndpoint))
            amazonDynamoDB.setEndpoint(amazonDynamoDBEndpoint);

        return amazonDynamoDB;
    }

    @Bean
    public AWSCredentials amazonAWSCredentials(){
        return new BasicAWSCredentials(amazonAWSAccessKey, amazonAWSSecretKey);
    }


    @Autowired
    private AmazonDynamoDB amazonDynamoDB;

    private AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8080", "us-west-1"))
            .build();
    private DynamoDB dynamoDB = new DynamoDB(client);
    private Table table = dynamoDB.getTable("CGTable");


    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @RequestMapping(value = "/uploadFile", method = RequestMethod.POST, consumes = {"multipart/form-data"})
    @ResponseBody
    public String uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("metadata") MultipartFile metadata, @RequestParam("authtext") MultipartFile authtext){
        FileInfo info = new FileInfo(file, metadata, authtext);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", info.getId());
        attributes.put("file", info.getFile());
        attributes.put("metadata", info.getMetadata());
        attributes.put("authtext", info.getAuthtext());
        PutItemOutcome outcome = table.putItem(new Item().withPrimaryKey("id", info.getId()).withMap("info", attributes));
        //uploadToS3(info);
        return info.getId();
    }

    public String uploadToS3(FileInfo info){
        Region region = Region.US_WEST_1;
        try(S3Client s3 = S3Client.builder()
                .region(region)
                .build()){
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(awsBucket)
                    .key(info.getId())
                    .build();
            PutObjectResponse response = s3.putObject(request, RequestBody.fromBytes(info.getFile().getBytes()));
            return response.eTag();

        } catch (IOException | S3Exception e) {
            System.out.println(e.getMessage());
        }
        return "Error uploading file.";
    }

    @RequestMapping(value = "/downloadMetaData", method = RequestMethod.POST)
    @ResponseBody
    public Object download(@RequestParam("id") String id){
        GetItemSpec spec = new GetItemSpec().withPrimaryKey("id", id);
        try{
            Item outcome = table.getItem(spec);
            return outcome.get("metadata");
        } catch(Exception e) {
            System.err.println("Unable to retrieve metadata.");
        }
        return null;
    }

    @RequestMapping(value = "/downloadS3Link", method = RequestMethod.POST)
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
    }
}
