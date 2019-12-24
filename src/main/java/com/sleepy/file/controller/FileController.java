package com.sleepy.file.controller;

import com.alibaba.fastjson.JSONObject;
import com.sleepy.file.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
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

    @GetMapping(value = "/get")
    public Object getDir(HttpServletRequest request, HttpServletResponse response, @RequestParam("dir") String path, @RequestParam(name = "ratio", defaultValue = "1") String ratio) throws IOException {
        String dir = rootDir + path;
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
                        String suffix = f.getName().substring(f.getName().lastIndexOf('.'));
                        item.put("type", suffix);
                        item.put("category", CommonUtil.getCategoryBySuffix(suffix));
                    } else {
                        item.put("type", "default");
                    }
                }
                dirList.add(item);
            }
            result.put("dir", dirList);
            currentPath = path;
        } else {
            String suffix = file.getName().substring(file.getName().lastIndexOf('.'));
            if (CommonUtil.CATEGORY_IMAGE.equals(CommonUtil.getCategoryBySuffix(suffix))) {
                getImageStream(response, dir, ratio);
                return null;
            }
            if (CommonUtil.CATEGORY_AUDIO.equals(CommonUtil.getCategoryBySuffix(suffix))) {
                getAudioStream(request, response, file);
                return null;
            }
            result.put("content", dirList);
        }
        return result;
    }

    private void getAudioStream(HttpServletRequest request, HttpServletResponse response, File file) throws IOException {
        String range = request.getHeader("Range");
        String[] rs = range.split("\\=");
        range = rs[1].split("\\-")[0];
        int start = Integer.parseInt(range);
        long length = file.length();
        response.addHeader("Accept-Ranges", "bytes");
        response.addHeader("Content-Length", length + "");
        response.addHeader("Content-Range", "bytes " + start + "-" + (length - 1) + "/" + length);
        response.addHeader("Content-Type", "audio/mpeg;charset=UTF-8");

        OutputStream os = response.getOutputStream();
        FileInputStream fis = new FileInputStream(file);
        fis.skip(start);
        FileCopyUtils.copy(fis, os);
    }

    private void getImageStream(HttpServletResponse response, String dir, String ratio) {
        OutputStream outputStream = null;
        try {
            response.setContentType("image/jpeg");
            response.addHeader("Connection", "keep-alive");
            response.addHeader("Cache-Control", "max-age=604800");
            outputStream = response.getOutputStream();
            Thumbnails.of(dir).scale(Float.parseFloat(ratio)).outputFormat("jpeg").toOutputStream(outputStream);
        } catch (NumberFormatException e) {
            log.error("图片压缩失败，ratio值应为float类型，如ratio=0.25f(缩小至0.25倍)，失败URL：{}", dir);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("{} 获取图片失败！{} {}", "/compress请求", e.getMessage(), dir);
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                log.error("{} 流关闭失败！{}", "/compress请求", e.getMessage());
            }
        }
    }
}