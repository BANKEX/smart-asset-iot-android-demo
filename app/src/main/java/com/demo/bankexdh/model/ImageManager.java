package com.demo.bankexdh.model;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

import timber.log.Timber;

public class ImageManager {
    private static final String storageConnectionString = "DefaultEndpointsProtocol=https;"
            + "AccountName=devicehiveiotdemo;"
            + "AccountKey=V4JIH4fX6ypbHcStKsugRzPQ2nBK42JSizWMZp2yjj6j0vlzUfdo7MIgHdfPDtqeWz2QH9sBvd+SNhtxIGNy0w==";
    private static final String CONTAINER_NAME = "images";
    private static final String SEPARATOR = "/";

    private static class InstanceHolder {
        static final ImageManager INSTANCE = new ImageManager();
    }

    public static ImageManager getInstance() {
        return InstanceHolder.INSTANCE;
    }


    private CloudBlobContainer getContainer() throws Exception {
        // Retrieve storage account from connection-string.

        CloudStorageAccount storageAccount = CloudStorageAccount
                .parse(storageConnectionString);

        // Create the blob client.
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        Timber.d(blobClient.getEndpoint().toString());
        // Get a reference to a container.
        // The container name must be lower case
        CloudBlobContainer container = blobClient.getContainerReference(CONTAINER_NAME);

        return container;
    }

    public String uploadImage(String imageName, InputStream image, int imageLength) throws Exception {
        CloudBlobContainer container = getContainer();
        container.createIfNotExists();
        CloudBlockBlob imageBlob = container.getBlockBlobReference(imageName);
        imageBlob.upload(image, imageLength);

        return getLink(imageName, container);

    }

    private String getLink(String imageName, CloudBlobContainer container) {
        String endpoint = container.getServiceClient().getEndpoint().toString();
        return endpoint +
                SEPARATOR +
                CONTAINER_NAME +
                SEPARATOR +
                imageName;
    }

    public String[] listImages() throws Exception {
        CloudBlobContainer container = getContainer();

        Iterable<ListBlobItem> blobs = container.listBlobs();

        LinkedList<String> blobNames = new LinkedList<>();
        for (ListBlobItem blob : blobs) {
            blobNames.add(((CloudBlockBlob) blob).getName());
        }

        return blobNames.toArray(new String[blobNames.size()]);
    }

    public void getImage(String name, OutputStream imageStream, long imageLength) throws Exception {
        CloudBlobContainer container = getContainer();

        CloudBlockBlob blob = container.getBlockBlobReference(name);

        if (blob.exists()) {
            blob.downloadAttributes();

            imageLength = blob.getProperties().getLength();

            blob.download(imageStream);
        }
    }

}
