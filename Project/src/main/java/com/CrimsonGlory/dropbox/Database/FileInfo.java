package com.CrimsonGlory.dropbox.Database;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class FileInfo {
    private String id;
    private ByteBuffer file;
    private ByteBuffer metadata;
    private ByteBuffer authtext;
    Map<String, AttributeValue> attributes = new HashMap<>();

    public FileInfo(MultipartFile file, MultipartFile metadata, MultipartFile authtext) throws IOException {
        id = UUID.randomUUID().toString();
        this.file = ByteBuffer.wrap(file.getBytes());
        this.metadata = ByteBuffer.wrap(metadata.getBytes());
        this.authtext = ByteBuffer.wrap(authtext.getBytes());
        
        // Screw AWS type conversions.
        attributes.put("id", AttributeValue.builder().s(id).build());
        attributes.put("metadata", AttributeValue.builder().b(SdkBytes.fromByteBuffer(this.metadata)).build());
        attributes.put("authtext", AttributeValue.builder().b(SdkBytes.fromByteBuffer(this.authtext)).build());
    }

    public String getId(){
        return id;
    }

    public ByteBuffer getFile(){
        return file;
    }

    public ByteBuffer getMetadata(){
        return metadata;
    }

    public ByteBuffer getAuthtext(){
        return authtext;
    }

    public Map<String, AttributeValue> getAttributeMap() {
        return attributes;
    }
}
