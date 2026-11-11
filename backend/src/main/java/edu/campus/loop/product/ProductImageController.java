package edu.campus.loop.product;

import edu.campus.loop.common.ApiResponse;
import edu.campus.loop.common.BusinessException;
import edu.campus.loop.user.CurrentUserContext;
import edu.campus.loop.user.UserFailure;
import edu.campus.loop.user.UserDtos.UserStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 商品图片持久保存到本地目录，重新编码以避免直接提供原始上传文件。 */
@RestController
@RequestMapping("/api/v1/products/images")
public class ProductImageController {
    private final CurrentUserContext context;
    private final Path directory;

    public ProductImageController(CurrentUserContext context,
            @Value("${PRODUCT_IMAGE_DIR:./data/product-images}") String directory) {
        this.context = context;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    @PostMapping
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        try {
            if (context.requireUser().status() != UserStatus.ACTIVE)
                throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "用户已停用");
        } catch (UserFailure failure) {
            throw new BusinessException(HttpStatus.valueOf(failure.getStatusCode().value()), failure.code(), failure.getReason());
        }
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
            throw invalid("请选择不超过 5MB 的 JPG 或 PNG 图片");
        java.awt.image.BufferedImage source;
        try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid("图片无法读取，请选择 JPG 或 PNG");
            var reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!(format.equalsIgnoreCase("PNG") || format.equalsIgnoreCase("JPEG")))
                    throw invalid("只支持 JPG 或 PNG 图片");
                reader.setInput(input);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels < 1 || pixels > 16000000) throw invalid("图片尺寸不能超过 1600 万像素");
                source = reader.read(0);
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException failure) {
            throw invalid("图片损坏或无法读取");
        }
        Files.createDirectories(directory);
        String name = UUID.randomUUID() + ".png";
        ImageIO.write(source, "PNG", directory.resolve(name).toFile());
        return new ApiResponse<>("OK", "图片已上传", "/api/v1/products/images/" + name, null);
    }

    @GetMapping("/{name}")
    public ResponseEntity<byte[]> image(@PathVariable String name) throws IOException {
        if (!name.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png"))
            return ResponseEntity.notFound().build();
        Path path = directory.resolve(name);
        if (!Files.isRegularFile(path)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .header("X-Content-Type-Options", "nosniff").body(Files.readAllBytes(path));
    }

    private BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
