package am.ik.blog.image;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.springframework.boot.jackson.JsonComponent;
import org.springframework.http.MediaType;

@JsonComponent
public class MediaTypeJsonDeserializer extends JsonDeserializer<MediaType> {

	@Override
	public MediaType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
			throws IOException, JacksonException {
		return MediaType.valueOf(jsonParser.getText());
	}

}
