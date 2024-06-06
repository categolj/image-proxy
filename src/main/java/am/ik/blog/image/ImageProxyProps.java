package am.ik.blog.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import am.ik.yavi.builder.ValidatorBuilder;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@ConfigurationProperties(prefix = "image-proxy")
public record ImageProxyProps(@DefaultValue("22") int compressionLevel, @DefaultValue("1024") int maxWidth,
		@DefaultValue("/tmp/image-proxy") Path storePath) implements Validator {
	@Override
	public boolean supports(Class<?> clazz) {
		return clazz == ImageProxyProps.class;
	}

	@Override
	public void validate(Object target, Errors errors) {
		Validator.forInstanceOf(ImageProxyProps.class, ValidatorBuilder.<ImageProxyProps>of()
			.constraint(ImageProxyProps::compressionLevel, "compressionLevel",
					c -> c.notNull().greaterThanOrEqual(1).lessThanOrEqual(22))
			.constraint(ImageProxyProps::maxWidth, "maxWidth", c -> c.notNull().greaterThan(0).lessThanOrEqual(4096))
			.constraintOnObject(ImageProxyProps::storePath, "storePath",
					c -> c
						.predicate(Path::isAbsolute, "path.isAbsolute", "The given \"{0}\" must be absolute path ({1})")
						.notNull())
			.constraintOnObject(ImageProxyProps::storePath, "storePath", c -> c.predicate(path -> {
				try {
					Files.createDirectories(path);
					return true;
				}
				catch (IOException e) {
					return false;
				}
			}, "path.cannotCreate", "The given \"{0}\" cannot be created ({1})").notNull())
			.build()
			.toBiConsumer(Errors::rejectValue)).validate(target, errors);
	}
}
