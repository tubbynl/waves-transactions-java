package im.mak.waves.transactions.serializers.json.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import im.mak.waves.transactions.common.Alias;

import java.io.IOException;

public class AliasSer extends JsonSerializer<Alias> {

    @Override
    public void serialize(Alias alias, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(alias.toString());
    }

}
