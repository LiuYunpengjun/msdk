/*
 * (C) Copyright 2015-2018 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.msdk.id.sirius;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.msdk.MSDKException;

/**
 * <p>
 * Class NativeLibraryLoader
 * </p>
 * This class allows to dynamically load native libraries from .jar files (also works with IDE) with
 * updating java.library.path variable
 */
public class NativeLibraryLoader {

  private static final Logger logger = LoggerFactory.getLogger(NativeLibraryLoader.class);

  private static final String GLPK_RESOURCES_FOLDER = "glpk-4.60";

  private NativeLibraryLoader() {}

  /**
   * Returns path to requested resource
   * 
   * @param resource - file to find Path to
   * @return Path
   * @throws MSDKException if any
   */
  private static Path getResourcePath(String resource) throws MSDKException {
    final URL url = NativeLibraryLoader.class.getClassLoader().getResource(resource);
    try {
      return Paths.get(url.toURI()).toAbsolutePath();
    } catch (URISyntaxException e) {
      throw new MSDKException(e);
    }
  }



  /**
   * Public method for external usage, copies all files from `folder`
   * <p>
   * Loads libraries from `folder` in order specified by `libs` array
   * </p>
   *
   * The folder structure is strict folder -windows64 --lib1 --lib2 -windows32 -- lib -linux64 --
   * lib1 ... -linux32" -- lib1 ... -mac64 --lib1 ...
   *
   * @param folder - specify the name of the library to be loaded (example - glpk_4_60)
   * @param libs - array of exact names of libraries (without extensions)
   * @throws MSDKException if any
   * @throws IOException if any
   */
  public static void loadNativeGLPKLibriaries() throws MSDKException, IOException {

    logger.debug("Started loading GLPK libraries");

    final String javaLibPath = System.getProperty("java.library.path");
    if (javaLibPath == null)
      throw new MSDKException("Cannot read java.library.path system property");
    logger.debug("java.library.path = " + javaLibPath);

    final String javaLibPathSplit[] = javaLibPath.split(":");
    for (String libPath : javaLibPathSplit) {
      final File libPathFile = new File(libPath);
      if (libPathFile.exists() && libPathFile.canWrite()) {
        copyGLPKLibraryFiles(libPath);
        return;
      }
    }

    // Could not find a suitable library path
    throw new MSDKException(
        "The java.library.path system property does not contain any writable folders, cannot copy GLPK libraries (cjava.library.path = "
            + javaLibPath + ")");
  }

  public static void copyGLPKLibraryFiles(String libraryPath) throws MSDKException, IOException {

    final String arch = getArch();
    final String osname = getOsName();
    logger.debug("OS type = {} and OS arch = {}", osname, arch);

    // GLPK requires two libraries
    final String[] requiredLibraryFiles = new String[2];
    switch (osname) {
      case "win":
        requiredLibraryFiles[0] = "glpk_4_60.dll";
        requiredLibraryFiles[1] = "glpk_4_60_java.dll";
        break;
      case "linux":
        requiredLibraryFiles[0] = "libglpk.so";
        requiredLibraryFiles[1] = "libglpk_java.so";
        break;
      case "mac":
        requiredLibraryFiles[0] = "libglpk.dylib";
        requiredLibraryFiles[1] = "libglpk_java.dylib";
        break;
      default:
        throw new MSDKException("Unsupported OS (" + osname + "), cannot load GLPK libraries");
    }

    for (String libName : requiredLibraryFiles) {
      final String libResourcePath =
          GLPK_RESOURCES_FOLDER + File.separator + osname + arch + File.separator + libName;

      InputStream inFileStream =
          NativeLibraryLoader.class.getClassLoader().getResourceAsStream(libResourcePath);
      if (inFileStream == null)
        throw new MSDKException("Failed to open resource " + libResourcePath);

      final Path targetPath = Path.of(libraryPath, libName);

      // Copy the library file
      logger.debug("Copying library file " + libResourcePath + " to " + targetPath);
      Files.copy(inFileStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

      inFileStream.close();

    }

  }



  /**
   * <p>
   * Method returns architecture of the computer
   * </p>
   * 
   * @return formatted architecture
   * @throws MSDKException - if any
   */
  private static String getArch() throws MSDKException {
    String arch = System.getProperty("os.arch");
    if (arch == null)
      throw new MSDKException("Can not identify os.arch property");

    return arch.endsWith("64") ? "64" : "32";
  }

  /**
   * <p>
   * Method returns formatted OS name
   * </p>
   * 
   * @return OS name
   * @throws MSDKException - if any
   */
  public static String getOsName() throws MSDKException {
    String osname = System.getProperty("os.name");

    if (osname == null)
      return "unknown";

    if (osname.toLowerCase().contains("win")) {
      return "windows";
    }
    if (osname.toLowerCase().contains("linux")) {
      return "linux";
    }
    if (osname.toLowerCase().contains("mac")) {
      return "mac";
    }

    return "unknown";
  }


}
