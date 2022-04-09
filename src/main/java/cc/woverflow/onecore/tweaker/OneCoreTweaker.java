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

public class OneCoreTweaker extends EssentialSetupTweaker {

    private ITweaker loader = null;

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
                        "For more information, please join our discord server (https://woverflow.cc/discord) and go to #support-bug-report\n" +
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

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        super.injectIntoClassLoader(classLoader);
        try {
            if (Launch.blackboard.get("onecore") == null) {
                File loadLocation = new File(new File(new File(Launch.minecraftHome, "W-OVERFLOW"), "OneCore"), "OneCore-Loader.jar");
                JsonObject json = null;
                try {
                    if (!loadLocation.getParentFile().exists()) loadLocation.getParentFile().mkdirs();
                    String theJson = InternetUtils.getStringOnline("https://woverflow.cc/static/data/onecore-all.json");
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
                    showErrorScreen();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorScreen();
        }
        if (loader != null) {
            loader.injectIntoClassLoader(classLoader);
        }
    }
}
