package com.fitness.ai.controller;

import com.fitness.ai.common.Result;
import com.fitness.ai.entity.User;
import com.fitness.ai.mapper.UserMapper;
import com.fitness.ai.util.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/** 个人资料：当前登录用户的头像上传等 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    /** 头像存储目录，见 app.avatars.dir */
    @Value("${app.avatars.dir:./avatars}")
    private String avatarDir;

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 上传头像（multipart 表单字段 file）。
     * 前端先用 cropperjs 裁剪为方形 JPG/PNG 再上传；返回可匿名访问的 /api/avatars/** 地址。
     */
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("仅支持 jpg / png / webp 图片");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("图片不能超过 5MB");
        }

        Long userId = UserContext.getUserId();
        Path dir = Path.of(avatarDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        // 同名覆盖：一个用户只有一张头像
        String fileName = userId + "." + ext;
        Path target = dir.resolve(fileName);
        Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        String url = "/api/avatars/" + fileName;
        User update = new User();
        update.setId(userId);
        update.setAvatar(url);
        userMapper.updateById(update);
        return Result.ok(Map.of("url", url));
    }
}
