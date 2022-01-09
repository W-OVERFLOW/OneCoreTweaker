package cc.woverflow.wcore.tweaker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.util.List;
import java.util.function.Supplier;

public class ActualWCoreTweaker implements ITweaker {

    private final HttpClientBuilder builder =
            HttpClients.custom().setUserAgent("WCore/1.1.0")
                    .addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
                        if (!request.containsHeader("Pragma")) request.addHeader("Pragma", "no-cache");
                        if (!request.containsHeader("Cache-Control")) request.addHeader("Cache-Control", "no-cache");
                    });

    private static void showErrorScreen() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JOptionPane.showMessageDialog(
                null,
                "W-CORE has failed to download!\n" +
                        "This may be because the servers are down.\n" +
                        "For more information, please join our discord server: https://woverflow.cc/discord\n" +
                        "or try again later.",
                "W-CORE has failed!", JOptionPane.ERROR_MESSAGE
        );
        try {
            Method exit = Class.forName("java.lang.Shutdown").getDeclaredMethod("exit", int.class);
            exit.setAccessible(true);
            exit.invoke(null, 1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
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

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        File loadLocation = new File(new File(new File(Launch.minecraftHome, "W-OVERFLOW"), "W-CORE"), "W-CORE-LOADER.jar");
        try {
            if (!loadLocation.getParentFile().exists()) loadLocation.getParentFile().mkdirs();
            Supplier<String> supplier = () -> {
                try (CloseableHttpClient client = builder.build()) {
                    HttpGet request = new HttpGet(new URL("https://woverflow.cc/static/data/core.json").toURI());
                    request.setProtocolVersion(HttpVersion.HTTP_1_1);
                    HttpResponse response = client.execute(request);
                    HttpEntity entity = response.getEntity();
                    if (response.getStatusLine().getStatusCode() == 200) {
                        return EntityUtils.toString(entity);
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
                if (!loadLocation.exists()) {
                    showErrorScreen();
                }
                return "";
            };
            JsonObject json = new JsonParser().parse(supplier.get()).getAsJsonObject();
            if (json.has("loader")) {
                if (!loadLocation.exists() || !getChecksumOfFile(loadLocation.getPath()).equals(json.get("checksum_loader").getAsString())) {
                    System.out.println("Downloading / updating W-CORE updater...");
                    FileUtils.copyURLToFile(
                            new URL(json.get("loader").getAsString()),
                            loadLocation,
                            5000,
                            5000);
                }
            } else {
                // oh
                if (!loadLocation.exists()) {
                    showErrorScreen();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            if (!loadLocation.exists()) {
                showErrorScreen();
            }
        }

        try {
            URL fileURL = loadLocation.toURI().toURL();
            classLoader.addURL(fileURL);
            classLoader.addClassLoaderExclusion("cc.woverflow.wcore.loader.");
            try {
                ClassLoader classClassLoader = classLoader.getClass().getClassLoader();
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(classClassLoader, fileURL);
            } catch (Throwable e) {
                e.printStackTrace();
                System.out.println("W-CORE was unable to add the W-CORE loader to classpath, we may have some issues.");
            }
            loader = ((ITweaker) classLoader.findClass("cc.woverflow.wcore.loader.WCoreLoader").newInstance());
        } catch (Throwable e) {
            e.printStackTrace();
            showErrorScreen();
        }
    }
}
