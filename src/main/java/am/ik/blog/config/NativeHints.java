package am.ik.blog.config;

import java.lang.reflect.Constructor;

import am.ik.blog.image.Image;
import co.elastic.logging.logback.EcsEncoder;
import jakarta.annotation.Nullable;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(NativeHints.RuntimeHints.class)
public class NativeHints {

	public static class RuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(org.springframework.aot.hint.RuntimeHints hints, @Nullable ClassLoader classLoader) {
			for (Constructor<?> constructor : Image.class.getConstructors()) {
				hints.reflection().registerConstructor(constructor, ExecutableMode.INVOKE);
			}
			try {
				hints.reflection().registerConstructor(EcsEncoder.class.getConstructor(), ExecutableMode.INVOKE);
			}
			catch (NoSuchMethodException e) {
				throw new IllegalStateException(e);
			}
		}

	}

}