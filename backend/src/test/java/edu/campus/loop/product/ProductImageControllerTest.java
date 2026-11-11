package edu.campus.loop.product;

import edu.campus.loop.common.ApiExceptionHandler;
import edu.campus.loop.user.CurrentUserContext;
import edu.campus.loop.user.UserDtos.ActiveUser;
import edu.campus.loop.user.UserDtos.UserStatus;
import edu.campus.loop.user.UserFailure;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductImageControllerTest {
    @TempDir Path directory;
    private final CurrentUserContext user = () -> new ActiveUser(10, "卖家", UserStatus.ACTIVE);

    @Test void uploadedImageSurvivesControllerRestartAndKeepsDimensions() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(64, 32, BufferedImage.TYPE_INT_RGB), "PNG", output);
        var mvc = MockMvcBuilders.standaloneSetup(new ProductImageController(user, directory.toString()))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        String response = mvc.perform(multipart("/api/v1/products/images").file(
                new MockMultipartFile("file", "photo.png", "image/png", output.toByteArray())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String url = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("data").asText();
        var restarted = MockMvcBuilders.standaloneSetup(new ProductImageController(user, directory.toString())).build();
        byte[] saved = restarted.perform(get(url)).andExpect(status().isOk())
                .andExpect(content().contentType("image/png")).andReturn().getResponse().getContentAsByteArray();
        var image = ImageIO.read(new java.io.ByteArrayInputStream(saved));
        assertEquals(64, image.getWidth());
        assertEquals(32, image.getHeight());
        restarted.perform(get("/api/v1/products/images/invalid.png")).andExpect(status().isNotFound());
    }

    @Test void rejectsFakeImageOversizedFileAndAnonymousUpload() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ProductImageController(user, directory.toString()))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(multipart("/api/v1/products/images").file(new MockMultipartFile("file", "fake.png", "image/png", "fake".getBytes())))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/products/images").file(new MockMultipartFile("file", new byte[5 * 1024 * 1024 + 1])))
                .andExpect(status().isBadRequest());
        var failure = org.mockito.Mockito.mock(UserFailure.class);
        org.mockito.Mockito.when(failure.getStatusCode()).thenReturn(org.springframework.http.HttpStatus.UNAUTHORIZED);
        org.mockito.Mockito.when(failure.code()).thenReturn("UNAUTHORIZED");
        org.mockito.Mockito.when(failure.getReason()).thenReturn("请先登录");
        var anonymous = MockMvcBuilders.standaloneSetup(new ProductImageController(() -> { throw failure; }, directory.toString()))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        anonymous.perform(multipart("/api/v1/products/images").file(new MockMultipartFile("file", new byte[0])))
                .andExpect(status().isUnauthorized());
    }
}
