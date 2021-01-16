package com.CrimsonGlory.dropbox.Database;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@DynamoDBTable(tableName = "FileInfo")
public class FileInfo {
    private String id;
    private MultipartFile file;
    private MultipartFile metadata;
    private MultipartFile authtext;

    public FileInfo(MultipartFile file, MultipartFile metadata, MultipartFile authtext){
        id = UUID.randomUUID().toString();
        this.file = file;
        this.metadata = metadata;
        this.authtext = authtext;
    }

    @DynamoDBHashKey
    public String getId(){
        return id;
    }

    @DynamoDBAttribute
    public MultipartFile getFile(){
        return file;
    }

    @DynamoDBAttribute
    public MultipartFile getMetadata(){
        return metadata;
    }

    @DynamoDBAttribute
    public MultipartFile getAuthtext(){
        return authtext;
    }

}
