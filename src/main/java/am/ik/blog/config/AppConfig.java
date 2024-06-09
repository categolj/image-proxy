package am.ik.blog.config;

import java.time.Duration;

import am.ik.spring.http.client.RetryableClientHttpRequestInterceptor;
import io.micrometer.core.instrument.config.MeterFilter;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSampler;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration(proxyBeanMethods = false)
public class AppConfig {

	@Bean
	public RestClientCustomizer restClientCustomizer(
			LogbookClientHttpRequestInterceptor logbookClientHttpRequestInterceptor) {
		return builder -> builder
			.requestFactory(ClientHttpRequestFactories.get(SimpleClientHttpRequestFactory::new,
					ClientHttpRequestFactorySettings.DEFAULTS.withReadTimeout(Duration.ofSeconds(3))))
			.requestInterceptor(logbookClientHttpRequestInterceptor)
			.requestInterceptor(new RetryableClientHttpRequestInterceptor(new ExponentialBackOff() {
				{
					setMaxElapsedTime(12_000);
				}
			}));
	}

	@Bean
	public MeterFilter customMeterFilter() {
		return MeterFilter.deny(id -> {
			String uri = id.getTag("uri");
			return uri != null && (uri.startsWith("/readyz") || uri.startsWith("/livez") || uri.startsWith("/actuator")
					|| uri.startsWith("/cloudfoundryapplication"));
		});
	}

	@Bean
	public static BeanPostProcessor ruleBasedRoutingSampler() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (bean instanceof Sampler) {
					AttributeKey<String> uri = AttributeKey.stringKey("uri");
					return RuleBasedRoutingSampler.builder(SpanKind.SERVER, (Sampler) bean)
						.drop(uri, "^/readyz")
						.drop(uri, "^/livez")
						.drop(uri, "^/actuator")
						.drop(uri, "^/cloudfoundryapplication")
						.build();
				}
				return bean;
			}
		};
	}

}
