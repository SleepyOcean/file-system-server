package com.sleepy.file.controller;

import com.alibaba.fastjson.JSONObject;
import com.sleepy.file.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 *
 * 文件服务Controller
 *
 * @author gehoubao
 * @create 2019-12-20 15:29
 **/
@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/resource/file")
public class FileController {
    private String rootDir = CommonUtil.getRootDir();
    private String currentPath = "FileServer";

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile files, HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject json = new JSONObject();
        response.setCharacterEncoding("utf-8");
        String msg = "添加成功";
        try {
            String name = files.getOriginalFilename();
            String path = rootDir + currentPath + name;
            File uploadFile = new File(path);
            files.transferTo(uploadFile);
        } catch (Exception e) {
            msg = "添加失败: " + e;
        }
        json.put("msg", msg);
        return CommonUtil.buildJsonOfJsonObject(json);
    }

    @PostMapping(value = "/get")
    public Map<String, Object> getDir(@RequestBody Map<String, Object> params) {
        String dir = rootDir + params.get("dir");
        Map<String, Object> result = new HashMap<>(4);
        List<Map> dirList = new ArrayList<>();
        File file = new File(dir);
        if (file.isDirectory()) {
            String[] list = file.list();
            for (String s : list) {
                File f = new File(dir + File.separator + s);
                Map<String, Object> item = new HashMap<>(2);
                item.put("name", f.getName());
                if (f.isDirectory()) {
                    item.put("type", "dir");
                } else {
                    if (f.getName().lastIndexOf('.') > -1) {
                        item.put("type", f.getName().substring(f.getName().lastIndexOf('.')));
                    } else {
                        item.put("type", "default");
                    }
                }
                dirList.add(item);
            }
            result.put("dir", dirList);
            currentPath = params.get("dir").toString();
        } else {
            // TODO 下载该文件，或返回文件内容
            result.put("content", dirList);
        }
        return result;
    }
}