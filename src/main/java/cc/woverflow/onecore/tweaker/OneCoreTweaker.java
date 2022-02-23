package cc.woverflow.onecore.tweaker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
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
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;
import java.util.function.Supplier;

public class OneCoreTweaker implements ITweaker {

    private ITweaker loader = null;

    private final HttpClientBuilder builder =
            HttpClients.custom().setUserAgent("OneCore/1.1.1")
            .addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
                if (!request.containsHeader("Pragma")) request.addHeader("Pragma", "no-cache");
                if (!request.containsHeader("Cache-Control")) request.addHeader("Cache-Control", "no-cache");
            });

    public OneCoreTweaker() {
        super();
        if (Launch.blackboard.get("onecore") == null) {
            File loadLocation = new File(new File(new File(Launch.minecraftHome, "W-OVERFLOW"), "OneCore"), "OneCore-Loader.jar");
            JsonObject json = null;
            try {
                if (!loadLocation.getParentFile().exists()) loadLocation.getParentFile().mkdirs();
                Supplier<String> supplier = () -> {
                    try (CloseableHttpClient client = builder.build()) {
                        HttpGet request = new HttpGet(new URL("https://woverflow.cc/static/data/onecore.json").toURI());
                        request.setProtocolVersion(HttpVersion.HTTP_1_1);
                        HttpResponse response = client.execute(request);
                        HttpEntity entity = response.getEntity();
                        if (response.getStatusLine().getStatusCode() == 200) {
                            return EntityUtils.toString(entity);
                        }
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                    return "ERROR";
                };
                String theJson = supplier.get();
                if (theJson.equals("ERROR")) {
                    if (!loadLocation.exists()) {
                        showErrorScreen();
                    } else {
                        URL fileURL = loadLocation.toURI().toURL();
                        if (!Launch.classLoader.getSources().contains(fileURL)) {
                            Launch.classLoader.addURL(fileURL);
                        }
                        loader = ((ITweaker) Launch.classLoader.findClass("cc.woverflow.onecore.loader.OneCoreLoader").newInstance());
                    }
                    return;
                }
                json = new JsonParser().parse(theJson).getAsJsonObject();
                if (json.has("loader")) {
                    if (!loadLocation.exists() || !getChecksumOfFile(loadLocation.getPath()).equals(json.get("checksum_loader").getAsString())) {
                        System.out.println("Downloading / updating OneCore loader...");
                        if (!download(json.get("loader").getAsString(), loadLocation)) {
                            if (!loadLocation.exists()) {
                                showErrorScreen();
                            }
                        }
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

            if (json != null) {
                try {
                    URL fileURL = loadLocation.toURI().toURL();
                    if (!Launch.classLoader.getSources().contains(fileURL)) {
                        Launch.classLoader.addURL(fileURL);
                    }
                    loader = ((ITweaker) Launch.classLoader.findClass(json.getAsJsonObject("classpath").get("loader").getAsString()).newInstance());
                } catch (Throwable e) {
                    e.printStackTrace();
                    showErrorScreen();
                }
            }
        }
    }

    private static void showErrorScreen() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JOptionPane.showMessageDialog(
                null,
                "OneCore has failed to download!\n" +
                        "This may be because the servers are down.\n" +
                        "For more information, please join our discord server: https://woverflow.cc/discord\n" +
                        "or try again later.",
                "OneCore has failed!", JOptionPane.ERROR_MESSAGE
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

    private boolean download(String url, File file) {
        url = url.replace(" ", "%20");
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            HttpResponse downloadResponse = builder.build().execute(new HttpGet(url));
            byte[] buffer = new byte[1024];

            int read;
            while ((read = downloadResponse.getEntity().getContent().read(buffer)) > 0) {
                fileOut.write(buffer, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        if (loader != null) {
            loader.acceptOptions(args, gameDir, assetsDir, profile);
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        if (loader != null) {
            loader.injectIntoClassLoader(classLoader);
        }
    }

    @Override
    public String getLaunchTarget() {
        return loader == null ? "net.minecraft.client.main.Main" : loader.getLaunchTarget();
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[]{};
    }
}
