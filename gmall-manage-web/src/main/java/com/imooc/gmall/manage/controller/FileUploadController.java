package com.imooc.gmall.manage.controller;

import com.imooc.gmall.manage.config.FileServerConfig;
import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin
public class FileUploadController {

    @Autowired
    private FileServerConfig fileServerConfig;
    @RequestMapping("fileUpload")
    public String fileUpload(MultipartFile file)throws IOException, MyException {

        String imgUrl = fileServerConfig.getUrl();
        // 当文件不为空的时候，进行上传！
        if (file!=null){
            String configFile  = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(configFile);
            TrackerClient trackerClient=new TrackerClient();
            // 获取连接
            TrackerServer trackerServer=trackerClient.getTrackerServer();
            StorageClient storageClient=new StorageClient(trackerServer,null);
            // 获取上传文件名称
            String originalFilename = file.getOriginalFilename();
            // 获取文件的后缀名
            String extName  = StringUtils.substringAfterLast(originalFilename, ".");
            // String orginalFilename="d://img//zly.jpg";

            // String[] upload_file = storageClient.upload_file(originalFilename, extName, null); 获取本地文件
            // 上传图片
            String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);
            for (int i = 0; i < upload_file.length; i++) {
                String path = upload_file[i];
                /*
                s = group1
                s = M00/00/00/wKhD2106tuSAY9S9AACGx2c4tJ4084.jpg
                 */
//                imgUrl=http://192.168.67.219/group1/M00/00/00/wKhD2106tuSAY9S9AACGx2c4tJ4084.jpg
                imgUrl+="/"+path;
            }
        }

        return imgUrl;
    }

}
