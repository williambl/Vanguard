package ladysnake.vanguard.client;

import io.github.prospector.modmenu.api.ModMenuApi;
import ladysnake.vanguard.Config;
import ladysnake.vanguard.Vanguard;
import ladysnake.vanguard.VanguardModScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.TranslatableText;

import java.util.function.Function;

public class VanguardModMenuIntegration implements ModMenuApi {
    @Override
    public String getModId() {
        return Vanguard.MODID;
    }

    @Override
    public Function<Screen, ? extends Screen> getConfigScreenFactory() {
        return parent -> {
            // load config
            Config.load();

            return new VanguardConfigScreen(MinecraftClient.getInstance().currentScreen);
        };
    }
}
