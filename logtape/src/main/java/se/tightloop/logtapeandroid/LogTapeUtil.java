package se.tightloop.logtapeandroid;

import android.graphics.Bitmap;
import android.util.Base64;
import android.view.View;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by dnils on 14/10/16.
 */

public class LogTapeUtil {
    // We only support headers with unique keys for now. If
    // this turns out to be problematic we will add support
    // for multiple keys later on.
    public static Map<String, String> multiValueMapToSingleValueMap(Map<String, List<String>> multiMap) {
        Map<String, String> ret = new HashMap<String, String>();

        for (Map.Entry<String, List<String>> entry : multiMap.entrySet()) {
            if (entry.getKey() != null && !entry.getValue().isEmpty()) {
                ret.put(entry.getKey(), entry.getValue().get(0));
            }
        }

        return ret;
    }

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

    public static String readStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static Bitmap getScreenShot(View view) {
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }

    public static String encodeImageToBase64(Bitmap image)
    {
        Bitmap immagex=image;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immagex.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);
        return imageEncoded;
    }

    public static String getUTCDateString(Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }
}
