package cc.woverflow.wcore.tweaker;

import gg.essential.loader.stage0.EssentialSetupTweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class WCoreTweaker extends EssentialSetupTweaker {

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        if (this.getClass().getClassLoader().getResource("gg/essential/Essential.class") == null) {
            super.injectIntoClassLoader(classLoader);
            System.out.println("Essential has been loaded.");
        } else {
            System.out.println("Essential has already loaded, not injecting...");
        }
        if (Launch.blackboard.get("requisite") == null) {
            RequisiteLaunchwrapper.inject(classLoader);
            System.out.println("Requisite has been loaded.");
        } else {
            System.out.println("Requisite has already loaded, not injecting...");
        }
    }
}
