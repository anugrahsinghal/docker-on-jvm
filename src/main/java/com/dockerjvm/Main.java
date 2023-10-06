package com.dockerjvm;

import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

// your_docker.sh run <image>       <command> <arg1> <arg2> ...
//                run ubuntu:latest /usr/local/bin/docker-explorer exit 1
//                run ubuntu:latest /usr/local/bin/docker-explorer ls /some_dir
//                run ubuntu:latest echo hey
public class Main {

  public interface LibC extends Library {
    // GO: <CLONE_NEWPID = 0x20000000 // New pid namespace>
    int CLONE_NEWPID_NAMESPACE_FLAG = 0x20000000;

    int chroot(String path); // Define the JNA interface for libc, which includes the chroot function

    int unshare(int flags);
  }


  public static void main(String[] args) {

    System.out.println(Arrays.toString(args));

    String imageName = args[1];

    String command = args[2];

    String[] commandWithArgs = Arrays.copyOfRange(args, 2, args.length);
    System.out.println("commandWithArgs - " + Arrays.toString(commandWithArgs));

//    if (!com.sun.jna.Platform.isLinux()) {
//      System.err.println("This example is only supported on Unix-like systems.");
//      System.exit(1);
//    }

    try {

      final File tempDir = Files.createTempDirectory("a").toAbsolutePath().toFile();

      String[] imageNameAndTag = getImageInfo(imageName);

      System.out.println(Arrays.toString(imageNameAndTag));

      DockerClient dockerClient = new DockerClient();
      dockerClient.imageName = imageNameAndTag[0];
      dockerClient.imageTag = imageNameAndTag[1];
      dockerClient.token = dockerClient.authToken();

      dockerClient.pullContainerImage(tempDir.getAbsolutePath());

      // copyCommandExecutableToTempDir(tempDir.getAbsolutePath(), command);

      createDevNull(tempDir.getAbsolutePath());

      // Load the libc library
      LibC libc = Native.load("c", LibC.class);

      // Call chroot to change the root directory
      int resultChroot = libc.chroot(tempDir.getPath());
      if (resultChroot != 0) {
        System.err.println("chroot failed. Error code: " + resultChroot);
        return;
      }
      System.out.println("chroot successful.");

      // Create a new namespace for the current process
      int resultUnshare = libc.unshare(LibC.CLONE_NEWPID_NAMESPACE_FLAG);
      if (resultUnshare != 0) {
        System.err.println("Process Namespace Isolation Failed. Error code: " + resultUnshare);
        return;
      }
      System.out.println("Process Namespace Isolation successful.");

      try {
        ProcessBuilder processBuilder = new ProcessBuilder(commandWithArgs);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = processBuilder.start();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
          System.err.printf("0Err: %d\n", exitCode);
          System.exit(1);
        }
      } catch (IOException | InterruptedException e) {
        System.err.printf("EErr: %s\n", e.getMessage());
        System.exit(1);
      }

    } catch (UnsatisfiedLinkError e) {
      System.err.println("Native library not found. Make sure you have JNA installed." + e);
      e.printStackTrace(System.err);
    } catch (IOException e) {
      System.err.println("Error creating temp directory." + e.getMessage());
      e.printStackTrace(System.err);
    } catch (Exception e) {
      System.err.println("Shucks!!! \uD83D\uDE31 " + e.getMessage());
      e.printStackTrace(System.err);
    }

  }

  public static void copyCommandExecutableToTempDir(String tempDirPath, String command) throws IOException {
    Path source = Paths.get(command);
    Path destination = Paths.get(tempDirPath, "usr/local/bin", source.getFileName().toString());

    Files.createDirectories(destination.getParent());

    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
  }

  public static void createDevNull(String tempDirPath) throws IOException {
    Path devPath = Paths.get(tempDirPath, "dev");
    Files.createDirectories(devPath);

    Path nullPath = devPath.resolve("null");
    Files.createFile(nullPath);
  }

  static String[] getImageInfo(String dockerImage) {
    String[] image = dockerImage.split(":");
    String imageName = "library/" + image[0];

    String imageTag;
    if (image.length != 2) {
      imageTag = "latest";
    } else {
      imageTag = image[1];
    }

    return new String[]{imageName, imageTag};
  }

}