package am.ik.blog.image;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.springframework.boot.jackson.JsonComponent;
import org.springframework.http.MediaType;

@JsonComponent
public class MediaTypeJsonSerializer extends JsonSerializer<MediaType> {

	@Override
	public void serialize(MediaType mediaType, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
			throws IOException {
		jsonGenerator.writeString(mediaType.toString());
	}

}
