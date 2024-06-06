package am.ik.blog.image;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class ImageProxyController {

	private final RestClient restClient;

	private final ImageStore imageStore;

	private final ImageProxyProps proxyProps;

	public ImageProxyController(RestClient.Builder restClientBuilder, ImageStore imageStore,
			ImageProxyProps proxyProps) {
		this.restClient = restClientBuilder.build();
		this.imageStore = imageStore;
		this.proxyProps = proxyProps;
	}

	@GetMapping(path = "/{owner}/{repo}/assets/{userId}/{imageId}")
	public ResponseEntity<?> proxy(@PathVariable String owner, @PathVariable String repo, @PathVariable String userId,
			@PathVariable String imageId,
			@RequestHeader(name = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedSince,
			@RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch,
			@RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) List<String> acceptEncodings)
			throws IOException {
		String path = "%s/%s/assets/%s/%s".formatted(owner, repo, userId, imageId);
		Image image = this.imageStore.readOrStore(path, () -> {
			var downloaded = this.restClient.get()
				.uri("https://github.com/{owner}/{repo}/assets/{userId}/{imageId}", owner, repo, userId, imageId)
				.retrieve()
				.toEntity(byte[].class);
			HttpHeaders responseHeaders = downloaded.getHeaders();
			return ImageBuilder.image()
				.path(path)
				.contentType(responseHeaders.getContentType())
				.eTag(responseHeaders.getETag())
				.lastModified(responseHeaders.getLastModified())
				.body(downloaded.getBody())
				.build();
		});
		ResponseEntity.BodyBuilder builder = ResponseEntity.ok().headers(httpHeaders -> {
			httpHeaders.setETag(image.eTag());
			httpHeaders.setLastModified(image.lastModified());
			httpHeaders.setContentType(image.contentType());
			httpHeaders.setCacheControl(CacheControl.maxAge(Duration.ofDays(1)));
		});
		if (acceptEncodings != null) {
			if (acceptEncodings.contains("gzip")) {
				return builder.header(HttpHeaders.CONTENT_ENCODING, "gzip")
					.body(Objects.requireNonNull(image.resourceLoader()).apply(Image.Type.GZIP));
			}
		}
		return builder.body(Objects.requireNonNull(image.resourceLoader()).apply(Image.Type.RAW));
	}

	@DeleteMapping(path = "/{owner}/{repo}/assets/{userId}/{imageId}")
	public ResponseEntity<?> delete(@PathVariable String owner, @PathVariable String repo, @PathVariable String userId,
			@PathVariable String imageId) {
		String path = "%s/%s/assets/%s/%s".formatted(owner, repo, userId, imageId);
		this.imageStore.clear(path);
		return ResponseEntity.noContent().build();
	}

}
