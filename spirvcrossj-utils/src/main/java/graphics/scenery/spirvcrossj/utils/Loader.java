package graphics.scenery.spirvcrossj.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Created by ulrik on 11/14/2016.
 * Rewritten by wwg on 25. Feb 2021 for use with Java 9+
 */
public enum Loader
{
  INSTANCE();

  private final String MODULE_LIBPATH = "graphics/scenery/spirvcrossj/natives";
  private boolean nativesReady = false;

  Loader()
  {
  }

  public Platform getPlatform()
  {
    final String os = System.getProperty("os.name").toLowerCase();

    if (os.contains("win")) {
      return Platform.WINDOWS;
    } else if (os.contains("linux")) {
      return Platform.LINUX;
    } else if (os.contains("mac")) {
      return Platform.MACOS;
    }

    return Platform.UNKNOWN;
  }

  private void deleteTmpFiles(Path tmpDir, ArrayList<Path> tmpPaths)
  {
    if (tmpDir != null) {
      for (Path path : tmpPaths) {
        if (path.toFile().exists()) {
          try {
            Files.delete(path);
          } catch (IOException x) {
            Logger.getGlobal()
                  .warning("Unable to delete temporary spirvcrossj library: " + x);
          }
        }
      }
      if (tmpDir.toFile().exists()) {
        try {
          Files.delete(tmpDir);
        } catch (Exception x) {
          Logger.getGlobal()
                .warning("Unable to delete temporary spirvcrossj directory: " + x);
        }
      }
    }
  }

  private Optional<Path> unpackLibs(ArrayList<String> libNames,
                                    Consumer<Path> tmpFileUser)
  {
    Path tmpDir;

    try {
      tmpDir = Files.createTempDirectory("spirvcrossj-natives");
    } catch (IOException x) {
      Logger.getGlobal()
            .severe("Unable to create temporary spirvcrossj directory: " + x);
      return Optional.empty();
    }

    ClassLoader loader = Loader.class.getClassLoader();

    for (String libName : libNames) {
      String resource = MODULE_LIBPATH + "/" + libName;
      URL url = loader.getResource(resource);

      Logger.getGlobal().fine("Resource url: " + url);

      if (url == null) {
        Logger.getGlobal()
              .severe("Module resource " + resource + " not available!");
      } else {
        try (BufferedInputStream bis = new BufferedInputStream(url.openConnection()
                                                                  .getInputStream())) {
          Path tmpFilePath = tmpDir.resolve(libName);

          Logger.getGlobal().fine("Temporary path: " + tmpFilePath);

          // Note: Overwriting an existing tmp file crashes the JVM!
          // But since loadNatives() returns without calling unpackLibs() if loaded ==
          // true, that doesn't happen anyway.
          try (BufferedOutputStream bos =
                 new BufferedOutputStream(new FileOutputStream(
                   tmpFilePath.toFile()))) {
            bos.write(bis.readAllBytes());
            bos.flush();
            // Success
            tmpFileUser.accept(tmpFilePath);
          } catch (IOException x) {
            Logger.getGlobal()
                  .severe("Error while writing " + libName + ": " + x);
            break;
          }
        } catch (IOException x) {
          Logger.getGlobal()
                .severe("Error while reading " + libName + ": " + x);
          break;
        }
      }
    }
    return Optional.ofNullable(tmpDir);
  }

  public boolean loadNatives()
  {
    if (nativesReady) {
      Logger.getGlobal().info("Native libs have already been loaded!");
      return true;
    }

    String libExtension;
    Platform os = getPlatform();

    switch (os) {
      case LINUX:
        libExtension = "so";
        break;
      case MACOS:
        libExtension = "jnilib";
        break;
      case WINDOWS:
        libExtension = "dll";
        break;
      default:
      case UNKNOWN:
        Logger.getGlobal().severe("Unknown os: " + os);
        return false;
    }

    ArrayList<String> libNames = new ArrayList<>()
    {{
      add("libspirvcrossj." + libExtension);
      add("libSPIRV-Tools-shared." + libExtension);
    }};
    ArrayList<Path> tmpFiles = new ArrayList<>();
    Optional<Path> optionalTmpDir = unpackLibs(libNames, path -> {
      try {
        System.load(path.toString());
        tmpFiles.add(path);
      } catch (Exception | UnsatisfiedLinkError x) {
        Logger.getGlobal()
              .severe("Could not load native lib '" + path + "': " + x);
      }
    });

    if (optionalTmpDir.isPresent()) {
      Path tmpDir = optionalTmpDir.get();
      deleteTmpFiles(tmpDir, tmpFiles);
      nativesReady = tmpFiles.size() == libNames.size();
    }

    return nativesReady;
  }
}
