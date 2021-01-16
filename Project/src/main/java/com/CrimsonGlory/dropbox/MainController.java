package com.CrimsonGlory.dropbox;

import com.CrimsonGlory.dropbox.Database.FileInfo;
import com.CrimsonGlory.dropbox.Database.FileRepository;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
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

    @Autowired
    FileRepository repository;

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @RequestMapping(value = "/uploadFile", method = RequestMethod.POST, consumes = {"multipart/form-data"})
    @ResponseBody
    public String uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("metadata") MultipartFile metadata, @RequestParam("authtext") MultipartFile authtext){
        FileInfo info = new FileInfo(file, metadata, authtext);
        repository.save(info);
        uploadToS3(info);
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

    public String download(){
        return null;
    }
}
