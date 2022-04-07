package cc.woverflow.onecore.tweaker.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;

public class InternetUtils {
    public static InputStream setupConnection(String url) throws IOException {
        HttpURLConnection connection = ((HttpURLConnection) new URL(url).openConnection());
        connection.setRequestMethod("GET");
        connection.setUseCaches(false);
        connection.addRequestProperty("User-Agent", "OneCoreLoader/1.2.2");
        connection.setReadTimeout(5000);
        connection.setConnectTimeout(5000);
        connection.setDoOutput(true);
        return connection.getInputStream();
    }

    public static String getStringOnline(String url) {
        try (InputStreamReader input = new InputStreamReader(setupConnection(url), Charset.defaultCharset())) {
            StringBuilderWriter builder = new StringBuilderWriter();
            char[] buffer = new char[4096];
            int n;
            while ((n = input.read(buffer)) > 0) {
                builder.write(buffer, 0, n);
            }
            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean download(String url, File file) {
        url = url.replace(" ", "%20");
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            try (BufferedInputStream in = new BufferedInputStream(setupConnection(url))) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = (in.read(buffer, 0, 1024))) > 0) {
                    fileOut.write(buffer, 0, read);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String getChecksumOfFile(String filename) {
        try (FileInputStream inputStream = new FileInputStream(filename)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytesBuffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                digest.update(bytesBuffer, 0, bytesRead);
            }

            return convertByteArrayToHexString(digest.digest());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuilder stringBuffer = new StringBuilder();
        for (byte arrayByte : arrayBytes) {
            stringBuffer.append(Integer.toString((arrayByte & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return stringBuffer.toString();
    }

    /**
     * <code>java.io.StringWriter</code> but un-synchronized.
     * Taken from https://commons.apache.org/proper/commons-io/download_io.cgi under Apache License 2.0.
     */
    private static class StringBuilderWriter extends Writer implements Serializable {

        private final StringBuilder builder = new StringBuilder();

        @Override
        public Writer append(char value) {
            builder.append(value);
            return this;
        }

        @Override
        public Writer append(CharSequence value) {
            builder.append(value);
            return this;
        }

        @Override
        public Writer append(CharSequence value, int start, int end) {
            builder.append(value, start, end);
            return this;
        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }


        @Override
        public void write(String value) {
            builder.append(value);
        }

        @Override
        public void write(char[] value, int offset, int length) {
            builder.append(value, offset, length);
        }

        public StringBuilder getBuilder() {
            return builder;
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }
}
