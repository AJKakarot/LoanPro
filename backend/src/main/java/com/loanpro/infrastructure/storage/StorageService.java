package com.loanpro.infrastructure.storage;

import java.io.InputStream;

public interface StorageService {
    void store(String key, InputStream content, long size, String contentType);
    byte[] load(String key);
    void delete(String key);
}
