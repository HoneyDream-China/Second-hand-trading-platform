package edu.campus.loop.user;

import edu.campus.loop.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 头像只保存重新编码后的圆形 PNG，不信任文件名或浏览器提供的 MIME。 */
@RestController
@RequestMapping("/api/v1/users")
public class UserAvatarController {
    private final UserService service;
    private final Path directory;
    public UserAvatarController(UserService service, @Value("${USER_AVATAR_DIR:./target/user-data/avatars}") String directory) {
        this.service = service; this.directory = Path.of(directory).toAbsolutePath().normalize();
    }
    @PostMapping("/me/avatar")
    public ApiResponse<UserDtos.UserView> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        service.me();
        if (file.isEmpty() || file.getSize() > 1024 * 1024) throw UserFailure.invalid("请选择不超过1MB的 JPG 或 PNG 图片");
        BufferedImage source;
        try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw UserFailure.invalid("图片无法读取，请选择 JPG 或 PNG");
            var reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!(format.equalsIgnoreCase("PNG") || format.equalsIgnoreCase("JPEG"))) throw UserFailure.invalid("只支持 JPG 或 PNG 图片");
                reader.setInput(input);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > 16000000 || pixels < 1) throw UserFailure.invalid("图片尺寸过大，请使用不超过1600万像素的图片");
                source = reader.read(0);
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException error) { throw UserFailure.invalid("图片损坏或无法读取"); }
        BufferedImage result = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setClip(new Ellipse2D.Double(0, 0, 256, 256));
            int side = Math.min(source.getWidth(), source.getHeight());
            int x = (source.getWidth() - side) / 2, y = (source.getHeight() - side) / 2;
            graphics.drawImage(source, 0, 0, 256, 256, x, y, x + side, y + side, null);
        } finally { graphics.dispose(); }
        Files.createDirectories(directory);
        String name = UUID.randomUUID() + ".png";
        Path target = directory.resolve(name);
        ImageIO.write(result, "PNG", target.toFile());
        try {
            var user = service.updateAvatar("/api/v1/users/avatars/" + name);
            return new ApiResponse<>("OK", "头像已更新", user, UserExceptionHandler.trace(request));
        } catch (RuntimeException failure) { Files.deleteIfExists(target); throw failure; }
    }
    @GetMapping("/avatars/{name}")
    public ResponseEntity<byte[]> image(@PathVariable String name) throws IOException {
        if (!name.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png")) return ResponseEntity.notFound().build();
        Path path = directory.resolve(name);
        if (!Files.isRegularFile(path)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options", "nosniff").body(Files.readAllBytes(path));
    }
}
