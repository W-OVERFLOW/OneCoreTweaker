package cc.woverflow.onecore.tweaker;

import cc.woverflow.onecore.tweaker.utils.InternetUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.essential.loader.stage0.EssentialSetupTweaker;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import io.sentry.Sentry;

public class OneCoreTweaker extends EssentialSetupTweaker {

    private ITweaker loader = null;
    
    public OneCoreTweaker() {
        Sentry.init(options -> {
            options.setDsn("https://4477f7a4c4c8432f9f757b2b1443de72@o1071772.ingest.sentry.io/6256057");
            options.setTracesSampleRate(1.0);
        });
    }

    private static void showErrorScreen() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
            Sentry.captureException(e);
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
            Sentry.captureException(e);
            System.exit(1);
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        super.injectIntoClassLoader(classLoader);
        if (Launch.blackboard.get("onecore") == null) {
            File loadLocation = new File(new File(new File(Launch.minecraftHome, "W-OVERFLOW"), "OneCore"), "OneCore-Loader.jar");
            JsonObject json = null;
            try {
                if (!loadLocation.getParentFile().exists()) loadLocation.getParentFile().mkdirs();
                String theJson = InternetUtils.getStringOnline("https://woverflow.cc/static/data/onecore.json");
                if (theJson == null) {
                    if (!loadLocation.exists()) {
                        showErrorScreen();
                    } else {
                        URL fileURL = loadLocation.toURI().toURL();
                        if (!Launch.classLoader.getSources().contains(fileURL)) {
                            Launch.classLoader.addURL(fileURL);
                        }
                        loader = ((ITweaker) Launch.classLoader.findClass("cc.woverflow.onecore.loader.OneCoreLoader").newInstance());
                    }
                }
                if (theJson != null) {
                    json = new JsonParser().parse(theJson).getAsJsonObject();
                    if (json.has("loader")) {
                        if (!loadLocation.exists() || !InternetUtils.getChecksumOfFile(loadLocation.getPath()).equals(json.get("checksum_loader").getAsString())) {
                            System.out.println("Downloading / updating OneCore loader...");
                            if (!InternetUtils.download(json.get("loader").getAsString(), loadLocation)) {
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
                }
            } catch (Throwable e) {
                e.printStackTrace();
                Sentry.captureException(e);
                if (!loadLocation.exists()) {
                    showErrorScreen();
                }
            }

            try {
                URL fileURL = loadLocation.toURI().toURL();
                if (!Launch.classLoader.getSources().contains(fileURL)) {
                    Launch.classLoader.addURL(fileURL);
                }
                loader = ((ITweaker) Launch.classLoader.findClass(json == null ? "cc.woverflow.onecore.loader.OneCoreLoader" : json.getAsJsonObject("classpath").get("loader").getAsString()).newInstance());
            } catch (Throwable e) {
                e.printStackTrace();
                Sentry.captureException(e);
                showErrorScreen();
            }
        }
        if (loader != null) {
            loader.injectIntoClassLoader(classLoader);
        }
    }
}
