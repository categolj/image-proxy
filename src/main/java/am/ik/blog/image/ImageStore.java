package am.ik.blog.image;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

@Component
public class ImageStore {

	private final ImageProxyProps proxyProps;

	private final ObjectMapper objectMapper;

	private final Logger logger = LoggerFactory.getLogger(ImageStore.class);

	private final ConcurrentHashMap<String, Image> images = new ConcurrentHashMap<>();

	public ImageStore(ImageProxyProps proxyProps, ObjectMapper objectMapper) {
		this.proxyProps = proxyProps;
		this.objectMapper = objectMapper;
	}

	public void clear(String path) {
		try {
			this.images.remove(path);
			Path base = this.proxyProps.storePath().resolve(path);
			logger.info("op=delete path=\"{}\"", base);
			FileSystemUtils.deleteRecursively(base);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Image readOrStore(String path, Supplier<Image> imageSupplier) {
		return this.images.computeIfAbsent(path, p -> this.compute(p, imageSupplier));
	}

	Image compute(String path, Supplier<Image> imageDownloader) {
		try {
			Path base = this.proxyProps.storePath().resolve(path);
			Files.createDirectories(base);
			Path raw = base.resolve("raw");
			Path gzip = base.resolve("gzip");
			Path metadata = base.resolve("metadata.json");
			Function<Image.Type, Resource> resourceLoader = type -> new FileSystemResource(
					base.resolve(type.name().toLowerCase(Locale.ROOT)));
			if (Files.exists(raw) && Files.exists(metadata) && Files.exists(gzip)) {
				try (InputStream inputStream = Files.newInputStream(metadata)) {
					Image loaded = this.objectMapper.readValue(inputStream, Image.class);
					return ImageBuilder.from(loaded).resourceLoader(resourceLoader).build();
				}
			}
			Image downloaded = imageDownloader.get();
			Image image = ImageBuilder.from(downloaded).body(downloaded.resize(this.proxyProps.maxWidth())).build();
			logger.info("op=store base=\"{}\"", base);
			Files.write(raw, Objects.requireNonNull(image.body()), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
			Files.write(gzip, Objects.requireNonNull(Image.Type.GZIP.compress(image.body())), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
			Files.write(metadata, this.objectMapper.writeValueAsBytes(image), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
			return ImageBuilder.from(image).resourceLoader(resourceLoader).build();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
