package ladysnake.vanguard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.Level;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VanguardUpdater {
    public static void addCustomUpdater(String modid, String updateUrl) {
        // verify it's not a dev environment
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            Vanguard.logger.info("Vanguard is looking for updates for " + modid);

            String minecraftVersion = MinecraftClient.getInstance().getGame().getVersion().getName();
            String modVersion = FabricLoader.getInstance().getModContainer(modid).get().getMetadata().getVersion().getFriendlyString();
            CompletableFuture.supplyAsync(() -> {
                try (Reader reader = new InputStreamReader(new URL(updateUrl + minecraftVersion).openStream())) {
                    JsonParser jp = new JsonParser();
                    JsonElement jsonElement = jp.parse(reader);
                    return jsonElement.getAsJsonObject();
                } catch (MalformedURLException e) {
                    Vanguard.logger.log(Level.ERROR, "Could not get update information because of malformed URL: " + e.getMessage());
                } catch (IOException e) {
                    Vanguard.logger.log(Level.ERROR, "Could not get update information because of I/O Error: " + e.getMessage());
                }

                return null;
            }).thenAcceptAsync(latestVersionJson -> {
                downloadLatestVersion(latestVersionJson, modVersion, modid);
                Vanguard.UPDATED_MODS.add(modid);
            }, MinecraftClient.getInstance());
        }
    }

    public static void addCurseProxyUpdater(String modid, String cfProjectId) {
        // verify it's not a dev environment
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            Vanguard.logger.info("Vanguard is looking for updates for " + modid);

            String minecraftVersion = MinecraftClient.getInstance().getGame().getVersion().getName();
            String modVersion = FabricLoader.getInstance().getModContainer(modid).get().getMetadata().getVersion().getFriendlyString();
            CompletableFuture.supplyAsync(() -> {
                try (Reader reader = new InputStreamReader(new URL("https://curse.nikky.moe/api/addon/" + cfProjectId + "/files").openStream())) {
                    JsonParser jp = new JsonParser();
                    JsonElement jsonElement = jp.parse(reader);

                    JsonObject latestFile = null;
                    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

                    Iterator<JsonElement> iterator = jsonElement.getAsJsonArray().iterator();
                    while (iterator.hasNext()) {
                        JsonObject fileJson = iterator.next().getAsJsonObject();

                        if (fileJson.get("gameVersion").getAsJsonArray().contains(new JsonPrimitive(minecraftVersion))) {
                            try {
                                if (latestFile == null || sdf.parse(fileJson.get("fileDate").getAsString()).after(sdf.parse(latestFile.get("fileDate").getAsString()))) {
                                    latestFile = fileJson.getAsJsonObject();
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (latestFile != null) {
                        JsonObject ret = new JsonObject();

                        Pattern pattern = Pattern.compile("-(\\d+\\.\\d+(\\.\\d)*)");
                        Matcher matcher = pattern.matcher(latestFile.get("fileName").getAsString());
                        matcher.find();
                        ret.add("version", new JsonPrimitive(matcher.group(1)));
                        ret.add("filename", latestFile.get("fileName"));
                        ret.add("download", latestFile.get("downloadUrl"));

                        return ret;
                    }
                } catch (MalformedURLException e) {
                    Vanguard.logger.log(Level.ERROR, "Could not get update information because of malformed URL: " + e.getMessage());
                } catch (IOException e) {
                    Vanguard.logger.log(Level.ERROR, "Could not get update information because of I/O Error: " + e.getMessage());
                }

                return null;
            }).thenAcceptAsync(latestVersionJson -> {
                downloadLatestVersion(latestVersionJson, modVersion, modid);
                Vanguard.UPDATED_MODS.add(modid);
            }, MinecraftClient.getInstance());
        }
    }

    public static void downloadLatestVersion(JsonObject latestVersionJson, String modVersion, String modid) {
        if (latestVersionJson != null) {
            String latestVersion = latestVersionJson.get("version").getAsString();
            String latestFileName = latestVersionJson.get("filename").getAsString() + ".future";
            // if not the latest version, update toast
            if (!latestVersion.equalsIgnoreCase(modVersion)) {
                Vanguard.logger.log(Level.INFO, "Currently present version of " + modid + " is " + modVersion + " while the latest version is " + latestVersion + "; downloading update");

                try {
                    // download new jar
                    URL website = new URL(latestVersionJson.get("download").getAsString());
                    ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                    FileOutputStream fos = new FileOutputStream("mods/" + latestFileName);
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                    // add the new jar to uninstaller params
                    Vanguard.UNINSTALLER_PARAMS.add(latestFileName);
                    Vanguard.UPDATED_MODS.add(modid);
                } catch (MalformedURLException e) {
                    Vanguard.logger.log(Level.ERROR, "Could not download update because of malformed URL: " + e.getMessage());
                } catch (IOException e) {
                    Vanguard.logger.log(Level.ERROR, "Could not download update because of I/O Error: " + e.getMessage());
                }
            }
        } else {
            Vanguard.logger.log(Level.WARN, "Update information could not be retrieved, auto-update will not be available");
        }
    }

}
