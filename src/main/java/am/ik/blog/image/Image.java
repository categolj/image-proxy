package am.ik.blog.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Function;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

@Builder(style = BuilderStyle.STAGED, toBuilder = "from")
public record Image(@JsonIgnore String path, @Nullable MediaType contentType, @Nullable String eTag, long lastModified,
		@JsonIgnore @Nullable byte[] body, @JsonIgnore @Nullable Function<Type, Resource> resourceLoader) {

	enum Type {

		RAW {
			@Override
			public byte[] compress(byte[] raw) {
				return raw;
			}
		},
		GZIP {
			@Override
			public byte[] compress(byte[] raw) {
				try {
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
							BufferedInputStream bufferedInputStream = new BufferedInputStream(
									new ByteArrayInputStream(raw));) {
						byte[] buffer = new byte[5120];
						int length;
						while ((length = bufferedInputStream.read(buffer)) > 0) {
							gzipOutputStream.write(buffer, 0, length);
						}
					}
					return outputStream.toByteArray();
				}
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		};

		public abstract byte[] compress(byte[] raw);

	}

	public byte[] resize(int maxWidth) throws IOException {
		BufferedImage inputImage = ImageIO.read(new ByteArrayInputStream(Objects.requireNonNull(this.body())));
		int newWidth = Math.min(inputImage.getWidth(), maxWidth);
		int newHeight = inputImage.getWidth() != newWidth ? (newWidth * inputImage.getHeight()) / inputImage.getWidth()
				: inputImage.getHeight();
		java.awt.Image tmp = inputImage.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH);
		BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = resizedImage.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(resizedImage, this.contentType() == null ? "png" : this.contentType().getSubtype(), outputStream);
		return outputStream.toByteArray();
	}
}
