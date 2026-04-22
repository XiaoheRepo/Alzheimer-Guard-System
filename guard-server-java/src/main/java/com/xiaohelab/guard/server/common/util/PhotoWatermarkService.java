package com.xiaohelab.guard.server.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 路人端照片水印服务（HC-07, BR-010）。
 *
 * <p>所有向路人端下发的照片资源，均须在右下角叠加半透明时间戳水印，
 * 格式："yyyy-MM-dd HH:mm 寻人专用"，防止截图被滥用。
 *
 * <p>实现选型：使用 JDK 内置 {@link java.awt.Graphics2D}，无额外第三方依赖，
 * 水印以白色粗体文字叠加在 60% 不透明黑色圆角背景条上，输出 JPEG 格式。
 */
@Service
public class PhotoWatermarkService {

    private static final Logger log = LoggerFactory.getLogger(PhotoWatermarkService.class);

    /** 水印时间戳格式。 */
    private static final DateTimeFormatter WM_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 生成当前时刻的水印文字。
     *
     * @return 例如 "2026-04-22 14:30 寻人专用"
     */
    public String buildWatermarkText() {
        return OffsetDateTime.now().format(WM_FORMATTER) + " 寻人专用";
    }

    /**
     * 对图片字节数组叠加半透明时间戳水印。
     *
     * <p>水印位于图片右下角，由白色 Bold 文字 + 60% 不透明黑色圆角背景条组成。
     * 字号根据图片宽度自适应（≥16px），确保各分辨率下均清晰可读。
     *
     * @param srcBytes 原始图片字节数组（JPEG / PNG，最大 5MB）
     * @param text     水印文字（如 "2026-04-22 14:30 寻人专用"）
     * @return 添加水印后的图片字节数组（JPEG 格式输出，消除透明通道）
     * @throws IOException 图片解码失败（格式不支持或数据损坏）
     */
    public byte[] applyWatermark(byte[] srcBytes, String text) throws IOException {
        // 1. 解码原始图片
        BufferedImage src;
        try (ByteArrayInputStream in = new ByteArrayInputStream(srcBytes)) {
            src = ImageIO.read(in);
        }
        if (src == null) {
            throw new IOException("Cannot decode image: unsupported format or corrupt data");
        }

        // 2. 创建带透明通道的工作画布（TYPE_INT_ARGB 支持 alpha blending）
        BufferedImage canvas = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        // 开启抗锯齿，提升文字可读性
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 3. 将原始图片绘制到画布
        g.drawImage(src, 0, 0, null);

        // 4. 自适应字号（图片宽度 / 25，下限 16px）
        int fontSize = Math.max(16, src.getWidth() / 25);
        Font font = new Font("SansSerif", Font.BOLD, fontSize);
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        int textWidth  = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();
        int padding    = Math.max(6, fontSize / 4);

        // 5. 计算水印背景区域（右下角，外边距 10px）
        int bgW = textWidth  + padding * 2;
        int bgH = textHeight + padding;
        int bgX = src.getWidth()  - bgW - 10;
        int bgY = src.getHeight() - bgH - 10;

        // 6. 绘制 60% 不透明黑色圆角背景（alpha = 153/255 ≈ 60%）
        g.setColor(new Color(0, 0, 0, 153));
        g.fillRoundRect(bgX, bgY, bgW, bgH, 8, 8);

        // 7. 绘制白色水印文字（alpha = 220/255，略带透明柔化边缘）
        g.setColor(new Color(255, 255, 255, 220));
        int textX = bgX + padding;
        int textY = bgY + textHeight - metrics.getDescent() + padding / 2;
        g.drawString(text, textX, textY);
        g.dispose();

        // 8. 转换为 TYPE_INT_RGB（JPEG 不支持透明通道），白色背景兜底填充
        BufferedImage output = new BufferedImage(
                canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = output.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, output.getWidth(), output.getHeight());
        g2.drawImage(canvas, 0, 0, null);
        g2.dispose();

        // 9. 编码为 JPEG 输出（体积更小，路人端加载更快）
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(output, "jpg", out);
            if (out.size() == 0) {
                throw new IOException("ImageIO.write produced empty output for JPEG format");
            }
            log.debug("[PhotoWatermark] 水印叠加成功: src={}bytes, out={}bytes, text={}",
                    srcBytes.length, out.size(), text);
            return out.toByteArray();
        }
    }
}
