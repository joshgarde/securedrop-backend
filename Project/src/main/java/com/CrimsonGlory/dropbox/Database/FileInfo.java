package com.CrimsonGlory.dropbox.Database;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import org.springframework.web.multipart.MultipartFile;

@DynamoDBTable(tableName = "FileInfo")
public class FileInfo {
    private MultipartFile metadata;
    private MultipartFile authtext;


    @DynamoDBAttribute
    public MultipartFile getMetadata(){
        return metadata;
    }

    @DynamoDBAttribute
    public MultipartFile getAuthtext(){
        return authtext;
    }

}
