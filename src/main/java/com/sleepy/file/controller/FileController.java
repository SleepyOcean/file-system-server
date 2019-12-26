package com.sleepy.file.controller;

import com.sleepy.file.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private static String CHUNK_SUFFIX = ".chunk";
    private ConcurrentHashMap<String, String> uploadFileMap = new ConcurrentHashMap<>(4);

    @GetMapping("/checkExist/{name}")
    public Boolean checkExist(@PathVariable("name") String name) throws IOException {
        String path = rootDir + currentPath + name;
        File uploadFile = new File(path);
        if (uploadFile.exists()) {
            return true;
        } else {
            return false;
        }
    }

    @GetMapping("/merge/{name}")
    public String merge(@PathVariable("name") String name) throws IOException {
        if (CommonUtil.stringIsNotEmpty(uploadFileMap.get(name))) {
            File chunkFileFolder = new File(uploadFileMap.get(name) + CHUNK_SUFFIX);
            File[] files = chunkFileFolder.listFiles();

            File mergeFile = new File(uploadFileMap.get(name));
            List<File> fileList = Arrays.asList(files);
            mergeFile(fileList, mergeFile);
            CommonUtil.delFile(chunkFileFolder);
            uploadFileMap.remove(name);
        } else {
            return "未找到" + name + "文件分片";
        }
        return "success";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile files, @RequestParam(value = "fileMd5", required = false) String fileMd5, @RequestParam(name = "chunk", defaultValue = "-1") Integer chunk) throws IOException {
        String name = files.getOriginalFilename();
        String msg = "200";
        if (chunk < 0) {
            try {
                String path = rootDir + currentPath + name;
                File uploadFile = new File(path);
                files.transferTo(uploadFile);
                msg = "添加成功";
            } catch (Exception e) {
                msg = "添加失败: " + e;
            }
        } else {
            if (!CommonUtil.stringIsNotEmpty(uploadFileMap.get(name))) {
                uploadFileMap.put(name, rootDir + currentPath + name);
            }
            String path = uploadFileMap.get(name);
            File tmpFolder = new File(path + CHUNK_SUFFIX);
            File chunkFile = new File(path + CHUNK_SUFFIX + File.separator + chunk);
            if (!tmpFolder.exists()) {
                tmpFolder.mkdirs();
            }
            if (!chunkFile.exists()) {
                // 上传文件输入流
                InputStream inputStream = null;
                FileOutputStream outputStream = null;
                inputStream = files.getInputStream();
                outputStream = new FileOutputStream(chunkFile);
                IOUtils.copy(inputStream, outputStream);
                inputStream.close();
                outputStream.close();
            }
        }
        return msg;
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
                item.put("size", CommonUtil.getFormatFileSize(f.length()));
                dirList.add(item);
            }
            result.put("dir", dirList);
            currentPath = path + File.separator;
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

    private File mergeFile(List<File> chunkFileList, File mergeFile) {
        try {
            // 有删 无创建
            if (mergeFile.exists()) {
                mergeFile.delete();
            } else {
                mergeFile.createNewFile();
            }
            // 排序
            Collections.sort(chunkFileList, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if (Integer.parseInt(o1.getName()) > Integer.parseInt(o2.getName())) {
                        return 1;
                    }
                    return -1;
                }
            });

            byte[] b = new byte[1024];
            RandomAccessFile writeFile = new RandomAccessFile(mergeFile, "rw");
            for (File chunkFile : chunkFileList) {
                RandomAccessFile readFile = new RandomAccessFile(chunkFile, "r");
                int len = -1;
                while ((len = readFile.read(b)) != -1) {
                    writeFile.write(b, 0, len);
                }
                readFile.close();
            }
            writeFile.close();
            return mergeFile;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}