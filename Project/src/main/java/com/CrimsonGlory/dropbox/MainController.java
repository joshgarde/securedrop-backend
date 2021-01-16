package com.CrimsonGlory.dropbox;

import com.CrimsonGlory.dropbox.Database.FileInfo;
import com.CrimsonGlory.dropbox.Database.FileRepository;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MainController {

    private DynamoDBMapper dynamoDBMapper;

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
        FileInfo info = new FileInfo(metadata, authtext);
        repository.save(info);
        return info.getId();
    }
}
