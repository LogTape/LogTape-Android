package se.tightloop.logtapeandroid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  This class contains util methods that can be useful for implementing
 *  network library integrations to LogTape.
 */
public class LogTapeUtil {
    /**
     * Some network libraries use a map with a list of strings as the value for
     * representing headers. We currently only support headers with unique keys
     * for now, this utility function converts such a map to a LogTape compatible
     * map. It will only use the first value of the list.
     *
     * If this turns out to be problematic we will add support for such header
     * map representations in the future.
     *
     * @param multiMap A map with (potentially) multiple values for a header key
     * @return A map with single values for each key.
     */

    public static Map<String, String> multiValueMapToSingleValueMap(Map<String, List<String>> multiMap) {
        Map<String, String> ret = new HashMap<String, String>();

        for (Map.Entry<String, List<String>> entry : multiMap.entrySet()) {
            if (entry.getKey() != null && !entry.getValue().isEmpty()) {
                ret.put(entry.getKey(), entry.getValue().get(0));
            }
        }

        return ret;
    }

    /**
     * Reads bytes from input stream. Similar to IOUtils.toByteArray method.

     * @param is The input stream to read from
     * @return A byte array with the contentse of the input stream.
     * @throws IOException
     */
    public static byte[] getBytesFromInputStream(InputStream is) throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];

        for (int len; (len = is.read(buffer)) != -1;)
            os.write(buffer, 0, len);

        os.flush();

        is.reset();
        return os.toByteArray();
    }
}
